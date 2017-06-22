package org.eso.ias.plugin.test.publisher;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.eso.ias.plugin.filter.FilteredValue;
import org.eso.ias.plugin.publisher.BufferedMonitoredSystemData;
import org.eso.ias.plugin.publisher.BufferedPublisherBase;
import org.eso.ias.plugin.publisher.PublisherException;
import org.eso.ias.plugin.publisher.impl.BufferedListenerPublisher;
import org.eso.ias.plugin.publisher.impl.BufferedListenerPublisher.PublisherEventsListener;
import org.eso.ias.plugin.publisher.impl.ListenerPublisher;
import org.eso.ias.plugin.thread.PluginThreadFactory;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
	 * to be sent to {@link ListenerPublisher#publish(BufferedMonitoredSystemData)}.
	 * <P>
	 * This is not the number of messages, but the number of {@link FilteredValue}
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
	
	@Before
	public void setUp() throws Exception {
		// Build the publisher
		int poolSize = Runtime.getRuntime().availableProcessors()/2;
		ScheduledExecutorService schedExecutorSvc= Executors.newScheduledThreadPool(poolSize, new PluginThreadFactory());
		assertEquals(10L, BufferedPublisherBase.maxBufferSize);
		publisher = new BufferedListenerPublisher(pluginId, pluginServerName, pluginServerPort, schedExecutorSvc,this);
		logger.debug("Set up");
		publisher.setUp();
		publisher.startSending();
	}
	
	@After
	public void tearDown() throws PublisherException {
		logger.debug("Releasing resource");
		publisher.tearDown();

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
		
		List<FilteredValue> values = PublisherTestCommon.generateFileteredValues(numOfValuesToSend, "ID-bfzSize", false,1121 , 3);
		
		for (FilteredValue value: values) {
			publisher.offer(Optional.of(value));
		}
		
		assertTrue(expectedValues.await(10+BufferedPublisherBase.throttlingTime, TimeUnit.MILLISECONDS));
		assertEquals(numOfValuesToSend,receivedValues);
	}
}
