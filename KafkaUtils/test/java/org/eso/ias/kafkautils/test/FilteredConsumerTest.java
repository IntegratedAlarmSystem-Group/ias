package org.eso.ias.kafkautils.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.eso.ias.kafkautils.KafkaHelper;
import org.eso.ias.kafkautils.KafkaIasiosConsumer;
import org.eso.ias.kafkautils.SimpleStringProducer;
import org.eso.ias.kafkautils.KafkaIasiosConsumer.IasioListener;
import org.eso.ias.kafkautils.KafkaIasiosProducer;
import org.eso.ias.kafkautils.SimpleStringConsumer.StartPosition;
import org.eso.ias.prototype.input.java.IASTypes;
import org.eso.ias.prototype.input.java.IASValue;
import org.eso.ias.prototype.input.java.IasValidity;
import org.eso.ias.prototype.input.java.IasValueJsonSerializer;
import org.eso.ias.prototype.input.java.IasValueStringSerializer;
import org.eso.ias.prototype.input.java.OperationalMode;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A test to 
 * <UL>
 * 	<LI>check if the filtered listener effectively discard the IASIOs whose IDs do not match with the passed filter
 * 	<LI>test publishing of IASValues by {@link KafkaIasiosProducer}
 * </UL>
 * <P>
 * The test is performed by publishing strings obtained deserializing IASValues
 * and checking what is received by the listener.
 * <P>
 * @author acaproni
 */
public class FilteredConsumerTest implements IasioListener {
	
	/**
	 * The logger
	 */
	private static final Logger logger = LoggerFactory.getLogger(FilteredConsumerTest.class);
	
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
	private final String topicName = "FilteredProducerTest-topic";
	
	/**
	 * The IASIOs received from the listener
	 */
	private final List<IASValue<?>> receivedIasios = Collections.synchronizedList(new ArrayList<>());
	
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
	
	/**
	 * The number of events to wait for
	 */
	private CountDownLatch numOfEventsToReceive;
	
	/**
	 * Initialize
	 */
	@Before
	public void setUp() throws Exception {
		logger.info("Initializing...");
		consumer = new KafkaIasiosConsumer(KafkaHelper.DEFAULT_BOOTSTRAP_BROKERS, topicName, "FilteredConsumser-Test");
		consumer.setUp();
		receivedIasios.clear();
		producer = new KafkaIasiosProducer(KafkaHelper.DEFAULT_BOOTSTRAP_BROKERS, topicName, "Consumer-ID",serializer);
		producer.setUp();
		
		logger.info("Initialized.");
	}
	
	/**
	 * Clean up
	 */
	@After
	public void tearDown() {
		logger.info("Closing...");
		receivedIasios.clear();
		consumer.tearDown();
		producer.tearDown();
		logger.info("Closed after processing {} messages",processedMessages.get());
	}

	@Override
	public void iasioReceived(IASValue<?> event) {
		System.out.println("\nVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVV");
		assertNotNull(event);
		System.out.println(">1");
		processedMessages.incrementAndGet();
		System.out.println(">2");
		receivedIasios.add(event);
		System.out.println(">3");
		numOfEventsToReceive.countDown();
		System.out.println(">4");
		System.out.println("\n-------------------------------");
		logger.info("Event of id [{}] received",event.id);
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
		for (String id: ids) {
			IASValue<?> iasio = IASValue.buildIasValue(
					10L, 
					System.currentTimeMillis(), 
					OperationalMode.OPERATIONAL, 
					IasValidity.RELIABLE, 
					id, 
					"RunningID", 
					IASTypes.LONG);
			producer.push(iasio);
		}
	}
	
	/**
	 * Check if Ids of the passed iasios are all contained in the
	 * list of expected Ids
	 * 
	 * @param iasios The received IASIOs
	 * @param expectedIds the Ids accepted
	 * @return 
	 */
	private boolean checkIds(List<IASValue<?>> iasios, Set<String> expectedIds) {
		Objects.requireNonNull(iasios);
		Objects.requireNonNull(expectedIds);
		for (IASValue<?> value: iasios) {
			if (!expectedIds.contains(value.id)) {
				return false;
			}
		}
		return true;
	}
	
	/**
	 * Test the {@link KafkaIasiosConsumer} when no filters have been set
	 * 
	 * @throws Exception
	 */
	@Test
	public void testIasioConsumerNoFilters() throws Exception {
		logger.info("Test testIasioConsumerNoFilters started");
		// Ensure there are no active filters
		consumer.clearFilter();
		
		List<String> idsOfIasios = new ArrayList<String>();
		for (int i=1; i<50; i++) idsOfIasios.add("ID-"+i);
		
		numOfEventsToReceive = new CountDownLatch(idsOfIasios.size());
		consumer.startGettingEvents(StartPosition.END,this);

		publishIasValues(idsOfIasios);
		logger.info("Waiting for events");
		numOfEventsToReceive.await(1, TimeUnit.MINUTES);
		logger.info("Test testIasioConsumerNoFilters done");
	}
	
	/**
	 * Test the {@link KafkaIasiosConsumer} when no filters have been set
	 * 
	 * @throws Exception
	 */
	@Test
	public void testIasioConsumerWithFilters() throws Exception {
		logger.info("Test testIasioConsumerWithFilters started");
		
		
		// Accepted Ids
		Set<String> idsOfIasios = new HashSet<>();
		for (int i=1; i<50; i++) idsOfIasios.add("ID-"+i);
		// Set the filter
		consumer.setFilter(idsOfIasios);
		
		// The IASValues to submit are more then the accepted IDs
		List<String> idsToSubmit  = new ArrayList<String>(idsOfIasios);
		for (int i=1; i<50; i++) idsToSubmit.add("ID-ToDiscard-"+i);
		
		logger.info("Going to submit {} IASValues and expect to be notified of {}",idsToSubmit.size(),idsOfIasios.size());
		
		// We should have a timeout since we expect to receive less values then those submitted
		numOfEventsToReceive = new CountDownLatch(idsToSubmit.size());
		
		consumer.startGettingEvents(StartPosition.END,this);

		publishIasValues(idsToSubmit);
		logger.info("Waiting for events (timeout expected)....");
		assertFalse(numOfEventsToReceive.await(10, TimeUnit.SECONDS));
		assertEquals(idsOfIasios.size(), receivedIasios.size());
		assertTrue(checkIds(receivedIasios, idsOfIasios));
		logger.info("Test testIasioConsumerWithFilters done");
	}

}
