package org.eso.ias.plugin.test.publisher;

import org.eso.ias.plugin.ValueToSend;
import org.eso.ias.plugin.publisher.BufferedMonitoredSystemData;
import org.eso.ias.plugin.publisher.BufferedPublisherBase;
import org.eso.ias.plugin.publisher.PublisherBase;
import org.eso.ias.plugin.publisher.PublisherException;
import org.eso.ias.plugin.publisher.impl.BufferedListenerPublisher;
import org.eso.ias.plugin.publisher.impl.BufferedListenerPublisher.PublisherEventsListener;
import org.eso.ias.plugin.publisher.impl.ListenerPublisher;
import org.eso.ias.plugin.thread.PluginThreadFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class PublisherMaxBufferSizeTest implements PublisherEventsListener {
	
	/**
	 * The logger
	 */
	private final static Logger logger = LoggerFactory.getLogger(PublisherMaxBufferSizeTest.class);
	
	/**
	 * The ID of the plugin for testing
	 */
	protected final String pluginId = "PublisheMaxBuffer-Test-ID";
	
	/**
	 * The ID of system monitored by the plugin 
	 */
	protected final String monitoredSysId = "PublisheMaxBuffer-System-ID";
	
	/**
	 * The name of a server of the plugin for testing
	 */
	protected final String pluginServerName = "iasdevel.eso.org";
	
	/**
	 * The port of the server of the plugin for testing
	 */
	protected final int pluginServerPort = 8192;
	
	/**
	 * The object to test
	 */
	protected BufferedListenerPublisher publisher;
	
	private int receivedValues=0;
	
	/**
	 * The latch to wait for the expected number of values
	 * to be sent to {@link ListenerPublisher#publish(org.eso.ias.plugin.publisher.MonitorPointData)}.
	 * <P>
	 * This is not the number of messages, but the number of {@link org.eso.ias.plugin.filter.FilteredValue}
	 * objects as the {@link BufferedPublisherBase} could group more values into the same
	 * {@link BufferedMonitoredSystemData}.
	 * <P>
	 * The latch is not used by all tests.
	 */
	protected CountDownLatch expectedValues=null;

	public PublisherMaxBufferSizeTest() {
		// TODO Auto-generated constructor stub
	}

	@Override
	public void initialized() {
		logger.info("Initialized");

	}

	@Override
	public void closed() {
		logger.info("Closed");

	}

	@Override
	public void dataReceived(BufferedMonitoredSystemData data) {
		assertTrue(data.getMonitorPoints().size()<= BufferedPublisherBase.maxBufferSize);
		receivedValues+=data.getMonitorPoints().size();
		for (int t=0; t<data.getMonitorPoints().size(); t++) { 
			expectedValues.countDown(); 
		}
	}

	@BeforeAll
	public static void beforeAll() {
		// Set the max buffer size property before any tests run
		System.setProperty(PublisherBase.MAX_BUFFER_SIZE_PROPNAME, "10");
	}

	@AfterAll
	public static void afterAll() {
    	System.clearProperty(PublisherBase.MAX_BUFFER_SIZE_PROPNAME);
	}
	
	@BeforeEach
	public void setUp() throws Exception {
		// Build the publisher
		int poolSize = Runtime.getRuntime().availableProcessors()/2;
		ScheduledExecutorService schedExecutorSvc= Executors.newScheduledThreadPool(poolSize, new PluginThreadFactory());
		assertEquals(10L, BufferedPublisherBase.maxBufferSize);
		publisher = new BufferedListenerPublisher(pluginId, monitoredSysId, schedExecutorSvc,this);
		logger.debug("Set up");
		publisher.setUp();
		publisher.startSending();
	}
	
	@AfterEach
	public void tearDown() throws PublisherException {
		logger.debug("Releasing resource");
		if (publisher!=null) { // In case of failure in setUp
			publisher.tearDown();
		}

	}
	
	/**
	 * Send many values and check that the number of 
	 * monitor point values in each message is less then the
	 * max size of the buffer.
	 * <P>
	 * Messages are sent as fast as possible in a loop: the test is actually done in
	 * {@link #dataReceived(BufferedMonitoredSystemData)}
	 */
	@Test
	public void testSendManyValues() throws Exception {
		int sizeOfBuffer=BufferedPublisherBase.maxBufferSize;
		int numOfValuesToSend=sizeOfBuffer*1027+15; // randomly chosen
		
		expectedValues = new CountDownLatch(numOfValuesToSend);
		// Generate the values to send
		
		List<ValueToSend> values = PublisherTestCommon.generateValuesToSend(numOfValuesToSend, "ID-bfzSize", false,1121 , 3);
		
		for (ValueToSend value: values) {
			publisher.offer(value);
		}
		
		assertTrue(expectedValues.await(10+BufferedPublisherBase.throttlingTime, TimeUnit.MILLISECONDS));
		assertEquals(numOfValuesToSend,receivedValues);
	}
}
