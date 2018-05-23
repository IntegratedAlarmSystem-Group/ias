package org.eso.ias.plugin.test.publisher;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.eso.ias.types.IasValidity;
import org.eso.ias.types.OperationalMode;
import org.eso.ias.plugin.Sample;
import org.eso.ias.plugin.ValueToSend;
import org.eso.ias.plugin.filter.Filter.EnrichedSample;
import org.eso.ias.plugin.publisher.BufferedMonitoredSystemData;
import org.eso.ias.plugin.publisher.BufferedPublisherBase;
import org.eso.ias.plugin.publisher.MonitorPointDataToBuffer;
import org.eso.ias.plugin.publisher.PublisherException;
import org.eso.ias.plugin.publisher.impl.BufferedListenerPublisher;
import org.eso.ias.plugin.publisher.impl.ListenerPublisher;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Test the {@link BufferedPublisherBase}.
 * <P>
 * <code>PublisherBaseTest</code> instantiates a {@link BufferedListenerPublisher} object
 * to be notified about events of the {@link BufferedPublisherBase}.
 * <P>
 * To check the publishing of monitor points, an arbitrary number of {@link FilteredValue} 
 * objects is sent to the {@link BufferedPublisherBase#offer(java.util.Optional)}.
 * The test checks if all the messages are routed to the {@link BufferedListenerPublisher} by means of a 
 * {@link CountDownLatch}. Received messages are stored in a Map, for comparison with the ones
 * being offered.
 * 
 * @author acaproni
 *
 */
public class BufferedPublisherBaseTest extends PublisherTestCommon {
	
	/**
	 * The logger
	 */
	private final static Logger logger = LoggerFactory.getLogger(BufferedPublisherBaseTest.class);
	

	
	/**
	 * Check if {@link BufferedListenerPublisher#publish(BufferedMonitoredSystemData)} is invoked to 
	 * publish just one {@link FilteredValue}.
	 * <P> 
	 * It also checks if the offered and the received values match.
	 * 
	 * @throws PublisherException
	 */
	@Test
	public void testPublishOneValue() throws PublisherException {
		bufferedPublisher.setUp();
		bufferedPublisher.startSending();
		expectedValues = new CountDownLatch(1);
		
		List<EnrichedSample> samples = Arrays.asList(new EnrichedSample(new Sample(Integer.valueOf(67)),true));
		ValueToSend v = new ValueToSend(
				"OneID", 
				Integer.valueOf(67), 
				samples, 
				System.currentTimeMillis(),
				OperationalMode.DEGRADED,
				IasValidity.RELIABLE);
		publishedValues.put(v.id,v);
		bufferedPublisher.offer(v);
		try {
			assertTrue(expectedValues.await(2*BufferedPublisherBase.throttlingTime, TimeUnit.MILLISECONDS));
		} catch (InterruptedException ie) {
			Thread.currentThread().interrupt();
		}
		assertEquals(1L,bufferedPublisher.getPublishedMonitorPoints());
		assertEquals(bufferedPublisher.getPublishedMessages(),numOfPublishInvocationInBufferedPub.get());
		MonitorPointDataToBuffer d = receivedValuesFromBufferedPub.get(v.id);
		assertNotNull(d,"Expected value not published");
		assertTrue( match(v,d),"Offered and published values do not match "+v.toString()+"<->"+d.toString());
		
	}
	
	/**
	 * Check if {@link ListenerPublisher#publish(BufferedMonitoredSystemData)} is invoked only once
	 * to one {@link FilteredValue} (i.e. it checks for repeated publications of the same value)
	 * 
	 * @throws PublisherException
	 */
	@Test
	public void testPublishOneValueOnlyOnce() throws PublisherException {
		bufferedPublisher.setUp();
		bufferedPublisher.startSending();
		assertEquals(0L,bufferedPublisher.getPublishedMessages());
		
		Integer val = Integer.valueOf(67);
		List<EnrichedSample> samples = Arrays.asList(new EnrichedSample(new Sample(val),true));
		ValueToSend v = new ValueToSend("OneID", val, samples, System.currentTimeMillis(),OperationalMode.UNKNOWN,IasValidity.RELIABLE);
		publishedValues.put(v.id,v);
		bufferedPublisher.offer(v);
		
		try {
			Thread.sleep(10*BufferedPublisherBase.throttlingTime);
		} catch (InterruptedException ie) {
			Thread.currentThread().interrupt();
		}
		assertEquals(1L,bufferedPublisher.getPublishedMessages());
	}
	
	/**
	 * Check if {@link BufferedListenerPublisher#publish(BufferedMonitoredSystemData)} is invoked to 
	 * publish all the {@link FilteredValue}.
	 * <P> 
	 * It also checks if the offered and the received values match.
	 * 
	 * @throws PublisherException
	 */
	@Test
	public void testPublishManyValues() throws PublisherException {
		bufferedPublisher.setUp();
		bufferedPublisher.startSending();
		expectedValues = new CountDownLatch(5);
		
		List<EnrichedSample> samples = Arrays.asList(new EnrichedSample(new Sample(Integer.valueOf(67)),true));
		
		List<ValueToSend> values = Arrays.asList(
				new ValueToSend("FV-ID1", Integer.valueOf(67), samples, System.currentTimeMillis(),OperationalMode.UNKNOWN,IasValidity.RELIABLE),
				new ValueToSend("FV-ID2", Long.valueOf(123), samples, System.currentTimeMillis(),OperationalMode.UNKNOWN,IasValidity.RELIABLE),
				new ValueToSend("FV-ID3", "A string", samples, System.currentTimeMillis(),OperationalMode.UNKNOWN,IasValidity.RELIABLE),
				new ValueToSend("FV-ID4", Boolean.valueOf(true), samples, System.currentTimeMillis(),OperationalMode.UNKNOWN,IasValidity.RELIABLE),
				new ValueToSend("FV-ID5", Integer.valueOf(11), samples, System.currentTimeMillis(),OperationalMode.UNKNOWN,IasValidity.RELIABLE));

		for (ValueToSend v: values) {
			publishedValues.put(v.id, v);
			bufferedPublisher.offer(v);
		};
		
		try {
			assertTrue(expectedValues.await(2*BufferedPublisherBase.throttlingTime, TimeUnit.MILLISECONDS));
		} catch (InterruptedException ie) {
			Thread.currentThread().interrupt();
		}
		assertEquals(values.size(),bufferedPublisher.getPublishedMonitorPoints());
		assertEquals(bufferedPublisher.getPublishedMessages(),numOfPublishInvocationInBufferedPub.get());
		
		for (ValueToSend v: values) {
			MonitorPointDataToBuffer d = receivedValuesFromBufferedPub.get(v.id);
			assertNotNull(d,"Expected value not published");
			assertTrue( match(v,d),"Offered and published values do not match");
		}
	}
	
	/**
	 * Check if {@link BufferedListenerPublisher#publish(BufferedMonitoredSystemData)} is invoked to 
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
		bufferedPublisher.setUp();
		bufferedPublisher.startSending();
		
		final int valuesToOffer=5000;
		
		List<ValueToSend> fValues = generateValuesToSend(valuesToOffer,"BaseID-",true,11,3);
		ValueToSend lastOffered=null;
		for (ValueToSend v: fValues) {
			publishedValues.put(v.id,v);
			bufferedPublisher.offer(v);
			lastOffered=v;
		}
		
		try {
			Thread.sleep(2*BufferedPublisherBase.throttlingTime);
		} catch (InterruptedException ie) {
			Thread.currentThread().interrupt();
		}
		
		logger.info("Last offered value: {}",lastOffered.toString());
		
		assertEquals(bufferedPublisher.getPublishedMessages(),numOfPublishInvocationInBufferedPub.get());
		assertEquals(publishedValues.size(), receivedValuesFromBufferedPub.size());
		
		MonitorPointDataToBuffer d = receivedValuesFromBufferedPub.get(lastOffered.id);
		assertNotNull(d,"Expected value not published");
		assertTrue( match(lastOffered,d),"Offered and published values do not match");
	}
}
