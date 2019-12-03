package org.eso.ias.command;

/**
 * The interface of the listener of commands received from
 * the command topic
 */
public interface CommandListener {

    /**
     * A new command has been received and must be exexuted by the listener.
     *
     * @param cmd The command received from the command topic
     * @return  The reply produced executing the passed command
     */
    public ReplyMessage newCommand(CommandMessage cmd);
}
