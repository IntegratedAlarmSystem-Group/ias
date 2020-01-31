package org.eso.ias.plugin.publisher.impl;

import org.eso.ias.plugin.publisher.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Date;
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
public class ListenerPublisher extends PublisherBase {
	
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
		 * @param mpData The data published
		 */
		public void dataReceived(MonitorPointData mpData);
	}
	
	/**
	 * The logger
	 */
	private static final Logger logger = LoggerFactory.getLogger(ListenerPublisher.class);
	
	/**
	 * The listener of events generated by this object.
	 */
	private final PublisherEventsListener listener;
	
	/**
	 * The number of published messages is the same of the number
	 * of monitor points sent.
	 */
	private final AtomicInteger publishedMessages=new AtomicInteger(0);
	
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
	 * @param listener the listener
	 * 
	 * @see BufferedPublisherBase#BufferedPublisherBase(String, String, ScheduledExecutorService)
	 */
	public ListenerPublisher(
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
	 * @param mpData The data to send to the listener
	 * @throws PublisherException never
	 */
	@Override
	protected long publish(MonitorPointData mpData) throws PublisherException {
		publishedMessages.incrementAndGet();

		synchronized (iso8601dateFormat) {
			mpData.setPublishTime(iso8601dateFormat.format(new Date(System.currentTimeMillis())));
		}

		listener.dataReceived(mpData);
		return mpData.toJsonString().length();
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

	/**
	 * @return the publishedMessages
	 */
	public int getPublishedMessages() {
		return publishedMessages.get();
	}

}
