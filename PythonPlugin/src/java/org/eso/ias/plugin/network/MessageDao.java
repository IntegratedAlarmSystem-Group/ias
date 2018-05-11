package org.eso.ias.plugin.network;

/**
 * The message sent from the remote client to the
 * networked plugin
 * 
 * @author acaproni
 *
 */
public class MessageDao {

	/**
	 * ISO 8601 time stamp
	 */
	private String timestamp;
	
	/**
	 * The value (sample) to be sent to the BSDB by the plugin library
	 */
	private String value;
	
	/**
	 * The type of the value
	 */
	private String valueType;
	
	/**
	 * The ID of the monitor point
	 */
	private String monitorPointId;
	
	/**
	 * The operational mode (can be null)
	 */
	private String operMode=null;
	
	/**
	 * Empty constructor
	 */
	public MessageDao() {}
	
	/**
	 * 
	 * Constructor
	 * 
	 * @param mPointId the ID of the monitor point
	 * @param timestamp ISO 8601 time stamp
	 * @param value The value (sample) to be sent to the BSDB
	 * @param valueType The type of the value
	 * @param operMode The operational mode (can be <code>null</code>)
	 */
	public MessageDao(
			String mPointId,
			String timestamp, 
			String value, 
			String valueType,
			String operMode) {
		if (mPointId==null || mPointId.isEmpty()) {
			throw new IllegalArgumentException("Invalid null or empty ID");
		}
		this.monitorPointId = mPointId;
		if (timestamp==null || timestamp.isEmpty()) {
			throw new IllegalArgumentException("Invalid null or empty timestamp");
		}
		this.timestamp = timestamp;
		
		if (value==null || value.isEmpty()) {
			throw new IllegalArgumentException("Invalid null or empty value");
		}
		this.value = value;
		
		if (valueType==null || valueType.isEmpty()) {
			throw new IllegalArgumentException("Invalid null or empty type");
		}
		this.valueType = valueType;
		
		this.operMode=operMode;
	}

	/**
	 * Getter
	 * 
	 * @return the timestamp
	 */
	public String getTimestamp() {
		return timestamp;
	}

	/**
	 * Setter 
	 * 
	 * @param timestamp the timestamp
	 */
	public void setTimestamp(String timestamp) {
		this.timestamp = timestamp;
	}

	/**
	 * Getter
	 * 
	 * @return the type
	 */
	public String getValueType() {
		return valueType;
	}

	/**
	 * Setter 
	 * 
	 * @param valueType type of the value
	 */
	public void setValueType(String valueType) {
		this.valueType = valueType;
	}

	/**
	 * Getter
	 * 
	 * @return the value
	 */
	public String getValue() {
		return value;
	}

	/**
	 * Setter 
	 * 
	 * @param value the value
	 */
	public void setValue(String value) {
		this.value = value;
	}

	public String getMonitorPointId() {
		return monitorPointId;
	}

	public void setMonitorPointId(String id) {
		this.monitorPointId = id;
	}

	public String getOperMode() {
		return operMode;
	}

	public void setOperMode(String operMode) {
		this.operMode = operMode;
	}
	
	
}
