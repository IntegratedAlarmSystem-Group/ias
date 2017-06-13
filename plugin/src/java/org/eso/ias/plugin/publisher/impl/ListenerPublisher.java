package org.eso.ias.plugin.publisher.impl;

import java.util.concurrent.ScheduledExecutorService;

import org.eso.ias.plugin.publisher.BufferedPublisherBase;
import org.eso.ias.plugin.publisher.MonitorPointData;
import org.eso.ias.plugin.publisher.MonitorPointSender;
import org.eso.ias.plugin.publisher.PublisherBase;
import org.eso.ias.plugin.publisher.PublisherException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
		 * @param data The data published
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
	private volatile int publishedMessages=0;
	
	/**
	 * Record the number of times {@link #setUp()} has been executed
	 */
	private volatile int numOfSetUpInvocations=0;
	
	/**
	 * Record the number of times {@link #tearDown()} has been executed
	 */
	private volatile int numOfTearDownInvocations=0;

	/**
	 * Constructor 
	 * @param pluginId
	 * @param serverName
	 * @param port
	 * @param executorSvc
	 * @see BufferedPublisherBase
	 */
	public ListenerPublisher(
			String pluginId, 
			String serverName, 
			int port, 
			ScheduledExecutorService executorSvc,
			PublisherEventsListener listener) {
		super(pluginId, serverName, port, executorSvc);
		this.listener=listener;
		logger.info("Created");
	}
	
	@Override
	protected long publish(MonitorPointData mpData) throws PublisherException {
		publishedMessages++;
		listener.dataReceived(mpData);
		return mpData.toJsonString().length();
	}

	@Override
	protected void start() throws PublisherException {
		listener.initialized();
		numOfSetUpInvocations++;
	}

	@Override
	protected void shutdown() throws PublisherException {
		listener.closed();
		numOfTearDownInvocations++;
	}

	/**
	 * @return the numOfSetUpInvocations
	 */
	public int getNumOfSetUpInvocations() {
		return numOfSetUpInvocations;
	}

	/**
	 * @return the numOfTearDownInvocations
	 */
	public int getNumOfTearDownInvocations() {
		return numOfTearDownInvocations;
	}

	/**
	 * @return the publishedMessages
	 */
	public int getPublishedMessages() {
		return publishedMessages;
	}

}
