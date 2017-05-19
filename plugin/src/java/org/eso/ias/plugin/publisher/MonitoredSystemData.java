package org.eso.ias.plugin.publisher;

import java.util.Collection;

/**
 * The data structure encapsulating monitor point values
 * and alarm retrieved from a monitored system and sent to the IAS core.
 * It is composed of a global data structure (this one) plus a list
 * of monitor points.
 * <P>
 * <code>MonitoredSystemData</code> is a java POJO that is easily
 * serializable to a string that is what the plugin sends to
 * the IAS core.
 * <P>
 * To improve network performances, whenever possible the plugin 
 * collects and sends the monitor points with one single message 
 * instead of one by one.
 * 
 * @see MonitorPointData
 * @author acaproni
 *
 */
public class MonitoredSystemData {
	
	/**
	 * The id of the plugin.
	 */
	private String systemID;
	
	/**
	 * ISO-8601 formatted time when the 
	 * data structure has been sent to the IAS core
	 */
	private String publishTime;
	
	/**
	 * The monitor points and alarms collected from the remote
	 * system to be sent to the IAS core;
	 */
	private Collection<MonitorPointData> monitorPoints;

	/**
	 * Empty constructor
	 */
	public MonitoredSystemData() {}

	/**
	 * @return the systemID
	 */
	public String getSystemID() {
		return systemID;
	}

	/**
	 * @param systemID the systemID to set
	 */
	public void setSystemID(String systemID) {
		this.systemID = systemID;
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
	 * @return the monitorPoints
	 */
	public Collection<MonitorPointData> getMonitorPoints() {
		return monitorPoints;
	}

	/**
	 * @param monitorPoints the monitorPoints to set
	 */
	public void setMonitorPoints(Collection<MonitorPointData> monitorPoints) {
		if (this.monitorPoints!=null) {
			this.monitorPoints.clear();
		}
		this.monitorPoints = monitorPoints;
	}

	@Override
	public String toString() {
		StringBuilder ret = new StringBuilder("Monitored System Data packet [ID=");
		ret.append(systemID);
		ret.append(", at ");
		ret.append(publishTime);
		ret.append(", ");
		ret.append(monitorPoints.size());
		ret.append(" monitor points: ");
		monitorPoints.forEach( mp -> {
			ret.append(' ');
			ret.append(mp.toString());
		});
		ret.append(']');
		return ret.toString();
	}
}
