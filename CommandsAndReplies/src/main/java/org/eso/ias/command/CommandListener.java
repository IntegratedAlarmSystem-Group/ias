package org.eso.ias.command;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * The interface of the listener of commands received from
 * the command topic
 */
public interface CommandListener {

    public class CmdExecutionResult {

        /** The execution status of the command */
        public final CommandExitStatus status;

        /** Additional properties, if any */
        public final Optional<Map<String, String>> properties;

        /**
         * Constructor
         *
         * @param status The exit status of the ocmmand
         * @param props The map of propertis, if any
         */
        public CmdExecutionResult(CommandExitStatus status, Map<String, String> props) {
            Objects.requireNonNull(status);
            this.status=status;
            properties = Optional.ofNullable(props);
        }

        /**
         * Constructor with no properties
         *
         * @param status The exit status of the command
         */
        public CmdExecutionResult(CommandExitStatus status) {
            this(status,null);
        }
    }

    /**
     * A new command has been received and must be exexuted by the listener.
     *
     * @param cmd The command received from the command topic
     * @return  The not null reply produced executing the passed command
     * @throws Exception in case of error running the command
     */
    public CmdExecutionResult newCommand(CommandMessage cmd) throws Exception;
}
