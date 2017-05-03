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
	private String SystemID;
	
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

}
