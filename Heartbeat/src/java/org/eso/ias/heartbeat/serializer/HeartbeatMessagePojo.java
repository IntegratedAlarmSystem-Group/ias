package org.eso.ias.heartbeat.serializer;

import java.util.Map;
import java.util.Objects;

import org.eso.ias.heartbeat.HeartbeatStatus;
import org.eso.ias.utils.ISO8601Helper;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

/**
 * The a java pojo with the fields  of a {@link HbMessage}
 * plus the timestamp
 * 
 * @see HbMessage
 * @author acaproni
 *
 */
public class HeartbeatMessagePojo {
	
	/**
	 * The timestamp
	 */
	private String timestamp;
	
	/**
	 * The full running ID
	 */
	private String fullRunningId;
	
	/**
	 * The state of the tool
	 */
	private HeartbeatStatus state;
	
	/**
	 * additional properties
	 */
	@JsonInclude(Include.NON_NULL)
	private Map< String, String> props;
	
	/**
	 * Empty constructor
	 */
	public HeartbeatMessagePojo() {}
	
	/**
	 * Constructor
	 * 
	 * @param id the full running id
	 * @param hbStatus the status
	 * @param props additional properties
	 * @param tStamp the timestamp
	 */
	public HeartbeatMessagePojo(
			String fullRunningId,
			HeartbeatStatus hbStatus,
			Map<String, String> props,
			long tStamp) {
		if (Objects.isNull(fullRunningId) || fullRunningId.isEmpty()) {
			throw new IllegalArgumentException("Invalid null/empty full running id");
		}
		Objects.requireNonNull(hbStatus);
		
		this.timestamp=ISO8601Helper.getTimestamp(tStamp);
		this.fullRunningId=fullRunningId;
		this.state=hbStatus;
		
		if (props!=null && !props.isEmpty()) {
			this.props = props;
		}
	}

	public String getTimestamp() {
		return timestamp;
	}

	public void setTimestamp(String timestamp) {
		this.timestamp = timestamp;
	}

	public String getFullRunningId() {
		return fullRunningId;
	}

	public void setFullRunningId(String fullRunningId) {
		this.fullRunningId = fullRunningId;
	}

	public HeartbeatStatus getState() {
		return state;
	}

	public void setState(HeartbeatStatus state) {
		this.state = state;
	}

	public Map<String, String> getProps() {
		return props;
	}

	public void setProps(Map<String, String> props) {
		this.props = props;
	}

}
