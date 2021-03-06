package org.eso.ias.plugin.publisher.impl;

import org.eso.ias.plugin.publisher.BufferedMonitoredSystemData;
import org.eso.ias.plugin.publisher.BufferedPublisherBase;
import org.eso.ias.plugin.publisher.MonitorPointSender;
import org.eso.ias.plugin.publisher.PublisherException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * <code>ListenerPublisher</code> is an implementation of {@link MonitorPointSender}
 * for testing the {@link BufferedPublisherBase}: monitor points are sent to the
 * listener.
 * 
 * @author acaproni
 *
 */
public class BufferedListenerPublisher extends BufferedPublisherBase {
	
	/**
	 * The interface to be notified about events happening in the {@link BufferedPublisherBase}.
	 * 
	 * @author acaproni
	 *
	 */
	public interface PublisherEventsListener {

		/**
		 * Method invoked when {@link BufferedPublisherBase} invokes setUp
		 */
		public void initialized();
		
		/**
		 * Method invoked when {@link BufferedPublisherBase} invokes tearDown
		 */
		public void closed();
		
		/**
		 * Invoked when the publisher sends data
		 * 
		 * @param data The data published
		 */
		public void dataReceived(BufferedMonitoredSystemData data);
	}
	
	/**
	 * The logger
	 */
	private static final Logger logger = LoggerFactory.getLogger(BufferedListenerPublisher.class);
	
	/**
	 * The listener of events generated by this object.
	 */
	private final PublisherEventsListener listener;
	
	/**
	 * The number of messages 
	 */
	private final AtomicInteger publishedMessages=new AtomicInteger(0);
	
	/**
	 * The number of published monitor points can be greater then
	 * the number of messages published ({@link #publishedMessages}) because
	 * of the throttling.
	 */
	private final AtomicInteger publishedMonitorPoints=new AtomicInteger(0);
	
	/**
	 * Record the number of times {@link #setUp()} has been executed
	 */
	private final AtomicInteger numOfSetUpInvocations=new AtomicInteger(0);
	
	/**
	 * Record the number of times {@link #tearDown()} has been executed
	 */
	private final AtomicInteger numOfTearDownInvocations=new AtomicInteger(0);

	/**
	 * Constructor 
	 *
	 * @param pluginId The identifier of the plugin
	 * @param monitoredSystemId The identifier of the system monitored by the plugin
	 * @param executorSvc The executor service
	 * @param listener The listener of events
	 * 
	 * @see BufferedPublisherBase
	 */
	public BufferedListenerPublisher(
			String pluginId, 
			String monitoredSystemId,
			ScheduledExecutorService executorSvc,
			PublisherEventsListener listener) {
		super(pluginId, monitoredSystemId, executorSvc);
		this.listener=listener;
		logger.info("Created");
	}
	
	/**
	 * Send the passed data to listener.
	 * 
	 * @param data The data to send to the listener
	 * @throws PublisherException never
	 */
	@Override
	protected long publish(BufferedMonitoredSystemData data) throws PublisherException {
		publishedMessages.incrementAndGet();
		publishedMonitorPoints.getAndAdd(data.getMonitorPoints().size());
		listener.dataReceived(data);
		return data.toJsonString().length();
	}

	/**
	 * Performs the initialization.
	 * 
	 * @throws PublisherException never
	 */
	@Override
	protected void start() throws PublisherException {
		listener.initialized();
		numOfSetUpInvocations.incrementAndGet();
	}

	/**
	 * Performs the cleanup.
	 * 
	 * @throws PublisherException in case of error releasing the writer
	 */
	@Override
	protected void shutdown() throws PublisherException {
		listener.closed();
		numOfTearDownInvocations.incrementAndGet();
	}

	/**
	 * @return the publishedMessages
	 */
	public int getPublishedMessages() {
		return publishedMessages.get();
	}

	/**
	 * @return the publishedMonitorPoints
	 */
	public int getPublishedMonitorPoints() {
		return publishedMonitorPoints.get();
	}

	/**
	 * @return the numOfSetUpInvocations
	 */
	public int getNumOfSetUpInvocations() {
		return numOfSetUpInvocations.get();
	}

	/**
	 * @return the numOfTearDownInvocations
	 */
	public int getNumOfTearDownInvocations() {
		return numOfTearDownInvocations.get();
	}

}
