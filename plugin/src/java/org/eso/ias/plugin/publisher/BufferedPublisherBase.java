package org.eso.ias.plugin.publisher;

import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

import org.eso.ias.plugin.filter.FilteredValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Base class for publishing data to the IAS core with buffering: 
 * received monitor points are queued to be sent all at once when 
 * <UL>
 * 	<LI>the time interval ({@link #throttlingTime}) elapses or
 * 	<LI>the max allowed size of the buffer has been reached
 * </UL>
 * <P>
 * <code>BufferedPublisherBase</code> gets all the values from the monitored system and saves them
 * in a map (the buffer) until the throttling time expires or the max allowed size has been reached 
 * and only then sends all of them at once.
 * The map allows to easily save only the last update value for each managed monitor point preventing
 * a misbehaving plugin to fire tons of messages per time interval.
 * <P>
 * Reaching the max size of the buffer, triggers the immediate sending 
 * of the values of the monitor points. The periodic thread will continue
 * to run. 
 *  
 * <P>
 * <em>Life cyle</em>: 
 * <UL>
 * 	<LI>{@link #start()} is the first method to call to allow a correct initialization.
 *  <LI>{@link #shutdown()} must be called when down with this object to clean the resources
 * @author acaproni
 *
 */
public abstract class BufferedPublisherBase implements MonitorPointSender {
	
	/**
	 * The name of the property to set the throttling sending values to the IAS (100<=msec<=1000)
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
	private final Logger logger = LoggerFactory.getLogger(BufferedPublisherBase.class);
	
	/**
	 * The name of the server to send monitor points to
	 */
	public final String serverName; 
	
	/**
	 * The port of the server to send monitor points to
	 */
	public final int serverPort;
	
	/**
	 * The ID of of the plugin.
	 */
	public final String pluginId;
	
	/**
	 * The map to store the monitor points received during the throttling time interval.
	 * <P> 
	 * A map allows to save only the last received update of a monitor point if a misbehaving
	 * implementation is continuously updating a value.
	 * <P>
	 * The key is the ID of the monitor point, the value is the {@link FilteredValue} as 
	 * returned applying the filter to a set of samples.
	 */
	private final Map<String, FilteredValue>monitorPoints = Collections.synchronizedMap(new HashMap<>());
	
	/**
	 * The executor service to start the timer thread to send values to the 
	 * core of the IAS.
	 */
	private final ScheduledExecutorService executorService;
	
	/**
	 * ISO 8601 date formatter
	 */
	private final SimpleDateFormat iso8601dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.S");
	
	/**
	 * The data structure to send to the core of the IAS
	 */
	private final BufferedMonitoredSystemData monitorPointsToSend = new BufferedMonitoredSystemData();
	
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
	private final AtomicLong publishedMessages = new AtomicLong(0);

	/**
	 * Constructor
	 * 
	 * @param pluginId The identifier of the plugin
	 * @param serverName The name of the server
	 * @param port The port of the server
	 * @param executorSvc The executor service
	 */
	public BufferedPublisherBase(
			String pluginId,
			String serverName, 
			int port,
			ScheduledExecutorService executorSvc) {
		if (pluginId==null || pluginId.isEmpty()) {
			throw new IllegalArgumentException("The ID can't be null nor empty");
		}
		this.pluginId=pluginId;
		if (serverName==null || serverName.isEmpty()) {
			throw new IllegalArgumentException("The sink server name can't be null nor empty");
		}
		this.serverName=serverName;
		if (port<-0) {
			throw new IllegalArgumentException("Invalid port number: "+port);
		}
		this.serverPort=port;
		if (executorSvc==null) {
			throw new IllegalArgumentException("The executor service can't be null");
		}
		this.monitorPointsToSend.setSystemID(pluginId);
		this.executorService=executorSvc;
		logger.info("Plugin {} sends monitor points to {}:{} at a rate of {} msec",pluginId,serverName,serverPort,throttlingTime);
		monitorPointsToSend.setSystemID(pluginId);
	}
	
	/**
	 * Send the passed data to the core of the IAS.
	 * 
	 * @param data The data to send to the core of the IAS
	 */
	protected abstract void publish(BufferedMonitoredSystemData data);
	
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
		// Start the thread to send the values to the core of the IAS
		executorService.scheduleAtFixedRate(new Runnable() {
			
			@Override
			public void run() {
				sendMonitoredPointsToIas();
			}
		},throttlingTime, throttlingTime, TimeUnit.MILLISECONDS);
		logger.debug("Generation of statistics activated with a frequency of {} minutes",throttlingTime);
		logger.debug("Invoking implementers defined setUp");
		try {
			start();
		} catch (Exception e) {
			throw new PublisherException("Eception invoking setUp", e);
		}
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
	private synchronized void sendMonitoredPointsToIas() {
		if (monitorPoints.isEmpty()) {
			return;
		}
		if (stopped) {
			monitorPoints.clear();
			return;
		}
		// No need to synchronize iso8601dateFormat that is used only 
		// by this (already synchronized) method
		String now = iso8601dateFormat.format(new Date(System.currentTimeMillis()));
		monitorPointsToSend.setPublishTime(now);
		synchronized (monitorPoints) {
			monitorPointsToSend.setMonitorPoints(
					monitorPoints.values().stream().map(v -> new MonitorPointData(v)).collect(Collectors.toList()));
			monitorPoints.clear();
		}
		publishedMessages.incrementAndGet();
		publish(monitorPointsToSend);
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
	 */
	protected abstract void shutdown() throws PublisherException;

	/**
	 * A new value has been produced by the monitored system:
	 * it is queued ready to be sent when the throttling time interval elapses.
	 * 
	 * @see MonitorPointSender#offer(java.util.Optional)
	 */
	@Override
	public void offer(Optional<FilteredValue> monitorPoint) throws PublisherException {
		if (closed || stopped) {
			return;
		}
		if (!initialized) {
			throw new PublisherException("Publishing monitor points before initialization");
		}
		monitorPoint.ifPresent(mp -> monitorPoints.put(mp.id, mp));
		if (monitorPoints.size()>=maxBufferSize) {
			// Ops the buffer size reached the maximum allowed size: send the values to the core
			sendMonitoredPointsToIas();
		}
	}
	
	@Override
	public long numOfMessagesSent() {
		return publishedMessages.getAndSet(0L);
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
}
