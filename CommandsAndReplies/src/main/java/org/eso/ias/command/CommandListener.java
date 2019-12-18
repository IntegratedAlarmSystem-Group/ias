package org.eso.ias.command;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * The interface of the listener of commands received from
 * the command topic
 */
public interface CommandListener {

    /**
     * The data structure that the listener returns to the {@link CommandManager}
     */
    public class CmdExecutionResult {

        /** The execution status of the command */
        public final CommandExitStatus status;

        /** Additional properties, if any */
        public final Optional<Map<String, String>> properties;

        /**
         * Set if the the manager must restart the process
         *
         * Only one between mustRestart and mustShutdown can be set at the same time
         */
        public final boolean mustRestart;

        /**
         * Set if the the manager must shut the process down
         *
         * Only one between mustRestart and mustShutdown can be set at the same time
         */
        public final boolean mustShutdown;

        /**
         * Constructor.
         *
         * Only one between mustRestart and mustShutdown can be set at the same time
         *
         * @param status The exit status of the command
         * @param props The map of propertis, if any
         * @param mustRestart Set if the the manager must restart the process
         * @param mustShutdown Set if the the manager must shut the process down
         */
        public CmdExecutionResult(
                CommandExitStatus status,
                Map<String, String> props,
                boolean mustShutdown,
                boolean mustRestart) {
            Objects.requireNonNull(status);
            if (mustRestart && mustShutdown) {
                throw new IllegalArgumentException("Only one between mustRestart and mustShutdown can be set at the same time");
            }
            this.status=status;
            properties = Optional.ofNullable(props);
            this.mustRestart = mustRestart;
            this.mustShutdown = mustShutdown;
        }

        /**
         * Constructor with no properties when no stratup and no shutdown is required
         *
         * @param status The exit status of the command
         */
        public CmdExecutionResult(CommandExitStatus status) {
            this(status,null,false,false);
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
