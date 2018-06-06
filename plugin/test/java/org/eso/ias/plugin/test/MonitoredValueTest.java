package org.eso.ias.plugin.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

import org.eso.ias.plugin.ChangeValueListener;
import org.eso.ias.plugin.MonitoredValue;
import org.eso.ias.plugin.Sample;
import org.eso.ias.plugin.ValueToSend;
import org.eso.ias.plugin.filter.NoneFilter;
import org.eso.ias.plugin.thread.PluginThreadFactory;
import org.eso.ias.types.IasValidity;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
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
	 * The thread factory
	 */
	protected static ThreadFactory threadFactory = new PluginThreadFactory();
	
	/**
	 * The scheduled executor service
	 */
	private static final ScheduledExecutorService schedExecutorSvc = Executors.newScheduledThreadPool(
			Runtime.getRuntime().availableProcessors()/2,
			threadFactory);
	
	/**
	 * The object to test
	 */
	private MonitoredValue mVal;
	
	/**
	 * The refresh rate of the monitor point in msec as it is defined in the
	 * monitored system
	 */
	private final long refreshRate = 1000;
	
	/**
	 * The time to send the value to the BSDB if it did not change
	 */
	private final int iasPeriodicSendTI = 4;
	
	/**
	 * The samaphore to wait for the sending of events
	 */
	private CountDownLatch countDownLatch;
	
	/**
	 * The {@link FilteredValue}s notified to the listener
	 */
	private final List<ValueToSend> receivedValues = new LinkedList<>();
	
	/**
	 * The identifier
	 */
	private static final String mValueId= "A-Valid-ID";
	
	@BeforeEach
	public void setUp() {
		mVal = new MonitoredValue(mValueId, refreshRate, schedExecutorSvc, this,iasPeriodicSendTI);
		mVal.start();
	}
	
	@AfterEach
	public void tearDown() {
		mVal.shutdown();
	}
	
	@AfterAll
	public static void shutdownAll() {
		schedExecutorSvc.shutdown();
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
		assertTrue(countDownLatch.await(iasPeriodicSendTI/2, TimeUnit.SECONDS));
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
			ValueToSend v = receivedValues.get(t);
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
		assertTrue(countDownLatch.await((1+expectedNotifications)*iasPeriodicSendTI, TimeUnit.SECONDS));
		assertEquals(expectedNotifications, receivedValues.size());
	}
	
	/**
	 * Test the enabling/disabling of the periodic notification
	 */
	@Test
	public void testPeriodicNotification() throws Exception {
		Sample s = new Sample(Integer.valueOf(17751));
		logger.info("testPeriodicNotification sumbitted a sample with value {}",s.value.toString());
		mVal.submitSample(s);
		mVal.enablePeriodicNotification(false);
		// Give time to send the value as effect of the submit
		Thread.sleep(1000);
		countDownLatch = new CountDownLatch(1);
		// Check that notifications are disabled 
		logger.info("Is the periodic send disabled?");
		assertFalse(countDownLatch.await(2*iasPeriodicSendTI, TimeUnit.SECONDS));
		mVal.enablePeriodicNotification(true);
		logger.info("Is the periodic send enabled?");
		assertTrue(countDownLatch.await(2*iasPeriodicSendTI, TimeUnit.SECONDS));
		logger.info("testPeriodicNotification done");
	}
	

	/**
	 * @see org.eso.ias.plugin.ChangeValueListener#monitoredValueUpdated(java.util.Optional)
	 */
	@Override
	public void monitoredValueUpdated(ValueToSend value) {
		assertNotNull(value);
		logger.info("{} value {} sent to the core of the IAS ",value.id,value.value.toString());
		receivedValues.add(value);
		if (countDownLatch!=null) {
			countDownLatch.countDown();
		}
	}
	
	/**
	 * Tests if the validity is properly set before sending a Value to the BSDB
	 */
	@Test
	public void testSettingOfValidity() throws Exception {
		logger.info("testSettingOfValidity test started");
		Sample s = new Sample(Integer.valueOf(3));
		
		mVal.submitSample(s);
		while (receivedValues.size()<1) {
			Thread.sleep(50);
		}
		ValueToSend receivedValue = receivedValues.get(0);
		assertEquals(IasValidity.RELIABLE, receivedValue.iasValidity);
		
		Thread.sleep(2*refreshRate);
		while (receivedValues.size()<2) {
			Thread.sleep(50);
		}
		receivedValue = receivedValues.get(1);
		assertEquals(IasValidity.UNRELIABLE, receivedValue.iasValidity);
		
		logger.info("testSettingOfValidity test done");
	}
	
	/**
	 * Checks that a value is not sent if its
	 * value did not change
	 */
	@Test
	public void testSendOnlyDifferentValues() throws Exception {
		logger.info("testSendOnlyDifferentValues test started");
		
		mVal.enablePeriodicNotification(false);
		
		// Sends many time the same value
		for (int t=0; t<=11; t++) {
			Sample s = new Sample(Integer.valueOf(13));
			mVal.submitSample(s);
		}
		Thread.sleep(1000);
		assertEquals(1, receivedValues.size());
		
		logger.info("testSendOnlyDifferentValues done");
	}
	
}
