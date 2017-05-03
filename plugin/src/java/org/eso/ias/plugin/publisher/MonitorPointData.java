package org.eso.ias.plugin.publisher;

import org.eso.ias.plugin.filter.NoneFilter;

/**
 * A java POJO representing a monitor point or alarms
 * to be sent to the IAS.
 * 
 * @see MonitoredSystemData
 * @author acaproni
 *
 */
public class MonitorPointData {
	
	/**
	 * The point in time when the value has been provided 
	 * by the monitored system.
	 * <P>
	 * The meaning of this timestamp depends on the applied filter.
	 * If the filters return the last sampled value like {@link NoneFilter}
	 * then {@link #sampleTime} is the point in time when such sample
	 * has been provided by the remote system.
	 * But what does it represent if the filter returns a the average
	 * of the samples read in a time interval? 
	 */
	private long sampleTime;
	
	/**
	 * The point in time when the input(s) have been
	 * elaborated against the filter to produce the value
	 * that must finally be sent to the IAS core
	 */
	private long filteredTime;
	
	/**
	 * The value of the monitor point after passing the filter 
	 */
	private String value;

	/**
	 * Empty constructor
	 */
	public MonitorPointData() {}

}
