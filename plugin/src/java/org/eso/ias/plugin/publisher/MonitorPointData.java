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
		setId(value.id);
		setValue(value.value.toString());
		synchronized (iso8601dateFormat) {
			setSampleTime(iso8601dateFormat.format(new Date(value.producedTimestamp)));
			setFilteredTime(iso8601dateFormat.format(new Date(value.filteredTimestamp)));
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

	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		StringBuilder ret = new StringBuilder("MonitorPointData(id=");
		ret.append(id);
		ret.append(", value=");
		ret.append(value);
		ret.append(", filteredTime=");
		ret.append(filteredTime);
		ret.append(", sampleTime=");
		ret.append(sampleTime);
		ret.append(')');
		return ret.toString();
	}

	/**
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((filteredTime == null) ? 0 : filteredTime.hashCode());
		result = prime * result + ((id == null) ? 0 : id.hashCode());
		result = prime * result + ((sampleTime == null) ? 0 : sampleTime.hashCode());
		result = prime * result + ((value == null) ? 0 : value.hashCode());
		return result;
	}

	/**
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		MonitorPointData other = (MonitorPointData) obj;
		if (filteredTime == null) {
			if (other.filteredTime != null)
				return false;
		} else if (!filteredTime.equals(other.filteredTime))
			return false;
		if (id == null) {
			if (other.id != null)
				return false;
		} else if (!id.equals(other.id))
			return false;
		if (sampleTime == null) {
			if (other.sampleTime != null)
				return false;
		} else if (!sampleTime.equals(other.sampleTime))
			return false;
		if (value == null) {
			if (other.value != null)
				return false;
		} else if (!value.equals(other.value))
			return false;
		return true;
	}
}
