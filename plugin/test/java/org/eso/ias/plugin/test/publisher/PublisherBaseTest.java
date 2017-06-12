package org.eso.ias.plugin.test.publisher;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.eso.ias.plugin.Sample;
import org.eso.ias.plugin.filter.FilteredValue;
import org.eso.ias.plugin.publisher.BufferedMonitoredSystemData;
import org.eso.ias.plugin.publisher.BufferedPublisherBase;
import org.eso.ias.plugin.publisher.MonitorPointData;
import org.eso.ias.plugin.publisher.PublisherBase;
import org.eso.ias.plugin.publisher.PublisherException;
import org.eso.ias.plugin.publisher.impl.ListenerPublisher;
import org.eso.ias.plugin.thread.PluginThreadFactory;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Test the {@link PublisherBase}.
 * <P>
 * <code>PublisherBaseTest</code> instantiates a {@link ListenerPublisher} object
 * to be notified about events of the {@link BufferedPublisherBase}.
 * <P>
 * To check the publishing of monitor points, an arbitrary number of {@link FilteredValue} 
 * objects is sent to the {@link BufferedPublisherBase#offer(java.util.Optional)}.
 * The test checks if all the messages are routed to the {@link ListenerPublisher} by means of a 
 * {@link CountDownLatch}. Received messages are stored in a Map, for comparison with the ones
 * being offered.
 * 
 * @author acaproni
 *
 */
public class PublisherBaseTest implements ListenerPublisher.PublisherEventsListener {
	
	/**
	 * The publisher to test
	 */
	private ListenerPublisher publisher;
	
	/**
	 * The logger
	 */
	private final static Logger logger = LoggerFactory.getLogger(PublisherBaseTest.class);
	
	/**
	 * The ID of the plugin
	 */
	private final String pluginId="AnotherPlugin-ID";
	
	/**
	 * The name of a server of the plugin for testing
	 */
	private final String pluginServerName = "iasdevel.hq.eso.org";
	
	/**
	 * The port of the server of the plugin for testing
	 */
	private final int pluginServerPort = 6789;
	
	/**
	 * The monitor points received from the listener
	 */
	private final Map<String,MonitorPointData> receivedMonitorPoints = new HashMap<>();
	
	/**
	 * The number of monitor point values received
	 */
	private final AtomicInteger monitorPointsReceived = new AtomicInteger(0);
	
	/**
	 * The latch to wait for the expected number of monitor point values
	 * to be sent to {@link ListenerPublisher#publish(BufferedMonitoredSystemData)}.
	 * <P>
	 * The latch is not used by all tests.
	 */
	protected CountDownLatch expectedValues=null;
	
	/**
	 * The values sent to the publisher
	 */
	private final Map<String, FilteredValue> publishedValues = new HashMap<>(); 
	
	@Before
	public void setUp() {
		// Build the publisher
		int poolSize = Runtime.getRuntime().availableProcessors()/2;
		ScheduledExecutorService schedExecutorSvc= Executors.newScheduledThreadPool(poolSize, PluginThreadFactory.getThreadFactory());
		publisher = new ListenerPublisher(pluginId, pluginServerName, pluginServerPort, schedExecutorSvc,this);
		logger.debug("Set up");
	}
	
	@After
	public void tearDown() throws PublisherException {
		logger.debug("Releasing resource");
		publisher.tearDown();
		receivedMonitorPoints.clear();
		publishedValues.clear();
	}
	
	@Test
	public void testBasicData() {
		assertNotNull(publisher);
		assertEquals("Plugin-IDs differ",pluginId,publisher.pluginId);
		assertEquals("Servers differ",pluginServerName,publisher.serverName);
		assertEquals("Servers ports",pluginServerPort,publisher.serverPort);
		assertEquals("Default buffer size",BufferedPublisherBase.defaultBufferSize,BufferedPublisherBase.maxBufferSize);
		assertEquals("Default throttling time",BufferedPublisherBase.defaultThrottlingTime,BufferedPublisherBase.throttlingTime);
	}

	/**
	 * Test if the user provided initialization method is invoked
	 * while initializing the publisher
	 */
	@Test
	public void testSetUp() throws PublisherException {
		publisher.setUp();
		assertEquals("Not initilized", 1L, publisher.getNumOfSetUpInvocations());
	}
	
	/**
	 * Test if the initializing the publisher more then once throws an exception
	 */
	@Test(expected=PublisherException.class)
	public void testSetUpTwice() throws PublisherException {
		publisher.setUp();
		assertEquals("Not initilized", 1L, publisher.getNumOfSetUpInvocations());
		publisher.setUp();
	}
	
	/**
	 * Test if initializing a closed publisher throws an exception
	 */
	@Test(expected=PublisherException.class)
	public void testSetUpWhenClosed() throws PublisherException {
		publisher.setUp();
		assertEquals("Not initilized", 1L, publisher.getNumOfSetUpInvocations());
		
		// Close the publisher
		//
		// The exception must be caught otherwise the test cannot distinguish
		// if the PublisherException is thrown shuttinf down or while initializing 
		// after the close
		try {
			publisher.tearDown();
		} catch (PublisherException pe) {
			throw new IllegalStateException("Unexpected exception while shuttig down");
		}
		publisher.setUp();
	}
	
	/**
	 * Test if the user provided shutdown method is invoked
	 * while shutting down the publisher
	 */
	@Test
	public void testShutdown() throws PublisherException {
		assertEquals("tearDown count wrong",0L,publisher.getNumOfTearDownInvocations());
		publisher.tearDown();
		assertEquals("tearDown not executed",1L,publisher.getNumOfTearDownInvocations());
		publisher.tearDown();
		publisher.tearDown();
		publisher.tearDown();
		assertEquals("tearDown executed more then once",1L,publisher.getNumOfTearDownInvocations());
	}
	
	/**
	 * Check if {@link ListenerPublisher#publish(BufferedMonitoredSystemData)} is invoked to 
	 * publish just one {@link FilteredValue}.
	 * <P> 
	 * It also checks if the offered and the received values match.
	 * 
	 * @throws PublisherException
	 */
	@Test
	public void testPublishOneValue() throws PublisherException {
		publisher.setUp();
		publisher.startSending();
		expectedValues = new CountDownLatch(1);
		
		List<Sample> samples = Arrays.asList(new Sample(Integer.valueOf(67)));
		FilteredValue v = new FilteredValue("OneID", Integer.valueOf(67), samples, System.currentTimeMillis());
		publishedValues.put(v.id,v);
		Optional<FilteredValue> optVal = Optional.of(v);
		publisher.offer(optVal);
		try {
			assertTrue(expectedValues.await(2*BufferedPublisherBase.throttlingTime, TimeUnit.MILLISECONDS));
		} catch (InterruptedException ie) {
			Thread.currentThread().interrupt();
		}
		assertEquals(1L,publisher.getPublishedMessages());
		MonitorPointData d = receivedMonitorPoints.get(v.id);
		assertNotNull("Expected value not published",d);
		assertTrue("Offered and published values do not match "+v.toString()+"<->"+d.toString(), PublisherTestCommon.match(v,d));
		assertEquals(pluginId,d.getSystemID());
		assertTrue(d.getPublishTime()!=null && !d.getPublishTime().isEmpty());	
	}
	
	/**
	 * Check if {@link ListenerPublisher#publish(BufferedMonitoredSystemData)} is invoked to 
	 * publish all the {@link FilteredValue}.
	 * <P> 
	 * It also checks if the offered and the received values match.
	 * 
	 * @throws PublisherException
	 */
	@Test
	public void testPublishManyValues() throws PublisherException {
		publisher.setUp();
		publisher.startSending();
		expectedValues = new CountDownLatch(5);
		
		List<Sample> samples = Arrays.asList(new Sample(Integer.valueOf(67)));
		
		List<FilteredValue> values = Arrays.asList(
				new FilteredValue("FV-ID1", Integer.valueOf(67), samples, System.currentTimeMillis()),
				new FilteredValue("FV-ID2", Long.valueOf(123), samples, System.currentTimeMillis()),
				new FilteredValue("FV-ID3", "A string", samples, System.currentTimeMillis()),
				new FilteredValue("FV-ID4", Boolean.valueOf(true), samples, System.currentTimeMillis()),
				new FilteredValue("FV-ID5", Integer.valueOf(11), samples, System.currentTimeMillis()));

		for (FilteredValue v: values) {
			publishedValues.put(v.id, v);
			publisher.offer(Optional.of(v));
		};
		
		try {
			assertTrue(expectedValues.await(2*BufferedPublisherBase.throttlingTime, TimeUnit.MILLISECONDS));
		} catch (InterruptedException ie) {
			Thread.currentThread().interrupt();
		}
		assertEquals(values.size(),publisher.getPublishedMessages());
		assertEquals(publisher.getPublishedMessages(),monitorPointsReceived.get());
		
		for (FilteredValue v: values) {
			MonitorPointData d = receivedMonitorPoints.get(v.id);
			assertNotNull("Expected value not published",d);
			assertTrue("Offered and published values do not match", PublisherTestCommon.match(v,d));
			assertEquals(pluginId,d.getSystemID());
			assertTrue(d.getPublishTime()!=null && !d.getPublishTime().isEmpty());
		}
	}
	
	/**
	 * Check if {@link ListenerPublisher#publish(BufferedMonitoredSystemData)} is invoked to 
	 * publish just the last sent {@link FilteredValue}.
	 * <P> 
	 * It also checks if the offered and the received values match.
	 * <P>
	 * Due to the throttling publish can be invoked more the once but the last published
	 * value must match with the last offered FilteredValue.
	 * For this test we do not use a latch because we do not know how many times
	 * the same value will be sent.
	 * 
	 * @throws PublisherException
	 */
	@Test
	public void testPublishOneValueManyTimes() throws PublisherException {
		publisher.setUp();
		publisher.startSending();
		
		final int valuesToOffer=5000;
		
		List<FilteredValue> fValues = PublisherTestCommon.generateFileteredValues(valuesToOffer,"BaseID-",true,11,3);
		FilteredValue lastOffered=null;
		for (FilteredValue v: fValues) {
			publishedValues.put(v.id,v);
			publisher.offer(Optional.of(v));
			lastOffered=v;
		}
		
		try {
			Thread.sleep(2*BufferedPublisherBase.throttlingTime);
		} catch (InterruptedException ie) {
			Thread.currentThread().interrupt();
		}
		
		logger.info("Last offered value: {}",lastOffered.toString());
		
		assertEquals(publisher.getPublishedMessages(),monitorPointsReceived.get());
		assertEquals(publishedValues.size(), receivedMonitorPoints.size());
		
		MonitorPointData d = receivedMonitorPoints.get(lastOffered.id);
		assertNotNull("Expected value not published",d);
		assertTrue("Offered and published values do not match", PublisherTestCommon.match(lastOffered,d));
		assertEquals(pluginId,d.getSystemID());
		assertTrue(d.getPublishTime()!=null && !d.getPublishTime().isEmpty());
	}
	
	/**
	 * Test the setting of the boolean to start/stop the sending
	 * of values to the core of the IAS.
	 *  
	 * @throws PublisherException
	 */
	@Test
	public void testStoppedBoolean() throws PublisherException {
		publisher.setUp();
		assertFalse(publisher.isStopped());
		publisher.stopSending();
		assertTrue(publisher.isStopped());
		publisher.startSending();
		assertFalse(publisher.isStopped());
	}
	
	/**
	 * Check if starting/stopping the publisher, effectively 
	 * trigger the invocation or not invocation of {@link ListenerPublisher#publish(BufferedMonitoredSystemData)}
	 * 
	 * @throws PublisherException
	 */
	@Test
	public void testStopped() throws PublisherException {
		publisher.setUp();
		
		// Check if the value is received when no stopped
		publisher.startSending();
		expectedValues = new CountDownLatch(1);
		
		List<Sample> samples = Arrays.asList(new Sample(Integer.valueOf(67)));
		FilteredValue v = new FilteredValue("OneID", Integer.valueOf(67), samples, System.currentTimeMillis());
		publishedValues.put(v.id,v);
		Optional<FilteredValue> optVal = Optional.of(v);
		publisher.offer(optVal);
		try {
			assertTrue(expectedValues.await(2*BufferedPublisherBase.throttlingTime, TimeUnit.MILLISECONDS));
		} catch (InterruptedException ie) {
			Thread.currentThread().interrupt();
		}
		
		// Check if the value is NOT received when stopped
		publisher.stopSending();
		expectedValues = new CountDownLatch(1);
		publishedValues.put(v.id,v);
		publisher.offer(Optional.of(v));
		try {
			assertFalse(expectedValues.await(2*BufferedPublisherBase.throttlingTime, TimeUnit.MILLISECONDS));
		} catch (InterruptedException ie) {
			Thread.currentThread().interrupt();
		}
		
		// Enable the sending again and check if the value is published
		publisher.startSending();
		expectedValues = new CountDownLatch(1);
		publishedValues.put(v.id,v);
		publisher.offer(Optional.of(v));
		try {
			assertTrue(expectedValues.await(2*BufferedPublisherBase.throttlingTime, TimeUnit.MILLISECONDS));
		} catch (InterruptedException ie) {
			Thread.currentThread().interrupt();
		}
	}

	/* (non-Javadoc)
	 * @see org.eso.ias.plugin.publisher.impl.ListenerPublisher.PublisherEventsListener#initialized()
	 */
	@Override
	public void initialized() {
		logger.info("Publisher initialized");
	}

	/* (non-Javadoc)
	 * @see org.eso.ias.plugin.publisher.impl.ListenerPublisher.PublisherEventsListener#closed()
	 */
	@Override
	public void closed() {
		logger.info("Publisher closed");
	}

	/**
	 * @see org.eso.ias.plugin.publisher.impl.ListenerPublisher.PublisherEventsListener#dataReceived(org.eso.ias.plugin.publisher.MonitorPointData)
	 */
	@Override
	public void dataReceived(MonitorPointData mpData) {
		monitorPointsReceived.incrementAndGet();
		receivedMonitorPoints.put(mpData.getId(), mpData);
		if (expectedValues!=null) {
			expectedValues.countDown();
		}
		
	}
}
