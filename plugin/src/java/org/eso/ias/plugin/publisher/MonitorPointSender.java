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
 * each value as soon as it arrives but collect them and send all at once
 * when the throttling time (msec) expires.
 * 
 * @author acaproni
 */
public interface MonitorPointSender {
	
	/**
	 * Offer a monitor point to the publisher for sending to the core
	 * 
	 * @param monitorPoint
	 */
	void offer(Optional<FilteredValue> monitorPoint);
}
