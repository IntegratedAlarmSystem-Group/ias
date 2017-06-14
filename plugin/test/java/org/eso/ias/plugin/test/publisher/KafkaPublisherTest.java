package org.eso.ias.plugin.test.publisher;

import static org.junit.Assert.assertNotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import org.eso.ias.plugin.Sample;
import org.eso.ias.plugin.filter.FilteredValue;
import org.eso.ias.plugin.publisher.PublisherException;
import org.eso.ias.plugin.publisher.impl.KafkaPublisher;
import org.eso.ias.plugin.thread.PluginThreadFactory;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Test the {@link KafkaPublisher}
 * @author acaproni
 *
 */
public class KafkaPublisherTest {
	
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
	 * Initialization
	 */
	@Before
	public void setUp() {
		// Build the publisher
		logger.info("Initializing...");
		int poolSize = Runtime.getRuntime().availableProcessors()/2;
		schedExecutorSvc= Executors.newScheduledThreadPool(poolSize, PluginThreadFactory.getThreadFactory());
		kPub = new KafkaPublisher(pluginId, serverName, port, schedExecutorSvc);
		logger.info("Kafka producer initialized");
		
		consumer = new SimpleKafkaConsumer(KafkaPublisher.topicName, serverName, port);
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

	@Test
	public void testOneEvent() throws PublisherException {
		kPub.setUp();
		kPub.startSending();
		
		Long val = Long.valueOf(123456789);
		List<Sample> samples = Arrays.asList(new Sample(val));
		FilteredValue fv = new FilteredValue("MP-ID", val, samples, System.currentTimeMillis());
		
		kPub.offer(Optional.of(fv));
		
		try {
			Thread.sleep(10000);
		} catch (InterruptedException ie) {
			ie.printStackTrace();
		}
	}

}
