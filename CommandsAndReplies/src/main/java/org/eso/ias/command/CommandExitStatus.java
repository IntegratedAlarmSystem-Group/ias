package org.eso.ias.command;

/**
 * The execution status of a command
 */
public enum  CommandExitStatus {
    OK,
    REJECTED,
    ERROR,
    UNKNOWN // A command did not terminate
}
