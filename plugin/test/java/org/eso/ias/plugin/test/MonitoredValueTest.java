package org.eso.ias.plugin.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.eso.ias.plugin.ChangeValueListener;
import org.eso.ias.plugin.MonitoredValue;
import org.eso.ias.plugin.Sample;
import org.eso.ias.plugin.filter.FilteredValue;
import org.eso.ias.plugin.filter.NoneFilter;
import org.eso.ias.plugin.thread.PluginScheduledExecutorSvc;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Test the {@link MonitoredValue}.
 * <P>
 * As the scope is to test the {@link MonitoredValue}, we use a {@link NoneFilter}
 * being not really interested about the published value.
 * 
 * @author acaproni
 */
public class MonitoredValueTest implements ChangeValueListener {
	
	/**
	 * The logger
	 */
	private static final Logger logger = LoggerFactory.getLogger(MonitoredValueTest.class);
	
	/**
	 * The scheduled executor service
	 */
	private static final PluginScheduledExecutorSvc schedExeSvc = PluginScheduledExecutorSvc.getInstance();
	
	/**
	 * The object to test
	 */
	private MonitoredValue mVal;
	
	/**
	 * The refresh rate of the monitor point in msec
	 * <P>
	 * This value must be chosen big enough to let the value
	 * submit a values before the timer takes control
	 */
	private final long refreshRate = 1000;
	
	/**
	 * The samaphore to wait for the sending of events
	 */
	private CountDownLatch countDownLatch;
	
	/**
	 * The {@link FilteredValue}s notified to the listener
	 */
	private final List<FilteredValue> receivedValues = new LinkedList<>();
	
	/**
	 * The identifier
	 */
	private static final String mValueId= "A-Valid-ID";
	
	@Before
	public void setUp() {
		mVal = new MonitoredValue(mValueId, refreshRate, schedExeSvc, this);
	}
	
	@After
	public void tearDown() {
		mVal.enablePeriodicNotification(false);

	}
	
	@AfterClass
	public static void shutdownAll() {
		schedExeSvc.shutdown();
	}
	
	/**
	 * Test if the value is sent after submitting a new
	 * sample.
	 *  
	 */
	@Test
	public void testUpdateValue() throws Exception {
		countDownLatch = new CountDownLatch(1);
		Sample s = new Sample(Integer.valueOf(127));
		mVal.submitSample(s);
		// Wait but not enough to let the timer shot
		assertTrue(countDownLatch.await(refreshRate/2, TimeUnit.MILLISECONDS));
		assertEquals(1L, receivedValues.size());
		assertEquals(s.value, receivedValues.get(0).value);
		assertEquals(mValueId,receivedValues.get(0).id);
	}
	
	/**
	 * Test if all the values are sent after submitting a new
	 * sample.
	 *  
	 */
	@Test
	public void testUpdateValues() throws Exception {
		int samplesToSend=15;
		countDownLatch = new CountDownLatch(samplesToSend);
		Sample[] samples = new Sample[samplesToSend];
		long tStamp=19975;
		for (int t=0; t<samplesToSend; t++) {
			samples[t]=new Sample(Long.valueOf(13147+t),tStamp++);
			mVal.submitSample(samples[t]);
		}
		assertTrue(countDownLatch.await(refreshRate/2, TimeUnit.MILLISECONDS));
		assertEquals(samplesToSend, receivedValues.size());
		tStamp=19975;
		for (int t=0; t<samplesToSend; t++) {
			FilteredValue v = receivedValues.get(t);
			assertEquals(mValueId,v.id);
			assertEquals(samples[t].value, v.value);
			assertEquals(v.producedTimestamp, tStamp++);
		}
	}
	
	/**
	 * Test if the same value is sent by the timer task
	 */
	@Test
	public void testTimerNotification() throws Exception {
		int expectedNotifications=5;
		countDownLatch = new CountDownLatch(expectedNotifications);
		Sample s = new Sample(Integer.valueOf(135));
		mVal.submitSample(s);
		// Wait but not enough to let the timer shot
		assertTrue(countDownLatch.await((1+expectedNotifications)*refreshRate, TimeUnit.MILLISECONDS));
		assertEquals(expectedNotifications, receivedValues.size());
	}
	
	/**
	 * Test the enabling/disabling of the periodic notification
	 */
	@Test
	public void testPeriodicNotification() throws Exception {
		Sample s = new Sample(Integer.valueOf(17751));
		mVal.submitSample(s);
		mVal.enablePeriodicNotification(false);
		// Give time to send the value as effect of the submit
		Thread.sleep(1000);
		countDownLatch = new CountDownLatch(1);
		// Check that notifications are disabled 
		assertFalse(countDownLatch.await(2000, TimeUnit.MILLISECONDS));
		mVal.enablePeriodicNotification(true);
		assertTrue(countDownLatch.await(2000, TimeUnit.MILLISECONDS));
	}
	

	/**
	 * @see org.eso.ias.plugin.ChangeValueListener#monitoredValueUpdated(java.util.Optional)
	 */
	@Override
	public void monitoredValueUpdated(Optional<FilteredValue> value) {
		assertNotNull(value);
		value.ifPresent(v -> {
			logger.info("{} value {} sent to the core of the IAS ",v.id,v.value.toString());
			receivedValues.add(v);
			if (countDownLatch!=null) {
				countDownLatch.countDown();
			}
		});
		
	}
}
