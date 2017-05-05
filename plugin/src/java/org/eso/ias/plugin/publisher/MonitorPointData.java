package org.eso.ias.plugin.publisher;

import java.text.SimpleDateFormat;
import java.util.Date;

import org.eso.ias.plugin.filter.FilteredValue;
import org.eso.ias.plugin.filter.NoneFilter;

/**
 * A java POJO representing a monitor point or alarm
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
	private String sampleTime;
	
	/**
	 * The point in time when the input(s) have been
	 * elaborated against the filter to produce the value
	 * that must finally be sent to the IAS core
	 */
	private String filteredTime;
	
	/**
	 * The value of the monitor point after passing the filter 
	 */
	private String value;
	
	/**
	 * The identifier of the monitor point
	 */
	private String id;
	
	/**
	 * ISO 8601 date formatter
	 */
	private final SimpleDateFormat iso8601dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.S");

	/**
	 * Empty constructor
	 */
	public MonitorPointData() {}
	
	/**
	 * Constructor
	 * 
	 * @param value The filtered value produced by the monitored system
	 */
	public MonitorPointData(FilteredValue value) {
		synchronized (iso8601dateFormat) {
			setFilteredTime(iso8601dateFormat.format(new Date(value.filteredTimestamp)));
		}
		setId(value.id);
		setValue(value.value.toString());
		synchronized (iso8601dateFormat) {
			setSampleTime(iso8601dateFormat.format(new Date(value.producedTimestamp)));
		}
	}

	/**
	 * @return the sampleTime
	 */
	public String getSampleTime() {
		return sampleTime;
	}

	/**
	 * @param sampleTime the sampleTime to set
	 */
	public void setSampleTime(String sampleTime) {
		this.sampleTime = sampleTime;
	}

	/**
	 * @return the filteredTime
	 */
	public String getFilteredTime() {
		return filteredTime;
	}

	/**
	 * @param filteredTime the filteredTime to set
	 */
	public void setFilteredTime(String filteredTime) {
		this.filteredTime = filteredTime;
	}

	/**
	 * @return the value
	 */
	public String getValue() {
		return value;
	}

	/**
	 * @param value the value to set
	 */
	public void setValue(String value) {
		this.value = value;
	}

	/**
	 * @return the id
	 */
	public String getId() {
		return id;
	}

	/**
	 * @param id the id to set
	 */
	public void setId(String id) {
		this.id = id;
	}

}
