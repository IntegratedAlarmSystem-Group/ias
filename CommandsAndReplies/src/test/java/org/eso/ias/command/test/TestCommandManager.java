package org.eso.ias.command.test;

import org.eso.ias.command.*;
import org.eso.ias.kafkautils.KafkaHelper;
import org.eso.ias.kafkautils.KafkaStringsConsumer;
import org.eso.ias.kafkautils.SimpleStringConsumer;
import org.eso.ias.kafkautils.SimpleStringProducer;
import org.junit.jupiter.api.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Vector;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Test the {@link org.eso.ias.command.CommandManager}.
 *
 *
 * The test sends command to the manager and check
 * - the reception of the reply
 * - that the execution of commands is properly forwarded to the listener
 *   of commands
 *
 * Objects of this class are the listener of the commands so we expect that every time a command is submitted
 * the listener {@link #newCommand(CommandMessage)} runs and the reply is sent by the manager
 * This means that objects of this class are senders of commands and replies. But they are alsio the recipt of the
 * commands
 *
 */
public class TestCommandManager implements CommandListener, SimpleStringConsumer.KafkaConsumerListener {

    /** The logger */
    private static final Logger logger = LoggerFactory.getLogger(TestCommandManager.class);

    /** The full running ID of the command executor i.e {@link CommandManager} to test */
    public static final String commandManagerFullRunningId = "commandManagerFullRunningId";

    /** The full running ID of the sender of commands i.e. this test */
    public static final String commandSenderFullRunningId = "commandSenderFullRunningId";

    /**
     * The id of the sender of commands i.e. the test
     */
    public final String senderId ="CmdSenderID";

    /**
     * The id of the receiver of commands and sender of replies
     * i.e. the CommandManager
     */
    public static final String commandManagerId ="cmdManagerId";

    /**
     * The producer of command: the test sends through this producer the command to be processed
     * by the {@link org.eso.ias.command.CommandManager}
     */
    private static SimpleStringProducer cmdProducer;

    /**
     * The consumer of replies: the test get from this consumer the replies
     * produced by the {@link org.eso.ias.command.CommandManager}
     */
    private static SimpleStringConsumer replyConsumer;

    /** The serializer of replies */
    private static final ReplyStringSerializer replySerializer = new ReplyJsonSerializer();

    /** The serializer of commands */
    private static final CommandStringSerializer cmdSerializer = new CommandJsonSerializer();

    /**
     * Signal that the first test will be executed soon
     */
    private static volatile boolean beforeFirstTest = false;

    /** The {@link CommandManager} to test */
    private static CommandManager manager;

    /** The number of replies to wait for in a test */
    private int numOfRepliesToGet;

    /**
     * The replies received are saved in this list
     */
    private static final List<ReplyMessage> repliesReceived = new Vector<>();

    /**
     * The lock set when the desired number of replies
     * have been received
     */
    private static final  AtomicReference<CountDownLatch> lock = new AtomicReference<>();

    @BeforeAll
    public static void setUpAll() throws Exception {
        logger.debug("Setting up static producer and consumer");
        replyConsumer = new SimpleStringConsumer(
                KafkaHelper.DEFAULT_BOOTSTRAP_BROKERS,
                KafkaHelper.REPLY_TOPIC_NAME,
                "repCons");
        cmdProducer = new SimpleStringProducer(
            KafkaHelper.DEFAULT_BOOTSTRAP_BROKERS,
            KafkaHelper.CMD_TOPIC_NAME,
            "CmdProd");
        replyConsumer.setUp();
        cmdProducer.setUp();
        logger.info("Static producer and consumer built and set up");
        manager = new CommandManager(
                commandManagerFullRunningId,
                commandManagerId,
                KafkaHelper.DEFAULT_BOOTSTRAP_BROKERS);
        logger.info("CommandManager to test built");
    }

    @AfterAll
    public static void tearDownAll() throws Exception {
        if (replyConsumer!=null) {
            replyConsumer.tearDown();
        }
        if (cmdProducer!=null) {
            cmdProducer.tearDown();
        }
        if (manager!=null) {
            manager.close();
        }
    }

    @BeforeEach
    public void setUp() throws Exception {
        logger.debug("Setting up for a new test");

        if (!beforeFirstTest) {
            replyConsumer.startGettingEvents(KafkaStringsConsumer.StreamPosition.END, this);
            beforeFirstTest =true;
            logger.info("Processing of replies published by the CommandManager activated");
            manager.start(this);
            logger.info("CommandManager to test started");
        }
        logger.debug("Ready to run a new test");
    }

    @AfterEach
    public void tearDown() throws Exception {
        repliesReceived.clear();
    }

    /**
     * Simulate the execution of the command: it is the listener of commands
     * of the {@link CommandManager}
     *
     * @param cmd The command received from the command topic
     * @return the state of the execution of the command
     */
    @Override
    public CmdExecutionResult newCommand(CommandMessage cmd) {
        logger.debug("Processing command {}",cmd);
        return new CmdExecutionResult(CommandExitStatus.OK,null);
    }


    @Test
    public void testCommandReply() throws Exception {
        logger.info("Test the sending of a command and reception of the reply");
        numOfRepliesToGet=1;
        lock.set(new CountDownLatch(numOfRepliesToGet));
        CommandMessage cmd = new CommandMessage(
                commandSenderFullRunningId,
                commandManagerId,
                CommandType.PING,
                1,
                null,
                System.currentTimeMillis(),
                null);

        String jSonStr =cmdSerializer.iasCmdToString(cmd);
        logger.debug("Command {} will be sent as json string [{}]",cmd.toString(),jSonStr);
        cmdProducer.push(jSonStr,null,commandManagerId);
        logger.info("Command sent. Waiting for the reply...");
        assertTrue(lock.get().await(5, TimeUnit.SECONDS),"Reply not received");

        ReplyMessage reply = repliesReceived .get(0);
        assertEquals(reply.getId(),1);
        assertEquals(reply.getCommand(),CommandType.PING);
        assertEquals(reply.getExitStatus(),CommandExitStatus.OK);
        logger.info("Done testCommandReply");
    }

    @Test
    public void testBroadcastCommandReply() throws Exception {
        logger.info("Test the sending of a broadcast command and reception of the reply");
        numOfRepliesToGet=1;
        lock.set(new CountDownLatch(numOfRepliesToGet));
        CommandMessage cmd = new CommandMessage(
                commandSenderFullRunningId,
                CommandMessage.BROADCAST_ADDRESS,
                CommandType.SHUTDOWN,
                2,
                null,
                System.currentTimeMillis(),
                null);

        String jSonStr =cmdSerializer.iasCmdToString(cmd);
        logger.debug("Broadcast command {} will be sent as json string [{}]",cmd.toString(),jSonStr);
        cmdProducer.push(jSonStr,null,commandManagerId);
        logger.info("Command sent. Waiting for the reply...");
        assertTrue(lock.get().await(5, TimeUnit.SECONDS),"Reply not received");

        ReplyMessage reply = repliesReceived .get(0);
        assertEquals(reply.getId(),2);
        assertEquals(reply.getCommand(),CommandType.SHUTDOWN);
        assertEquals(reply.getExitStatus(),CommandExitStatus.OK);

        logger.info("Done testBroadcastCommandReply");
    }


    /**
     * The listener that gets the replies
     *
     * @param event The string received in the topic
     */
    @Override
    public void stringEventReceived(String event) {
        if (event==null || event.isEmpty()) {
            logger.warn("Gor an empty reply");
            return;
        }
        logger.info("Processing JSON reply [{}]",event);
        ReplyMessage reply;
        try {
            reply = replySerializer.valueOf(event);
        } catch (Exception e) {
            logger.error("Error.parsing the JSON string {} into a reply",event,e);
            return;
        }
        repliesReceived.add(reply);
        logger.info("Reply received: {}",reply.toString());
        if (repliesReceived.size()==numOfRepliesToGet) {
            logger.debug("All expected replies have been received");
            lock.get().countDown();
        } else {
            logger.debug("Replies to get {}, replies received {}: {} missing replies",
                    numOfRepliesToGet,
                    repliesReceived.size(),
                    numOfRepliesToGet-repliesReceived.size());
        }
    }
}
