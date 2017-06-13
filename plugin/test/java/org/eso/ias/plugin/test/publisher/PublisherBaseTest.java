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
 * 
 * @author acaproni
 *
 */
public class PublisherBaseTest extends PublisherTestCommon {
	
	/**
	 * The logger
	 */
	private final static Logger logger = LoggerFactory.getLogger(PublisherBaseTest.class);
	/**
	 * The values sent to the publisher
	 */
	private final Map<String, FilteredValue> publishedValues = new HashMap<>(); 
	
	@Test
	public void testBasicData() {
		assertNotNull(unbufferedPublisher);
		assertEquals("Plugin-IDs differ",pluginId,unbufferedPublisher.pluginId);
		assertEquals("Servers differ",pluginServerName,unbufferedPublisher.serverName);
		assertEquals("Servers ports",pluginServerPort,unbufferedPublisher.serverPort);
		assertEquals("Default buffer size",BufferedPublisherBase.defaultBufferSize,BufferedPublisherBase.maxBufferSize);
		assertEquals("Default throttling time",BufferedPublisherBase.defaultThrottlingTime,BufferedPublisherBase.throttlingTime);
	}

	/**
	 * Test if the user provided initialization method is invoked
	 * while initializing the publisher
	 */
	@Test
	public void testSetUp() throws PublisherException {
		unbufferedPublisher.setUp();
		assertEquals("Not initilized", 1L, unbufferedPublisher.getNumOfSetUpInvocations());
	}
	
	/**
	 * Test if the initializing the publisher more then once throws an exception
	 */
	@Test(expected=PublisherException.class)
	public void testSetUpTwice() throws PublisherException {
		unbufferedPublisher.setUp();
		assertEquals("Not initilized", 1L, unbufferedPublisher.getNumOfSetUpInvocations());
		unbufferedPublisher.setUp();
	}
	
	/**
	 * Test if initializing a closed publisher throws an exception
	 */
	@Test(expected=PublisherException.class)
	public void testSetUpWhenClosed() throws PublisherException {
		unbufferedPublisher.setUp();
		assertEquals("Not initilized", 1L, unbufferedPublisher.getNumOfSetUpInvocations());
		
		// Close the publisher
		//
		// The exception must be caught otherwise the test cannot distinguish
		// if the PublisherException is thrown shuttinf down or while initializing 
		// after the close
		try {
			unbufferedPublisher.tearDown();
		} catch (PublisherException pe) {
			throw new IllegalStateException("Unexpected exception while shuttig down");
		}
		unbufferedPublisher.setUp();
	}
	
	/**
	 * Test if the user provided shutdown method is invoked
	 * while shutting down the publisher
	 */
	@Test
	public void testShutdown() throws PublisherException {
		assertEquals("tearDown count wrong",0L,unbufferedPublisher.getNumOfTearDownInvocations());
		unbufferedPublisher.tearDown();
		assertEquals("tearDown not executed",1L,unbufferedPublisher.getNumOfTearDownInvocations());
		unbufferedPublisher.tearDown();
		unbufferedPublisher.tearDown();
		unbufferedPublisher.tearDown();
		assertEquals("tearDown executed more then once",1L,unbufferedPublisher.getNumOfTearDownInvocations());
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
		unbufferedPublisher.setUp();
		unbufferedPublisher.startSending();
		expectedValues = new CountDownLatch(1);
		
		List<Sample> samples = Arrays.asList(new Sample(Integer.valueOf(67)));
		FilteredValue v = new FilteredValue("OneID", Integer.valueOf(67), samples, System.currentTimeMillis());
		publishedValues.put(v.id,v);
		Optional<FilteredValue> optVal = Optional.of(v);
		unbufferedPublisher.offer(optVal);
		try {
			assertTrue(expectedValues.await(2*BufferedPublisherBase.throttlingTime, TimeUnit.MILLISECONDS));
		} catch (InterruptedException ie) {
			Thread.currentThread().interrupt();
		}
		assertEquals(1L,unbufferedPublisher.getPublishedMessages());
		MonitorPointData d = receivedValuesFromUnbufferedPub.get(v.id);
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
		unbufferedPublisher.setUp();
		unbufferedPublisher.startSending();
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
			unbufferedPublisher.offer(Optional.of(v));
		};
		
		try {
			assertTrue(expectedValues.await(2*BufferedPublisherBase.throttlingTime, TimeUnit.MILLISECONDS));
		} catch (InterruptedException ie) {
			Thread.currentThread().interrupt();
		}
		assertEquals(values.size(),unbufferedPublisher.getPublishedMessages());
		assertEquals(unbufferedPublisher.getPublishedMessages(),numOfPublishInvocationInUnbufferedPub.get());
		
		for (FilteredValue v: values) {
			MonitorPointData d = receivedValuesFromUnbufferedPub.get(v.id);
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
		unbufferedPublisher.setUp();
		unbufferedPublisher.startSending();
		
		final int valuesToOffer=5000;
		
		List<FilteredValue> fValues = PublisherTestCommon.generateFileteredValues(valuesToOffer,"BaseID-",true,11,3);
		FilteredValue lastOffered=null;
		for (FilteredValue v: fValues) {
			publishedValues.put(v.id,v);
			unbufferedPublisher.offer(Optional.of(v));
			lastOffered=v;
		}
		
		try {
			Thread.sleep(2*BufferedPublisherBase.throttlingTime);
		} catch (InterruptedException ie) {
			Thread.currentThread().interrupt();
		}
		
		logger.info("Last offered value: {}",lastOffered.toString());
		
		assertEquals(unbufferedPublisher.getPublishedMessages(),numOfPublishInvocationInUnbufferedPub.get());
		assertEquals(publishedValues.size(), receivedValuesFromUnbufferedPub.size());
		
		MonitorPointData d = receivedValuesFromUnbufferedPub.get(lastOffered.id);
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
		unbufferedPublisher.setUp();
		assertFalse(unbufferedPublisher.isStopped());
		unbufferedPublisher.stopSending();
		assertTrue(unbufferedPublisher.isStopped());
		unbufferedPublisher.startSending();
		assertFalse(unbufferedPublisher.isStopped());
	}
	
	/**
	 * Check if starting/stopping the publisher, effectively 
	 * trigger the invocation or not invocation of {@link ListenerPublisher#publish(BufferedMonitoredSystemData)}
	 * 
	 * @throws PublisherException
	 */
	@Test
	public void testStopped() throws PublisherException {
		unbufferedPublisher.setUp();
		
		// Check if the value is received when no stopped
		unbufferedPublisher.startSending();
		expectedValues = new CountDownLatch(1);
		
		List<Sample> samples = Arrays.asList(new Sample(Integer.valueOf(67)));
		FilteredValue v = new FilteredValue("OneID", Integer.valueOf(67), samples, System.currentTimeMillis());
		publishedValues.put(v.id,v);
		Optional<FilteredValue> optVal = Optional.of(v);
		unbufferedPublisher.offer(optVal);
		try {
			assertTrue(expectedValues.await(2*BufferedPublisherBase.throttlingTime, TimeUnit.MILLISECONDS));
		} catch (InterruptedException ie) {
			Thread.currentThread().interrupt();
		}
		
		// Check if the value is NOT received when stopped
		unbufferedPublisher.stopSending();
		expectedValues = new CountDownLatch(1);
		publishedValues.put(v.id,v);
		unbufferedPublisher.offer(Optional.of(v));
		try {
			assertFalse(expectedValues.await(2*BufferedPublisherBase.throttlingTime, TimeUnit.MILLISECONDS));
		} catch (InterruptedException ie) {
			Thread.currentThread().interrupt();
		}
		
		// Enable the sending again and check if the value is published
		unbufferedPublisher.startSending();
		expectedValues = new CountDownLatch(1);
		publishedValues.put(v.id,v);
		unbufferedPublisher.offer(Optional.of(v));
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
		numOfPublishInvocationInUnbufferedPub.incrementAndGet();
		receivedValuesFromUnbufferedPub.put(mpData.getId(), mpData);
		if (expectedValues!=null) {
			expectedValues.countDown();
		}
		
	}
}
