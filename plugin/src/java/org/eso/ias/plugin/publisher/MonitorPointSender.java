package org.eso.ias.plugin.publisher;

import java.util.Optional;

import org.eso.ias.plugin.filter.FilteredValue;

/**
 * The <code>MonitorPointSender</code> defines the interface
 * to send monitor point values and alarms to the IAS core.
 * <P>
 * When a value is received from the monitored system and,
 * if it is the case, filtered for noise, it is ready to be
 * sent to the IAS core by invoking {@link #offer(Optional)}.
 * <P> 
 * To avoid flooding the network, the sender must not send
 * each value as soon as it arrives but group them and send all 
 * at once when the throttling time (msec) expires.
 * For that reason, the number of messages sent to the core is 
 * expected to be less than the number of monitor points
 * submitted by calling {@link #offer(Optional)}.
 * <P>
 * Implementers of this class, provides the number of messages
 * sent to the core of the IAS in the last observation period
 * ({@link #numOfMessagesSent()}).
 * 
 * @author acaproni
 */
public interface MonitorPointSender {
	
	/**
	 * Offer a monitor point to the publisher for sending to the core
	 * 
	 * @param monitorPoint
	 */
	public void offer(Optional<FilteredValue> monitorPoint) throws PublisherException;
	
	/**
	 * The number of messages sent to the core of the IAS
	 * before the last invocation of this method.
	 * <P>
	 * When this method is invoked, the counter is reset to allow to
	 * count the messages between two consecutive invocations of this method. 
	 * 
	 * @return The number of messages sent to the core of the IAS
	 */
	public long numOfMessagesSent();
}
