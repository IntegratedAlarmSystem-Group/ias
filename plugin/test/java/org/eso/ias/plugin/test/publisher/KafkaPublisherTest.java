package org.eso.ias.plugin.test.publisher;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.eso.ias.plugin.Sample;
import org.eso.ias.plugin.ValueToSend;
import org.eso.ias.plugin.filter.Filter.ValidatedSample;
import org.eso.ias.plugin.publisher.MonitorPointData;
import org.eso.ias.plugin.publisher.PublisherException;
import org.eso.ias.plugin.publisher.impl.KafkaPublisher;
import org.eso.ias.plugin.test.publisher.SimpleKafkaConsumer.KafkaConsumerListener;
import org.eso.ias.plugin.thread.PluginThreadFactory;
import org.eso.ias.prototype.input.java.IasValidity;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Test the sneding of {@link MonitorPointData} by the {@link KafkaPublisher}.
 * <P>
 * <code>KafkaPublisherTest</code> always send items with different IDs because
 * it tests if all the events published are pushed in the kafka topic.
 * Checking the buffering to avoid to send items with the same ID in short time 
 * as well as the throttling has already been tested by {@link PublisherBaseTest}.
 *   
 * @author acaproni
 *
 */
public class KafkaPublisherTest implements KafkaConsumerListener {
	
	/**
	 * The logger
	 */
	private static final Logger logger = LoggerFactory.getLogger(KafkaPublisher.class);
	
	/**
	 * Th ekafka publisher to test
	 */
	private KafkaPublisher kPub;
	
	/**
	 * The ID of the puglin producing monitor point values
	 */
	private final String pluginId="PluginForKafka";
	
	/**
	 * The ID of the system monitored by the plugin 
	 */
	protected final String monitoredSystemId = "Kafka-Monitored-System-ID";
	
	/**
	 * The name of the server where kafka runs
	 */
	private final String serverName="localhost";
	
	/**
	 * The port 
	 */
	private final int port = 9092;
		
	/**
	 * The executor service
	 */
	private ScheduledExecutorService schedExecutorSvc;
	
	/**
	 * The consumer to get events from the topic
	 */
	private SimpleKafkaConsumer consumer;
	
	/**
	 * The semaphore to wait on the desired number of events
	 */
	private CountDownLatch eventsToReceive=null;
	
	/**
	 * The {@link MonitorPointData} received by the consumer
	 */
	private final Map<String, MonitorPointData> receivedMonitorPoints = Collections.synchronizedMap(new HashMap<>());
	
	/**
	 * Initialization
	 */
	@Before
	public void setUp() {
		// Build the publisher
		logger.info("Initializing...");
		int poolSize = Runtime.getRuntime().availableProcessors()/2;
		schedExecutorSvc= Executors.newScheduledThreadPool(poolSize, new PluginThreadFactory());
		kPub = new KafkaPublisher(pluginId, monitoredSystemId, serverName, port, schedExecutorSvc);
		logger.info("Kafka producer initialized");
		
		consumer = new SimpleKafkaConsumer(KafkaPublisher.defaultTopicName, serverName, port,this);
		consumer.setUp();
		logger.info("Kafka consumer initialized");
		
		logger.info("Initialized");
	}
	
	/**
	 * Clean up
	 */
	@After
	public void tearDown() throws PublisherException {
		kPub.tearDown();
		consumer.tearDown();
		schedExecutorSvc.shutdown();
		logger.info("Cleaned up");
	}

	/**
	 * Test the sending of only one event through the kafka topic
	 * 
	 * @throws PublisherException
	 */
	@Test
	public void testOneEvent() throws PublisherException {
		kPub.setUp();
		kPub.startSending();
		eventsToReceive = new CountDownLatch(1);
		
		// The value is dinamically to be sure we are receiving the
		// very same record we offered
		String mpId="MP-ID";
		Long val = Long.valueOf(System.currentTimeMillis());
		List<ValidatedSample> samples = Arrays.asList(new ValidatedSample(new Sample(val),IasValidity.RELIABLE));
		ValueToSend fv = new ValueToSend("MP-ID", val, samples, System.currentTimeMillis());
		
		kPub.offer(fv);
		
		try {
			assertTrue("Timeout, event not received",eventsToReceive.await(2, TimeUnit.MINUTES));
		} catch (InterruptedException ie) {
			ie.printStackTrace();
		}
		
		// Check the received MonitorPointData
		assertEquals(1L,receivedMonitorPoints.size());
		MonitorPointData mpData = receivedMonitorPoints.get("MP-ID");
		assertNotNull(mpData);
		assertEquals(pluginId, mpData.getPluginID());
		assertEquals(monitoredSystemId, mpData.getMonitoredSystemID());
		assertEquals(val.longValue(), Long.parseLong(mpData.getValue()));
	}
	
	/**
	 * Test the sending of many events through the kafka topic
	 * 
	 * @throws PublisherException
	 */
	@Test
	public void testManyEvents() throws PublisherException {
		kPub.setUp();
		kPub.startSending();
		int eventsToPublish=50000;
		eventsToReceive = new CountDownLatch(eventsToPublish);
		
		// The value is dinamically to be sure we are receiving the
		// very same record we offered
		String mpIdPrefix="MPID-";
		int valBase=10;
		int valIncrement=7;
		
		for (int t=0; t<eventsToPublish; t++) {
			Integer val = Integer.valueOf(10+valIncrement*t);
			String id = mpIdPrefix+t;
			List<ValidatedSample> samples = Arrays.asList(new ValidatedSample(new Sample(val),IasValidity.RELIABLE));
			ValueToSend fv = new ValueToSend(id, val, samples, System.currentTimeMillis());
			kPub.offer(fv);
		}
		
		try {
			assertTrue("Timeout, events not received",eventsToReceive.await(2, TimeUnit.MINUTES));
		} catch (InterruptedException ie) {
			ie.printStackTrace();
		}
		logger.info("[] events received",eventsToPublish);
		
		// Check the received MonitorPointData
		assertEquals(eventsToPublish,receivedMonitorPoints.size());
		for (int t=0; t<eventsToPublish; t++) {
			Integer val = Integer.valueOf(10+valIncrement*t);
			String id = mpIdPrefix+t;
			MonitorPointData mpData = receivedMonitorPoints.get(id);
			assertNotNull(mpData);
			assertEquals(pluginId, mpData.getPluginID());
			assertEquals(monitoredSystemId, mpData.getMonitoredSystemID());
			assertEquals(val.intValue(), Integer.parseInt(mpData.getValue()));
		}
	}

	/**
	 * @see org.eso.ias.plugin.test.publisher.SimpleKafkaConsumer.KafkaConsumerListener#consumeKafkaEvent(java.lang.String)
	 */
	@Override
	public void consumeKafkaEvent(String event) {
		try {
			MonitorPointData mpData = MonitorPointData.fromJsonString(event);
			receivedMonitorPoints.put(mpData.getId(), mpData);
		} catch (PublisherException pe) {
			logger.error("Error building the monitor point value",pe);
		}
		if (eventsToReceive!=null) {
			eventsToReceive.countDown();
		}
	}

}
