package org.eso.ias.command.test;

import org.eso.ias.command.CommandManager;
import org.eso.ias.command.DefaultCommandExecutor;
import org.eso.ias.command.kafka.CommandManagerKafkaImpl;
import org.eso.ias.heartbeat.*;
import org.eso.ias.heartbeat.publisher.HbKafkaProducer;
import org.eso.ias.heartbeat.serializer.HbJsonSerializer;
import org.eso.ias.kafkautils.KafkaHelper;
import org.eso.ias.kafkautils.SimpleStringProducer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * A little program that instantiates a {@link org.eso.ias.command.CommandManager} to get commands and send replies.
 *
 * It simulates a IAS tool that gets commands, executes them and send replies.
 * In addition it also sends HBs as IAS tools do.
 * This program will be used for testing: simulate a generic IAS tool that gets commands rpduces replies and HBs.
 * It must be shutdown through the SHUTDOWN command.
 * If SHUTDWON is not received, the process terminates after a timeout.
 */
public class CommandManagerSimulator implements AutoCloseable {

    /** The id of the tool */
    public static final String id = "CommandManagerSimulator";

    /** The logger */
    private static final Logger logger = LoggerFactory.getLogger(CommandManagerSimulator.class);

    /** The {@link org.eso.ias.command.CommandManager} */
    private final CommandManager cmdMgr;

    private final CountDownLatch latch = new CountDownLatch(1);

    private final SimpleStringProducer stringProducer =
            new SimpleStringProducer(KafkaHelper.DEFAULT_BOOTSTRAP_BROKERS,id);

    /** The HB produced by the simulator */
    private final Heartbeat hb = new Heartbeat(HeartbeatProducerType.CLIENT,id);

    /** Serializer of HBs */
    private final HbMsgSerializer hbSerializer = new HbJsonSerializer();

    private final HbProducer hbProducer = new HbKafkaProducer(stringProducer,id,hbSerializer);

    private HbEngine hbSender = new HbEngine(hb,1,hbProducer);

    /** true if the process has been closed */
    private final AtomicBoolean closed = new AtomicBoolean(false);

    public CommandManagerSimulator() {
        cmdMgr = new CommandManagerKafkaImpl(id, KafkaHelper.DEFAULT_BOOTSTRAP_BROKERS,stringProducer);
    }

    /** Start the command manager */
    public CountDownLatch start() throws Exception {
        stringProducer.setUp();
        hbSender.start();
        cmdMgr.start(new DefaultCommandExecutor(),this);
        hbSender.updateHbState(HeartbeatStatus.RUNNING);
        logger.info("Started");
        return latch;
    }

    /** Close the command manager */
    @Override
    public void close() {
        boolean alreadyClosed = closed.getAndSet(true);
        if (alreadyClosed) {
            logger.warn("Already closed");
            return;
        }
        logger.info("Closing the manager");
        hbSender.updateHbState(HeartbeatStatus.EXITING);
        hbSender.shutdown();
        stringProducer.tearDown();
        latch.countDown();
        logger.info("Closed");
    }

    public static void main(String [] args) {
        CommandManagerSimulator cmdt = new CommandManagerSimulator();
        CountDownLatch latch;
        try {
            latch = cmdt.start();
        } catch (Exception e) {
            logger.error("Exception caught starting",e);
            return;
        }

        try {
            if (!latch.await(5, TimeUnit.MINUTES)) {
                logger.warn("Timeout. SHUTDOWN not received?");
            }
        } catch (InterruptedException ie) {
            logger.error("Interrupted",ie);
        }


        logger.info("Closing");
        cmdt.close();
        logger.info("Done");


    }
}
