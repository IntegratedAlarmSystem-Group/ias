package org.eso.ias.plugin.thread;

import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The ScheduledExecutorService for the plugin.
 * <P>
 * The executor is a singleton encapsulated by <code>ScheduledExecutorService</code>.
 * The execution of {@link ScheduledExecutorService} methods is delegated to the singleton.
 * 
 * Note: implementing the {@link ScheduledExecutorService} would have required
 *       to write much more code to delegate all the methods of the super interfaces
 *       (<code>Executor</code> and <code>ExecutorService</code> for instance).
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
	 * The PluginScheduledExecutorSvc singleton
	 */
	private static PluginScheduledExecutorSvc singleton = null;
	
	/**
	 * Return the PluginScheduledExecutorSvc singleton
	 * @return The singleton
	 */
	public synchronized static final PluginScheduledExecutorSvc getInstance() {
		if (singleton == null) {
			singleton = new PluginScheduledExecutorSvc();
		}
		return singleton;
	}
	
	/**
	 * The constructor
	 */
	private PluginScheduledExecutorSvc() {
		logger.debug("PluginScheduledExecutorSvc singleton created");
	}
	
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
	 * @see java.util.concurrent.ScheduledExecutorService#scheduleAtFixedRate(java.lang.Runnable, long, long, java.util.concurrent.TimeUnit)
	 */
	public ScheduledFuture<?> scheduleAtFixedRate(Runnable command, long initialDelay, long period, TimeUnit unit) {
		return schedExecutorSvc.scheduleAtFixedRate(command, initialDelay, period, unit);
	}

	/**
	 * @see java.util.concurrent.ScheduledExecutorService#schedule(java.util.concurrent.Callable, long, java.util.concurrent.TimeUnit)
	 */
	public <V> ScheduledFuture<V> schedule(Callable<V> callable, long delay, TimeUnit unit) {
		return schedExecutorSvc.schedule(callable, delay, unit);
	}

	/** 
	 * @see java.util.concurrent.ScheduledExecutorService#schedule(java.lang.Runnable, long, java.util.concurrent.TimeUnit)
	 */
	public ScheduledFuture<?> schedule(Runnable command, long delay, TimeUnit unit) {
		return schedExecutorSvc.schedule(command, delay, unit);
	}

	/**
	 * Delegate to to the {@link ScheduledThreadPoolExecutor}
	 * 
	 * @see java.util.concurrent.ScheduledExecutorService#scheduleWithFixedDelay(java.lang.Runnable, long, long, java.util.concurrent.TimeUnit)
	 */
	public ScheduledFuture<?> scheduleWithFixedDelay(Runnable command, long initialDelay, long delay, TimeUnit unit) {
		return schedExecutorSvc.scheduleWithFixedDelay(command, initialDelay, delay, unit);
	}
}
