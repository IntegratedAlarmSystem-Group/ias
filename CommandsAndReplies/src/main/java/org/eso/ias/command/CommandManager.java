package org.eso.ias.command;

import org.eso.ias.kafkautils.*;
import org.eso.ias.types.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.management.ManagementFactory;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Vector;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

/**
 * The CommandManager subscribes as a consumer of the command topic and a producer of the reply topic.
 * Its task is to receive the commands for the process where it runs, discarding the commands targeted to
 * other processes.
 *
 * Normally commands are forwarded to the listener for execution and the replies published
 * in the reply topic. SHUTDOWN and RESTART must be executed by the {@link CommandManager} because a
 * reply must be sent before shutting down.
 *
 * Commands are executed in a dedicated thread in FIFO order.
 * Received commands are queued and discarded when the queue is full as we do not expect many commands.
 */
public class CommandManager implements SimpleStringConsumer.KafkaConsumerListener, Runnable {

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
    public static final int LENGTH_OF_CMD_QUEUE = 16;

    /** The consumer of commands from the kafka topic */
    private  SimpleStringConsumer cmdsConsumer;

    /** The produce of replies in the kafka topic */
    private SimpleStringProducer repliesProducer;

    /** Signal if the CommandManager has been initialized */
    private volatile boolean initialized = false;

    /** Signal if the CommandManager has been started i.e. receiving commands from the topic */
    private volatile boolean started = false;

    /** Signal if the CommandManager has been closed */
    private volatile boolean closed = false;

    /** The full running ID of the process where the object of this class runs */
    private final String fullRunningId;

    /** The ID of the process where the object of this class runs */
    private final String id;

    /** Thread factory */
    private final ThreadFactory threadFactory = new CmdReplyThreadFactory();

    /** The serializer of Commands */
    private final CommandStringSerializer cmdSerializer = new CommandJsonSerializer();

    /** The serializer of replies */
    private final ReplyStringSerializer replySerializer = new ReplyJsonSerializer();

    /**
     * The thread that gets comamnds from the queue {@link #cmdsToProcess} and sends them to the listener
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
     * @param fullRunningId the full running Id of the process
     * @param id the id of the process
     * @param servers The servers to connect to
     */
    public CommandManager(String fullRunningId, String id, String servers) {
        if (fullRunningId==null || fullRunningId.isEmpty()) {
            throw new IllegalArgumentException("Invalid null/empty full running ID");
        }
        this.fullRunningId=fullRunningId;
        if (id==null || id.isEmpty()) {
            throw new IllegalArgumentException("Invalid null/empty ID");
        }
        this.id=id;
        if (servers==null || servers.isEmpty()) {
            throw new IllegalArgumentException("Invalid null/empty kafka servers");
        }
        cmdsConsumer = new SimpleStringConsumer(servers, KafkaHelper.CMD_TOPIC_NAME,id);
        repliesProducer = new SimpleStringProducer(servers, KafkaHelper.REPLY_TOPIC_NAME,id+"-REPLY");
    }

    /**
     * Constructor
     *
     * Commodity constructor with the Identifier
     *
     * @param identifier The identifier of the tool where the CommandManager runs
     * @param servers The address of the servers to connect to
     */
    public CommandManager(Identifier identifier, String servers) {
        this(identifier.fullRunningID(), identifier.id(),servers);
    }

                          /**
     * Initialize producer and consumer but do not start getting events
     */
    private void initialize() {
        if (initialized) {
            logger.warn("Already initialized: initialization skipped");
            return;
        } else {
            initialized=true;
        }
        logger.debug("Initializing");
        // Connect to the CMD topic
        logger.debug("Connecting consumer to the command topic");
        cmdsConsumer.setUp();
        logger.info("Command consumer connected");
        logger.debug("Connecting the producer to the reply topic");
        repliesProducer.setUp();
        logger.info("Initialized");
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
        if (started) {
            logger.warn("Already started: skipped");
            return;
        }

        Objects.requireNonNull(commandListener,"The listener of commands can't be null");
        Objects.requireNonNull(closeable,"The method to release the resources can't be null");
        started=true;
        this.cmdListener = commandListener;
        this.closeable=closeable;
        initialize();
        cmdThread.start();
        logger.info("Commands processor thread started");
        cmdsConsumer.startGettingEvents(KafkaStringsConsumer.StreamPosition.END,this);
        logger.info("Started to get events from the command topic");
    }

    /**
     * Close the producer and the consumer and release all the allocated resources.
     */
    public void close() {
        if (closed) {
            logger.warn("Already closed");
            return;
        }
        closed=true;
        logger.debug("Shutting down");
        cmdsConsumer.tearDown();
        logger.info("Command consumer shut down");
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
            repliesProducer.push(jsonStr,null,reply.getCommand().toString());
        } catch (KafkaUtilsException e) {
            logger.error("Error pushing the reply [{}] in the topic the reply {}: reply lost!",jsonStr,e);
        }
    }

    /**
     * Push a reply in the reply topic.
     *
     * Build the reply and delegate to {@link #sendReply(ReplyMessage)}
     *
     * @param exitStatus The exit status of the command
     * @param destFullRunningId The full running ID of the receiver of the command
     * @param id The unique identifier (in the context of the sender) of the command
     * @param cmd The command just executed
     * @param receptionTStamp The point in time when the command has been received from the kafka topic
     * @param props Additional properties, if any
     */
    private void sendReply(
            CommandExitStatus exitStatus,
            String destFullRunningId,
            long id,
            CommandType cmd,
            long receptionTStamp,
            Map<String,String> props) {
        ReplyMessage reply = new ReplyMessage(
                fullRunningId,
                destFullRunningId,
                id,
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
        // Discard commands when closed
        if (closed) {
            return;
        }
        if (event==null) {
            logger.warn("A NULL set strings has been received from the command topic");
            return;
        }
        if (event.isEmpty()) {
            logger.warn("An empty command string has been received from the command topic");
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
        logger.debug("Pushing the command {} in the queue for execution",cmd.toString());
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
    private final void restartProcess() throws Exception {
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
        CommandListener.CmdExecutionResult cmdResult = null;
        while (!closed) {
            try {
                tStampedCmd = cmdsToProcess.poll(500, TimeUnit.MILLISECONDS);
            } catch (InterruptedException ie) {
                continue;
            }
            if (tStampedCmd == null) {
                // Timeout
                continue;
            }
            logger.debug("Command {} received from the queue", tStampedCmd.command);
            try {
                logger.debug("Sending the command {} to the listener for execution",tStampedCmd.command.getCommand());
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
                close();
                if (!Objects.isNull(closeable)) {
                    logger.debug("Freeing the resources...");
                    try {
                        closeable.close();
                    } catch (Exception e) {
                        logger.error("Error caught while freeing the resources",e);
                    }
                }
                System.exit(0);
            }

            // If the command was RESTART, restart the process and exit
            if (cmdResult.mustRestart) {
                logger.info("Restarting process as requested by {}", tStampedCmd.command.getSenderFullRunningId());
                close();
                if (!Objects.isNull(closeable)) {
                    logger.debug("Freeing the resources...");
                    try {
                        closeable.close();
                    } catch (Exception e) {
                        logger.error("Error caught while freeing the resources",e);
                    }
                }
                try {
                    restartProcess();
                } catch (Exception e) { }
                System.exit(0);

            }
            logger.info("Commands processor thread terminated");
        }
    }
}
