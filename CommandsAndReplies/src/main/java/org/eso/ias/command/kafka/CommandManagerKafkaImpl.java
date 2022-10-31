package org.eso.ias.command.kafka;

import org.eso.ias.command.*;
import org.eso.ias.kafkautils.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.management.ManagementFactory;
import java.util.*;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * The {@code CommandManagerKafkaImpl} is the command executors that subscribes as a consumer of the kafka command topic
 * to get commands and as producer of the kafka reply topic to publish replies
 * Its task is to receive the commands fon behalf of the process where it runs, discarding the commands targeted to
 * other processes.
 *
 * Commands are forwarded to the listener for execution and the replies published
 * in the reply topic. SHUTDOWN and RESTART must be executed by the {@link CommandManager } because a
 * reply must be sent before shutting down however the lister is invoked to run customized code.
 *
 * Commands are executed in a dedicated thread in FIFO order.
 * Received commands are queued and discarded when the queue is full as we do not expect many commands.
 */
public class CommandManagerKafkaImpl
        extends CommandManager
        implements SimpleStringConsumer.KafkaConsumerListener, Runnable {

    /**
     * Objects of this class are pushed in the queue when a new message arrives: the purpose
     * is to save the reception timestamp that will be part of the reply
     *
     */
    private static final class TimestampedCommand {

        /** The point in time when the command has been received */
        public final long receptionTStamp;

        /** The command to execute */
        public final CommandMessage command;

        public TimestampedCommand(long receptionTStamp,CommandMessage command) {
            this.receptionTStamp=receptionTStamp;
            this.command=command;
        }
    }

    /**
     * Max number of commands in the queue waiting to be processed.
     *
     * Commands arriving when the queue is full are rejected
     */
    public static final int LENGTH_OF_CMD_QUEUE = 128;

    /** The consumer of commands from the kafka topic */
    private  SimpleStringConsumer cmdsConsumer;

    /** The produce of replies in the kafka topic */
    private SimpleStringProducer repliesProducer;

    /** Signal if the CommandManager has been initialized */
    private volatile AtomicBoolean initialized = new AtomicBoolean(false);

    /** Signal if the CommandManager has been started i.e. receiving commands from the topic */
    private volatile AtomicBoolean started = new AtomicBoolean(false);

    /** Signal if the CommandManager has been closed */
    private volatile AtomicBoolean closed = new AtomicBoolean(false);

    /** Thread factory */
    private final ThreadFactory threadFactory = new CmdReplyThreadFactory();

    /** The serializer of Commands */
    private final CommandStringSerializer cmdSerializer = new CommandJsonSerializer();

    /** The serializer of replies */
    private final ReplyStringSerializer replySerializer = new ReplyJsonSerializer();

    /**
     * The thread that gets commands from the queue {@link #cmdsToProcess} and sends them to the listener
     * that will execute them
     *
     * @see #run()
     */
    private final Thread cmdThread = threadFactory.newThread(this);

    /**
     * The list of commands to process
     */
    private final LinkedBlockingQueue<TimestampedCommand> cmdsToProcess = new LinkedBlockingQueue<>(LENGTH_OF_CMD_QUEUE);

    /**
     * The listener of commands that executes them
     */
    private CommandListener cmdListener=null;

    /**
     * The logger
     */
    private static final Logger logger = LoggerFactory.getLogger(CommandManager.class);

    /**
     * The closeable class that loffers a close(0 method to free the
     * resources before shutdown
     */
    private AutoCloseable closeable;

    /**
     * Constructor
     *
     * @param id the id of the process
     * @param servers The servers to connect to
     * @param repliesProducer The producer of replies in the rpely topic
     */
    public CommandManagerKafkaImpl(String id, String servers, SimpleStringProducer repliesProducer) {
        super(id);
        if (servers==null || servers.isEmpty()) {
            throw new IllegalArgumentException("Invalid null/empty kafka servers");
        }
        cmdsConsumer = new SimpleStringConsumer(servers, KafkaHelper.CMD_TOPIC_NAME,id);
        Objects.requireNonNull(repliesProducer,"The producer of replies can't be null");
        this.repliesProducer = repliesProducer;
    }

    /**
     * Initialize producer and consumer but do not start getting events
     */
    private void initialize() {
        boolean alreadyInited = initialized.getAndSet(true);
        if (alreadyInited) {
            logger.warn("Already initialized: initialization skipped");
            return;
        }
        logger.debug("Initializing the cmd producer and the reply consumer");
        // Connect to the CMD topic
        logger.debug("Connecting the consumer to the command topic");

        Properties props = new Properties();
        props.setProperty("group.id","GroupID-"+System.currentTimeMillis());
        cmdsConsumer.setUp(props);
        logger.info("Command consumer connected");
        logger.debug("Connecting the producer to the reply topic");
        repliesProducer.setUp();
        logger.info("Reply producer connected");
    }

    /**
     * Start getting events from the command topic and send them to the passed listener.
     * The listener is usually an instance of {@link DefaultCommandExecutor} or an object
     * extending {@link DefaultCommandExecutor} to customize the commands
     *
     * @param  commandListener The listener of commands that execute all the commands
     * @param closeable The closeable class to free the resources while exiting/restating
     */
    public void start(CommandListener commandListener, AutoCloseable closeable) throws KafkaUtilsException {
        boolean alreadyStarted = started.getAndSet(true);
        if (alreadyStarted) {
            logger.warn("Already started: skipped");
            return;
        }

        Objects.requireNonNull(commandListener,"The listener of commands can't be null");
        Objects.requireNonNull(closeable,"The method to release the resources can't be null");
        this.cmdListener = commandListener;
        this.closeable=closeable;
        initialize();
        logger.debug("Starting the command processor thread...");
        cmdThread.start();
        logger.debug("Start getting commands from the topic");
        cmdsConsumer.startGettingStrings(KafkaStringsConsumer.StreamPosition.END,this);
        logger.info("Started to get events from the command topic");
    }

    /**
     * Close the producer and the consumer and release all the allocated resources.
     */
    @Override
    public void close() {
        boolean alreadyClosed = closed.getAndSet(true);
        if (alreadyClosed) {
            logger.warn("Already closed");
            return;
        }
        logger.debug("Shutting down");
        cmdsConsumer.tearDown();
        logger.info("Command consumer shut down");
        repliesProducer.flush();
        repliesProducer.tearDown();
        logger.info("Reply producer shut down");
        cmdThread.interrupt();
        logger.info("Is shut down");
    }

    /**
     * Push a reply in the reply topic
     *
     * @param reply the reply to push in the reply topic
     */
    private void sendReply(ReplyMessage reply) {
        reply.setProcessedTStampMillis(System.currentTimeMillis());
        String jsonStr;
        try {
            jsonStr = replySerializer.iasReplyToString(reply);
        } catch (StringSerializerException e) {
            logger.error("Error serializing the reply {}: reply lost!",reply.toString(),e);
            return;
        }
        try {
            repliesProducer.push(jsonStr,KafkaHelper.REPLY_TOPIC_NAME,null,reply.getCommand().toString());
        } catch (KafkaUtilsException e) {
            logger.error("Error pushing the reply [{}] in the topic the reply {}: reply lost!",jsonStr,e);
            return;
        }
        repliesProducer.flush();
        logger.debug("Reply {} of command {} sent to {} with id {}",
                reply.getExitStatus(),
                reply.getCommand(),
                reply.getDestFullRunningId(),
                reply.getId());
    }

    /**
     * Push a reply in the reply topic.
     *
     * Build the reply and delegate to {@link #sendReply(ReplyMessage)}
     *
     * @param exitStatus The exit status of the command
     * @param destId The ID of the receiver of the command
     * @param cmdUID The unique identifier (in the context of the sender) of the command
     * @param cmd The command just executed
     * @param receptionTStamp The point in time when the command has been received from the kafka topic
     * @param props Additional properties, if any
     */
    private void sendReply(
            CommandExitStatus exitStatus,
            String destId,
            long cmdUID,
            CommandType cmd,
            long receptionTStamp,
            Map<String,String> props) {
        ReplyMessage reply = new ReplyMessage(
                this.id,
                destId,
                cmdUID,
                cmd,
                exitStatus,
                receptionTStamp,
                System.currentTimeMillis(),
                props);
        sendReply(reply);
    }

    /**
     * Get the JSON strings representing commands.
     *
     * The string is converted into a {@link CommandMessage} and pushed in the {@link #cmdsToProcess}
     * queue only if the destination of the command has the same ID of the manager.
     * Another thread, ({@link #run()}, is in charge of poling commands from the queue and
     * send them to the lister for execution.
     *
     * @param event The string received in the topic
     */
    @Override
    public void stringEventReceived(String event) {
        if (event==null) {
            logger.warn("A NULL set strings has been received from the command topic");
            return;
        }
        if (event.isEmpty()) {
            logger.warn("An empty command string has been received from the command topic");
            return;
        }
        if (closed.get()) {
            logger.debug("Command manager closed: string [{}] rejected",event);
            return;
        }

        logger.debug("Command json string [{}] received",event);
        long receptionTStamp = System.currentTimeMillis();

        CommandMessage cmd;
        try {
            cmd = cmdSerializer.valueOf(event);
        } catch (StringSerializerException e) {
            logger.error("Error deserializing JSON string to cmd: {}",event,e);
            return;
        }
        // Check if the command is for this procees by comparing the IDs
        if (!cmd.getDestId().equals(id) && !cmd.getDestId().equals(CommandMessage.BROADCAST_ADDRESS)) {
            logger.debug("Recipient of command {} is {}: command discarded because is not for us",
                    cmd.getCommand().toString(),
                    cmd.getDestId());
            return;
        }
        // Push the command in the queue of commands to process
        logger.debug("Pushing the command {} with id {} in the queue for execution",cmd.getCommand(), cmd.getId());
        if (!cmdsToProcess.offer(new TimestampedCommand(receptionTStamp, cmd))) {
            logger.error("Queue of command full: command {} with ID {} from {} rejected",
                    cmd.getCommand().toString(),
                    cmd.getId(),
                    cmd.getSenderFullRunningId());
            sendReply(
                    CommandExitStatus.REJECTED,
                    cmd.getSenderFullRunningId(),
                    cmd.getId(),
                    cmd.getCommand(),
                    System.currentTimeMillis(),
                    null);
        }
    }

    /**
     * Restart the process i.e. run the RESTART command
     *
     * @throws Exception
     */
    private void restartProcess() throws Exception {
        logger.debug("Restarting...");
        for (String jvmArg : ManagementFactory.getRuntimeMXBean().getInputArguments()) {
            System.out.println("jvmArgs "+jvmArg);
        }
        String classPath = ManagementFactory.getRuntimeMXBean().getClassPath();
        System.out.println("\nclassPath "+classPath);

        String sunJavaCmd = System.getProperty("sun.java.command");
        System.out.println("\nsun.java.command "+sunJavaCmd);
        String pidAndHost =  ManagementFactory.getRuntimeMXBean().getName();
        System.out.println("\npid@host "+pidAndHost);

        // Build the command line
        List<String> cmdToRun = new Vector<>();
        cmdToRun.add("java");

        for (String jvmArg : ManagementFactory.getRuntimeMXBean().getInputArguments()) {
            logger.debug("Adding JVM arg {}",jvmArg);
            cmdToRun.add(jvmArg.trim());
        }

        // Adds the classpath
        String cleanedClassPath = classPath.replace("\"","").trim();
        if (!cleanedClassPath.isEmpty()) {
            logger.debug("Adding classpath [{}]",cleanedClassPath);
            cmdToRun.add("-cp");
            cmdToRun.add(cleanedClassPath);
        }

        logger.debug("Adding command [{}]",sunJavaCmd);
        String[] sunCommandParts = sunJavaCmd.split(" ");
        for (String part: sunCommandParts) {
            String cleaned = part.trim();
            if (!cleaned.isEmpty()) {
                logger.debug("Adding part {} of command",cleaned);
                cmdToRun.add(cleaned);
            }
        }

        if (logger.isDebugEnabled()) {
            StringBuilder str = new StringBuilder();
            for (String s:  cmdToRun) {
                str.append(s);
                str.append(' ');
            }
            logger.debug("Going to run [{}]",str.toString());
        }

        ProcessBuilder procBuilder = new ProcessBuilder(cmdToRun);
        procBuilder.inheritIO();
        Process p = procBuilder.start();
        logger.info("New process launched. Exiting");
        logger.debug("Process is alive {}",p.isAlive());
    }

    /**
     * Fetch commands from the queue and sends them to the lister for execution.
     *
     * The return code and properties provided by the listener are packed into the reply before being
     * sent to the reply topic.
     *
     * This method catches the case fo an error from the lister implementation of the command and
     * pushes a reply with an ERROR return code in the reply topic.
     *
     * <B>Note</B>: the only commands that are directly executed by this method are SHUTDOWN
     *              and RESTART because in these cases the reply must be sent <EM>before</EM>
     *              before shutting down the process.
     */
    @Override
    public void run() {
        logger.debug("Commands processor thread started");
        TimestampedCommand tStampedCmd;
        CommandListener.CmdExecutionResult cmdResult;
        while (!closed.get()) {
            try {
                tStampedCmd = cmdsToProcess.poll(500, TimeUnit.MILLISECONDS);
            } catch (InterruptedException ie) {
                continue;
            }
            if (tStampedCmd == null) {
                // Timeout
                continue;
            }
            logger.debug("Command {} with id {} retrieved from the queue",
                    tStampedCmd.command.getCommand(),
                    tStampedCmd.command.getId());
            try {
                logger.debug("Sending the command {} to the listener for execution...",tStampedCmd.command.getCommand());
                cmdResult = cmdListener.newCommand(tStampedCmd.command);
                logger.debug("Command executed");
            } catch (Exception e) {
                logger.error("Exception [{}] got executing the command: will reply with ERROR", e.getMessage(), e);
                // Exception executing the command: send the reply

                sendReply(
                        CommandExitStatus.ERROR,
                        tStampedCmd.command.getSenderFullRunningId(),
                        tStampedCmd.command.getId(),
                        tStampedCmd.command.getCommand(),
                        tStampedCmd.receptionTStamp,
                        null);
                continue;
            }

            if (cmdResult == null) {
                logger.error("The executor of the command returned null");
                sendReply(
                        CommandExitStatus.ERROR,
                        tStampedCmd.command.getSenderFullRunningId(),
                        tStampedCmd.command.getId(),
                        tStampedCmd.command.getCommand(),
                        tStampedCmd.receptionTStamp,
                        null);
                continue;
            }

            logger.debug("Sending reply {} to {} with ID {}",
                    cmdResult.status,
                    tStampedCmd.command.getSenderFullRunningId(),
                    tStampedCmd.command.getId());

            Map<String, String> props = null;
            if (cmdResult.properties.isPresent()) {
                props = cmdResult.properties.get();
            }
            sendReply(cmdResult.status,
                    tStampedCmd.command.getSenderFullRunningId(),
                    tStampedCmd.command.getId(),
                    tStampedCmd.command.getCommand(),
                    tStampedCmd.receptionTStamp,
                    props);

            // If the command was a SHUTDOWN, exit the JVM
            if (cmdResult.mustShutdown) {
                logger.info("Exit due to a SHUTDOWN command requested by {}", tStampedCmd.command.getSenderFullRunningId());
                if (!Objects.isNull(closeable)) {
                    logger.debug("Freeing the resources...");
                    try {
                        closeable.close();
                    } catch (Exception e) {
                        logger.error("Error caught while freeing the resources",e);
                    }
                }
                close();
                System.exit(0);
            }

            // If the command was RESTART, restart the process and exit
            if (cmdResult.mustRestart) {
                logger.info("Restarting process as requested by {}", tStampedCmd.command.getSenderFullRunningId());
                if (!Objects.isNull(closeable)) {
                    logger.debug("Freeing the resources...");
                    try {
                        closeable.close();
                    } catch (Exception e) {
                        logger.error("Error caught while freeing the resources",e);
                    }
                }

                // Close producer and consumer before restarting
                close();

                try {
                    restartProcess();
                } catch (Exception e) {
                    logger.error("Exception restarting the process",e);
                }
                System.exit(0);
            }
        }
        logger.info("Commands processor thread terminated");
    }


}
