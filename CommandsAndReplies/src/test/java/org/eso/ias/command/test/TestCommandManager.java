package org.eso.ias.command.test;

import org.eso.ias.command.*;
import org.eso.ias.kafkautils.KafkaHelper;
import org.eso.ias.kafkautils.KafkaStringsConsumer;
import org.eso.ias.kafkautils.SimpleStringConsumer;
import org.eso.ias.kafkautils.SimpleStringProducer;
import org.junit.Assert;
import org.junit.jupiter.api.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
    public final String commandManagerId ="cmdManagerId";

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

    /**
     * Signal that the getting of  replies has been activated
     */
    private static volatile boolean gettingEventsFromReply = false;

    /** The {@link CommandManager} to test */
    private CommandManager manager;

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
    }

    @AfterAll
    public static void tearDownAll() throws Exception {
        if (replyConsumer!=null) {
            replyConsumer.tearDown();
        }
        if (cmdProducer!=null) {
            cmdProducer.tearDown();
        }
    }

    @BeforeEach
    public void setUp() throws Exception {
        logger.debug("Setting up for a new test");
        if (!gettingEventsFromReply) {
            replyConsumer.startGettingEvents(KafkaStringsConsumer.StreamPosition.END, this);
            gettingEventsFromReply=true;
            logger.info("Processing of replies published by the CommandManager activated");
        }
        manager = new CommandManager(
                commandManagerFullRunningId,
                commandManagerId,
                KafkaHelper.DEFAULT_BOOTSTRAP_BROKERS);
        Assert.assertNotNull(manager);
        logger.info("CommandManager to test built");
    }

    @AfterEach
    public void tearDown() throws Exception {
        logger.debug("Closing the CommandManager");
        manager.close();
        logger.info("CommandManager closed");
    }

    @Override
    public ReplyMessage newCommand(CommandMessage cmd) {
        return null;
    }

    /**
     * Invokes the int and close method of the {@link CommandManager}
     *
     * @throws Exception
     */
    @Test
    public void testStart() throws Exception {


        logger.info("Starting the CommandManager");
        manager.start(this);

    }

    public void testSendingCommand() throws Exception {
        manager.start(this);

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
        logger.info("reply received: {}",reply.toString());
    }
}