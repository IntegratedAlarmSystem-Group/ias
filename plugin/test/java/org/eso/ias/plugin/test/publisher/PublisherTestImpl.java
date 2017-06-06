package org.eso.ias.plugin.test.publisher;

import java.util.concurrent.ScheduledExecutorService;

import org.eso.ias.plugin.publisher.MonitorPointSender;
import org.eso.ias.plugin.publisher.MonitoredSystemData;
import org.eso.ias.plugin.publisher.PublisherBase;
import org.eso.ias.plugin.publisher.PublisherException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <code>PublisherImpl</code> is an implementation of {@link MonitorPointSender}
 * for testing the {@link PublisherBase}.
 * 
 * @author acaproni
 *
 */
public class PublisherTestImpl extends PublisherBase {
	
	/**
	 * The logger
	 */
	private static final Logger logger = LoggerFactory.getLogger(PublisherTestImpl.class);
	
	/**
	 * The listener of events generated by this object.
	 */
	private final PublisherEventsListener listener;

	/**
	 * Constructor 
	 * @param pluginId
	 * @param serverName
	 * @param port
	 * @param executorSvc
	 * @see PublisherBase
	 */
	public PublisherTestImpl(
			String pluginId, 
			String serverName, 
			int port, 
			ScheduledExecutorService executorSvc,
			PublisherEventsListener listener) {
		super(pluginId, serverName, port, executorSvc);
		this.listener=listener;
		logger.info("Created");
	}
	
	/**
	 * The number of messages 
	 */
	private volatile int publishedMessages=0;
	
	/**
	 * The number of published monitor points can be greater then
	 * the number of messages published ({@link #publishedMessages}) because
	 * of the throttling.
	 */
	private volatile int publishedMonitorPoints=0;
	
	/**
	 * Record the number of times {@link #setUp()} has been executed
	 */
	private volatile int numOfSetUpInvocations=0;
	
	/**
	 * Record the number of times {@link #tearDown()} has been executed
	 */
	private volatile int numOfTearDownInvocations=0;

	@Override
	protected void publish(MonitoredSystemData data) {
		logger.info("Subsystem {} is going to publish {} monitor points",data.getSystemID(),data.getMonitorPoints().size());
		publishedMessages++;
		publishedMonitorPoints+=data.getMonitorPoints().size();
		listener.dataReceived(data);
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
	 * @return the publishedMessages
	 */
	public int getPublishedMessages() {
		return publishedMessages;
	}

	/**
	 * @return the publishedMonitorPoints
	 */
	public int getPublishedMonitorPoints() {
		return publishedMonitorPoints;
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

}
