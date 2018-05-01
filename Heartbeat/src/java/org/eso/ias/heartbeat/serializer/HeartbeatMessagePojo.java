package org.eso.ias.heartbeat.serializer;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import org.eso.ias.heartbeat.HbMessage;
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
	private String state;
	
	/**
	 * additional properties
	 */
	@JsonInclude(Include.NON_NULL)
	private Map< String, String> props;
	
	/**
	 * Constructor
	 * 
	 * @param msg the HB message
	 * @param tStamp the timestamp
	 */
	public HeartbeatMessagePojo(HbMessage msg, long tStamp) {
		Objects.requireNonNull(msg);
		this.timestamp=ISO8601Helper.getTimestamp(tStamp);
		this.fullRunningId=msg.fullRunningId();
		this.state=msg.hbState().toString();
		
		Map<String,String> addProps = msg.getPropsAsJavaMap(); 
		if (addProps.isEmpty()) {
			this.props = addProps;
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

	public String getState() {
		return state;
	}

	public void setState(String state) {
		this.state = state;
	}

	public Map<String, String> getProps() {
		return props;
	}

	public void setProps(Map<String, String> props) {
		this.props = props;
	}

}
