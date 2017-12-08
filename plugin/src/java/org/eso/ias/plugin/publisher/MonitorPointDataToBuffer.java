package org.eso.ias.plugin.publisher;

import java.text.SimpleDateFormat;
import java.util.Date;

import org.eso.ias.plugin.ValueToSend;
import org.eso.ias.plugin.filter.NoneFilter;

/**
 * A java POJO representing a monitor point or alarm
 * to be sent to the IAS in a buffered way.
 * <P>
 * A <code>MonitorPointDataToBuffer</code> will be sent encapsulated 
 * in a {@link BufferedMonitoredSystemData} i.e. for buffered publishers.
 * 
 * @see BufferedMonitoredSystemData
 * @author acaproni
 *
 */
public class MonitorPointDataToBuffer {
	
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
	protected String sampleTime;
	
	/**
	 * The point in time when the input(s) have been
	 * elaborated against the filter to produce the value
	 * that must finally be sent to the IAS core
	 */
	protected String filteredTime;
	
	/**
	 * The value of the monitor point after passing the filter 
	 */
	protected String value;
	
	/**
	 * The identifier of the monitor point
	 */
	protected String id;
	
	/**
	 * The operational mode
	 * 
	 * @see OperationalMode
	 */
	protected String operationalMode;
	
	/**
	 * The validity
	 * 
	 * @see IasValidity
	 */
	protected String validity;
	
	/**
	 * ISO 8601 date formatter
	 */
	protected final SimpleDateFormat iso8601dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.S");

	/**
	 * Empty constructor
	 */
	public MonitorPointDataToBuffer() {}
	
	/**
	 * Constructor
	 * 
	 * @param value The filtered value produced by the monitored system
	 */
	public MonitorPointDataToBuffer(ValueToSend value) {
		setId(value.id);
		setValue(value.value.toString());
		synchronized (iso8601dateFormat) {
			setSampleTime(iso8601dateFormat.format(new Date(value.producedTimestamp)));
			setFilteredTime(iso8601dateFormat.format(new Date(value.filteredTimestamp)));
		}
		setOperationalMode(value.operationalMode.toString().toUpperCase());
		setValidity(value.iasValidity.toString().toUpperCase());
	}

	/**
	 * @return the sampleTime
	 */
	public String getSampleTime() {
		return sampleTime;
	}

	/**
	 * Set the time of the sample 
	 * 
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
	 * Set the filtered timestamp
	 * 
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
	 * Set the value of th emonitor point
	 * 
	 * @param value the value to set
	 */
	public void setValue(String value) {
		this.value = value;
	}

	/**
	 * @return the id of the monitor point
	 */
	public String getId() {
		return id;
	}

	/**
	 * Set the ID of the monitor point
	 * 
	 * @param id the id to set
	 */
	public void setId(String id) {
		this.id = id;
	}

	/**
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		StringBuilder ret = new StringBuilder("MonitorPointDataToBuffer(id=");
		ret.append(id);
		ret.append(", value=");
		ret.append(value);
		ret.append(", filteredTime=");
		ret.append(filteredTime);
		ret.append(", sampleTime=");
		ret.append(sampleTime);
		ret.append(", operational mode=");
		ret.append(operationalMode);
		ret.append(", validity=");
		ret.append(validity);
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
		result = prime * result + ((operationalMode == null) ? 0 : operationalMode.hashCode());
		result = prime * result + ((validity == null) ? 0 : validity.hashCode());
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
		MonitorPointDataToBuffer other = (MonitorPointDataToBuffer) obj;
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
		if (operationalMode == null) {
			if (other.operationalMode != null)
				return false;
		} else if (!operationalMode.equals(other.operationalMode))
			return false;
		if (validity == null) {
			if (other.validity != null)
				return false;
		} else if (!validity.equals(other.validity))
			return false;
		if (value == null) {
			if (other.value != null)
				return false;
		} else if (!value.equals(other.value))
			return false;
		return true;
	}

	/**
	 * @return the operationalMode
	 */
	public String getOperationalMode() {
		return operationalMode;
	}

	/**
	 * @param operationalMode the operationalMode to set
	 */
	public void setOperationalMode(String operationalMode) {
		this.operationalMode = operationalMode;
	}

	/**
	 * @return the validity
	 */
	public String getValidity() {
		return validity;
	}

	/**
	 * param validity the validity
	 */
	public void setValidity(String validity) {
		this.validity = validity;
	}
}
