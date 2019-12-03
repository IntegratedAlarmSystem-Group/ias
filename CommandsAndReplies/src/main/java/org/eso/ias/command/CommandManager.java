package org.eso.ias.command;

import org.eso.ias.kafkautils.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

/**
 * The CommandManager subscribes as a consumer of the command topic and a producer of the reply topic.
 * Its task is to receive the commands for the process where it runs, discarding the commands targeted to
 * other processes. Each command is forwarded to the listener for execution and the replies published
 * in the reply topic.
 *
 * Commands are executed in a dedicated thread in FIFO order.
 * Received commands are queued and discarded when the queue is full as we do not expect many commands.
 */
public class CommandManager implements SimpleStringConsumer.KafkaConsumerListener, Runnable {

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
    private final LinkedBlockingQueue<CommandMessage> cmdsToProcess = new LinkedBlockingQueue<>(LENGTH_OF_CMD_QUEUE);

    /**
     * The listener of commands that executes them
     */
    private CommandListener cmdListener=null;

    /**
     * The logger
     */
    private static final Logger logger = LoggerFactory.getLogger(CommandManager.class);

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
        repliesProducer = new SimpleStringProducer(servers, KafkaHelper.REPLY_TOPIC_NAME,id);
    }

    /**
     * Initialize producer and consumer but do not start getting events
     */
    private void initialize() {
        if (initialized) {
            logger.warn("Already initialized: initialization skipped");
            return;
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

    /** Start getting events from the command topic */
    public void start(CommandListener commandListener) {
        Objects.requireNonNull(commandListener,"The listener of commands can't be null");
        this.cmdListener = commandListener;
        try {
            cmdsConsumer.startGettingEvents(KafkaStringsConsumer.StreamPosition.END,this);
            logger.info("Started to get events from the command topic");
        } catch (KafkaUtilsException e) {
            logger.error("Error getting ready to receive commands: will shutdown",e);
            close();
        }
        cmdThread.start();
        logger.info("Commands processor thread started");
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
        cmdThread.interrupt();
        logger.debug("Shutting down");
        cmdsConsumer.tearDown();
        logger.info("Command consumer shut down");
        repliesProducer.tearDown();
        logger.info("Reply producer shut down");
        logger.info("Shut down");
    }

    /**
     * Push a reply in the reply topic
     *
     * @param reply
     */
    private void sendReply(ReplyMessage reply) {
        reply.setProcessedTStamp(System.currentTimeMillis());
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

    @Override
    public void stringEventReceived(String event) {
        // Discard commands when closed
        if (closed) {
            return;
        }
        if (event==null) {
            logger.warn("A NULL set stringshas been received from the command topic");
            return;
        }
        if (event.isEmpty()) {
            logger.debug("An empty command string has been received from the command topic");
            return;
        }

        CommandMessage cmd;
        try {
            cmd = cmdSerializer.valueOf(event);
        } catch (StringSerializerException e) {
            logger.error("Error deserializing JSON string to cmd: {}",event,e);
            return;
        }
        // Check if the command is for this procees by comparing the IDs
        if (cmd.getDestId()!=id) {
            logger.debug("Recipient of command {} is {}: command discarded",
                    cmd.getCommand().toString(),
                    cmd.getDestId());
            return;
        }
        // Push the command in the queue of commands to process
        if (!cmdsToProcess.offer(cmd)) {
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
     *
     */
    @Override
    public void run() {
        logger.debug("Commands processro thread started");
        CommandMessage cmd;
        ReplyMessage reply;
        while (!closed) {
             try {
                 cmd = cmdsToProcess.poll(500, TimeUnit.MILLISECONDS);
             } catch (InterruptedException ie) {
                 continue;
             }
             try {
                 reply = cmdListener.newCommand(cmd);
             } catch (Exception e) {
                 // Exception executing the comamnd: send the reply
                 sendReply(
                         CommandExitStatus.ERROR,
                         cmd.getSenderFullRunningId(),
                         cmd.getId(),
                         cmd.getCommand(),
                         System.currentTimeMillis(),
                         null);
             }
        }
    }
}
