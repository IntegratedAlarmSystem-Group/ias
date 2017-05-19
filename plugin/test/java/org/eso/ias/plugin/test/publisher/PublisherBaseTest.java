package org.eso.ias.plugin.test.publisher;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.eso.ias.plugin.Sample;
import org.eso.ias.plugin.filter.FilteredValue;
import org.eso.ias.plugin.publisher.MonitorPointData;
import org.eso.ias.plugin.publisher.MonitoredSystemData;
import org.eso.ias.plugin.publisher.PublisherBase;
import org.eso.ias.plugin.publisher.PublisherException;
import org.eso.ias.plugin.thread.PluginThreadFactory;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Test the {@link PublisherBase}.
 * <P>
 * <code>PublisherBaseTest</code> instantiates a {@link PublisherTestImpl} object
 * to be notified about events of the {@link PublisherBase}.
 * <P>
 * To check the publishing of monitor points, an arbitrary number of {@link FilteredValue} 
 * objects is sent to the {@link PublisherBase#offer(java.util.Optional)}.
 * The test checks if all the messages are routed to the {@link PublisherTestImpl} by means of a 
 * {@link CountDownLatch}. Received messages are stored in a Map, for comparison with the ones
 * being offered.
 * 
 * @author acaproni
 *
 */
public class PublisherBaseTest implements PublisherEventsListener {
	
	/**
	 * The logger
	 */
	private final static Logger logger = LoggerFactory.getLogger(PublisherBaseTest.class);
	
	/**
	 * The ID of the plugin for testing
	 */
	private final String pluginId = "IAS-Publisher-Test-ID";
	
	/**
	 * The name of a server of the plugin for testing
	 */
	private final String pluginServerName = "iasdev.hq.eso.org";
	
	/**
	 * The port of the server of the plugin for testing
	 */
	private final int pluginServerPort = 12345;
	
	/**
	 * The FileterdValues to be offered to the {@link PublisherBase}
	 */
	private final Map<String, FilteredValue> publishedValues = new HashMap<>();
	
	/**
	 * The FileterdValues sent to {@link PublisherTestImpl#publish(MonitoredSystemData)}
	 * to be sent to the core
	 */
	private final Map<String, MonitorPointData> receivededValues = new HashMap<>();
	
	/**
	 * The latch to wait for the expected number of values
	 * to be sent to {@link PublisherTestImpl#publish(MonitoredSystemData)}.
	 * <P>
	 * This is not the number of messages, but the number of {@link FilteredValue}
	 * objects as the {@link PublisherBase} could group more values into the same
	 * {@link MonitoredSystemData}.
	 */
	private CountDownLatch expectedValues;
	
	/**
	 * Record the numer of times publish has been called
	 */
	private int numOfPublishInvocation=0;
	
	/**
	 * The object to test
	 */
	private PublisherTestImpl publisher;
	
	/**
	 * Compare a {@link FilteredValue} (i.e. the value offered) with a
	 * {@link MonitorPointData} (i.e. the value to be sent to the OAS core)
	 *  
	 * @param v The not <code>null</code> value offered
	 * @param d The not <code>null</code> value to be sent to the core
	 * @return <code>true</code> if v and d matches
	 */
	private boolean match(FilteredValue v, MonitorPointData d) {
		assertNotNull(v);
		assertNotNull(d);
		
		/**
		 * ISO 8601 date formatter
		 */
		SimpleDateFormat iso8601dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.S");
		
		boolean ret = v.id==d.getId();
		ret = ret && v.value.toString().equals(d.getValue());
		ret = ret && iso8601dateFormat.format(new Date(v.producedTimestamp)).equals(d.getFilteredTime());
		return ret;
	}
	
	@Before
	public void setUp() {
		// Build the publisher
		ThreadGroup threadGroup = new ThreadGroup("Plugin thread group");
		int poolSize = Runtime.getRuntime().availableProcessors()/2;
		ScheduledExecutorService schedExecutorSvc= Executors.newScheduledThreadPool(poolSize, PluginThreadFactory.getThreadFactory());
		publisher = new PublisherTestImpl(pluginId, pluginServerName, pluginServerPort, schedExecutorSvc,this);
	}
	
	@After
	public void tearDown() {
		receivededValues.clear();
		publishedValues.clear();
	}
	
	@Test
	public void testBasicData() {
		assertNotNull(publisher);
		assertEquals("Plugin-IDs differ",pluginId,publisher.pluginId);
		assertEquals("Servers differ",pluginServerName,publisher.serverName);
		assertEquals("Servers ports",pluginServerPort,publisher.serverPort);
	}

	/**
	 * Test if the user provided initialization method is invoked
	 * while initializing the publisher
	 */
	@Test
	public void testSetUp() throws PublisherException {
		publisher.start();
		assertEquals("Not initilized", 1L, publisher.getNumOfSetUpInvocations());
	}
	
	/**
	 * Test if the initializing the publisher more then once throws an exception
	 */
	@Test(expected=PublisherException.class)
	public void testSetUpTwice() throws PublisherException {
		publisher.start();
		assertEquals("Not initilized", 1L, publisher.getNumOfSetUpInvocations());
		publisher.start();
	}
	
	/**
	 * Test if initializing a closed publisher throws an exception
	 */
	@Test(expected=PublisherException.class)
	public void testSetUpWhenClosed() throws PublisherException {
		publisher.start();
		assertEquals("Not initilized", 1L, publisher.getNumOfSetUpInvocations());
		
		// Close the publisher
		//
		// The exception must be caught otherwise the test cannot distinguish
		// if the PublisherException is thrown shuttinf down or while initializing 
		// after the close
		try {
			publisher.shutdown();
		} catch (PublisherException pe) {
			throw new IllegalStateException("Unexpected exception while shuttig down");
		}
		publisher.start();
	}
	
	/**
	 * Test if the user provided tearDown method is invoked
	 * while shutting down the publisher
	 */
	@Test
	public void testShutdown() throws PublisherException {
		assertEquals("tearDown count wrong",0L,publisher.getNumOfTearDownInvocations());
		publisher.shutdown();
		assertEquals("tearDown not executed",1L,publisher.getNumOfTearDownInvocations());
		publisher.shutdown();
		publisher.shutdown();
		publisher.shutdown();
		assertEquals("tearDown executed more then once",1L,publisher.getNumOfTearDownInvocations());
	}
	
	/**
	 * Check if {@link PublisherTestImpl#publish(MonitoredSystemData)} is invoked to 
	 * publish just one {@link FilteredValue}.
	 * <P> 
	 * It also checks if the offered and the received values match.
	 * 
	 * @throws PublisherException
	 */
	@Test
	public void testPublishOneValue() throws PublisherException {
		publisher.start();
		expectedValues = new CountDownLatch(1);
		
		List<Sample> samples = Arrays.asList(new Sample(Integer.valueOf(67)));
		FilteredValue v = new FilteredValue("OneID", Integer.valueOf(67), samples, System.currentTimeMillis());
		publishedValues.put(v.id,v);
		Optional<FilteredValue> optVal = Optional.of(v);
		publisher.offer(optVal);
		try {
			assertTrue(expectedValues.await(2*PublisherBase.throttlingTime, TimeUnit.MILLISECONDS));
		} catch (InterruptedException ie) {
			Thread.currentThread().interrupt();
		}
		assertEquals(1L,publisher.getPublishedMonitorPoints());
		assertEquals(publisher.getPublishedMessages(),numOfPublishInvocation);
		MonitorPointData d = receivededValues.get(v.id);
		assertNotNull("Expected value not published",d);
		assertTrue("Offered and published values do not match", match(v,d));
	}

	/**
	 * @see org.eso.ias.plugin.test.publisher.PublisherEventsListener#initialized()
	 */
	@Override
	public void initialized() {
		logger.info("Publisher initialized");
		
	}

	/**
	 * @see org.eso.ias.plugin.test.publisher.PublisherEventsListener#closed()
	 */
	@Override
	public void closed() {
		logger.info("Publisher closed");
		
	}

	/**
	 * @see org.eso.ias.plugin.test.publisher.PublisherEventsListener#dataReceived(org.eso.ias.plugin.publisher.MonitoredSystemData)
	 */
	@Override
	public void dataReceived(MonitoredSystemData data) {
		assertNotNull(data);
		numOfPublishInvocation++;
		logger.info("{} monitor points received from {}",data.getMonitorPoints().size(),data.getSystemID());
		data.getMonitorPoints().forEach(d-> { 
			expectedValues.countDown(); 
			receivededValues.put(d.getId(), d);
			logger.info("Received {}",d.toString());} );
	}
}
