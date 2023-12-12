package org.eso.ias.command.test;

import ch.qos.logback.classic.Level;
import org.eso.ias.command.CommandSender;
import org.eso.ias.command.CommandType;
import org.eso.ias.command.ReplyMessage;
import org.eso.ias.heartbeat.consumer.HbKafkaConsumer;
import org.eso.ias.heartbeat.consumer.HbListener;
import org.eso.ias.heartbeat.consumer.HbMsg;
import org.eso.ias.kafkautils.KafkaHelper;
import org.eso.ias.kafkautils.SimpleStringProducer;
import org.eso.ias.logging.IASLogger;
import org.eso.ias.types.Identifier;
import org.eso.ias.types.IdentifierType;
import org.junit.jupiter.api.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import scala.Option;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Vector;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Test the command sender by running the {@link CommandManagerSimulator}.
 *
 * The simulator gets and executes commands that this test sends using the {@link CommandSender}.
 */
public class TestCommandSender implements HbListener {

    /** The logger */
    private static final Logger logger = LoggerFactory.getLogger(TestCommandSender.class);

    public static final String id ="TestCommandSenderID";

    /**
     * The Identifier of this tool
     */
    public static final Identifier identifier = new Identifier(id, IdentifierType.CLIENT, Option.empty());

    /**
     * The shared kafka string producer
     */
    public static final SimpleStringProducer stringProducer =
            new SimpleStringProducer(KafkaHelper.DEFAULT_BOOTSTRAP_BROKERS,identifier.id());

    /**
     * The {@link CommandSender} to test
     */
    public static final CommandSender cmdSender = new CommandSender(identifier, stringProducer, KafkaHelper.DEFAULT_BOOTSTRAP_BROKERS);

    /** The id of the simulator */
    private static final String destId = CommandManagerSimulator.id;

    /** The process of the simulator i.e. iasRun */
    private static Process simulatorProc;

    /**
     * The Identifier of the command executor
     */
    public static final Identifier executorId = new Identifier(destId, IdentifierType.CLIENT, Option.empty());

    /** The consumer of HBs */
    private HbKafkaConsumer hbConsumer;

    /** The HBs received */
    public static final List<HbMsg> hbs = Collections.synchronizedList(new Vector<>());

    @BeforeAll
    public static void setUpAll() throws Exception {
        logger.info("Setting up the CommandSender");
        IASLogger.setRootLogLevel(Level.DEBUG);
        cmdSender.setUp();
        simulatorProc=runSimulator();
    }

    @BeforeEach
    public void setUp() throws Exception {
        hbs.clear();
        hbConsumer = new HbKafkaConsumer(KafkaHelper.DEFAULT_BOOTSTRAP_BROKERS,id+"-TestHbCons");
        hbConsumer.addListener(this);
        hbConsumer.start();
    }

    @AfterEach
    public void tearDown() throws Exception {
        hbConsumer.shutdown();
    }

    @AfterAll
    public static void close() throws Exception {
        logger.info("Sending command to terminate the simulator");
        cmdSender.sendAsync(destId,CommandType.SHUTDOWN,null,null);
        logger.debug("Closing the command sender");
        Thread.sleep(1000);
        cmdSender.close();
        logger.info("Terminated");
    }

    /**
     * Run the simulator invoking iasRun
     *
     * @return
     */
    private static Process runSimulator() throws Exception  {
        logger.info("Running the simulator");
        List<String> cmdToRun = new Vector<>();
        cmdToRun.add("iasRun");
        cmdToRun.add("-r");
        cmdToRun.add("org.eso.ias.command.test.CommandManagerSimulator");
        ProcessBuilder procBuilder = new ProcessBuilder(cmdToRun);
        procBuilder.inheritIO();
        Process p = procBuilder.start();
        // Give some time to init
        logger.info("Give the simulator time to be ready...");
        Thread.sleep(10000);
        assertTrue(p.isAlive());
        logger.info("Simulator started");
        return p;
    }

    /**
     * An HB has been received
     *
     * @param hbMsg
     */
    @Override
    public void hbReceived(HbMsg hbMsg) {
        hbs.add(hbMsg);
    }

    /**
     * Send a sync command to a non existent destination and wait for the reply that must never arrive
     *
     * @throws Exception
     */
    @Test
    public void testMissingReply() throws Exception {
        String dest = "DoesNotExist";

        logger.info("Sending (sync) PING to {}",dest);
        Optional<ReplyMessage> ret =cmdSender.sendSync(dest, CommandType.PING,null,null,20, TimeUnit.SECONDS);
        assertTrue(ret.isEmpty());
    }

    /** Synchronously sends a command and wait for its execution */
    @Test
    public void testSyncSend() throws Exception {
        logger.info("Sending (sync) PING to {}",destId);
        assertTrue(
                cmdSender.sendSync(destId,CommandType.PING,null,null,30,TimeUnit.SECONDS).isPresent(),
                "Send sync did not terminate i.e. timeout");
    }

    @Test
    public void testRestart() throws Exception {
        logger.info("Sending (sync) RESTART to {}",destId);
      assertTrue(
                cmdSender.sendSync(destId,CommandType.RESTART,null,null,30,TimeUnit.SECONDS).isPresent(),
                "Send sync did not terminate i.e. timeout");
      hbs.clear();
      logger.info("Give the process time to restart");
      Thread.sleep(10000);
      assertTrue(!hbs.isEmpty(),"NO HB received from the restarted process");
    }

}
