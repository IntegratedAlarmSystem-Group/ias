package org.eso.ias.converter.pluginconsumer;

import java.util.concurrent.TimeUnit;

import org.eso.ias.plugin.publisher.MonitorPointData;

/**
 * 
 * The RAW data feeder feeds the converter with raw data coming 
 * from the remote monitored system.
 * <P>
 * Normally the feeds gets the data produced by the plugins from the kafka
 * server. 
 * The interface allows to feed the converter with other sources decoupling
 * the code from the communication framework.
 * 
 * @author acaproni
 */
public interface RawDataReader {
	
	/**
	 * Synchronously get and return the next value produced by the monitored system.
	 * <P>
	 * If no value is available, the method waits until the timeout elapses.
	 * 
	 * @param timeout The time to wait for the new value
	 * @param timeunit The time unit of the timeout
	 * @return The next value produced by the remote system 
	 *         or <code>null</code> in case of timeout
	 */
	public MonitorPointData get(long timeout, TimeUnit timeunit);

}
