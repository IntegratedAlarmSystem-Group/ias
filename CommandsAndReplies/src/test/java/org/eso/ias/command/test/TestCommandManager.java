package org.eso.ias.command.test;

import org.eso.ias.command.*;
import org.eso.ias.kafkautils.KafkaHelper;
import org.eso.ias.kafkautils.KafkaStringsConsumer;
import org.eso.ias.kafkautils.SimpleStringConsumer;
import org.eso.ias.kafkautils.SimpleStringProducer;
import org.junit.jupiter.api.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

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
    private static int numOfRepliesToGet;

    /**
     * The replies received are saved in this list
     */
    private static final List<ReplyMessage> repliesReceived = Collections.synchronizedList(new Vector<>());

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
    public CmdExecutionResult newCommand(CommandMessage cmd) throws Exception {
        logger.debug("Processing command {}",cmd);

        switch (cmd.getCommand()) {
            case RESTART:
                throw new Exception("Simulated exception");
            case SET_LOG_LEVEL:
                List<String> params = cmd.getParams();
                int p1 = Integer.valueOf(params.get(0));
                int p2 = Integer.valueOf(params.get(1));
                Map<String,String> cmdProps = cmd.getProperties();
                int cp1 = Integer.valueOf(cmdProps.get("v1"));
                int cp2 = Integer.valueOf(cmdProps.get("v2"));
                int res = p1*p2*cp1*cp2;

                Map<String, String> props = new HashMap<>();
                props.put("FirstKey", "A property");
                props.put("SecondKey", "Another property");
                props.put("FromParams",""+res);
                return new CmdExecutionResult(CommandExitStatus.UNKNOWN, props);
            default:
                return new CmdExecutionResult(CommandExitStatus.OK);
        }
    }


    @Test
    public void testCommandReply() throws Exception {
        logger.info("Test the sending of a command and reception of the reply");
        numOfRepliesToGet=1;
        lock.set(new CountDownLatch(1));
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
        assertEquals(1,reply.getId());
        assertEquals(CommandType.PING,reply.getCommand());
        assertEquals(CommandExitStatus.OK, reply.getExitStatus());
        logger.info("Done testCommandReply");
    }

    @Test
    public void testBroadcastCommandReply() throws Exception {
        logger.info("Test the sending of a broadcast command and reception of the reply");
        numOfRepliesToGet=1;
        lock.set(new CountDownLatch(1));
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
        assertEquals(2,reply.getId());
        assertEquals(CommandType.SHUTDOWN,reply.getCommand());
        assertEquals(CommandExitStatus.OK,reply.getExitStatus());

        logger.info("Done testBroadcastCommandReply");
    }

    /**
     * This test sends many commands and waits for the replies.
     *
     * The test checks the reception of the replies to ensure that each command has been
     * received and processed. Some of the replies may contain errors or being rejected: that's fine for this
     * test because it means that they have been received and processed by the manager.
     *
     * @throws Exception
     */
    @Test
    public void testMultipleCommands() throws Exception {
        logger.info("Test the sending of many commands and the recetion of the replies");
        numOfRepliesToGet=128;
        lock.set(new CountDownLatch(1));

        logger.info("Sending {} commands",numOfRepliesToGet);
        for (int i =0; i<numOfRepliesToGet; i++) {

            CommandMessage cmd = new CommandMessage(
                    commandSenderFullRunningId,
                    CommandMessage.BROADCAST_ADDRESS,
                    CommandType.PING,
                    1000+i,
                    null,
                    System.currentTimeMillis(),
                    null);

            String jSonStr =cmdSerializer.iasCmdToString(cmd);
            cmdProducer.push(jSonStr,null,commandManagerId);
            logger.info("Command {} sent",i);

        }
        logger.info("{} commands sent. Waiting for the replies...",numOfRepliesToGet);



        assertTrue(lock.get().await(1, TimeUnit.MINUTES),"Replies not received");

        assertEquals(numOfRepliesToGet,repliesReceived.size());
        for (int c=0; c<repliesReceived.size(); c++) {
            ReplyMessage r = repliesReceived.get(c);
            assertTrue(r.getId()>=1000 && r.getId()<1000+numOfRepliesToGet);
        }

        logger.info("Done testMultipleCommands");
    }

    @Test
    public void testErrorFromListener() throws Exception {
        logger.info("Test that the status of a reply is ERROR when the listener thorws an exception");
        numOfRepliesToGet=1;
        lock.set(new CountDownLatch(1));
        CommandMessage cmd = new CommandMessage(
                commandSenderFullRunningId,
                commandManagerId,
                CommandType.RESTART,
                3,
                null,
                System.currentTimeMillis(),
                null);

        String jSonStr =cmdSerializer.iasCmdToString(cmd);
        logger.debug("Command {} will be sent as json string [{}]",cmd.toString(),jSonStr);
        cmdProducer.push(jSonStr,null,commandManagerId);
        logger.info("Command sent. Waiting for the reply...");
        assertTrue(lock.get().await(5, TimeUnit.SECONDS),"Reply not received");

        ReplyMessage reply = repliesReceived .get(0);
        assertEquals(3,reply.getId());
        assertEquals(CommandType.RESTART,reply.getCommand());
        assertEquals(CommandExitStatus.ERROR, reply.getExitStatus());
        logger.info("Done testErrorFromListener");
    }

    /**
     * Test if the properties set by the listener are part of the reply
     *
     * @throws Exception
     */
    @Test
    public void testParamsAndProperties() throws Exception {
        logger.info("Test presence of properties in the reply");
        numOfRepliesToGet=1;
        lock.set(new CountDownLatch(1));

        Map<String,String> props = new HashMap<>();
        props.put("v1","2");
        props.put("v2","3");

        List<String> params = new Vector<>();
        params.add("4");
        params.add("5");

        CommandMessage cmd = new CommandMessage(
                commandSenderFullRunningId,
                commandManagerId,
                CommandType.SET_LOG_LEVEL,
                4,
                params,
                System.currentTimeMillis(),
                props);

        String jSonStr =cmdSerializer.iasCmdToString(cmd);
        logger.debug("Sending command {}",cmd.toString());
        cmdProducer.push(jSonStr,null,commandManagerId);
        logger.info("Command sent. Waiting for the reply...");
        assertTrue(lock.get().await(5, TimeUnit.SECONDS),"Reply not received");

        ReplyMessage reply = repliesReceived .get(0);
        assertEquals(4,reply.getId());
        assertEquals(CommandType.SET_LOG_LEVEL, reply.getCommand());
        assertEquals(CommandExitStatus.UNKNOWN, reply.getExitStatus());
        assertNotNull(reply.getProperties());
        assertEquals(3, reply.getProperties().size());
        assertEquals("A property",reply.getProperties().get("FirstKey"));
        assertEquals("Another property",reply.getProperties().get("SecondKey"));
        assertEquals("120", reply.getProperties().get("FromParams"));
        logger.info("Done testParamsAndProperties");

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

        System.out.println("*** ==> received reply with id="+reply.getId());
        if (repliesReceived.size()==numOfRepliesToGet) {
            logger.debug("All expected replies {} have been received (in queue {} items)",numOfRepliesToGet,repliesReceived.size());
            lock.get().countDown();
        } else {
            logger.debug("Replies to get {}, replies received {}: {} missing replies",
                    numOfRepliesToGet,
                    repliesReceived.size(),
                    numOfRepliesToGet-repliesReceived.size());
        }
    }

}
