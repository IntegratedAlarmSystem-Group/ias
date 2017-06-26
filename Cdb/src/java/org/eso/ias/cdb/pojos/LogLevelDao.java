package org.eso.ias.cdb.pojos;

/**
 * The hibernate mapping for the log level 
 * (as defined by the minimum set of log levels recognized by the system 
 * according to slf4j).
 * 
 * @author acaproni
 * 
 */
public enum LogLevelDao {
	OFF, FATAL, ERROR, WARN, INFO, DEBUG, ALL
}
