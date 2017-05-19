package org.eso.ias.plugin.publisher;

import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.eso.ias.plugin.filter.FilteredValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Base class for publishing data to the IAS core.
 * <P>
 * <code>PublisherBase</code> gets all the values from the monitored system and saves them
 * in a map until the throttling time expire and then sends all of them at once.
 * The map allows to easily save only the last update for each managed monitor point.
 * <P>
 * <em>Life cyle</em>: 
 * <UL>
 * 	<LI>{@link #start()} is the first method to call to allow a correct initialization.
 *  <LI>{@link #shutdown()} must be called when down with this object to clean the resources
 * @author acaproni
 *
 */
public abstract class PublisherBase implements MonitorPointSender {
	
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
	 * The ID of of the plugin.
	 */
	public final String pluginId;
	
	/**
	 * The map to store the monitor points received during the throttling time interval.
	 * <P> 
	 * The allows to save only the last received update of a monitor point if a misbehaving
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
	private final MonitoredSystemData monitorPointsToSend = new MonitoredSystemData();
	
	/**
	 * Signal the thread that it has been closed and should reject 
	 * newly submitted monitor point values.
	 */
	private volatile boolean closed=false;
	
	/**
	 * Signal that the object has been correctly initialized.
	 * <P>
	 * Monitor points will not be accepted if the object has not been correctly initialized.
	 */
	private volatile boolean initialized=false;

	/**
	 * Constructor
	 * 
	 * @param pluginId The identifier of the plugin
	 * @param serverName The name of the server
	 * @param port The port of the server
	 */
	public PublisherBase(
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
		this.executorService=executorSvc;
		logger.info("Plugin {} sends monitor points to {}:{} at a rate of {} msec",pluginId,serverName,serverPort,throttlingTime);
		monitorPointsToSend.setSystemID(pluginId);
	}
	
	/**
	 * Send the passed data to the core of the IAS.
	 * 
	 * @param data The data to send to the core of the IAS
	 */
	protected abstract void publish(MonitoredSystemData data);
	
	/**
	 * Performs the initialization of the implementers of this
	 * abstract class.
	 * 
	 * @throws Exception Exception returned by the implementer
	 */
	protected abstract void setUp() throws Exception;
	
	/**
	 * Start the sender
	 * 
	 * @throws PublisherException in case of error initializing
	 */
	public synchronized void start() throws PublisherException {
		if (initialized) {
			// Ops double initialization!
			throw new PublisherException("PublisherBase already initialized");
		}
		if (closed) {
			// Ops double initialization!
			throw new PublisherException("Cannot initialize a closed PublisherBase");
		}
		logger.debug("Initializing");
		// Start the thread to send the values to the core of the IAS
		executorService.scheduleAtFixedRate(new Runnable() {
			
			@Override
			public void run() {
				if (monitorPoints.isEmpty()) {
					return;
				}
				synchronized (iso8601dateFormat) {
					String now = iso8601dateFormat.format(new Date(System.currentTimeMillis()));
					monitorPointsToSend.setPublishTime(now);
				}
				synchronized (monitorPoints) {
					monitorPointsToSend.setMonitorPoints(
							monitorPoints.values().stream().map(v -> new MonitorPointData(v)).collect(Collectors.toList()));
				}
				publish(monitorPointsToSend);
			}
		},
		throttlingTime, throttlingTime, TimeUnit.MILLISECONDS);
		logger.debug("Generation of statistics activated with a frequency of {} minutes",throttlingTime);
		logger.debug("Invoking implementers defined setUp");
		try {
			setUp();
		} catch (Exception e) {
			throw new PublisherException("Eception invoking setUp", e);
		}
		initialized=true;
		logger.debug("Initialized");
	}
	
	/**
	 * Performs the cleanup of the implementers of this
	 * abstract class
	 * 
	 *  @throws Exception Exception returned by the implementer
	 */
	protected abstract void tearDown() throws Exception;
	
	/**
	 * Shuts down the server cleaning all the associated resources
	 */
	public synchronized void shutdown() throws PublisherException{
		if (closed) {
			return;
		}
		closed=true;
		logger.debug("Invoking implementers defined tearDown");
		try {
			tearDown();
		} catch (Exception e) {
			throw new PublisherException("Eception invoking tearDown", e);
		}
		logger.debug("Shutted down");
	}

	/**
	 * A new value has been produced by the monitored system:
	 * it is queued ready to be sent when the throttling time interval elapses.
	 * 
	 * @see MonitorPointSender#offer(java.util.Optional)
	 */
	@Override
	public void offer(Optional<FilteredValue> monitorPoint) throws PublisherException {
		if (closed) {
			return;
		}
		if (!initialized) {
			throw new PublisherException("Publishing monitor points before initialization");
		}
		monitorPoint.ifPresent(mp -> monitorPoints.put(mp.id, mp));
	}
}
