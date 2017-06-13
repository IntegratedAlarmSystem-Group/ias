package org.eso.ias.plugin.test.publisher;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.eso.ias.plugin.Sample;
import org.eso.ias.plugin.filter.FilteredValue;
import org.eso.ias.plugin.publisher.BufferedMonitoredSystemData;
import org.eso.ias.plugin.publisher.BufferedPublisherBase;
import org.eso.ias.plugin.publisher.MonitorPointDataToBuffer;
import org.eso.ias.plugin.publisher.PublisherException;
import org.eso.ias.plugin.publisher.impl.BufferedListenerPublisher;
import org.junit.Test;
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
		
		List<Sample> samples = Arrays.asList(new Sample(Integer.valueOf(67)));
		FilteredValue v = new FilteredValue("OneID", Integer.valueOf(67), samples, System.currentTimeMillis());
		publishedValues.put(v.id,v);
		Optional<FilteredValue> optVal = Optional.of(v);
		bufferedPublisher.offer(optVal);
		try {
			assertTrue(expectedValues.await(2*BufferedPublisherBase.throttlingTime, TimeUnit.MILLISECONDS));
		} catch (InterruptedException ie) {
			Thread.currentThread().interrupt();
		}
		assertEquals(1L,bufferedPublisher.getPublishedMonitorPoints());
		assertEquals(bufferedPublisher.getPublishedMessages(),numOfPublishInvocationInBufferedPub.get());
		MonitorPointDataToBuffer d = receivedValuesFromBufferedPub.get(v.id);
		assertNotNull("Expected value not published",d);
		assertTrue("Offered and published values do not match "+v.toString()+"<->"+d.toString(), match(v,d));
		
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
		
		List<Sample> samples = Arrays.asList(new Sample(Integer.valueOf(67)));
		
		List<FilteredValue> values = Arrays.asList(
				new FilteredValue("FV-ID1", Integer.valueOf(67), samples, System.currentTimeMillis()),
				new FilteredValue("FV-ID2", Long.valueOf(123), samples, System.currentTimeMillis()),
				new FilteredValue("FV-ID3", "A string", samples, System.currentTimeMillis()),
				new FilteredValue("FV-ID4", Boolean.valueOf(true), samples, System.currentTimeMillis()),
				new FilteredValue("FV-ID5", Integer.valueOf(11), samples, System.currentTimeMillis()));

		for (FilteredValue v: values) {
			publishedValues.put(v.id, v);
			bufferedPublisher.offer(Optional.of(v));
		};
		
		try {
			assertTrue(expectedValues.await(2*BufferedPublisherBase.throttlingTime, TimeUnit.MILLISECONDS));
		} catch (InterruptedException ie) {
			Thread.currentThread().interrupt();
		}
		assertEquals(values.size(),bufferedPublisher.getPublishedMonitorPoints());
		assertEquals(bufferedPublisher.getPublishedMessages(),numOfPublishInvocationInBufferedPub.get());
		
		for (FilteredValue v: values) {
			MonitorPointDataToBuffer d = receivedValuesFromBufferedPub.get(v.id);
			assertNotNull("Expected value not published",d);
			assertTrue("Offered and published values do not match", match(v,d));
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
		
		List<FilteredValue> fValues = generateFileteredValues(valuesToOffer,"BaseID-",true,11,3);
		FilteredValue lastOffered=null;
		for (FilteredValue v: fValues) {
			publishedValues.put(v.id,v);
			bufferedPublisher.offer(Optional.of(v));
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
		assertNotNull("Expected value not published",d);
		assertTrue("Offered and published values do not match", match(lastOffered,d));
	}
}
