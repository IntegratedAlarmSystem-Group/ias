package org.eso.ias.converter.config;

import org.eso.ias.prototype.input.java.IASTypes;

/**
 * The configuration of the monitor point as read from the configuration database.
 * <P>
 * Objects of this class do not contain all the configuration read
 * from the configuration database but only the part needed 
 * by the conversion.
 * 
 * @author acaproni
 *
 */
public class MonitorPointConfiguration {
	
	/**
	 * The type of the monitor point.
	 */
	public  final IASTypes mpType;

	/**
	 * The type of the monitor point
	 * 
	 * @param mpType The type of the monitor point as read from the CDB
	 */
	public MonitorPointConfiguration(IASTypes mpType) {
		super();
		this.mpType = mpType;
	}

}
