package org.eso.ias.plugin.test.publisher;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;

import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import org.eso.ias.plugin.Sample;
import org.eso.ias.plugin.filter.FilteredValue;
import org.eso.ias.plugin.publisher.MonitorPointData;
import org.eso.ias.plugin.publisher.MonitoredSystemData;
import org.eso.ias.plugin.publisher.PublisherBase;
import org.eso.ias.plugin.publisher.PublisherException;
import org.eso.ias.plugin.thread.PluginThreadFactory;
import org.junit.After;
import org.junit.Before;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Base class for testing the publisher.
 * 
 * @author acaproni
 *
 */
public class PublisherTestCommon implements PublisherEventsListener {
	
	/**
	 * The logger
	 */
	private final static Logger logger = LoggerFactory.getLogger(PublisherTestCommon.class);
	
	/**
	 * The ID of the plugin for testing
	 */
	protected final String pluginId = "IAS-Publisher-Test-ID";
	
	/**
	 * The name of a server of the plugin for testing
	 */
	protected final String pluginServerName = "iasdev.hq.eso.org";
	
	/**
	 * The port of the server of the plugin for testing
	 */
	protected final int pluginServerPort = 12345;
	
	/**
	 * The FileterdValues to be offered to the {@link PublisherBase}
	 */
	protected final Map<String, FilteredValue> publishedValues = Collections.synchronizedMap(new HashMap<>());
	
	/**
	 * The FileterdValues sent to {@link PublisherTestImpl#publish(MonitoredSystemData)}
	 * to be sent to the core
	 */
	protected final Map<String, MonitorPointData> receivedValues = Collections.synchronizedMap(new HashMap<>());
	
	/**
	 * The latch to wait for the expected number of values
	 * to be sent to {@link PublisherTestImpl#publish(MonitoredSystemData)}.
	 * <P>
	 * This is not the number of messages, but the number of {@link FilteredValue}
	 * objects as the {@link PublisherBase} could group more values into the same
	 * {@link MonitoredSystemData}.
	 * <P>
	 * The latch is not used by all tests.
	 */
	protected CountDownLatch expectedValues=null;
	
	/**
	 * Record the numer of times publish has been called
	 */
	protected int numOfPublishInvocation=0;
	
	/**
	 * The object to test
	 */
	protected PublisherTestImpl publisher;
	
	/**
	 * Generates and return a list of filtered values.
	 * 
	 * @parm n - the number of values to generate
	 * @param baseId - the base of the ID of each value
	 * @param singleID - if <code>true</code> all generated values have the same ID
	 *                   otherwise a progressive number is appended to the baseId
	 * @param baseValue -  the value of the FilteredValue
	 * @param valueInc - the increment of each consecutive value (if 0 all
	 * 					 the generated values have the same baseValue value)   
	 * @return a list of newly generated values ready to 
	 *         be offered to the publisher
	 */
	protected List<FilteredValue> generatedFileteredValues(
			int numOfValues, 
			String baseId,
			boolean singleID,
			long baseValue,
			long valueInc) {
		List<FilteredValue> ret = new LinkedList<>();
		List<Sample> samples = new LinkedList<Sample>();
		int idCounter=0;
		long valueCounter=baseValue;
		for (int t=0; t<numOfValues; t++) {
			String id = (singleID)?baseId:baseId+idCounter++;
			long value=valueCounter;
			valueCounter+=valueInc;
			samples.clear();
			samples.add(new Sample(Long.valueOf(value)));
			FilteredValue fv = new FilteredValue(id, Long.valueOf(value), samples, System.currentTimeMillis());
			ret.add(fv);
		}
		return ret;
	}
	
	/**
	 * Compare a {@link FilteredValue} (i.e. the value offered) with a
	 * {@link MonitorPointData} (i.e. the value to be sent to the OAS core)
	 *  
	 * @param v The not <code>null</code> value offered
	 * @param d The not <code>null</code> value to be sent to the core
	 * @return <code>true</code> if v and d matches
	 */
	protected boolean match(FilteredValue v, MonitorPointData d) {
		assertNotNull(v);
		assertNotNull(d);
		
		/**
		 * ISO 8601 date formatter
		 */
		SimpleDateFormat iso8601dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.S");
		
		boolean ret = v.id==d.getId();
		ret = ret && v.value.toString().equals(d.getValue());
		ret = ret && iso8601dateFormat.format(new Date(v.producedTimestamp)).equals(d.getFilteredTime());
		
		if (!ret) {
			logger.error("The FileteredValue {} and the MonitorPointData {} do not match!",v.toString(),d.toString());
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
	 * @see org.eso.ias.plugin.test.publisher.PublisherEventsListener#dataReceived(org.eso.ias.plugin.publisher.MonitoredSystemData)
	 */
	@Override
	public void dataReceived(MonitoredSystemData data) {
		assertNotNull(data);
		assertEquals("ID differs",data.getSystemID(), pluginId);
		assertNotNull(data.getPublishTime());
		assertFalse(data.getPublishTime().isEmpty());
		numOfPublishInvocation++;
		logger.info("{} monitor points received from {}",data.getMonitorPoints().size(),data.getSystemID());
		for (MonitorPointData d: data.getMonitorPoints()) { 
			if (expectedValues!=null) {
				expectedValues.countDown(); 
			}
			receivedValues.put(d.getId(), d);
		}
	}
	
	@Before
	public void setUp() {
		// Build the publisher
		int poolSize = Runtime.getRuntime().availableProcessors()/2;
		ScheduledExecutorService schedExecutorSvc= Executors.newScheduledThreadPool(poolSize, PluginThreadFactory.getThreadFactory());
		publisher = new PublisherTestImpl(pluginId, pluginServerName, pluginServerPort, schedExecutorSvc,this);
		logger.debug("Set up");
	}
	
	@After
	public void tearDown() throws PublisherException {
		logger.debug("Releasing resource");
		receivedValues.clear();
		publishedValues.clear();
		publisher.shutdown();

	}
}
