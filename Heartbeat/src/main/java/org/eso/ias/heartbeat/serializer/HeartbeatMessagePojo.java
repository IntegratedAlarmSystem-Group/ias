package org.eso.ias.heartbeat.serializer;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import org.eso.ias.heartbeat.HeartbeatStatus;
import org.eso.ias.utils.ISO8601Helper;

import java.util.Map;
import java.util.Objects;

/**
 * The a java pojo with the heartbeat message.
 * 
 * @author acaproni
 */
public class HeartbeatMessagePojo {
	
	/**
	 * The timestamp
	 */
	private String timestamp;
	
	/**
	 * The full running ID
	 */
	private String hbStringrepresentation;
	
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
	 * @param hbStringrepresentation the string representation of the HB
	 * @param hbStatus the status
	 * @param props additional properties
	 * @param tStamp the timestamp
	 */
	public HeartbeatMessagePojo(
			String hbStringrepresentation,
			HeartbeatStatus hbStatus,
			Map<String, String> props,
			long tStamp) {
		if (Objects.isNull(hbStringrepresentation) || hbStringrepresentation.isEmpty()) {
			throw new IllegalArgumentException("Invalid null/empty full running id");
		}
		Objects.requireNonNull(hbStatus);
		
		this.timestamp=ISO8601Helper.getTimestamp(tStamp);
		this.hbStringrepresentation = hbStringrepresentation;
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

	public String getHbStringrepresentation() {
		return hbStringrepresentation;
	}

	public void setHbStringrepresentation(String hbStringrepresentation) {
		this.hbStringrepresentation = hbStringrepresentation;
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
