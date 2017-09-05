package org.eso.ias.plugin.publisher;

import java.util.Date;
import java.util.Objects;

import org.eso.ias.plugin.ValueToSend;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * A java POJO representing a monitor point or alarm
 * to be sent to the IAS.
 * <P>
 * A <code>MonitorPointData</code> will be be sent as it to the core of the IAS;
 * it is not meant to be buffered before sending.
 * 
 * @see BufferedMonitoredSystemData
 * @author acaproni
 *
 */
public class MonitorPointData extends MonitorPointDataToBuffer{
	
	/**
	 * The id of the plugin.
	 */
	private String pluginID;
	
	/**
	 * The id of the system monitored by the plugin.
	 */
	private String monitoredSystemID;
	
	/**
	 * ISO-8601 formatted time when the 
	 * data structure has been sent to the IAS core
	 */
	private String publishTime;
	
	/**
	 * The mapper to convert this pojo in a JSON string and vice-versa.
	 */
	private static final ObjectMapper MAPPER = new ObjectMapper();

	/**
	 * Empty constructor
	 */
	public MonitorPointData() {}
	
	/**
	 * Constructor
	 * 
	 * @param pluginID: The ID of the plugin
	 * @param monitoredSystemID: The id of the system monitored by the plugin
	 * @param value The filtered value produced by the monitored system
	 */
	public MonitorPointData(String pluginID, String monitoredSystemID,ValueToSend value) {
		super(value);
		setPluginID(pluginID);
		setMonitoredSystemID(monitoredSystemID);
		synchronized (iso8601dateFormat) {
			setPublishTime(iso8601dateFormat.format(new Date(System.currentTimeMillis())));
		}
	}
	
	/**
	 * @return the pluginID
	 */
	public String getPluginID() {
		return pluginID;
	}

	/**
	 * @param pluginID the pluginID to set
	 */
	public void setPluginID(String systemID) {
		Objects.requireNonNull(systemID);
		this.pluginID = systemID;
	}

	/**
	 * @return the publishTime
	 */
	public String getPublishTime() {
		return publishTime;
	}

	/**
	 * @param publishTime the publishTime to set
	 */
	public void setPublishTime(String publishTime) {
		this.publishTime = publishTime;
	}

	/**
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		StringBuilder ret = new StringBuilder("MonitorPointData(id=");
		ret.append(id);
		ret.append(", SystemID=");
		ret.append(pluginID);
		ret.append(", published at ");
		ret.append(publishTime);
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
		result = prime * result + ((publishTime == null) ? 0 : publishTime.hashCode());
		result = prime * result + ((sampleTime == null) ? 0 : sampleTime.hashCode());
		result = prime * result + ((pluginID == null) ? 0 : pluginID.hashCode());
		result = prime * result + ((monitoredSystemID == null) ? 0 : monitoredSystemID.hashCode());
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
		if (publishTime == null) {
			if (other.publishTime != null)
				return false;
		} else if (!publishTime.equals(other.publishTime))
			return false;
		if (sampleTime == null) {
			if (other.sampleTime != null)
				return false;
		} else if (!sampleTime.equals(other.sampleTime))
			return false;
		if (pluginID == null) {
			if (other.pluginID != null)
				return false;
		} else if (!pluginID.equals(other.pluginID))
			return false;
		if (monitoredSystemID == null) {
			if (other.monitoredSystemID != null)
				return false;
		} else if (!monitoredSystemID.equals(other.monitoredSystemID))
			return false;
		if (value == null) {
			if (other.value != null)
				return false;
		} else if (!value.equals(other.value))
			return false;
		return true;
	}
	
	/**
	 * Return a JSON string for this object.
	 * 
	 * @return A Json string representing this object
	 * @throws PublisherException In case of error generating the JSON string
	 */
	public String toJsonString() throws PublisherException {
		try {
			return MAPPER.writeValueAsString(this);
		} catch (JsonProcessingException jpe) {
			throw new PublisherException("Error creating the JSON string", jpe);
		}
	}
	
	/**
	 * Build and return a {@link MonitorPointData} parsing the passed JSON string
	 * 
	 * @param jsonString The JSON string with the monitor point value
	 * @return the monitor point built parsing the passed JSON string
	 * @throws PublisherException in case of error building the object
	 */
	public static MonitorPointData fromJsonString(String jsonString) throws PublisherException {
		if (jsonString==null || jsonString.isEmpty()) {
			throw new IllegalArgumentException("Invalid string");
			
		}
		try {
			return MAPPER.readValue(jsonString,MonitorPointData.class);
		} catch (Exception e) {
			throw new PublisherException("Error parsing the JSON string ["+jsonString+"]",e);
		}
	}

	/**
	 * 
	 * @return The id of the system monitored by the plugin
	 */
	public String getMonitoredSystemID() {
		return monitoredSystemID;
	}

	/**
	 * 
	 * @param monitoredSystemID The id of the system monitored by the plugin
	 */
	public void setMonitoredSystemID(String monitoredSystemID) {
		Objects.requireNonNull(monitoredSystemID);
		this.monitoredSystemID = monitoredSystemID;
	}
}
