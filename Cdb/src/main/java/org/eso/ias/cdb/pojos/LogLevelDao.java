package org.eso.ias.cdb.pojos;

import ch.qos.logback.classic.Level;

/**
 * The hibernate mapping for the log level 
 * (as defined by the minimum set of log levels recognized by the system 
 * according to slf4j).
 * 
 * @author acaproni
 * 
 */
public enum LogLevelDao {
	OFF(Level.OFF),
    ERROR(Level.ERROR),
    WARN(Level.WARN),
    INFO(Level.INFO),
    DEBUG(Level.DEBUG),
    TRACE(Level.TRACE),
    ALL(Level.ALL);

    /**
     * The corresponding level in logback
     */
	private final Level logbackLogLevel;

    /**
     * Constructor
     *
     * @param logbackLogLevel The logback Level
     */
	private LogLevelDao(Level logbackLogLevel) {
	    this.logbackLogLevel=logbackLogLevel;
    }

    /**
     * Returns the lcorresponding level in logback that can be useful
     * for programmatically change the log level.
     *
     * @return The logback log level
     */
	public Level toLoggerLogLevel() {
	    return logbackLogLevel;
    }
}
