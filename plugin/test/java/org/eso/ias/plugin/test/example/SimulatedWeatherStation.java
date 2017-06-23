package org.eso.ias.plugin.test.example;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The <code>SimulatedWeatherStation</code> produces monitor point values
 * for the dumb weather station of the example.
 * <P>
 * This is only an example and as such has nothing to share
 * with a real weather station.
 * <BR>
 * In a real case, the weather station data can be accessible by the API provided by the 
 * control system of the weather station like for example a REST API, a dedicated socket,
 * a database, a file. 
 * It can also happen that the data must be red directly from a device.
 * <P>
 * For this example we assume that the value changes every  {@link SimulatedMonitorPoint#refreshRate}
 * mseconds. In a real case the value could be updated any time the user gets the value
 * through the API.
 * In that case, the time to read and update the value must be carefully taken into account
 * in order to offer to the core of the IAS the values in a proper way.
 * 
 * 
 * @author acaproni
 *
 */
public class SimulatedWeatherStation {
	
	/**
	 * The logger
	 */
	private static final Logger logger = LoggerFactory.getLogger(SimulatedWeatherStation.class);
	
	/**
	 * The scheduled executor to update the values of the monitored points
	 */
	private static final ScheduledExecutorService schedExSvc = Executors.newScheduledThreadPool(3, new ThreadFactory() {
		@Override
		public Thread newThread(Runnable r) {
			Thread t = new Thread(r);
			t.setDaemon(true);
			return t;
		}
	}); 
	
	/**
	 * The definition of each monitor point.
	 * <P>
	 * The value is updated by a timer thread
	 * 
	 * @author acaproni
	 */
	public enum SimulatedMonitorPoint{
		
		TEMPERATURE(-50,50,2,1,TimeUnit.MINUTES),
		HUMIDITY(10,99,5,1,TimeUnit.MINUTES),
		PRESSURE(800,1040,0.5,30,TimeUnit.SECONDS),
		WIND_SPEED(0,150,30,2,TimeUnit.SECONDS),
		WIND_DIRECTION(0,299,30,2,TimeUnit.SECONDS);
		
		/**
		 * The generator of values
		 */
		private final SimulatedValueGenerator valueGenerator;
		
		/**
		 * Constructor
		 * 
		 * @param min Min possible value
		 * @param max Max possible value
		 * @param delta Max delta between consecutive values
		 * @param refreshRate Refresh time interval
		 * @param unit The time unit of the refresh rate 
		 */
		private SimulatedMonitorPoint(double min, double max, double delta, int refreshRate,TimeUnit unit) {
			valueGenerator = new SimulatedValueGenerator(min, max, delta, refreshRate, unit);
			schedExSvc.scheduleAtFixedRate(valueGenerator, 0, refreshRate, unit);
		}

		/**
		 * @return the actualValue
		 */
		public double getActualValue() {
			return valueGenerator.getActualValue();
		}
	}
	
	
	
	/**
	 * This is the method that the API of the weather station offers to get the
	 * value of a monitor point
	 * 
	 * @param mPoint The monitor point to read
	 * @return The actual value of the simulated monitor point
	 */
	public static double get(SimulatedMonitorPoint mPoint) {
		return mPoint.getActualValue();
	}
	
	/**
	 * initialize the simulated weather station
	 */
	public static void startRemoteSystemConnection() {
		logger.info("Simulated weathe station initialized");
	}
	
	/**
	 * Close the connection with the weather station: terminate the threads to update the values
	 */
	public static void releaseWethaerStation() {
		schedExSvc.shutdown();
	}
}
