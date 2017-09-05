package org.eso.ias.plugin.publisher;

import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import org.eso.ias.plugin.ValueToSend;
import org.eso.ias.plugin.filter.FilteredValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Base class for publishing data to the core of the IAS: 
 * received monitor points are queued to be sent when 
 * <UL>
 * 	<LI>the time interval ({@link #throttlingTime}) elapses or
 * 	<LI>the max allowed size of the buffer has been reached
 * </UL>
 * <P>
 * <code>PublisherBase</code> gets all the values from the monitored system and saves them
 * in a map (the buffer) until the throttling time expires or the max allowed size has been reached 
 * and only then sends all of them at once.
 * The map allows to easily save only the last update value for each managed monitor point preventing
 * a misbehaving plugin to fire tons of messages per time interval.
 * Monitor point values are sent one by one to the core of the IAS.
 * <P>
 * Reaching the max size of the buffer, triggers the immediate sending 
 * of the values of the monitor points. The periodic thread will continue
 * to run. 
 * <P>
 * <em>Life cyle</em>: 
 * <UL>
 * 	<LI>{@link #start()} is the first method to call to allow a correct initialization.
 *  <LI>{@link #shutdown()} must be called when down with this object to clean the resources
 * </UL>
 * 
 * @author acaproni
 *
 */
public abstract class PublisherBase implements MonitorPointSender {
	
	/**
	 * The name of the property to set the throttling sending values to the IAS (100&lt;=msec&lt;=1000)
	 */
	public static final String THROTTLING_PROPNAME = "org.eso.ias.plugin.throttling";
	
	/**
	 * The default throttling time in millisecond
	 */
	public static final long defaultThrottlingTime = 500;
	
	
	public static final long throttlingTime = 
			Long.getLong(THROTTLING_PROPNAME, defaultThrottlingTime)<100||Long.getLong(THROTTLING_PROPNAME, defaultThrottlingTime)>1000 ?
					defaultThrottlingTime : Long.getLong(THROTTLING_PROPNAME, defaultThrottlingTime);
	
	
	/**
	 * The name of the property to set max dimension of monitor points to buffer
	 */
	public static final String MAX_BUFFER_SIZE_PROPNAME = "org.eso.ias.plugin.buffersize";
	
	/**
	 * The default max size of the buffer
	 */
	public static final int defaultBufferSize = Integer.MAX_VALUE;
	
	/**
	 * The max number of monitor point values to keep in memory in the time interval.
	 * <P>
	 * If the size of the buffer is greater or equal the <code>maxBufferSize</code>, the 
	 * monitor point values are sent immediately to the core of the IAS
	 * 
	 */
	public static final int maxBufferSize = Integer.getInteger(MAX_BUFFER_SIZE_PROPNAME,defaultBufferSize)<=0 ?
			defaultBufferSize : Integer.getInteger(MAX_BUFFER_SIZE_PROPNAME,defaultBufferSize);
	
	/**
	 * The logger
	 */
	private final Logger logger = LoggerFactory.getLogger(PublisherBase.class);
	
	/**
	 * The name of the server to send monitor points to
	 */
	public final String serverName; 
	
	/**
	 * The port of the server to send monitor points to
	 */
	public final int serverPort;
	
	/**
	 * The ID of the plugin.
	 */
	public final String pluginId;
	
	/**
	 * The ID of the system monitored by the plugin.
	 */
	public final String monitoredSystemId;
	
	/**
	 * The map to store the monitor points received during the throttling time interval.
	 * <P> 
	 * A map allows to save only the last received update of a monitor point if a misbehaving
	 * implementation is continuously updating a value.
	 * <P>
	 * The key is the ID of the monitor point, the value is the {@link FilteredValue} as 
	 * returned applying the filter to a set of samples.
	 */
	protected final Map<String, ValueToSend>monitorPoints = Collections.synchronizedMap(new HashMap<>());
	
	/**
	 * The executor service to start the timer thread to send values to the 
	 * core of the IAS.
	 */
	private final ScheduledExecutorService executorService;
	
	/**
	 * ISO 8601 date formatter
	 */
	protected final SimpleDateFormat iso8601dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.S");
	
	/**
	 * Signal the thread that it has been closed and should reject 
	 * newly submitted monitor point values.
	 */
	private volatile boolean closed=false;
	
	/**
	 * <code>stopped</code> is set and unset calling
	 * {@link #stopSending()} and {@link #startSending()} respectively.
	 * <P>
	 * Stopped is not paused: monitor points collected while stopped
	 * are lost forever (i.e. never sent to the core of the IAS).
	 * 
	 * @see MonitorPointSender#stopSending()
	 * @see MonitorPointSender#startSending()
	 */
	private volatile boolean stopped=false;
	
	/**
	 * Signal that the object has been correctly initialized.
	 * <P>
	 * Monitor points will not be accepted if the object has not been correctly initialized.
	 */
	private volatile boolean initialized=false;
	
	/**
	 * The number of messages sent to the core of the IAS
	 */
	protected final AtomicLong publishedMessages = new AtomicLong(0);
	
	/**
	 * The number of bytes sent to the core of the IAS
	 */
	private final AtomicLong bytesSent = new AtomicLong(0);
	
	/**
	 * The number of monitor point values sent to the core of the IAS
	 */
	protected final AtomicLong monitorPointsSent = new AtomicLong(0);
	
	/**
	 * The number of monitor point values sent to the core of the IAS
	 */
	private final AtomicLong monitorPointsSubmitted = new AtomicLong(0);
	
	/**
	 * The number of errors while sending monitor point values to the core of the IAS
	 */
	protected final AtomicLong numOfErrorsSending = new AtomicLong(0);

	/**
	 * Constructor
	 * 
	 * @param pluginId The identifier of the plugin
	 * @param monitoredSystemId The identifier of the system monitored by the plugin
	 * @param serverName The name of the server
	 * @param port The port of the server
	 * @param executorSvc The executor service
	 */
	public PublisherBase(
			String pluginId,
			String monitoredSystemId,
			String serverName, 
			int port,
			ScheduledExecutorService executorSvc) {
		if (pluginId==null || pluginId.isEmpty()) {
			throw new IllegalArgumentException("The ID can't be null nor empty");
		}
		this.pluginId=pluginId;
		if (monitoredSystemId==null || monitoredSystemId.isEmpty()) {
			throw new IllegalArgumentException("The ID of the monitored system can't be null nor empty");
		}
		this.monitoredSystemId=monitoredSystemId;
		if (serverName==null || serverName.isEmpty()) {
			throw new IllegalArgumentException("The sink server name can't be null nor empty");
		}
		this.serverName=serverName;
		if (port<0) {
			throw new IllegalArgumentException("Invalid port number: "+port);
		}
		this.serverPort=port;
		if (executorSvc==null) {
			throw new IllegalArgumentException("The executor service can't be null");
		}
		this.executorService=executorSvc;
		logger.info("Plugin {} sends monitor points to {}:{} at a rate of {} msec",pluginId,serverName,serverPort,throttlingTime);
	}
	
	/**
	 * Send the passed monitor point value to the core of the IAS.
	 * <P>
	 * This method is supposed to effectively send the data to the core of the IAS
	 * but some implementation can buffer the date and send them asynchronously.
	 * 
	 * @param mpData The monitor point data to send to the core of the IAS
	 * @return The number of bytes sent to the core of the IAS
	 * @throws PublisherException In case of error publishing 
	 */
	protected abstract long publish(MonitorPointData mpData) throws PublisherException;
	
	/**
	 * Performs the initialization of the implementers of this
	 * abstract class.
	 * 
	 * @throws PublisherException Exception returned by the implementer
	 */
	protected abstract void start() throws PublisherException;
	
	/**
	 * Initialize the publisher
	 * 
	 * @throws PublisherException in case of error initializing
	 * @see MonitorPointSender#setUp()
	 */
	@Override
	public synchronized void setUp() throws PublisherException {
		if (initialized) {
			// Ops double initialization!
			throw new PublisherException("PublisherBase already initialized");
		}
		if (closed) {
			// Ops already closed!
			throw new PublisherException("Cannot initialize a closed PublisherBase");
		}
		logger.debug("Initializing");
		
		
		try {
			logger.debug("Initializing the publisher of monitor point values");
			start();
		} catch (Exception e) {
			throw new PublisherException("Eception invoking setUp", e);
		}
		
		
		// Start the thread to send the values to the core of the IAS
		executorService.scheduleAtFixedRate(new Runnable() {
			@Override
			public void run() {
				sendMonitoredPointsToIas();
			}
		},throttlingTime, throttlingTime, TimeUnit.MILLISECONDS);
		logger.debug("Generation of statistics activated with a frequency of {} minutes",throttlingTime);
		
		initialized=true;
		logger.debug("Initialized");
	}
	
	/**
	 * Send the monitor points to the core of the IAS.
	 * <P>
	 * The method is synchronized as it can be called by 2 different threads:
	 * when the throttling time interval elapses and if the max size
	 * of the buffer has been reached
	 */
	protected synchronized void sendMonitoredPointsToIas() {
		if (monitorPoints.isEmpty()) {
			return;
		}
		if (stopped || closed) {
			monitorPoints.clear();
			return;
		}
		// No need to synchronize iso8601dateFormat that is used only 
		// by this (already synchronized) method
		synchronized (monitorPoints) {
			monitorPoints.values().forEach(mpv -> {
				MonitorPointData mpData=new MonitorPointData(pluginId,monitoredSystemId,mpv);
				monitorPointsSent.incrementAndGet();
				try {
					publishedMessages.incrementAndGet();
					publish(mpData);
				} catch (PublisherException pe) {
					numOfErrorsSending.incrementAndGet();
					// Errors publishing are ignored
					if (numOfErrorsSending.get()<9) {
						logger.error("Error publishing data",pe);
					} else {
						if (numOfErrorsSending.get()%10L==0) {
							logger.error("Error publishing data (log of many other errors suppressed)",pe);
						}
					}
				}
			});
			monitorPoints.clear();
		}
		
	}
	
	/**
	 * Shuts down the server cleaning all the associated resources
	 * 
	 * @throws PublisherException returned by the implementer
	 * @see MonitorPointSender#tearDown()
	 */
	@Override
	public void tearDown() throws PublisherException {
		if (closed) {
			return;
		}
		closed=true;
		logger.debug("Invoking implementers defined tearDown");
		try {
			shutdown();
		} catch (Exception e) {
			throw new PublisherException("Eception invoking tearDown", e);
		}
		logger.debug("Shutted down");
	}
	
	/**
	 * Performs the cleanup of the implementers of this
	 * abstract class
	 * 
	 * @throws PublisherException In case of error shutting down
	 */
	protected abstract void shutdown() throws PublisherException;

	/**
	 * A new value has been produced by the monitored system:
	 * it is queued ready to be sent when the throttling time interval elapses.
	 * 
	 * @param monitorPoint The not <code>null</code> monitor point to be sent to the IAS
	 * @see MonitorPointSender#offer(ValueToSend)
	 * @throws IllegalStateException If the publisher has not been initialized before offering values
	 */
	@Override
	public void offer(ValueToSend monitorPoint) {
		Objects.requireNonNull(monitorPoint, "Cannot get a null value");
		if (closed || stopped) {
			return;
		}
		if (!initialized) {
			throw new IllegalStateException("Publishing monitor points before initialization");
		}
		monitorPoints.put(monitorPoint.id, monitorPoint);
		monitorPointsSubmitted.incrementAndGet();
		if (monitorPoints.size()>=maxBufferSize) {
			// Ops the buffer size reached the maximum allowed size: send the values to the core
			sendMonitoredPointsToIas();
		}
	}
	
	/**
	 * Return the statistics collected during the last time interval
	 * 
	 * @return the statistics collected during the last time interval
	 */
	@Override
	public SenderStats getStats() {
		SenderStats ret = new SenderStats(
				publishedMessages.getAndSet(0L), 
				monitorPointsSent.getAndSet(0L),
				monitorPointsSubmitted.getAndSet(0L), 
				bytesSent.getAndSet(0L),
				numOfErrorsSending.getAndSet(0));
		return ret;
	}

	/**
	 * @see #stopped
	 * @see org.eso.ias.plugin.publisher.MonitorPointSender#startSending()
	 * 
	 */
	@Override
	public void startSending() {
		stopped=false;
		logger.info("Sending of monitor points to the IAS has been resumed");
	}

	/**
	 * @see #stopped
	 * @see org.eso.ias.plugin.publisher.MonitorPointSender#stopSending()
	 */
	@Override
	public void stopSending() {
		stopped=true;
		monitorPoints.clear();
		logger.info("Sending of monitor points to the IAS has been stooped");
	}

	/**
	 * @see org.eso.ias.plugin.publisher.MonitorPointSender#isStopped()
	 */
	@Override
	public boolean isStopped() {
		return stopped;
	}
	
	/**
	 * @see org.eso.ias.plugin.publisher.MonitorPointSender#isStopped()
	 */
	@Override
	public boolean isClosed() {
		return closed;
	}
	
	/**
	 * Merges the default and user provided properties in the system properties
	 * 
	 * @param defaultStream: The path to load default user properties;
	 *                       <code>null</code> means no default
	 * @param  userStream: The path to load default user properties
	 *                     <code>null</code> means no user defined property file
	 * @throws PublisherException In case of error merging the properties
	 */
	protected void mergeProperties(InputStream defaultStream, InputStream userStream) throws PublisherException {
		Properties defaultProps = new Properties();
		if (defaultStream!=null) {
			try {
				defaultProps.load(defaultStream);
			} catch (IOException ioe) {
				throw new PublisherException("Error reading the default property file",ioe);
			}
		} else {
			logger.info("No default config file provided");
		}
		
		// Is there a user defined properties file?
		Properties userProps = new Properties(defaultProps);
		if (userStream!=null) {
			try {
				userProps.load(userStream);
			} catch (IOException ioe) {
				throw new PublisherException("Error reading the user property file",ioe);
			}
		} else {
			logger.info("No user properties file provided");
		}
		
		// Flushes the user defined properties (or the default ones) in the system properties
		// if not already there (command line and JSON props take precedence!)
		Set<String> propNames = userProps.stringPropertyNames();
		for (String propName: propNames) {
			if (!System.getProperties().contains(propName)) {
				System.getProperties().put(propName, userProps.getProperty(propName));
				logger.info("Setting property {} to val {}",propName,userProps.getProperty(propName));
			} else {
				logger.info("Command line property {} with value {} ovverrides the property in config files (where it has a value of {})",
						propName,
						System.getProperties().getProperty(propName),
						userProps.get(propName));
			}
		}
	}
}
