package org.eso.ias.plugin.thread;

import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The thread factory for the plugin.
 * <P>
 * Each thread has a name composed of the {@link #threadName}
 * string plus the {@link #threadIndex} suffix.
 * 
 * @author acaproni
 *
 */
public class PluginThreadFactory implements ThreadFactory {
	
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
	 * The factory assigns the threads to this group 
	 */
	private final ThreadGroup threadGroup;
	
	/**
	 * The number of created threads, is appended to the {@link #threadName}
	 * to form the name of each thread
	 */
	private static final AtomicInteger threadIndex = new AtomicInteger(0);

	public PluginThreadFactory(ThreadGroup threadGroup) {
		if (threadGroup==null) {
			throw new IllegalArgumentException("The thread group can't be null");
		}
		this.threadGroup=threadGroup;
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
