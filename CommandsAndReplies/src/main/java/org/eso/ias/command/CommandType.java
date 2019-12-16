package org.eso.ias.command;

/**
 * The commands supported by the core
 */
public enum CommandType {
    PING, // Do nothing
    SHUTDOWN, // Shuts down the process
    RESTART, // Restart the process
    SET_LOG_LEVEL, // Set the log level of the process
    ACK, // ACK an alarm
    TF_CHANGED // Signal that a TF has been changed
}
