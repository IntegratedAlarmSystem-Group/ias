package org.eso.ias.plugin;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

import org.eso.ias.plugin.config.PluginConfig;
import org.eso.ias.plugin.config.Value;
import org.eso.ias.plugin.filter.FilteredValue;
import org.eso.ias.plugin.thread.PluginScheduledExecutorSvc;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The main class to write IAS plugins.
 * <P>
 * The plugin takes care of collecting, filtering and sending to the IAS
 * core of all the monitored values.
 * <P>
 * Updates on the values of monitored point must be provided by calling {@link #updateMonitorPointValue(String, Sample)}.
 * <P>
 * <code>Plugin</code> logs statistics by default at {@value #defaultStatsGeneratorFrequency}
 * minutes. The time interval (in minutes) is customizable setting {@value #LOG_STATS_FREQUENCY_PROPNAME}
 * java property.
 * The logging of statistics is disabled if the time interval is lower or equal to 0.
 * Statistics are composed of a single log at INFO level.
 * <BR>
 * The generation of the statistics is done by a dedicated thread that is started only
 * if the time interval is greater then 0.
 *  
 * @author acaproni
 */
public class Plugin implements ChangeValueListener {
	
	/**
	 * The map of monitor points and alarms produced by the 
	 * monitored system and whose filtered values will
	 * finally be sent to the IAS core.
	 * <P>
	 * The key is the ID of the monitor point
	 */
	private final Map<String,MonitoredValue> monitorPoints = Collections.synchronizedMap(new HashMap<>());
	
	/**
	 * The name of the property to let the plugin provide detailed statistics
	 */
	public static final String LOG_STATS_DETAILED_PROPNAME = "org.eso.ias.plugin.stats.detailed";
	
	/**
	 * If <code>true</code>, the plugin provides detailed statistics of the updates
	 * of the monitor point values.
	 * <P>
	 * Due to the definition of {@link Boolean#getBoolean(String)}, <code>deepStats</code> 
	 * defaults to <code>false</code>.
	 */
	private static final boolean deepStats = Boolean.getBoolean(LOG_STATS_DETAILED_PROPNAME);
	
	/**
	 * The collector of the detailed statistics
	 */
	private Optional<DetailedStatsCollector> detailedStatsCollector = 
			deepStats?Optional.of(new DetailedStatsCollector()):Optional.empty();
	
	/**
	 * The name of the property to let the plugin publish logs about frequency
	 */
	public static final String LOG_STATS_FREQUENCY_PROPNAME = "org.eso.ias.plugin.stats.frequency";
	
	/**
	 * The default number of minutes to write logs with statistics
	 */
	public static final int defaultStatsGeneratorFrequency = 10;
	
	/**
	 * The time interval (in minutes) to log usage statistics.
	 */
	private static final int statsTimeInterval = Integer.getInteger(LOG_STATS_FREQUENCY_PROPNAME, defaultStatsGeneratorFrequency);
	
	/**
	 * The logger
	 */
	private final Logger logger = LoggerFactory.getLogger(Plugin.class);
	
	/**
	 * The identifier of the plugin
	 */
	public final String pluginId;
	
	/**
	 *  The server name to send monitor point values and alarms to
	 */
	private final String sinkServerName;
	
	/**
	 *  The IP port of the server to send monitor point values and alarms to
	 */
	private final int sinkServerPort;
	
	/**
	 * Signal that the plugin must terminate
	 */
	private final AtomicBoolean closed = new AtomicBoolean(false);
	
	/**
	 * The number of submitted samples in the last time interval ({@link #statsTimeInterval})
	 * to be used to log statistics. 
	 * <P>
	 * More sophisticated statistics are collected by {@link Statistics}, if requested.
	 */
	private final AtomicLong submittedUpdates = new AtomicLong(0);
	
	/**
	 * If submitting samples to a monitor points returns an error,
	 * it is added to this list and never updated again.
	 * <P>
	 * The reason to disable the update of a monitor point is to catch
	 * errors in the implementation of the filtering. 
	 */
	private final Set<String> disabledMonitorPoints = new TreeSet<>();

	/**
	 * Build a plugin with the passed parameters
	 * 
	 * @param id The Identifier of the plugin
	 * @param sinkServerName The server to send 
	 * 						 monitor point values and alarms to
	 * @param sinkServerPort The IP port number 
	 * @param values The list of monitor point values and alarms
	 */
	public Plugin(
			String id, 
			String sinkServerName,
			int sinkServerPort,
			Collection<Value> values) {
		if (id==null || id.trim().isEmpty()) {
			throw new IllegalArgumentException("The ID can't be null nor empty");
		}
		this.pluginId=id.trim();
		if (sinkServerPort<-0) {
			throw new IllegalArgumentException("Invalid port number: "+sinkServerPort);
		}
		this.sinkServerPort=sinkServerPort;
		if (sinkServerName==null || sinkServerName.isEmpty()) {
			throw new IllegalArgumentException("The sink server name can't be null nor empty");
		}
		this.sinkServerName=sinkServerName;
		if (values==null || values.isEmpty()) {
			throw new IllegalArgumentException("No monitor points definition found"); 
		}
		logger.info("Plugin (ID=%s) started",pluginId);
		values.forEach(v -> { 
			try {
			putMonitoredPoint(new MonitoredValue(v.getId(), v.getRefreshTime(), PluginScheduledExecutorSvc.getInstance(), this));
		}catch (Exception e){
			logger.error("Error adding monitor point "+v.getId(),e);
		} });
	}
	
	/**
	 * Build a plugin from the passed configuration
	 * @param config
	 */
	public Plugin(PluginConfig config) {
		this(
				config.getId(),
				config.getSinkServer(),
				config.getSinkPort(),
				config.getValuesAsCollection());
	}
	
	/**
	 * This method must be called at the beginning
	 * to acquire the needed resources.
	 */
	public void start() {
		logger.info("Started");
		
		if (statsTimeInterval>0) {
			// Start the logger of statistics
			Runnable r = new Runnable() {
				@Override
				public void run() {
					logger.info("#Submitted samples = "+submittedUpdates.getAndSet(0));
					detailedStatsCollector.ifPresent(detailedStats -> detailedStats.logAndReset());
				}
			};
			PluginScheduledExecutorSvc.getInstance().scheduleAtFixedRate(r,statsTimeInterval,statsTimeInterval,TimeUnit.MINUTES);
		}
		// Adds the shutdown hook
		Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
			@Override
			public void run() {
				shutdown();
			}
		}, "Plugin shutdown hook"));
	}
	
	/**
	 * This method must be called when finished using the object 
	 * to free the allocated resources. 
	 */
	public void shutdown() {
		boolean alreadyClosed=closed.getAndSet(true);
		if (!alreadyClosed) {
			logger.info("Shutting down the threads to update and send monitor points");
			PluginScheduledExecutorSvc.getInstance().shutdown();
		}
	}
	
	/**
	 * A new value of a monitor point has been provided by the monitored system: 
	 * the value must be sent to to the monitor point with the given ID for filtering.
	 * 
	 * @param mPointID The ID of the monitored point to submit the sample to
	 * @param s the new sample to submit to the monitored point
	 * @throws PluginException if adding the sample failed
	 */
	public void updateMonitorPointValue(String mPointID, Sample s) throws PluginException {
		if (closed.get()) {
			return;
		}
		if (mPointID==null || mPointID.trim().isEmpty()) {
			throw new IllegalArgumentException("Invalid monitor point ID: sample rejected");
		}
		if (statsTimeInterval>0) {
			submittedUpdates.incrementAndGet();
		}
		if (disabledMonitorPoints.contains(mPointID)) {
			return;
		}
		if(s==null) {
			throw new IllegalArgumentException("Cannot update monitor point "+mPointID+" with a null sample");
		}
		try {
			Optional.ofNullable(monitorPoints.get(mPointID)).
				orElseThrow(() -> new PluginException("A monitor point with ID "+mPointID+" is not present")).
					submitSample(s);
		} catch (Exception e) {
			disabledMonitorPoints.add(mPointID);
			logger.error("Exception sumbitting a sample to "+mPointID+": monitor point disabled");
			throw new PluginException("Unknown exception submitting a sample to "+mPointID+" monitor point", e);
		}
		// Upadates the detailed statistics, if requested
		detailedStatsCollector.ifPresent(stats -> stats.mPointUpdated(mPointID));
	}
	
	/**
	 * Adds a monitored point to the plugin managed map of monitor points.
	 * <P>
	 * The same monitor point (or if you prefer, 2 monitor points
	 * with the same ID ) cannot be added to the map.
	 * 
	 * @param mPoint The monitored point to add to the map
	 * @return the number of monitor points managed by the plugin
	 * @throws PluginException if a monitor point with the given id is already in the map
	 */
	public int putMonitoredPoint(MonitoredValue mPoint) throws PluginException {
		assert(mPoint!=null);
		int sz;
		synchronized(monitorPoints) {
			if (monitorPoints.containsKey(mPoint.id)) {
				throw new PluginException("Monitor point "+mPoint.id+" is already defined");
			}
			monitorPoints.put(mPoint.id, mPoint);
			sz=monitorPoints.size();
		}
		logger.info("IAS plugin %s now manages %d monitor points",pluginId,sz);
		return sz;
	}

	/**
	 * A  monitor point value has been updated and must be forwarded to the core of the IAS.
	 * 
	 * TODO: complete the implementation with statistics and the sending to the IAS
	 * 
	 * @see org.eso.ias.plugin.ChangeValueListener#monitoredValueUpdated(org.eso.ias.plugin.filter.FilteredValue)
	 */
	@Override
	public void monitoredValueUpdated(Optional<FilteredValue> value) {
		value.ifPresent(v -> logger.info("Value change %s",v.toString()));
		
	}
}
