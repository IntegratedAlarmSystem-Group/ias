package org.eso.ias.plugin.publisher;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.concurrent.ScheduledExecutorService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Extends {@link PublisherBase} for publishing data to the IAS core with buffering. 
 * <P>
 * Buffering in this context is at transmission level: 
 * all the collected monitor point values are sent with only one message instead of being 
 * sent one by one.

 * @author acaproni
 *
 */
public abstract class BufferedPublisherBase extends PublisherBase implements MonitorPointSender {
	
	/**
	 * The logger
	 */
	private final Logger logger = LoggerFactory.getLogger(BufferedPublisherBase.class);
	
	/**
	 * The data structure to send to the core of the IAS:
	 * it contains all the monitored point values and implements the buffering.
	 */
	private final BufferedMonitoredSystemData monitorPointsToSend = new BufferedMonitoredSystemData();


	/**
	 * Constructor
	 * 
	 * @param pluginId The identifier of the plugin
	 * @param monitoredSystemId The identifier of the system monitored by the plugin
	 * @param serverName The name of the server
	 * @param port The port of the server
	 * @param executorSvc The executor service
	 */
	public BufferedPublisherBase(
			String pluginId,
			String monitoredSystemId,
			String serverName, 
			int port,
			ScheduledExecutorService executorSvc) {
		super(pluginId,monitoredSystemId,serverName,port,executorSvc);
	}
	
	/**
	 * Provide a implementation of {@link PublisherBase#publish(MonitorPointData)}.
	 * <P>
	 * This method is never called because the {@link BufferedPublisherBase} publishes all the
	 * monitor point values in a single message instead of publishing one message
	 * for each monitor point value.
	 * 
	 * @return 0L: no data sent
	 */
	@Override
	protected final long publish(MonitorPointData mpData) throws PublisherException { 
		return 0L;
	}
	
	/**
	 * Send the passed data to the core of the IAS.
	 * <P>
	 * This method is supposed to effectively sent the data to the core of the IAS
	 * 
	 * @param data The data to send to the core of the IAS
	 * @return The number of bytes sent to the core of the IAS
	 * @throws PublisherException In case of error publishing 
	 */
	protected abstract long publish(BufferedMonitoredSystemData data) throws PublisherException;
	
	/**
	 * Send the monitor points to the core of the IAS.
	 * <P>
	 * The method is synchronized as it can be called by 2 different threads:
	 * when the throttling time interval elapses and if the max size
	 * of the buffer has been reached
	 */
	@Override
	protected synchronized void sendMonitoredPointsToIas() {
		if (monitorPoints.isEmpty()) {
			return;
		}
		if (isStopped() || isClosed()) {
			monitorPoints.clear();
			return;
		}
		// No need to synchronize iso8601dateFormat that is used only 
		// by this (already synchronized) method
		String now = iso8601dateFormat.format(new Date(System.currentTimeMillis()));
		monitorPointsToSend.setPublishTime(now);
		synchronized (monitorPoints) {
			Collection<MonitorPointDataToBuffer> valuesToSend = new ArrayList<>(monitorPoints.values().size());
			monitorPoints.values().forEach(mpv -> {
				valuesToSend.add(new MonitorPointDataToBuffer(mpv));
				monitorPointsSent.incrementAndGet();
			});
			monitorPointsToSend.setMonitorPoints(valuesToSend);
			monitorPoints.clear();
		}
		publishedMessages.incrementAndGet();
		try {
			publish(monitorPointsToSend);
		} catch (PublisherException pe) {
			numOfErrorsSending.incrementAndGet();
			// Errors publishing are ignored
			if (numOfErrorsSending.get()<9) {
				logger.error("Error publishing data",pe);
			} else {
				if (numOfErrorsSending.get()%10L==0) {
					logger.error("Error publishing data (log of many other errors suppressed)",pe);
				}
			}
		}
		monitorPointsToSend.getMonitorPoints().clear();
	}
	
	@Override
	public synchronized void setUp() throws PublisherException {
		super.setUp();
		monitorPointsToSend.setSystemID(pluginId);
		monitorPointsToSend.setMonitoredSystemID(monitoredSystemId);
	}
}
