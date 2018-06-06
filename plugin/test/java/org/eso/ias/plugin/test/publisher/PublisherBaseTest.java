package org.eso.ias.plugin.test.publisher;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.eso.ias.plugin.Sample;
import org.eso.ias.plugin.ValueToSend;
import org.eso.ias.plugin.filter.Filter.EnrichedSample;
import org.eso.ias.plugin.filter.FilteredValue;
import org.eso.ias.plugin.publisher.BufferedMonitoredSystemData;
import org.eso.ias.plugin.publisher.BufferedPublisherBase;
import org.eso.ias.plugin.publisher.MonitorPointData;
import org.eso.ias.plugin.publisher.PublisherBase;
import org.eso.ias.plugin.publisher.PublisherException;
import org.eso.ias.plugin.publisher.impl.ListenerPublisher;
import org.eso.ias.types.IasValidity;
import org.eso.ias.types.OperationalMode;
import org.junit.jupiter.api.Test;
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
		assertEquals(pluginId,unbufferedPublisher.pluginId,"Plugin-IDs differ");
		assertEquals(pluginServerName,unbufferedPublisher.serverName,"Servers differ");
		assertEquals(pluginServerPort,unbufferedPublisher.serverPort,"Servers ports");
		assertEquals(BufferedPublisherBase.defaultBufferSize,BufferedPublisherBase.maxBufferSize,"Default buffer size");
		assertEquals(BufferedPublisherBase.defaultThrottlingTime,BufferedPublisherBase.throttlingTime,"Default throttling time");
	}

	/**
	 * Test if the user provided initialization method is invoked
	 * while initializing the publisher
	 */
	@Test
	public void testSetUp() throws PublisherException {
		unbufferedPublisher.setUp();
		assertEquals( 1L, unbufferedPublisher.getNumOfSetUpInvocations(),"Not initilized");
	}
	
	/**
	 * Test if the initializing the publisher more then once throws an exception
	 */
	@Test
	public void testSetUpTwice() throws PublisherException {
		unbufferedPublisher.setUp();
		assertEquals(1L, unbufferedPublisher.getNumOfSetUpInvocations(),"Not initilized");
		assertThrows(PublisherException.class, () -> unbufferedPublisher.setUp());
	}
	
	/**
	 * Test if initializing a closed publisher throws an exception
	 */
	@Test
	public void testSetUpWhenClosed() throws PublisherException {
		unbufferedPublisher.setUp();
		assertEquals(1L, unbufferedPublisher.getNumOfSetUpInvocations(),"Not initilized");
		
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
		assertThrows(PublisherException.class, () -> unbufferedPublisher.setUp());
	}
	
	/**
	 * Test if the user provided shutdown method is invoked
	 * while shutting down the publisher
	 */
	@Test
	public void testShutdown() throws PublisherException {
		assertEquals(0L,unbufferedPublisher.getNumOfTearDownInvocations(),"tearDown count wrong");
		unbufferedPublisher.tearDown();
		assertEquals(1L,unbufferedPublisher.getNumOfTearDownInvocations(),"tearDown not executed");
		unbufferedPublisher.tearDown();
		unbufferedPublisher.tearDown();
		unbufferedPublisher.tearDown();
		assertEquals(1L,unbufferedPublisher.getNumOfTearDownInvocations(),"tearDown executed more then once");
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
		
		List<EnrichedSample> samples = Arrays.asList(new EnrichedSample(new Sample(Integer.valueOf(67)),true));
		ValueToSend v = new ValueToSend("OneID", Integer.valueOf(67), samples, System.currentTimeMillis(),OperationalMode.OPERATIONAL,IasValidity.RELIABLE);
		publishedValues.put(v.id,v);
		unbufferedPublisher.offer(v);
		try {
			assertTrue(expectedValues.await(2*BufferedPublisherBase.throttlingTime, TimeUnit.MILLISECONDS));
		} catch (InterruptedException ie) {
			Thread.currentThread().interrupt();
		}
		assertEquals(1L,unbufferedPublisher.getPublishedMessages());
		MonitorPointData d = receivedValuesFromUnbufferedPub.get(v.id);
		assertNotNull(d,"Expected value not published");
		assertTrue(PublisherTestCommon.match(v,d),"Offered and published values do not match "+v.toString()+"<->"+d.toString());
		assertEquals(pluginId,d.getPluginID());
		assertEquals(monitoredSystemId,d.getMonitoredSystemID());
		assertTrue(d.getPublishTime()!=null && !d.getPublishTime().isEmpty());	
	}
	
	/**
	 * Check if {@link ListenerPublisher#publish(BufferedMonitoredSystemData)} is invoked only once
	 * to one {@link FilteredValue} (i.e. it checks for repeated publications of the same value)
	 * 
	 * @throws PublisherException
	 */
	@Test
	public void testPublishOneValueOnlyOnce() throws PublisherException {
		unbufferedPublisher.setUp();
		unbufferedPublisher.startSending();
		assertEquals(0L,unbufferedPublisher.getPublishedMessages());
		
		Integer val = Integer.valueOf(67);
		List<EnrichedSample> samples = Arrays.asList(new EnrichedSample(new Sample(val),true));
		ValueToSend v = new ValueToSend("OneID", val, samples, System.currentTimeMillis(),OperationalMode.UNKNOWN,IasValidity.RELIABLE);
		publishedValues.put(v.id,v);
		unbufferedPublisher.offer(v);
		
		try {
			Thread.sleep(10*BufferedPublisherBase.throttlingTime);
		} catch (InterruptedException ie) {
			Thread.currentThread().interrupt();
		}
		assertEquals(1L,unbufferedPublisher.getPublishedMessages());
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
		
		List<EnrichedSample> samples = Arrays.asList(new EnrichedSample(new Sample(Integer.valueOf(67)),true));
		
		List<ValueToSend> values = Arrays.asList(
				new ValueToSend("FV-ID1", Integer.valueOf(67), samples, System.currentTimeMillis(),OperationalMode.UNKNOWN,IasValidity.RELIABLE),
				new ValueToSend("FV-ID2", Long.valueOf(123), samples, System.currentTimeMillis(),OperationalMode.OPERATIONAL,IasValidity.RELIABLE),
				new ValueToSend("FV-ID3", "A string", samples, System.currentTimeMillis(),OperationalMode.DEGRADED,IasValidity.RELIABLE),
				new ValueToSend("FV-ID4", Boolean.valueOf(true), samples, System.currentTimeMillis(),OperationalMode.UNKNOWN,IasValidity.RELIABLE),
				new ValueToSend("FV-ID5", Integer.valueOf(11), samples, System.currentTimeMillis(),OperationalMode.CLOSING,IasValidity.RELIABLE));

		for (ValueToSend v: values) {
			publishedValues.put(v.id, v);
			unbufferedPublisher.offer(v);
		};
		
		try {
			assertTrue(expectedValues.await(2*BufferedPublisherBase.throttlingTime, TimeUnit.MILLISECONDS));
		} catch (InterruptedException ie) {
			Thread.currentThread().interrupt();
		}
		assertEquals(values.size(),unbufferedPublisher.getPublishedMessages());
		assertEquals(unbufferedPublisher.getPublishedMessages(),numOfPublishInvocationInUnbufferedPub.get());
		
		for (ValueToSend v: values) {
			MonitorPointData d = receivedValuesFromUnbufferedPub.get(v.id);
			assertNotNull(d,"Expected value not published");
			assertTrue(PublisherTestCommon.match(v,d),"Offered and published values do not match");
			assertEquals(pluginId,d.getPluginID());
			assertEquals(monitoredSystemId,d.getMonitoredSystemID());
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
		
		List<ValueToSend> fValues = PublisherTestCommon.generateValuesToSend(valuesToOffer,"BaseID-",true,11,3);
		ValueToSend lastOffered=null;
		for (ValueToSend v: fValues) {
			publishedValues.put(v.id,v);
			unbufferedPublisher.offer(v);
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
		assertNotNull(d,"Expected value not published");
		assertTrue(PublisherTestCommon.match(lastOffered,d),"Offered and published values do not match");
		assertEquals(pluginId,d.getPluginID());
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
		
		List<EnrichedSample> samples = Arrays.asList(new EnrichedSample(new Sample(Integer.valueOf(67)),true));
		ValueToSend v = new ValueToSend("OneID", Integer.valueOf(67), samples, System.currentTimeMillis(),OperationalMode.UNKNOWN,IasValidity.RELIABLE);
		publishedValues.put(v.id,v);
		unbufferedPublisher.offer(v);
		try {
			assertTrue(expectedValues.await(2*BufferedPublisherBase.throttlingTime, TimeUnit.MILLISECONDS));
		} catch (InterruptedException ie) {
			Thread.currentThread().interrupt();
		}
		
		// Check if the value is NOT received when stopped
		unbufferedPublisher.stopSending();
		expectedValues = new CountDownLatch(1);
		publishedValues.put(v.id,v);
		unbufferedPublisher.offer(v);
		try {
			assertFalse(expectedValues.await(2*BufferedPublisherBase.throttlingTime, TimeUnit.MILLISECONDS));
		} catch (InterruptedException ie) {
			Thread.currentThread().interrupt();
		}
		
		// Enable the sending again and check if the value is published
		unbufferedPublisher.startSending();
		expectedValues = new CountDownLatch(1);
		publishedValues.put(v.id,v);
		unbufferedPublisher.offer(v);
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
