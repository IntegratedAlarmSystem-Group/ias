package org.eso.ias.plugin.test.publisher;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

import org.eso.ias.plugin.Sample;
import org.eso.ias.plugin.ValueToSend;
import org.eso.ias.plugin.filter.Filter.ValidatedSample;
import org.eso.ias.plugin.publisher.BufferedMonitoredSystemData;
import org.eso.ias.plugin.publisher.BufferedPublisherBase;
import org.eso.ias.plugin.publisher.MonitorPointData;
import org.eso.ias.plugin.publisher.MonitorPointDataToBuffer;
import org.eso.ias.plugin.publisher.MonitorPointSender;
import org.eso.ias.plugin.publisher.PublisherException;
import org.eso.ias.plugin.publisher.impl.BufferedListenerPublisher;
import org.eso.ias.plugin.publisher.impl.BufferedListenerPublisher.PublisherEventsListener;
import org.eso.ias.plugin.publisher.impl.ListenerPublisher;
import org.eso.ias.plugin.thread.PluginThreadFactory;
import org.eso.ias.prototype.input.java.IasValidity;
import org.junit.After;
import org.junit.Before;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Base class for testing the publisher.
 * <P>
 * <code>PublisherTestCommon</code> offers a buffered ({@link #bufferedPublisher})
 * and a unbuffered ({@link #unbufferedPublisher}) publisher.
 * <P>
 * A more elegant solution could be that of using only one object implementing the {@link MonitorPointSender}
 * interface.
 * 
 * @author acaproni
 *
 */
public class PublisherTestCommon implements PublisherEventsListener, org.eso.ias.plugin.publisher.impl.ListenerPublisher.PublisherEventsListener {
	
	/**
	 * <code>ValuesProducerCallable</code> allows to concurrently push values in the publisher.
	 * 
	 * @author acaproni
	 *
	 */
	protected class ValuesProducerCallable implements Callable<Integer> {
		
		/**
		 * The publisher to offer the values to
		 */
		private final MonitorPointSender mpSender;
		
		/**
		 * The map to save the values published for checking
		 */
		private final Map<String, ValueToSend> publishedValuesMap;
		
		/**
		 * The list of values to publish
		 */
		private final List<ValueToSend> values;
		
		/**
		 * Constructor
		 * 
		 * @param publisher The publisher to offer the values to
		 * @param publishedValues The map to save the values published for checking
		 */
		public ValuesProducerCallable(MonitorPointSender publisher, List<ValueToSend> values, Map<String, ValueToSend> publishedValues) {
			assertNotNull(publisher);
			assertNotNull(publishedValues);
			assertNotNull(values);
			assertFalse(values.isEmpty());
			this.mpSender=publisher;
			this.publishedValuesMap=publishedValues;
			this.values=values;
		}
		
		/**
		 * Push the values in the list into the publisher
		 * 
		 * @see Callable#call()
		 */
		@Override
		public Integer call() throws PublisherException {
			logger.info("Going to submit {} values",values.size());
			int published=0;
			for (ValueToSend fv : values) {
				publishedValuesMap.put(fv.id, fv);
				mpSender.offer(fv);
				published++;
			}
			logger.info("Terminating: {} values pushed",published);
			return Integer.valueOf(published);
		}
		
	}
	
	/**
	 * The logger
	 */
	private final static Logger logger = LoggerFactory.getLogger(PublisherTestCommon.class);
	
	/**
	 * The ID of the plugin for testing
	 */
	protected final String pluginId = "IAS-Publisher-Test-ID";
	
	/**
	 * The ID of the system monitored bu the plugin 
	 */
	protected final String monitoredSystemId = "Monitored-System-ID";
	
	/**
	 * The name of a server of the plugin for testing
	 */
	protected final String pluginServerName = "iasdev.hq.eso.org";
	
	/**
	 * The port of the server of the plugin for testing
	 */
	protected final int pluginServerPort = 12345;
	
	/**
	 * The FileterdValues to be offered to the {@link BufferedPublisherBase}
	 */
	protected final Map<String, ValueToSend> publishedValues = Collections.synchronizedMap(new HashMap<>());
	
	/**
	 * The FileterdValues {@link ListenerPublisher#publish(BufferedMonitoredSystemData)}
	 * to be sent to the core from the buffered publisher {@link bufferedPublisher}
	 */
	protected final Map<String, MonitorPointDataToBuffer> receivedValuesFromBufferedPub = Collections.synchronizedMap(new HashMap<>());
	
	/**
	 * The FileterdValues {@link ListenerPublisher#publish(BufferedMonitoredSystemData)}
	 * to be sent to the core from the buffered publisher {@link #unbufferedPublisher}
	 */
	protected final Map<String, MonitorPointData> receivedValuesFromUnbufferedPub = Collections.synchronizedMap(new HashMap<>());
	
	/**
	 * The latch to wait for the expected number of values
	 * to be sent to {@link ListenerPublisher#publish(BufferedMonitoredSystemData)}.
	 * <P>
	 * This is not the number of messages, but the number of {@link ValueToSend}
	 * objects as the {@link BufferedPublisherBase} could group more values into the same
	 * {@link BufferedMonitoredSystemData}.
	 * <P>
	 * The latch is not used by all tests.
	 */
	protected CountDownLatch expectedValues=null;
	
	/**
	 * Record the number of times publish has been called in the buffered publisher
	 */
	protected final AtomicInteger numOfPublishInvocationInBufferedPub= new AtomicInteger(0);
	
	/**
	 * Record the number of times publish has been called in the unbuffered publisher
	 */
	protected AtomicInteger numOfPublishInvocationInUnbufferedPub= new AtomicInteger(0);
	
	/**
	 * The (buffered or unbuffered) object to test
	 */
	protected BufferedListenerPublisher bufferedPublisher;
	
	/**
	 * The buffered object to test
	 */
	protected ListenerPublisher unbufferedPublisher;
	
	/**
	 * The thread factory
	 */
	protected ThreadFactory threadFactory = new PluginThreadFactory();
	
	/**
	 * The scheduled executor service
	 */
	protected ScheduledExecutorService schedExecutorSvc= Executors.newScheduledThreadPool(
			Runtime.getRuntime().availableProcessors()/2,
			threadFactory);
	
	/**
	 * Generates and return a list of filtered values.
	 * 
	 * @parm n - the number of values to generate
	 * @param baseId - the base of the ID of each value
	 * @param singleID - if <code>true</code> all generated values have the same ID
	 *                   otherwise a progressive number is appended to the baseId
	 * @param baseValue -  the value of the ValueToSend
	 * @param valueInc - the increment of each consecutive value (if 0 all
	 * 					 the generated values have the same baseValue value)   
	 * @return a list of newly generated values ready to 
	 *         be offered to the publisher
	 */
	public static List<ValueToSend> generateValuesToSend(
			int numOfValues, 
			String baseId,
			boolean singleID,
			long baseValue,
			long valueInc) {
		List<ValueToSend> ret = new LinkedList<>();
		List<ValidatedSample> samples = new LinkedList<>();
		int idCounter=0;
		long valueCounter=baseValue;
		for (int t=0; t<numOfValues; t++) {
			String id = (singleID)?baseId:baseId+idCounter++;
			long value=valueCounter;
			valueCounter+=valueInc;
			samples.clear();
			samples.add(new ValidatedSample(new Sample(Long.valueOf(value)),IasValidity.RELIABLE));
			ValueToSend fv = new ValueToSend(id, Long.valueOf(value), samples, System.currentTimeMillis());
			ret.add(fv);
		}
		return ret;
	}
	
	/**
	 * Compare a {@link ValueToSend} (i.e. the value offered) with a
	 * {@link MonitorPointDataToBuffer} (i.e. the value to be sent to the IAS core)
	 *  
	 * @param v The not <code>null</code> value offered
	 * @param d The not <code>null</code> value to be sent to the core
	 * @return <code>true</code> if v and d matches
	 */
	public static boolean match(ValueToSend v, MonitorPointDataToBuffer d) {
		assertNotNull(v);
		assertNotNull(d);
		
		/**
		 * ISO 8601 date formatter
		 */
		SimpleDateFormat iso8601dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.S");
		
		boolean ret = v.id.equals(d.getId());
		ret = ret && v.value.toString().equals(d.getValue());
		ret = ret && iso8601dateFormat.format(new Date(v.filteredTimestamp)).equals(d.getFilteredTime());
		ret = ret && iso8601dateFormat.format(new Date(v.producedTimestamp)).equals(d.getSampleTime());
		ret = ret && v.operationalMode.toString().toUpperCase().equals(d.getOperationalMode());
		ret = ret && v.iasValidity.toString().toUpperCase().equals(d.getValidity());
		
		if (!ret) {
			logger.error("The {} and the {} do not match!",v.toString(),d.toString());
		}
		return ret;
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
	 * Data received from the {@link bufferedPublisher} buffered receiver
	 * 
	 * @see org.eso.ias.plugin.test.publisher.PublisherEventsListener#dataReceived(org.eso.ias.plugin.publisher.BufferedMonitoredSystemData)
	 */
	@Override
	public void dataReceived(BufferedMonitoredSystemData data) {
		assertNotNull(data);
		assertEquals("ID differs",data.getSystemID(), pluginId);
		assertNotNull(data.getPublishTime());
		assertFalse(data.getPublishTime().isEmpty());
		assertTrue("There must be at least one monitor point value in a message",data.getMonitorPoints().size()>0);
		assertTrue("The number of monitor point values in a message acn't be geater then the max size of the buffer",
				data.getMonitorPoints().size()<=BufferedPublisherBase.maxBufferSize);
		numOfPublishInvocationInBufferedPub.incrementAndGet();
		logger.info("{} monitor points received from {}",data.getMonitorPoints().size(),data.getSystemID());
		for (MonitorPointDataToBuffer d: data.getMonitorPoints()) {
			receivedValuesFromBufferedPub.put(d.getId(), d);
			if (expectedValues!=null) {
				expectedValues.countDown(); 
			}
		}
	}
	
	/** Data received from the {@link #unbufferedPublisher} unbuffered receiver
	 * 
	 * @see org.eso.ias.plugin.publisher.impl.ListenerPublisher.PublisherEventsListener#dataReceived(org.eso.ias.plugin.publisher.MonitorPointData)
	 */
	@Override
	public void dataReceived(MonitorPointData mpData) {
		assertNotNull(mpData);
		assertEquals("ID differs",mpData.getPluginID(), pluginId);
		assertNotNull(mpData.getPublishTime());
		assertFalse(mpData.getPublishTime().isEmpty());
		numOfPublishInvocationInUnbufferedPub.incrementAndGet();
		receivedValuesFromUnbufferedPub.put(mpData.getId(), mpData);
		if (expectedValues!=null) {
			expectedValues.countDown(); 
		}
	}
	
	@Before
	public void setUp() {
		// Build the publisher
		int poolSize = Runtime.getRuntime().availableProcessors()/2;
		bufferedPublisher = new BufferedListenerPublisher(pluginId, monitoredSystemId, pluginServerName, pluginServerPort, schedExecutorSvc,this);
		unbufferedPublisher = new ListenerPublisher(pluginId, monitoredSystemId,pluginServerName, pluginServerPort, schedExecutorSvc,this);
		logger.debug("Set up");
	}
	
	@After
	public void tearDown() throws PublisherException {
		logger.debug("Releasing resource");
		receivedValuesFromBufferedPub.clear();
		publishedValues.clear();
		bufferedPublisher.stopSending();
		bufferedPublisher.tearDown();
		unbufferedPublisher.stopSending();
		unbufferedPublisher.tearDown();
		schedExecutorSvc.shutdown();
		logger.debug("tearDown complete");
	}

	
}
