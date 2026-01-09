package org.eso.ias.kafkautils.test;

import org.eso.ias.kafkautils.*;
import org.eso.ias.types.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test the SimpleKafkaIasiosConsumer when the processor is too slow consuming events.
 *
 * The test checks that the old IASIOs read from the topic are never forwarded to the listener
 *
 * The tests are based on the fact that the {@link SimpleKafkaIasiosConsumer} checks the SentToBsdb timestamp.
 */
public class SlowIasiosProcessorTest implements SimpleKafkaIasiosConsumer.IasioListener {

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
	 * IASIOs whose timestamp is too old are saved here
     * We actually expect this list to be empty as {@link SimpleKafkaIasiosConsumer} should discard
     * those IASIOs
	 */
	private final List<IASValue<?>> oldReceivedIasios = Collections.synchronizedList(new ArrayList<>());

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

    private final SimpleStringProducer stringProducer = new SimpleStringProducer(
            KafkaHelper.DEFAULT_BOOTSTRAP_BROKERS,
            "SlowIasiosProcessorTest");

    /**
     * Build the full running ID from the passed id
     *
     * @param id The Id of the IASIO
     * @return he full running ID
     */
    private String buildFullRunningID(String id) {
        return Identifier.coupleGroupPrefix()+id+Identifier.coupleSeparator()+"IASIO"+Identifier.coupleGroupSuffix();
    }

    /**
     * Build and return the IASValues to publish from their IDs
     * and assigning a different type to each value
     *
     * @param ids The Ids of the IASValues to build
     * @return The IASValues to publish
     */
    public Collection<IASValue<?>> buildValues(List<String> ids) {
        Objects.requireNonNull(ids);
        return ids.stream().map(id ->
                IASValue.build(
                        10L,
                        OperationalMode.OPERATIONAL,
                        IasValidity.RELIABLE,
                        buildFullRunningID(id),
                        IASTypes.LONG)
        ).collect(Collectors.toList());
    }

    @Override
    public void iasiosReceived(Collection<IASValue<?>> events) {
        assertNotNull(events);
        events.forEach( value -> {
            processedMessages.incrementAndGet();
            receivedIasios.add(value);
            value.sentToBsdbTStamp.ifPresent( tStamp -> {
                if (System.currentTimeMillis()-tStamp>oldThreshold) {
                    oldReceivedIasios.add(value);
                }
            });
        });
        try {
            Thread.sleep(oldThreshold+100);
        }catch (InterruptedException ie) {
            logger.warn("Interrupted");
        }

    }

    /**
	 * Publishes in the kafka topic, the IASValues with the passed IDs.
	 * This method creates the IASValues then publishes them: for this test
	 * to pass what is important are only the IDs of the IASValue but not
	 * their value, type and so on.
	 *
	 * @param ids The Ids of the IASValues to publish
	 */
	private void publishIasValues(List<String> ids) throws Exception {
		Objects.requireNonNull(ids);
		Collection<IASValue<?>> values = buildValues(ids);
		producer.push(values);
	}

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
        producer = new KafkaIasiosProducer(stringProducer, topicName, serializer);
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

    /**
     * Check that old IASIOs are discarded
     *
     * @throws Exception
     */
    @Test
    public void testRemovalOfOldIasios() throws Exception {
        int itemsToSend = 50000;
        List<String> idsOfIasios = new ArrayList<String>();
        for (int i=1; i<itemsToSend; i++) idsOfIasios.add("ID-"+i);

        consumer.startGettingEvents(KafkaStringsConsumer.StreamPosition.END,this);
        publishIasValues(idsOfIasios);
        logger.info("Publishing events and giving some time for reception...");
        Thread.sleep(15000);
        logger.info("Sent {} IASIOs, received {} of which {} are too old (should be 0)",
                itemsToSend,
                receivedIasios.size(),
                oldReceivedIasios.size());
        assertEquals(0,oldReceivedIasios.size(),"Received soe IASIOs that are old and should have bene discarded");
        assertTrue(receivedIasios.size()<itemsToSend,"No IASIOs have been discarded");
    }
}
