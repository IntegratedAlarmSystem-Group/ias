package org.eso.ias.plugin.thread;

import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The singleton thread factory for the plugin.
 * <P>
 * Each thread is assigned a name composed of the {@link #threadName}
 * string plus the {@link #threadIndex} suffix.
 * 
 * @author acaproni
 *
 */
public class PluginThreadFactory implements ThreadFactory {
	
	/**
	 * The thread group to which all the threads
	 * created by the plugin belong
	 */
	private final ThreadGroup threadGroup = new ThreadGroup("Plugin thread group");
	
	/**
	 * The logger
	 */
	private static final Logger logger = LoggerFactory.getLogger(PluginThreadFactory.class);
	
	/**
	 * The name of each thread created by the factory
	 * is composed of this string plus {@link #threadIndex}
	 */
	private static final String threadName = "Plugin thread - ";
	
	/**
	 * The number of created threads, is appended to the {@link #threadName}
	 * to form the name of each thread
	 */
	private static final AtomicInteger threadIndex = new AtomicInteger(0);
	
	/**
	 * The singleton thread factory
	 */
	private static PluginThreadFactory threadFactory;
	
	/**
	 * Build if it is the case and return the plugin
	 * thread factory singleton.
	 * 
	 * @return The thread factory instance
	 */
	public synchronized static ThreadFactory getThreadFactory() {
		if (threadFactory==null) {
			threadFactory = new PluginThreadFactory();
		}
		return threadFactory;
	}

	/**
	 * Constructor
	 * 
	 * @param threadGroup The group to which assign the created threads
	 */
	private PluginThreadFactory() {
		logger.trace("Thread factory created");
	}

	@Override
	public Thread newThread(Runnable arg0) {
		Thread t = new Thread(threadGroup,arg0, threadName+threadIndex.incrementAndGet());
		t.setDaemon(true);
		logger.debug("Thread "+t.getName()+" created");
		return t;
	}
}
