package org.eso.ias.prototype.input.java;

/**
 * The possible types of identifier.
 * <P>
 * The identifier is composed of a unique ID plus the 
 * identifier of the owner of a IAS item like for example 
 * the output of the DASU is composed by the ID of the IASIO
 * plus the ASCE that produced it plus the DASU where it runs.
 *  
 * @author acaproni
 *
 */
public enum IdentifierType {
	
	/**
	 * The value of a monitor point or alarm
	 * produced by a monitored software system
	 * <P>
	 * For example: [ID=Wind_Direction, type={@link #MONITOR_POINT_VALUE}]
	 */
	MONITOR_POINT_VALUE,
	
	/**
	 * The type for the monitored software system that
	 * produced a monitor point value or alarm
	 * <P>
	 * For example: [ID=ACS, type={@link #MONITORED_SOFTWARE_SYSTEM}]
	 * 
	 */
	MONITORED_SOFTWARE_SYSTEM,
	
	/**
	 * The type of a plugin that retrieved
	 * a monitor point value or alarm from a monitored system
	 * <P>
	 * For example: [ID=ACS_Plugin, type={@link #PLUGIN}
	 */
	PLUGIN,
	
	/**
	 * The type of IASIO identifier
	 */
	IASIO,
	
	/**
	 * The type of a computing element identifier
	 */
	ASCE,
	
	/**
	 * The type of a distributed unit identifier
	 */
	DASU;
}
