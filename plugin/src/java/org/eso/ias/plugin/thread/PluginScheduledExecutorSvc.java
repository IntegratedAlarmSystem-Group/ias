package org.eso.ias.plugin.thread;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The ScheduledExecutorService for the plugin.
 * 
 * @author acaproni
 *
 */
public class PluginScheduledExecutorSvc {
	
	private static final Logger logger = LoggerFactory.getLogger(PluginScheduledExecutorSvc.class);
	
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
	 * The scheduled executor service
	 */
	public static final ScheduledExecutorService schedExecutorSvc= Executors.newScheduledThreadPool(schedExecutorPoolSize, PluginThreadFactory.getThreadFactory());
	
	/**
	 * This method must be called when finished using the object 
	 * to free the allocated resources. 
	 */
	public void shutdown() {
		logger.info("Shutting down the scheduled executor service");
		schedExecutorSvc.shutdown();
		try {
			// Wait a while for existing tasks to terminate
			if (!schedExecutorSvc.awaitTermination(5, TimeUnit.SECONDS)) {
				logger.info("Not all threads terminated: trying to force the termination");
				schedExecutorSvc.shutdownNow(); 
				// Wait a while for tasks to respond to being cancelled
				if (schedExecutorSvc.awaitTermination(10, TimeUnit.SECONDS))
					System.err.println("Pool did not terminate");
			}
		} catch (InterruptedException ie) {
			schedExecutorSvc.shutdownNow();
			Thread.currentThread().interrupt();
		}
	}
}
