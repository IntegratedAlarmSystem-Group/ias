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
 * <P>
 * <EM>Life cycle:<?EM><BR><UL>
 * 	<LI>{@link #setUp()} must be called to allocate the resources needed 
 * for sending massages to the core of the IAS
 *  <LI>{@link #tearDown()} must be called to release the resources: 
 *      no more messages can be sent after calling this method
 * </UL>
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
	
	/**
	 * Initialization method to allocate the resources needed to send
	 * messages to the core of the IAS.
	 * <P>
	 * Even if the publisher is ready to send messages, no message will be
	 * published before calling {@link #startSending()}.
	 * 
	 * @throws PublisherException In case of error acquiring the resources
	 */
	public void setUp() throws PublisherException;
	
	/**
	 * Free all the resources when sending messages to the core of the IAS
	 * is not required anymore.
	 * 
	 * @throws PublisherException In case of errore releasing the resources
	 */
	public void tearDown() throws PublisherException;
	
	/**
	 * Publishing of messages begins after calling this method.
	 * 
	 * @see #stopSending()
	 */
	public void startSending();
	
	/**
	 * Stop sending messages to the core of the IAS.
	 * <P>
	 * Updates to be published when the sender is stopped are lost forever.
	 * 
	 * @see #startSending()
	 */
	public void stopSending();
	
	/**
	 * 
	 * @return <code>true</code> is sending values to the core of the IAS
	 *         has been stopped
	 */
	public boolean isStopped();
}
