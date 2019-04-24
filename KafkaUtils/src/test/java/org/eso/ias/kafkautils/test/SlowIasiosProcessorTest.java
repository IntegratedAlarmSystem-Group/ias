package org.eso.ias.kafkautils.test;

import org.eso.ias.kafkautils.KafkaHelper;
import org.eso.ias.kafkautils.KafkaIasiosConsumer;
import org.eso.ias.kafkautils.KafkaIasiosProducer;
import org.eso.ias.kafkautils.SimpleKafkaIasiosConsumer;
import org.eso.ias.types.IASValue;
import org.eso.ias.types.IasValueJsonSerializer;
import org.eso.ias.types.IasValueStringSerializer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
/**
 * Test the SimpleKafkaIasiosConsumer when the processor is too slow consuming events.
 *
 * The test checks that the old IASIOs read from the topic are never forwarded to the listener
 */
public class SlowIasiosProcessorTest {

    /**
     * IASIOs whose timestam is older than oldThreshold will be discarded
     */
    private static final int oldThreshold = 3000;

    /**
	 * The consumer of IASIOs received from the Kafka topic
	 */
	private KafkaIasiosConsumer consumer;

	/**
	 * The producer to publish IASValues (strings)
	 */
	private KafkaIasiosProducer producer;

	/**
	 * The topic to send to and receive strings from
	 */
	private final String topicName = "SlowProcessorTopicName";

	/**
	 * The IASIOs received from the listener
	 */
	private final List<IASValue<?>> receivedIasios = Collections.synchronizedList(new ArrayList<>());

    /**
	 * The logger
	 */
	private static final Logger logger = LoggerFactory.getLogger(SlowIasiosProcessorTest.class);

    /**
     * The number of processed messages including the
     * discarded ones
     */
    private final AtomicInteger processedMessages = new AtomicInteger(0);

    /**
	 * The serializer/deserializer to convert the string
	 * received by the BSDB in a IASValue
	*/
	private final IasValueStringSerializer serializer = new IasValueJsonSerializer();

    @BeforeAll
    public static void beforeAll() throws Exception {
        System.getProperties().setProperty(SimpleKafkaIasiosConsumer.SeekIfOlderThanProName,""+oldThreshold);
    }

    /**
     * Initialize
     */
    @BeforeEach
    public void setUp() throws Exception {
        logger.info("Initializing...");
        consumer = new KafkaIasiosConsumer(
                KafkaHelper.DEFAULT_BOOTSTRAP_BROKERS,
                topicName, "FilteredConsumser-Test",
                new HashSet<>(),
                new HashSet<>());
        consumer.setUp();
        receivedIasios.clear();
        producer = new KafkaIasiosProducer(KafkaHelper.DEFAULT_BOOTSTRAP_BROKERS, topicName, "Consumer-ID",serializer);
        producer.setUp();

        logger.info("Initialized.");
    }

    /**
     * Clean up
     */
    @AfterEach
    public void tearDown() {
        logger.info("Closing...");
        receivedIasios.clear();
        consumer.tearDown();
        producer.tearDown();
        logger.info("Closed after processing {} messages",processedMessages.get());
    }

    /**
     * Check if the value of the property to check for old timestamps
     * has been correctly set
     *
     * @throws Exception
     */
    @Test
    public void testPropValue() throws Exception {
        assertEquals(consumer.seekIfOlderThan,oldThreshold);
    }
}
