package org.eso.ias.types;

/**
 * Th eoperational mode of a monitor point value
 * 
 * @author acaproni
 */
public enum OperationalMode {
	/**
	 * Starting up
	 */
	STARTUP, 
	
	/**
	 * Initialization on going
	 */
	INITIALIZATION,
	
	/**
	 * Shutting down
	 */
	CLOSING,
	
	/**
	 * Shutted down
	 */
	SHUTTEDDOWN, 
	
	/**
	 * Maintenance
	 */
	MAINTENANCE, 
	
	/**
	 * Fully operational
	 */
	OPERATIONAL,
	
	/**
	 * Only partially operational
	 */
	DEGRADED,
	
	/**
	 * Unknown state
	 */
	UNKNOWN
}
