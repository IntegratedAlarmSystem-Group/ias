package org.eso.ias.command;

/**
 * The commands supported by the core
 */
public enum CommandType {
    PING(0), // Do nothing
    SHUTDOWN(0), // Shuts down the process
    RESTART(0), // Restart the process
    SET_LOG_LEVEL(1), // Set the log level of the process
    ACK(2), // ACK an alarm: one parameter is the ID of the alarm, the othe ris the comment of the operator
    TF_CHANGED(1); // Signal that a TF has been changed with the ID of the TF

    /**
     * The expected number of parameters of the command
     */
    public final int expectedNumOfParameters;

    private CommandType(int nParams) {
        expectedNumOfParameters=nParams;
    }
}
