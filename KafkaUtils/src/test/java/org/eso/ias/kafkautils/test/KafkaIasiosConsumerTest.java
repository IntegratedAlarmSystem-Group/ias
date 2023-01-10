package org.eso.ias.kafkautils.test;

import org.eso.ias.kafkautils.KafkaHelper;
import org.eso.ias.kafkautils.KafkaIasiosConsumer;
import org.eso.ias.kafkautils.KafkaIasiosProducer;
import org.eso.ias.kafkautils.KafkaStringsConsumer.StreamPosition;
import org.eso.ias.kafkautils.SimpleKafkaIasiosConsumer.IasioListener;
import org.eso.ias.kafkautils.SimpleStringProducer;
import org.eso.ias.types.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

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
public class KafkaIasiosConsumerTest implements IasioListener {
	
	/**
	 * The logger
	 */
	private static final Logger logger = LoggerFactory.getLogger(KafkaIasiosConsumerTest.class);
	
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

	private final SimpleStringProducer stringProducer = new SimpleStringProducer(
			KafkaHelper.DEFAULT_BOOTSTRAP_BROKERS,
			"KafkaIasiosConsumerTest");
	
	/**
	 * Initialize
	 */
	@BeforeEach
	public void setUp() throws Exception {
		logger.info("Initializing...");
		consumer = new KafkaIasiosConsumer(
				KafkaHelper.DEFAULT_BOOTSTRAP_BROKERS,
				topicName, "FilteredConsumer-Test",
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

	@Override
	public void iasiosReceived(Collection<IASValue<?>> events) {
		assertNotNull(events);

		events.forEach( value -> {
            processedMessages.incrementAndGet();
            receivedIasios.add(value);
            numOfEventsToReceive.countDown();
            logger.info("Event of id [{}] received",value.id);
        });
	}
	
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
	
	/**
	 * Build and return the IASValues of the given type to publish from the passed IDs
	 * 
	 * @param ids The Ids of the IASValues to build 
	 * @param value the value of the IASValues
	 * @param type the type of the IASValues
	 * @return The IASValues to publish
	 */
	public Collection<IASValue<?>> buildValues(List<String> ids, Object value, IASTypes type) {
		Objects.requireNonNull(ids);
		return ids.stream().map(id ->  
			IASValue.build(
					value, 
					OperationalMode.OPERATIONAL, 
					IasValidity.RELIABLE, 
					buildFullRunningID(id),
					type)
		).collect(Collectors.toList());
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
		consumer.startGettingEvents(StreamPosition.END,this);

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
	public void testIasioConsumerWithIDsFilters() throws Exception {
		logger.info("Test testIasioConsumerWithFilters started");
		
		
		// Accepted Ids
		Set<String> idsOfIasios = new HashSet<>();
		for (int i=1; i<50; i++) idsOfIasios.add("ID-"+i);
		// Set the filter
		consumer.setFilter(idsOfIasios,null);
		
		// The IASValues to submit are more then the accepted IDs
		List<String> idsToSubmit  = new ArrayList<String>(idsOfIasios);
		for (int i=1; i<50; i++) idsToSubmit.add("ID-ToDiscard-"+i);
		
		logger.info("Going to submit {} IASValues and expect to be notified of {}",idsToSubmit.size(),idsOfIasios.size());
		
		// We should have a timeout since we expect to receive less values then those submitted
		numOfEventsToReceive = new CountDownLatch(idsToSubmit.size());
		
		consumer.startGettingEvents(StreamPosition.END,this);

		publishIasValues(idsToSubmit);
		logger.info("Waiting for events (timeout expected)....");
		assertFalse(numOfEventsToReceive.await(10, TimeUnit.SECONDS));
		assertEquals(idsOfIasios.size(), receivedIasios.size());
		assertTrue(checkIds(receivedIasios, idsOfIasios));
		logger.info("Test testIasioConsumerWithFilters done");
	}
	
	/**
	 * Test the {@link KafkaIasiosConsumer} when filtering by type
	 * 
	 * @throws Exception
	 */
	@Test
	public void testIasioConsumerWithITypesFilters() throws Exception {
		logger.info("Test testIasioConsumerWithITypesFilters started");
		
		
		// Accepted types
		Set<IASTypes> typesOfIasios = new HashSet<>();
		typesOfIasios.add(IASTypes.LONG);
		typesOfIasios.add(IASTypes.ALARM);
		
		// Accepted longs
		List<String> idsOfLongs = new LinkedList<>();
		for (int i=1; i<60; i++) idsOfLongs.add("ID-TypeLong-"+i);
		Collection<IASValue<?>> longIasios = buildValues(idsOfLongs, 10L, IASTypes.LONG);
		
		// Accepted alarms
		List<String> idsOfAlarms = new LinkedList<>();
		for (int i=1; i<75; i++) idsOfAlarms.add("ID-TypeAlarm-"+i);
		Collection<IASValue<?>> alarmIasios = buildValues(idsOfAlarms, 10L, IASTypes.LONG);
		
		// Rejected boolean
		List<String> idsOfbooleans = new LinkedList<>();
		for (int i=1; i<100; i++) idsOfbooleans.add("ID-TypeBool-"+i);
		Collection<IASValue<?>> boolIasios = buildValues(idsOfbooleans, Boolean.TRUE, IASTypes.BOOLEAN);
		
		// Rejected doubles
		List<String> idsOfdoubles=new LinkedList<>();
		for (int i=1; i<100; i++) idsOfdoubles.add("ID-TypeBool-"+i);
		Collection<IASValue<?>> doubleIasios = buildValues(idsOfdoubles, Double.valueOf(10.5), IASTypes.DOUBLE);
		
		// Set the filter by types
		consumer.setFilter(null,typesOfIasios);
		
		// The IASValues to submit are more then the accepted IDs
		List<IASValue<?>> valuesToSubmit  = new ArrayList<>();
		valuesToSubmit.addAll(longIasios); // accepted
		valuesToSubmit.addAll(boolIasios); // rejected
		valuesToSubmit.addAll(alarmIasios); // accepted
		valuesToSubmit.addAll(doubleIasios); // rejected
		int expected = longIasios.size()+alarmIasios.size();

		// We should have a timeout since we expect to receive less values then those submitted
		numOfEventsToReceive = new CountDownLatch(valuesToSubmit.size());
		
		// Start getting events
		consumer.startGettingEvents(StreamPosition.END,this);
		
		// Push the values
		logger.info("Going to submit {} IASValues and expect to be notified of {}",valuesToSubmit.size(),expected);
		producer.push(valuesToSubmit);

		logger.info("Waiting for events (timeout expected)....");
		
		assertFalse(numOfEventsToReceive.await(10, TimeUnit.SECONDS));
		
		assertEquals(expected, receivedIasios.size());
		
		// Check types
		for (IASValue<?> value: receivedIasios) {
			assertTrue(typesOfIasios.contains(value.valueType));
		}
		
		logger.info("Test testIasioConsumerWithITypesFilters done");
	}
	
	/**
	 * Test the {@link KafkaIasiosConsumer} when filtering by Ids and types
	 * <P>
	 * This test replicates what is done by the test by filtering only but
	 * adding few IDs to restrict even more
	 * 
	 * @throws Exception
	 */
	@Test
	public void testIasioConsumerWithITypesAndIds() throws Exception {
		logger.info("Test testIasioConsumerWithITypesAndIds started");
		
		
		// Accepted types
		Set<IASTypes> typesOfIasios = new HashSet<>();
		typesOfIasios.add(IASTypes.LONG);
		typesOfIasios.add(IASTypes.ALARM);
		
		// Accepted longs
		List<String> idsOfLongs = new LinkedList<>();
		for (int i=1; i<60; i++) idsOfLongs.add("ID-TypeLong-"+i);
		Collection<IASValue<?>> longIasios = buildValues(idsOfLongs, 10L, IASTypes.LONG);
		
		// Accepted alarms
		List<String> idsOfAlarms = new LinkedList<>();
		for (int i=1; i<75; i++) idsOfAlarms.add("ID-TypeAlarm-"+i);
		Collection<IASValue<?>> alarmIasios = buildValues(idsOfAlarms, Alarm.getInitialAlarmState(Priority.HIGH).set(), IASTypes.ALARM);
		
		// Rejected boolean
		List<String> idsOfbooleans = new LinkedList<>();
		for (int i=1; i<100; i++) idsOfbooleans.add("ID-TypeBool-"+i);
		Collection<IASValue<?>> boolIasios = buildValues(idsOfbooleans, Boolean.TRUE, IASTypes.BOOLEAN);
		
		// Rejected doubles
		List<String> idsOfdoubles=new LinkedList<>();
		for (int i=1; i<100; i++) idsOfdoubles.add("ID-TypeBool-"+i);
		Collection<IASValue<?>> doubleIasios = buildValues(idsOfdoubles, Double.valueOf(10.5), IASTypes.DOUBLE);
		
		// The IDs to accept
		Set<String> accpetedIDs = new HashSet<>();
		for (int t=10; t<33; t++) {
			accpetedIDs.add("ID-TypeLong-"+t);
		}
		for (int t=25; t<45; t++) {
			accpetedIDs.add("ID-TypeAlarm-"+t);
		}
		
		// Set the filter by types
		consumer.setFilter(accpetedIDs,typesOfIasios);
		
		// The IASValues to submit are more then the accepted IDs
		List<IASValue<?>> valuesToSubmit  = new ArrayList<>();
		valuesToSubmit.addAll(longIasios); // accepted
		valuesToSubmit.addAll(boolIasios); // rejected
		valuesToSubmit.addAll(alarmIasios); // accepted
		valuesToSubmit.addAll(doubleIasios); // rejected
		
		int expected = Math.min(longIasios.size()+alarmIasios.size(), accpetedIDs.size());

		// We should have a timeout since we expect to receive less values then those submitted
		numOfEventsToReceive = new CountDownLatch(valuesToSubmit.size());
		
		// Start getting events
		consumer.startGettingEvents(StreamPosition.END,this);
		
		// Push the values
		logger.info("Going to submit {} IASValues and expect to be notified of {}",valuesToSubmit.size(),expected);
		producer.push(valuesToSubmit);

		logger.info("Waiting for events (timeout expected)....");
		
		assertFalse(numOfEventsToReceive.await(10, TimeUnit.SECONDS));
		
		assertEquals(expected, receivedIasios.size());
		
		// Check types and Ids
		for (IASValue<?> value: receivedIasios) {
			assertTrue(typesOfIasios.contains(value.valueType));
			assertTrue(accpetedIDs.contains(value.id));
		}
		
		logger.info("Test testIasioConsumerWithITypesAndIds done");
	}
}
