package org.eso.ias.test.simpleloop.plugin;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicReference;

import org.eso.ias.heartbeat.HbMsgSerializer;
import org.eso.ias.heartbeat.HbProducer;
import org.eso.ias.heartbeat.publisher.HbKafkaProducer;
import org.eso.ias.heartbeat.serializer.HbJsonSerializer;
import org.eso.ias.plugin.Plugin;
import org.eso.ias.plugin.Sample;
import org.eso.ias.plugin.config.PluginConfig;
import org.eso.ias.plugin.config.PluginConfigException;
import org.eso.ias.plugin.config.PluginConfigFileReader;
import org.eso.ias.plugin.publisher.MonitorPointSender;
import org.eso.ias.plugin.publisher.PublisherException;
import org.eso.ias.plugin.publisher.impl.KafkaPublisher;
import org.eso.ias.types.OperationalMode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The simple loop plugin that generates the 
 * monitor point for the SimpleLoop test.
 * 
 * The plugin runs for {@link #minutesToRun} minutes, 
 * increasing or decreasing the value of the monitor point 
 * at each step.
 * 
 * @author acaproni
 *
 */
public class SimpleLoopPlugin extends Plugin implements Runnable {
	
	/**
	 * The unique ID of this plugin
	 */
	public final static String pluginId = "SimpleLoopPlugin";
	
	/**
	 * The ID of the simulated monitored system
	 */
	public final static String monSysId = "SimulatedSystem";
	
	/**
	 * The ID of the monitor point published 
	 * by hte plugin
	 */
	public final static String mPointId = "MonitorPoint";
	
	/**
	 * The name of the server where kafka runs
	 */
	private static final String serverName="localhost";
	
	/**
	 * The amount of minutes to keep the plugin running
	 */
	public static final int minutesToRun = 5;
	
	/**
	 * The port 
	 */
	private static final int port = 9092;
	
	/**
	 * The min value assigned by this plugin to the monitor point
	 */
	private static final double min = -30;
	
	/**
	 * The max value assigned by this plugin to the monitor point
	 */
	private static final double max = 40;
	
	/**
	 * The step to increase decrease the value of 
	 * the monitor point at each step
	 */
	private static final double step = 0.5;
	
	/**
	 * <code>true</code> if the plugin 
	 * is increasing the value of the monitor point at
	 * each step; <code>false</code> otherwise
	 */
	private boolean increasing = true;
	
	/** 
	 * The value of the monitor point
	 * to be increased or decreased at each step
	 */
	private double valueToSend = 0;
	
	/**
	 * The time interval between steps (seconds)
	 */
	private static final int stepTime = 1;
	
	/**
	 * The periodic thread
	 */
	private volatile AtomicReference<ScheduledFuture<?>> future = new AtomicReference<ScheduledFuture<?>>(null);
	
	/**
	 * The logger
	 */
	private static final Logger logger = LoggerFactory.getLogger(SimpleLoopPlugin.class);

	/**
	 * Constructor
	 * 
	 * @param config The plugin coinfiguration
	 * @param sender The publisher of monitor point values to the IAS core
	 * @param hbProducer the publisher of HBs
	 */
	public SimpleLoopPlugin(
			PluginConfig config, 
			MonitorPointSender sender,
			HbProducer hbProducer) {
		super(config,sender,hbProducer);
	}
	
	/**
	 * The thread to send the value at each time interval
	 */
	@Override
	public void run() {
		Sample sample = new Sample(Double.valueOf(valueToSend));
		try {
			super.updateMonitorPointValue(mPointId, sample);
			logger.debug("Monitor point {} submitted with a value of {}",mPointId,sample.value.toString());
		} catch (Exception e) {
			logger.error("Error submitting the monitor point",e);
		}
		valueToSend = (increasing)? valueToSend+step : valueToSend-step;
		if (valueToSend>=max) {
			increasing = false;
		} else if (valueToSend<=min) {
			increasing = true;
		}
		
	}

	/**
	 * The main
	 *  
	 * @param args The name of the config file
	 */
	public static void main(String[] args) {
		if (args.length<1) {
			throw new IllegalArgumentException("Wrong number of params in cmd line: config file name expected"); 
		}
		
		String configFileName = args[0];
		logger.info("Reading {} config file",configFileName);
		
		PluginConfig config=null;
		try {
			PluginConfigFileReader jsonFileReader = new PluginConfigFileReader(new File(configFileName));
			Future<PluginConfig> futurePluginConfig = jsonFileReader.getPluginConfig();
			config = futurePluginConfig.get(1, TimeUnit.MINUTES);
		} catch (FileNotFoundException fnfe) {
			throw new IllegalArgumentException("Cannot read config file "+fnfe);
		} catch (PluginConfigException pce) {
			logger.error("Exception reading configuratiopn",pce);
			System.exit(-1);
		} catch (InterruptedException ie) {
			logger.error("Interrupted",ie);
			System.exit(-1);
		} catch (TimeoutException te) {
			logger.error("Timeout reading configuration",te);
			System.exit(-1);
		} catch (ExecutionException ee) {
			logger.error("Execution error",ee);
			System.exit(-1);
		}
		logger.info("Configuration successfully read");
		
		KafkaPublisher publisher = new KafkaPublisher(
				pluginId, 
				monSysId, 
				serverName, 
				port,
				Plugin.getScheduledExecutorService());
		
		logger.info("kafka publisher created");
		
		/**
		 * Instantiate the plugin
		 */
		SimpleLoopPlugin plugin = new SimpleLoopPlugin(
				config, 
				publisher,
				new HbKafkaProducer(pluginId, new HbJsonSerializer()));
		logger.info("Plugin built");
		
		try {
			plugin.start();
		} catch (Exception e) {
			logger.error("Exception starting the plugin",e);
		}
		
		logger.info("Let the plugin run for {} minutes",minutesToRun);
		try {
			Thread.sleep(TimeUnit.MILLISECONDS.convert(minutesToRun, TimeUnit.MINUTES));
			logger.info("Time expired: shuttnig down");
		} catch (Exception e) {
			logger.error("Intterrupted!",e);
		}
		
		plugin.shutdown();
		logger.info("Done.");
		
	}

	/**
	 * Override the start method of the plugin to start the thread
	 * to generate the sample
	 */
	@Override
	public void start() throws PublisherException {
		super.start();
		
		setPluginOperationalMode(OperationalMode.OPERATIONAL);
		
		// Start the thread to send values of the monitor point
		future.set(Plugin.getScheduledExecutorService().scheduleAtFixedRate(this, stepTime, stepTime, TimeUnit.SECONDS));
	}

	/**
	 * Override the shutdown method of the plugin to stop the thread
	 * to generate the sample
	 */
	@Override
	public void shutdown() {
		ScheduledFuture<?> f = future.getAndSet(null);
		if (f!=null) {
			f.cancel(false);
		}
		super.shutdown();
	}

}
