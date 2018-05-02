package org.eso.ias.plugin;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.eso.ias.heartbeat.HbEngine;
import org.eso.ias.heartbeat.HbMsgSerializer;
import org.eso.ias.heartbeat.HbProducer;
import org.eso.ias.heartbeat.HeartbeatStatus;
import org.eso.ias.heartbeat.publisher.HbKafkaProducer;
import org.eso.ias.plugin.config.PluginConfig;
import org.eso.ias.plugin.config.Value;
import org.eso.ias.plugin.filter.Filter;
import org.eso.ias.plugin.filter.FilterFactory;
import org.eso.ias.plugin.publisher.MonitorPointSender;
import org.eso.ias.plugin.publisher.MonitorPointSender.SenderStats;
import org.eso.ias.plugin.publisher.PublisherException;
import org.eso.ias.plugin.thread.PluginThreadFactory;
import org.eso.ias.types.Identifier;
import org.eso.ias.types.IdentifierType;
import org.eso.ias.types.OperationalMode;
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
 * Some terminology helps better understanding the methods and data structures:
 * <UL>
 * 	<LI><em>sample</em>: the value of a monitor point read from the monitored system; 
 *      samples are notified to {@link MonitoredValue}
 *  <LI><em>monitored value</em>: the value to send to the core of the IAS is generated by the {@link MonitoredValue}
 *      by applying the user defined filtering to clean for noise
 * </ul>
 * <P>
 * <code>Plugin</code> logs statistics by default at {@value #defaultStatsGeneratorFrequency}
 * minutes. The time interval (in minutes) is customizable setting {@value #LOG_STATS_FREQUENCY_PROPNAME}
 * java property.
 * The logging of statistics is disabled if the time interval is lower or equal to 0.
 * Statistics are composed of a single log at INFO level.
 * <BR>
 * The generation of the statistics is done by a dedicated thread that is started only
 * if the time interval is greater then 0.
 * <P>
 * The operational mode can be defined for the entire plugin  ({@link #pluginOperationalMode}) 
 * or for a specific monitor point value.
 * If the operational mode for the plugin is set, it takes priority over the operational mode 
 * of the monitor point value (i.e. if a operational mode is set at plugin level,
 * it will be sent as the operational mode of each monitored value independently of their settings).
 * <P>
 * The collected monitor points and alarms pass through the filtering and are finally sent to the 
 * BSDB where they will be processed by the DASUs.
 * Monitor points and alarms are sent on change and periodically 
 * if their values did not change (@see #autoSendRefreshRate).
 * 
 * <P><EM>Support for replication:</em>
 * <BR>The same plugin can be replicated over different identical instances
 * for example to collect monitor points and alarms from identical devices.
 * The user defined parts must know to which specific device to connect for each
 * replicated plugin.
 * To replicate a plugin, only one JSON configuration file is needed and the number 
 * of the instance must be passed in the constructor.
 * The plugin will transparently map the monitor point by properly setting their IDs
 * as defined in {@link ReplicatedIdsMapper}. 
 * <BR>The advantage is that only one JSON configuration must be written for 
 * all the replicated instances reducing the effort in configuring and reducing
 * the risk of errors.
 * <P>
 * Implementation note: internally the IDs of the monitor points are those 
 * read from the configuration: the IDs are changed just before sending to the BSDB
 * in {@link #monitoredValueUpdated(ValueToSend)}.
 * Only the ID of the plugin is changed at build time.
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
	private static final boolean DEEP_STATISTICS = Boolean.getBoolean(LOG_STATS_DETAILED_PROPNAME);
	
	/**
	 * The collector of the detailed statistics
	 */
	private Optional<DetailedStatsCollector> detailedStatsCollector = 
			DEEP_STATISTICS?Optional.of(new DetailedStatsCollector()):Optional.empty();
	
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
	private static final int STATS_TIME_INTERVAL = Integer.getInteger(LOG_STATS_FREQUENCY_PROPNAME, defaultStatsGeneratorFrequency);

	/**
	 * The property to let the use set the number of threads in the scheduled thread executor
	 */
	public static final String SCHEDULED_POOL_SIZE_PROPNAME = "org.eso.ias.plugin.scheduledthread.poolsize";
	
	/**
	 * The default number of threads in the core is a  bit less of the number of available CPUs.
	 * 
	 * The task executed by those threads is to get values of monitored values applying filters 
	 * and push the values to send in a queue (the sending will be done by another thread),
	 * so it is a pure calculation. This number should give us a proper CPU usage without stealing
	 * all the available resources in the server.
	 */
	public static final int defaultSchedExecutorPoolSize = Runtime.getRuntime().availableProcessors()/2;
	
	/**
	 * The number of threads in the scheduled pool executor that get filtered values out of the
	 * monitored values
	 */
	public static final int schedExecutorPoolSize = Integer.getInteger(SCHEDULED_POOL_SIZE_PROPNAME, defaultSchedExecutorPoolSize);
	
	/**
	 * The thread factory for the plugin
	 */
	protected static final ThreadFactory threadFactory = new PluginThreadFactory();

	/**
	 * The scheduled executor service
	 */
	protected static final ScheduledExecutorService schedExecutorSvc= Executors.newScheduledThreadPool(schedExecutorPoolSize, threadFactory);
	
	/**
	 * The logger
	 */
	private static final Logger logger = LoggerFactory.getLogger(Plugin.class);
	
	/**
	 * The ID of the plugin
	 */
	public final String pluginId;
	
	/**
	 * The identifier of the plugin
	 */
	public final Identifier pluginIdentifier;
	
	/**
	 * The property to set the auto-send time interval (in seconds)
	 */
	public static final String AUTO_SEND_TI_PROPNAME = "org.eso.ias.plugin.timeinterval";
	
	/**
	 * The default value of the auto-send refresh rate
	 */
	public static final int defaultAutoSendRefreshRate = 5;
	
	/**
	 * The refresh rate to automatically send the last 
	 * values of the monitor points even if did not change
	 */
	public final int autoSendRefreshRate; 
	
	/**
	 * The identifier of the system monitored by this plugin
	 * 
	 * The name of the monitored system is not affected by replication
	 * because the user defined implementation getting monitor points from 
	 * a device knows better the proper name to use.
	 * 
	 * For example a plugin getting values from an ALMA antenna
	 * could use monitoredSystemId like DA45 or PM04
	 */
	public final String monitoredSystemId;
	
	/**
	 * Signal that the plugin must terminate
	 */
	private final AtomicBoolean closed = new AtomicBoolean(false);
	
	/**
	 * If submitting samples to a monitor points returns an error,
	 * it is added to this list and never updated again.
	 * <P>
	 * The reason to disable the update of a monitor point is to catch
	 * errors in the implementation of the filtering. 
	 */
	private final Set<String> disabledMonitorPoints = new TreeSet<>();
	
	/**
	 * The object that sends monitor points to the core of the IAS.
	 */
	private final MonitorPointSender mpPublisher;
	
	/**
	 * The operational mode of the plugin.
	 * <P>
	 * If set, it is used for all the monitor point and values sent
	 * to the core of the IAS regardless of their specific
	 * operational mode.
	 */
	private Optional<OperationalMode> pluginOperationalMode = Optional.empty();
	
	/**
	 * The mapper to translate identifiers of replicated plugins;
	 * empty if the plugin is not replicated.
	 */
	private final Optional<ReplicatedIdsMapper> idsMapper;
	
	/**
	 * ThE engine to send heartbeats
	 */
	private final HbEngine hbEngine;
	
	/**
	 * Build a non templated plugin with the passed parameters.
	 * 
	 * @param id The Identifier of the plugin
	 * @param monitoredSystemId: the identifier of the monitored system
	 * @param values the monitor point values
	 * @param props The user defined properties 
	 * @param sender The publisher of monitor point values to the IAS core
	 * @param defaultFilter the default filter (can be <code>null</code>) to apply
	 *                      if there is not filter set in the value
	 * @param defaultFilterOptions the options for the default filter
	 *                             (can be <code>null</code>)                      
	 * @param refreshRate The auto-send time interval in seconds
	 * @param hbFrequency the frequency (seconds) to periodically publish HBs
	 * @param hbProducer the publisher of HBs
	 */
	public Plugin(
			String id, 
			String monitoredSystemId,
			Collection<Value> values,
			Properties props,
			MonitorPointSender sender,
			String defaultFilter,
			String defaultFilterOptions,
			int refreshRate,
			int hbFrequency,
			HbProducer hbProducer) {
		this(
				id,
				monitoredSystemId,
				values,props,
				sender,
				defaultFilter,
				defaultFilterOptions,
				refreshRate,
				null,
				hbFrequency,
				hbProducer);
	}

	/**
	 * Build a plugin with the passed parameters.
	 * 
	 * @param id The Identifier of the plugin
	 * @param monitoredSystemId: the identifier of the monitored system
	 * @param values the monitor point values
	 * @param props The user defined properties 
	 * @param sender The publisher of monitor point values to the IAS core
	 * @param defaultFilter the default filter (can be <code>null</code>) to apply
	 *                      if there is not filter set in the value
	 * @param defaultFilterOptions the options for the default filter
	 *                             (can be <code>null</code>)                      
	 * @param refreshRate The auto-send time interval in seconds
	 * @param hbFrequency the frequency of the heartbeat in seconds
	 * @param instanceNumber the number of the instance if the plugin is replicated,
	 *                       <code>null</code> if not replicated
	 * @param hbFrequency the frequency (seconds) to periodically publish HBs
	 * @param hbProducer the publisher of HBs
	 */
	public Plugin(
			String id, 
			String monitoredSystemId,
			Collection<Value> values,
			Properties props,
			MonitorPointSender sender,
			String defaultFilter,
			String defaultFilterOptions,
			int refreshRate,
			Integer instanceNumber,
			int hbFrequency,
			HbProducer hbProducer) {
		
		// Immediately checks if the plugin is replicated
		Optional<Integer> instanceNumberOpt = Optional.ofNullable(instanceNumber);
		instanceNumberOpt.ifPresent( instance -> {
			if (instance<0) {
				throw new IllegalArgumentException("Invalid negative instance number "+instance);
			}
			logger.debug("This plugin is the replicated instance number {}",instance);
		});
		idsMapper = instanceNumberOpt.map( num -> new ReplicatedIdsMapper(num));
		
		if (id==null || id.trim().isEmpty()) {
			throw new IllegalArgumentException("The ID can't be null nor empty");
		}
		
		Identifier monSystemIdentifier = new Identifier(monitoredSystemId, IdentifierType.MONITORED_SOFTWARE_SYSTEM);
		pluginIdentifier = new Identifier(id, IdentifierType.PLUGIN,monSystemIdentifier);
		
		this.pluginId=idsMapper.map( mapper -> mapper.toRealId(id.trim()))
				.orElse(id.trim());
		
		if (idsMapper.isPresent()) {
			logger.info("New iID of the replicated plugin is [{}]",this.pluginId);
		}
		
		if (monitoredSystemId==null || monitoredSystemId.trim().isEmpty()) {
			throw new IllegalArgumentException("The ID of th emonitored system can't be null nor empty");
		}
		this.monitoredSystemId= monitoredSystemId.trim();
		
		if (values==null || values.isEmpty()) {
			throw new IllegalArgumentException("No monitor points definition found"); 
		}
		
		if (sender==null) {
			throw new IllegalArgumentException("No monitor point sender");
		}
		
		Integer refreshRateFromProp = Integer.getInteger(AUTO_SEND_TI_PROPNAME);
		if (refreshRateFromProp!=null) {
			this.autoSendRefreshRate = refreshRateFromProp;
		} else {
			this.autoSendRefreshRate = refreshRate;
		}
		if (this.autoSendRefreshRate<=0) {
			throw new IllegalArgumentException("The auto-send time interval must be greater then 0 instead of "+refreshRate);
		}

		if (hbFrequency<=0) {
			throw new IllegalArgumentException("The HB frequency must be >0");
		}
		Objects.requireNonNull(hbProducer);
		this.hbEngine=HbEngine.apply(pluginIdentifier.fullRunningID(), hbFrequency, TimeUnit.SECONDS, hbProducer);		
		
		flushProperties(props);
		this.mpPublisher=sender;
		
		/** check if the monitor point has the filter or if take global*/ 
		values.forEach(v -> { 
			try {
				logger.info("ID: {}, filter: {}, filterOptions: {}",v.getId(),v.getFilter(),v.getFilterOptions());
				
				MonitoredValue mv = null;
				
				if (v.getFilter()==null && defaultFilter==null) {
					logger.info("No filter, neither default filter set for {}",v.getId());
					mv = new MonitoredValue(
							v.getId(), 
							v.getRefreshTime(), 
							schedExecutorSvc, 
							this,autoSendRefreshRate); 
				} else {
					
					String filterName = (v.getFilter()!=null)?v.getFilter():defaultFilter;
					String filterOptions = (v.getFilterOptions()!=null)?v.getFilterOptions():defaultFilterOptions;
					
					logger.debug("Instantiating filter {} for monitor point {}",filterName,v.getId());
					Filter filter = FilterFactory.getFilter(filterName, filterOptions);
					
					mv = new MonitoredValue(
							v.getId(), 
							v.getRefreshTime(),
							filter,
							schedExecutorSvc, 
							this,autoSendRefreshRate); 
				}
				
				putMonitoredPoint(mv);
		}catch (Exception e){
			logger.error("Error adding monitor point "+v.getId(),e);
		} });
		logger.info("Plugin [{}] built",pluginId);
	}
	
	/**
	 * Flushes the user defined properties in the System properties.
	 * <P>
	 * As java properties in the command line takes precedence over those defined
	 * in the configuration, the latter do replace existing properties
	 * 
	 *  @param usrProps: the user properties 
	 */
	private void flushProperties(Properties usrProps) {
		if (usrProps==null || usrProps.isEmpty()) {
			return;
		}
		for (String key: usrProps.stringPropertyNames()) {
			if (!System.getProperties().contains(key)) {
				System.getProperties().setProperty(key, usrProps.getProperty(key));
			} else {
				logger.warn("User defined property {} already defined {}: value from configuration file {} will be discarded",
						key,
						System.getProperties().getProperty(key),
						usrProps.getProperty(key));
			}
		}
	}
	
	/**
	 * Build a non replicated plugin from the passed configuration.
	 * 
	 * @param config The plugin coinfiguration
	 * @param sender The publisher of monitor point values to the IAS core
	 * @param hbProducer the publisher of HBs
	 */
	public Plugin(
			PluginConfig config,
			MonitorPointSender sender,
			HbProducer hbProducer) {
		this(config,sender,null,hbProducer);
		
	}
	
	/**
	 * Build a replicated plugin from the passed configuration.
	 * 
	 * @param config The plugin coinfiguration
	 * @param sender The publisher of monitor point values to the IAS core
	 * @param instanceNumber the number of the instance if the plugin is replicated,
	 *                       <code>null</code> if not replicated
	 * @param hbProducer the publisher of HBs
	 */
	public Plugin(
			PluginConfig config,
			MonitorPointSender sender,
			Integer instanceNumber,
			HbProducer hbProducer) {
		this(
				config.getId(),
				config.getMonitoredSystemId(),
				config.getValuesAsCollection(),
				config.getProps(),
				sender,
				config.getDefaultFilter(),
				config.getDefaultFilterOptions(),
				config.getAutoSendTimeInterval(),
				instanceNumber,
				config.getHbFrequency(),
				hbProducer);
	}
	
	/**
	 * This method must be called at the beginning
	 * to acquire the needed resources.
	 * 
	 * @throws PublisherException In case of error initializing the publisher
	 */
	public void start() throws PublisherException {
		logger.debug("Initializing");
		
		// Start sending the HB
		hbEngine.start();
		
		mpPublisher.setUp();
		logger.info("Publisher initialized.");
		
		if (STATS_TIME_INTERVAL>0) {
			// Start the logger of statistics
			Runnable r = new Runnable() {
				@Override
				public void run() {
					SenderStats senderStats = mpPublisher.getStats();
					logger.info("#Submitted samples = {}; #Monitored points sent to the IAS = {}; #Messages sent to the IAS {}; #Bytes sent to the IAS = {}; #errors publishing messages {}",
							senderStats.numOfMonitorPointValuesSubmitted,
							senderStats.numOfMonitorPointValuesSent,
							senderStats.numOfMessagesSent,
							senderStats.numOfBytesSent,
							senderStats.numOfErrorsPublishing);
					detailedStatsCollector.ifPresent( DetailedStatsCollector::logAndReset);
					
				}
			};
			schedExecutorSvc.scheduleAtFixedRate(r,STATS_TIME_INTERVAL,STATS_TIME_INTERVAL,TimeUnit.MINUTES);
		}
		// Adds the shutdown hook
		Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
			@Override
			public void run() {
				shutdown();
			}
		}, "Plugin shutdown hook"));
		
		logger.debug("Initiailizing {} monitor points", monitorPoints.values().size());
		monitorPoints.values().forEach(mp -> mp.start());
		
		hbEngine.updateHbState(HeartbeatStatus.RUNNING);
		
		logger.info("Plugin {} initialized",pluginId);
	}
	
	/**
	 * This method must be called when finished using the object 
	 * to free the allocated resources. 
	 */
	public void shutdown() {
		boolean alreadyClosed=closed.getAndSet(true);
		hbEngine.updateHbState(HeartbeatStatus.EXITING);
		if (!alreadyClosed) {
			shutdownExecutorSvc();
			logger.info("Stopping the sending of monitor point values to the core of the IAS");
			mpPublisher.stopSending();
			logger.info("Clearing the publisher of monitor point values");
			try {
				mpPublisher.tearDown();
			} catch (PublisherException pe) {
				// Logs (and ignore) the error
				logger.error("Error clearing the publisher: {}",pe.getMessage());
				pe.printStackTrace(System.err);
			}
			
			logger.debug("Shutting down the monitor points");
			monitorPoints.values().forEach(mp -> mp.shutdown());
			
			hbEngine.shutdown();
			
			logger.info("Plugin {} is shut down",pluginId);
		}
	}
	
	/**
	 * This method must be called when finished using the executor  
	 * service to free the allocated resources. 
	 */
	public void shutdownExecutorSvc() {
		logger.info("Shutting down the scheduled executor service");
		schedExecutorSvc.shutdown();
		try {
			// Wait a while for existing tasks to terminate
			if (!schedExecutorSvc.awaitTermination(5, TimeUnit.SECONDS)) {
				logger.info("Not all threads terminated: trying to force the termination");
				List<Runnable> neverRunTasks=schedExecutorSvc.shutdownNow();
				logger.info("{} tasks never started execution",neverRunTasks.size());
				// Wait a while for tasks to respond to being cancelled
				if (!schedExecutorSvc.awaitTermination(10, TimeUnit.SECONDS)) {
					logger.error("Pool did not terminate");
				} else {
					logger.info("The executor successfully terminated");
				}
			} else {
				logger.info("The executor successfully terminated");
			}
		} catch (InterruptedException ie) {
			schedExecutorSvc.shutdownNow();
			Thread.currentThread().interrupt();
		}
	}
	
	/**
	 * A new value of a monitor point has been provided by the monitored system: 
	 * the value must be sent to to the monitor point with the given ID for filtering.
	 * 
	 * @param mPointID The ID of the monitored point to submit the sample to
	 * @param value the new not <code>null</code> value to submit to the monitored point
	 * @throws PluginException if adding the sample failed
	 */
	public void updateMonitorPointValue(String mPointID, Object value) throws PluginException {
		Objects.requireNonNull(value,"Cannot update monitor point "+mPointID+" with a null value: rejected");
		updateMonitorPointValue(mPointID,new Sample(value));
	}
	
	/**
	 * A new value of a monitor point (a new sample) has been provided by the monitored system: 
	 * the value must be sent to to the monitor point with the given ID for filtering.
	 * 
	 * @param mPointID The ID of the monitored point to submit the sample to
	 * @param sample the new sample to submit to the monitored point
	 * @throws PluginException if adding the sample failed
	 */
	public void updateMonitorPointValue(String mPointID, Sample sample) throws PluginException {
		if (closed.get()) {
			return;
		}
		Objects.requireNonNull(mPointID, "The identifier of a monitor point can't be null");
		if (mPointID.trim().isEmpty()) {
			throw new IllegalArgumentException("Invalid empty monitor point ID: sample rejected");
		}
		if (disabledMonitorPoints.contains(mPointID)) {
			return;
		}
		Objects.requireNonNull(sample,"Cannot update monitor point "+mPointID+" with a null sample: rejected");
		
		try {
			Optional.ofNullable(monitorPoints.get(mPointID)).
				orElseThrow(() -> new PluginException("A monitor point with ID "+mPointID+" is not present")).
					submitSample(sample);
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
		logger.debug("IAS plugin {} will manage {} monitor points",pluginId,sz);
		return sz;
	}

	/**
	 * A  monitor point value has been updated and must be forwarded to the core of the IAS.
	 * <P>
	 * If the plugin is replicated, the ID of the value is changed before publishing.
	 * 
	 * @param value The value to send to the core of the IAS
	 * @see ChangeValueListener#monitoredValueUpdated(ValueToSend)
	 */
	@Override
	public void monitoredValueUpdated(ValueToSend value) {
		Objects.requireNonNull(value, "Cannot update a null monitored value");
		if (closed.get()) {
			return;
		}
		
		// Change the id if replicated
		ValueToSend vts = idsMapper.map( mapper -> value.withId(mapper.toRealId(value.id))).orElse(value);
		
		// The operational mode of the plugin override that of the value
		ValueToSend fv = pluginOperationalMode.map(mode -> vts.withMode(mode)).orElse(vts);
		
		// Finally publishes the value
		mpPublisher.offer(fv);
		
		logger.debug("Filtered value {} with value {} and mode {} has been forwarded for sending to the IAS",fv.id,fv.value.toString(),fv.operationalMode.toString());
	}
	
	/**
	 * Change the refresh rate of the monitor point with the passed ID.
	 * <P>
	 * The new refresh rate is bounded by a minimum ({@link MonitoredValue#minAllowedSendRate})
	 * and a maximum ({@link MonitoredValue#maxAllowedRefreshRate}) values.
	 * 
	 * @param mPointId The not <code>null</code> nor empty ID of a monitored point
	 * @param newRefreshRate the requested new refresh rate
	 * @return the refresh rate effectively set for the monitored value
	 * @throws PluginException if the monitored value with the passed ID does not exist
	 */
	public long setMonitorPointRefreshRate(String mPointId, long newRefreshRate) throws PluginException {
		Objects.requireNonNull(mPointId, "The monitored point ID can't be null");
		if (mPointId.isEmpty()) {
			throw new IllegalArgumentException("The monitored point ID can't be empty");
		}
		MonitoredValue mVal = monitorPoints.get(mPointId);
		if (mVal==null) {
			throw new PluginException("Monitor point "+mPointId+" does not exist");
		}
		return mVal.setRefreshRate(newRefreshRate);
	}
	
	/**
	 * Enable or disable the periodic sending of notifications.
	 * 
	 * @param mPointId The ID of the monitor point to enable or disable
	 * @param enable if <code>true</code> enables the periodic sending;
	 * @throws PluginException if the monitored value with the passed ID does not exist            
	 */
	public void enableMonitorPointPeriodicNotification(String mPointId, boolean enable) throws PluginException {
		if (Objects.isNull(mPointId) || mPointId.isEmpty()) {
			throw new IllegalArgumentException("The monitored point ID can't be null nor empty");
		}
		// Change the ID if the plugin is replicated
		Optional<MonitoredValue> mVal = Optional.ofNullable(monitorPoints.get(mPointId));
		mVal.orElseThrow(() -> new PluginException("Monitor point "+mPointId+" does not exist"))
			.enablePeriodicNotification(enable);
	}
	
	/**
	 * @return the scheduled executor
	 */
	public static ScheduledExecutorService getScheduledExecutorService() {
		return schedExecutorSvc;
	}

	/**
	 * @return the threadFactory
	 */
	public static ThreadFactory getThreadFactory() {
		return threadFactory;
	}
	
	/**
	 * Set the operational mode of the plugin overriding the
	 * operational mode set in the monitor point values
	 *  
	 * @param opMode The not null operational mode of the plugin
	 * @see #pluginOperationalMode
	 */
	public void setPluginOperationalMode(OperationalMode opMode) {
		Objects.requireNonNull(opMode, "Invalid operational mode");
		pluginOperationalMode=Optional.of(opMode);
		logger.debug("Plugin operational mode {} (operational mode of monitor point values overridden)",pluginOperationalMode.get());
	}
	
	/**
	 * Unset the plugin operational mode so that the 
	 * operational mode of each monitor point value is sent 
	 * to the core of the IAS.
	 */
	public void unsetPluginOperationalMode() {
		pluginOperationalMode = Optional.empty();
		logger.debug("Operational mode of plugin will not override the operational mode of monitor point values");
	}
	
	/**
	 * Return the operational mode of the plugin.
	 *
	 * @return the operational mode of the plugin.
	 */
	public Optional<OperationalMode> getPluginOperationalMode() {
		return pluginOperationalMode;
	}
	
	/**
	 * Set the operational mode of a monitor point value.
	 * <P>
	 * Note that this value is effectively sent to the core of the IAS only if
	 * not overridden by the plugin operational mode 
	 * 
	 * @param mPointId the ID of the monitor point to set the operation mode
	 * @param opMode The not <code>null</code> operational mode to set
	 * @return The old operational mode of the monitor point
	 * @throws PluginException if the monitored value with the passed ID does not exist
	 * @see #pluginOperationalMode
	 */
	public OperationalMode setOperationalMode(String mPointId, OperationalMode opMode) throws PluginException {
		Objects.requireNonNull(opMode, "Invalid operational mode");
		Objects.requireNonNull(mPointId, "The ID of a monitor point can't be null");
		if (mPointId.isEmpty()) {
			throw new IllegalArgumentException("Invalid empty monitor point ID");
		}
		MonitoredValue mVal = monitorPoints.get(mPointId);
		if (mVal==null) {
			throw new PluginException("Monitor point "+mPointId+" does not exist");
		}
		return mVal.setOperationalMode(opMode);
	}
}
