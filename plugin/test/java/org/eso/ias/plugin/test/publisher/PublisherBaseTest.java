package org.eso.ias.plugin.test.publisher;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertFalse;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Properties;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.eso.ias.plugin.Sample;
import org.eso.ias.plugin.filter.FilteredValue;
import org.eso.ias.plugin.publisher.MonitorPointData;
import org.eso.ias.plugin.publisher.BufferedMonitoredSystemData;
import org.eso.ias.plugin.publisher.BufferedPublisherBase;
import org.eso.ias.plugin.publisher.PublisherException;
import org.eso.ias.plugin.publisher.impl.ListenerPublisher;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Test the {@link BufferedPublisherBase}.
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
public class PublisherBaseTest extends PublisherTestCommon {
	
	/**
	 * The logger
	 */
	private final static Logger logger = LoggerFactory.getLogger(PublisherBaseTest.class);
	
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
		assertEquals(1L,publisher.getPublishedMonitorPoints());
		assertEquals(publisher.getPublishedMessages(),numOfPublishInvocation);
		MonitorPointData d = receivedValues.get(v.id);
		assertNotNull("Expected value not published",d);
		assertTrue("Offered and published values do not match "+v.toString()+"<->"+d.toString(), match(v,d));
		
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
		assertEquals(values.size(),publisher.getPublishedMonitorPoints());
		assertEquals(publisher.getPublishedMessages(),numOfPublishInvocation);
		
		for (FilteredValue v: values) {
			MonitorPointData d = receivedValues.get(v.id);
			assertNotNull("Expected value not published",d);
			assertTrue("Offered and published values do not match", match(v,d));
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
		
		List<FilteredValue> fValues = generatedFileteredValues(valuesToOffer,"BaseID-",true,11,3);
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
		
		assertEquals(publisher.getPublishedMessages(),numOfPublishInvocation);
		assertEquals(publishedValues.size(), receivedValues.size());
		
		MonitorPointData d = receivedValues.get(lastOffered.id);
		assertNotNull("Expected value not published",d);
		assertTrue("Offered and published values do not match", match(lastOffered,d));
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
}
