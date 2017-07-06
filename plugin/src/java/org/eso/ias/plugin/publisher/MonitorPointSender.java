package org.eso.ias.plugin.publisher;

import java.util.Optional;

import org.eso.ias.plugin.ValueToSend;

/**
 * The <code>MonitorPointSender</code> defines the interface
 * to send monitor point values and alarms to the IAS core.
 * <P>
 * When a value is received from the monitored system and,
 * if it is the case, filtered for noise, it is finally ready to be
 * sent to the IAS core by invoking {@link #offer(ValueToSend)}
 * <P> 
 * Implementers of this class, provides statistics collected during
 * the last observation period, {@link #getStats()}.
 * <P>
 * <EM>Life cycle:</EM><BR>
 * <UL>
 * 	<LI>{@link #setUp()} must be called to allocate the resources needed 
 * for sending massages to the core of the IAS
 *  <LI>{@link #tearDown()} must be called to release the resources: 
 *      no more messages can be sent after calling this method
 * </UL>
 * <P>
 * Offering an item to a <code>MonitorPointSender</code> does not ensure
 * that it is really sent to the final destination but the producer of
 * monitor pont values is supposed to continue providing monitor point
 * values at the proper time intervals.
 * <BR>
 * The sender is, in fact, totally decoupled from the core of the IAS
 * and its functioning is not affected by the availability of the core
 * or the transport mechanism.
 * This allows to shutdown the transport framework or the core of
 * the IAS without affecting the functioning of the plugin that results,
 * de facto, totally decoupled from the IAS.
 * 
 * @author acaproni
 */
public interface MonitorPointSender {
	
	/**
	 * The statistics provided by the sender in the last time interval.
	 * 
	 * @author acaproni
	 *
	 */
	public class SenderStats {

		/**
		 * The number of messages sent to the core of the IAS.
		 * <P>
		 * If messages are buffered, this number will probably be
		 * less then the number of monitor point values sent ({@link #numOfMonitorPointValuesSent}).
		 */
		public final long numOfMessagesSent;
		
		/**
		 * The number of monitor point values sent to the core of the IAS.
		 * 
		 * @see #numOfMessagesSent
		 */
		public final long numOfMonitorPointValuesSent;
		
		/**
		 * The number of monitor point values submitted to the publisher.
		 * <P>
		 * The number of monitor points offered to the publisher for sending in {@link MonitorPointSender#offer(ValueToSend)}
		 * <BR>To reduce the network traffic and prevent misbehaving plugins from flooding the network, the publisher
		 * can send less monitor point values then those submitted (for example sending only the last
		 * value between all offered in the throttling interval).
		 * 
		 * @see #numOfMessagesSent
		 */
		public final long numOfMonitorPointValuesSubmitted;
		
		/**
		 * The number of bytes sent to the core of the IAS
		 * <P>
		 * This is the total number of bytes, including header informations and so on.
		 */
		public final long numOfBytesSent;
		
		/**
		 * The number of errors publishing monitor point values to the core of the IAS
		 */
		public final long numOfErrorsPublishing;
		
		/**
		 * Constructor
		 * 
		 * @param numOfMessagesSent The number of messages sent in the last time interval
		 * @param numOfMonitorPointValuesSent The number of monitor point values sent in the last time interval
		 * @param numOfMonitorPointValuesSubmitted: The number of monitor point values submitted in the last time interval
		 * @param numOfBytesSent The number of bytes sent in the last time interval
		 * @param numOfErrorsPublishing The number of errors reported while publishing 
		 */
		public SenderStats(
				long numOfMessagesSent,
				long numOfMonitorPointValuesSent, 
				long numOfMonitorPointValuesSubmitted, 
				long numOfBytesSent,
				long numOfErrorsPublishing) {
			super();
			this.numOfMessagesSent = numOfMessagesSent;
			this.numOfMonitorPointValuesSent = numOfMonitorPointValuesSent;
			this.numOfMonitorPointValuesSubmitted=numOfMonitorPointValuesSubmitted;
			this.numOfBytesSent = numOfBytesSent;
			this.numOfErrorsPublishing=numOfErrorsPublishing;
		}
	}
	
	/**
	 * Offer a monitor point to the publisher for sending to the core
	 * 
	 * @param monitorPoint The not <code>null</code> monitor point to be sent to the IAS
	 */
	public void offer(ValueToSend monitorPoint);
	
	/**
	 * The statistics collected by the publisher after the previous invocation of this method.
	 * <P>
	 * Calling this method resets all the counters. 
	 * 
	 * @return The statistics collected by the publisher
	 */
	public SenderStats getStats();
	
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
	 * @throws PublisherException In case of error releasing the resources
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
	 * @return <code>true</code> if sending values to the core of the IAS
	 *         has been stopped
	 */
	public boolean isStopped();
	
	/**
	 * 
	 * @return <code>true</code> if the sender has been closed
	 */
	public boolean isClosed();
}
