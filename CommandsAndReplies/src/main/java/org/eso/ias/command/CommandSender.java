package org.eso.ias.command;

import org.eso.ias.kafkautils.KafkaHelper;
import org.eso.ias.kafkautils.KafkaStringsConsumer;
import org.eso.ias.kafkautils.SimpleStringConsumer;
import org.eso.ias.kafkautils.SimpleStringProducer;
import org.eso.ias.types.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Objects of this class sends command and optionally wait for the reply through the kafka topics.
 *
 */
public class CommandSender implements SimpleStringConsumer.KafkaConsumerListener {

    /** The logger */
    private static final Logger logger = LoggerFactory.getLogger(CommandSender.class);

    /**
     * The full running id of the sender
     */
    public final String senderFullRunningId;

    /**
     * The id of the sender
     */
    public final String senderId;

    /**
     * The URL to connect to the server
     */
    public final String brokersURL;

    /**
     * The producer of command: the test sends through this producer the command to be processed
     * by the {@link org.eso.ias.command.CommandManager}
     */
    private SimpleStringProducer cmdProducer;

    /**
     * The consumer of replies: the test get from this consumer the replies
     * produced by the {@link org.eso.ias.command.CommandManager}
     */
    private SimpleStringConsumer replyConsumer;

    /** The serializer of replies */
    private static final ReplyStringSerializer replySerializer = new ReplyJsonSerializer();

    /** The serializer of commands */
    private static final CommandStringSerializer cmdSerializer = new CommandJsonSerializer();

    /**
     * Signal if the object has been initialized
     */
    private final AtomicBoolean initialized = new AtomicBoolean(false);

    /**
     * Signal if the object has been closed
     */
    private final AtomicBoolean closed = new AtomicBoolean(false);

    /**
     * The IDs of the commands
     *
     * The id is used for the cmd-reply pattern
     */
    private final AtomicLong cmdId = new AtomicLong(0L);

    /**
     * true is a Request-Replay is in progress
     */
    private final AtomicBoolean requestReplyInProgress = new AtomicBoolean(false);

    /**
     * The lock to wait for the reply
     */
    private final AtomicReference<CountDownLatch> requestReplyLock = new AtomicReference<>();

    /**
     * The id to wait for in the reply
     *
     * This is the ID of the command that is forwarded in the reply
     */
    private final AtomicLong idToWait = new AtomicLong();


    /**
     * Constructor
     *
     * @param senderFullRuningId The full runing id of the sender
     * @param senderId The id of the sender
     * @param brokers URL of kafka brokers
     */
    public CommandSender(String senderFullRuningId, String senderId, String brokers) {
        Objects.requireNonNull(senderFullRuningId);
        Objects.requireNonNull(brokers);

        if (senderFullRuningId==null || senderFullRuningId.isEmpty()) {
            throw new IllegalArgumentException("Invalid null/empty full running ID of the sender");
        }
        this.senderFullRunningId=senderFullRuningId;
        if (senderId==null || senderId.isEmpty()) {
            throw new IllegalArgumentException("Invalid null/empty ID of the sender");
        }
        this.senderId=senderId;
        if (brokers==null || brokers.isEmpty()) {
            throw new IllegalArgumentException("Invalid null/empty broker");
        }
        this.brokersURL = brokers;
    }

    /**
     * Constructor
     *
     * @param identifier The identifier of the sender
     * @param brokers URL of kafka brokers
     */
    public CommandSender(Identifier identifier, String brokers) {
        this(identifier.fullRunningID(),identifier.id(),brokers);
    }

    /**
     *
     * @throws Exception
     */
    public void setUp() throws Exception {
        boolean alreadyInitialized = initialized.getAndSet(true);
        if (alreadyInitialized) {
            logger.warn("Already initialized: skipping initialization");
            return;
        }
        logger.debug("Setting up cmd producer");
        replyConsumer = new SimpleStringConsumer(
                KafkaHelper.DEFAULT_BOOTSTRAP_BROKERS,
                KafkaHelper.REPLY_TOPIC_NAME,
                senderId);
        logger.debug("Setting up replies consumer");
        cmdProducer = new SimpleStringProducer(
                KafkaHelper.DEFAULT_BOOTSTRAP_BROKERS,
                KafkaHelper.CMD_TOPIC_NAME,
                senderId+"-CMD");

        logger.debug("Initializing the consumer of replies...");
        replyConsumer.setUp();
        logger.debug("Initialize the producer of commands...");
        cmdProducer.setUp();
        logger.info("Static producer and consumer built and set up");
        logger.debug("Activating the reception of replies...");
        replyConsumer.startGettingEvents(KafkaStringsConsumer.StreamPosition.END, this);
        logger.info("Initialized");
    }

    /**
     * Close the object and release the resources
     *
     * @throws Exception
     */
    public void close() throws Exception {
        boolean alreadyClosed = closed.getAndSet(true);
        if (alreadyClosed) {
            logger.warn("Already closed!");
            return;
        }
        logger.debug("Closing...");
        if (replyConsumer!=null) {
            logger.debug("Closing the consumer of replies");
            replyConsumer.tearDown();
        }
        if (cmdProducer!=null) {
            logger.debug("Closing the producer of commands");
            cmdProducer.tearDown();
        }
        logger.info("Closed");
    }

    /**
     * The listener that gets the replies
     *
     * @param event The string received in the topic
     */
    @Override
    public void stringEventReceived(String event) {
        if (event==null || event.isEmpty()) {
            logger.warn("Got an empty reply");
            return;
        }
        logger.debug("Processing JSON reply [{}]",event);
        ReplyMessage reply;
        try {
            reply = replySerializer.valueOf(event);
        } catch (Exception e) {
            logger.error("Error.parsing the JSON string {} into a reply",event,e);
            return;
        }
        if (!requestReplyInProgress.get() || reply.getDestFullRunningId().equals(senderFullRunningId)) {
            // This process is not the sender of this command
            return;
        }

        // A request/reply is in progress
        if (reply.getId()==idToWait.get()) {
            logger.debug("Reply of command {} received from {}",reply.getId(),reply.getSenderFullRunningId());
            requestReplyInProgress.set(false);
            requestReplyLock.get().countDown();
        } else {
            logger.debug("Received reply {} but waiting for reply {}",reply.getId(),idToWait.get());
        }
    }

    /**
     * Send a command synchronously
     *
     * This method sends the commands and holds until receives the replay or a timeout
     * elapses.
     *
     * Send-reply is not available for broadcast
     *
     * @param destId The id of the destination of the command (cannot be BROADCAST)
     * @param command The command to send
     * @param params The optional parameters of the command
     * @param properties The optional properties of the command
     * @param timeout the time interval for the timeout
     * @param timeUnit the time unit for the timeout
     * @return true if the reply has been received and false if the waiting time elapsed before getting the reply
     * @throws InterruptedException
     */
    public synchronized boolean sendSync(
            String destId,
            CommandType command,
            List<String> params,
            Map<String, String> properties,
            long timeout,
            TimeUnit timeUnit) throws Exception {
        if (destId.equals(CommandMessage.BROADCAST_ADDRESS)) {
            throw new IllegalArgumentException("BROADCAT cannot be used for send-reply");
        }

        long id = cmdId.incrementAndGet();
        CommandMessage cmd = new CommandMessage(
                senderFullRunningId,
                destId,
                command,
                id,
                params,
                System.currentTimeMillis(),
                properties
        );
        idToWait.set(id);
        requestReplyLock.set(new CountDownLatch(1));
        requestReplyInProgress.set(true);

        if (!closed.get()) {
            cmdProducer.push(cmdSerializer.iasCmdToString(cmd), null, destId);
            logger.info("Command {} sent to {} with id {}. Waiting for the reply...", command,destId,id);
        } else {
            return false;
        }


        boolean replyReceived;
        try {
           replyReceived = requestReplyLock.get().await(timeout,timeUnit);
        } catch (InterruptedException ie) {
            requestReplyInProgress.set(false);
            throw ie;
        }
        if (!replyReceived) {
            // timeout
            logger.debug("Timeout while waiting for the reply of cmd {} from {} ", id,destId);
            requestReplyInProgress.set(false);
        } else {
            logger.info("Reply of command {} received from {}",id,destId);
        }
        return replyReceived;
    }

    /**
     * Send a command asynchronously
     *
     * This method sends the commands but do not wait for the reception of the reply
     *
     * @param destId The id of the destination of the command (cannot be BROADCAST)
     * @param command The command to send
     * @param params The optional parameters of the command
     * @param properties The optional properties of the command
     * @throws Exception
     */
    public synchronized void sendAsync(
            String destId,
            CommandType command,
            List<String> params,
            Map<String, String> properties,
            long timeout,
            TimeUnit timeUnit) throws Exception {


        CommandMessage cmd = new CommandMessage(
                senderFullRunningId,
                destId,
                command,
                cmdId.incrementAndGet(),
                params,
                System.currentTimeMillis(),
                properties
        );

        if (!closed.get()) {
            cmdProducer.push(cmdSerializer.iasCmdToString(cmd), null, destId);
            logger.info("Command {} sent to {}", command,destId);
        }

    }
}
