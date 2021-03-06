package org.eso.ias.command;

import ch.qos.logback.classic.Level;
import org.slf4j.ILoggerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The {@link DefaultCommandExecutor} provides a default implementation
 * of the commands defined in {@link CommandType}.
 *
 * An object of this class as usually passed as parameter of {@link CommandManager#start(CommandListener, AutoCloseable)}
 * if the default behavior is acceptable.
 * Otherwise it is possible to pass an object that extends this one overloading just the behavior to customize.
 *
 * If RESTART or SHUTDOWN are invoked, the implementations of those commands can execute
 * a pice of code before the process is effectively restarted or shutdown.
 * The restart and shutdwon of the process must be executed by the {@link CommandManager} that is in charge
 * to send the reply to the caller and free the resources before shutting down the current process.
 *
 */
public class DefaultCommandExecutor implements CommandListener {

    /**
     * The logger
     */
    private static final Logger logger = LoggerFactory.getLogger(DefaultCommandExecutor.class);

    /**
     * The method that processes the commands received from the command topic
     *
     * @param cmd The command received from the command topic
     * @return The result of the execution of the command
     * @throws Exception
     */
    @Override
    public final CmdExecutionResult newCommand(CommandMessage cmd) throws Exception {
        switch (cmd.getCommand()) {
            case PING: return ping(cmd);
            case SET_LOG_LEVEL: return setLogLevel(cmd);
            case ACK: return alarmAcknowledged(cmd);
            case RESTART: return restart(cmd);
            case SHUTDOWN: return shutdown(cmd);
            case TF_CHANGED: return tfChanged(cmd);

            default:
                throw new Exception("Unknown command "+cmd.getCommand());
        }
    }

    /**
     * A TF has been changed.
     *
     * This message is aimed to the ASCEs that produce th eoutput applying the TF with the passed ID.
     * The receiver of the message is the Supervisor on behalf of the ASCE(s).
     *
     * This implementation does nothing and return OK.
     *
     * Normally this message is broadcast.
     *
     * @param cmd The TF_CHANGED command received from the command topic
     * @return The result of the execution of the command
     * @throws Exception
     */
    public CmdExecutionResult tfChanged(CommandMessage cmd) throws Exception {
        return new CmdExecutionResult(CommandExitStatus.OK);
    }

    /**
     * A an Alarm has been acknowledged.
     *
     * This message is aimed to the DASU that produces such alarm (i.e. received by the Supervisor
     * where the DASU is deployed) all the other tools can safely ignore the command.
     *
     * This implementation does nothing and return OK.
     *
     * @param cmd The ACK command received from the command topic
     * @return The result of the execution of the command
     * @throws Exception
     */
     public CmdExecutionResult alarmAcknowledged(CommandMessage cmd) throws Exception {
        return new CmdExecutionResult(CommandExitStatus.OK);
     }

    /**
     * The default implementation of the PING command returns OK
     *
     * @param cmd The PING command received from the command topic
     * @return The result of the execution of the command
     * @throws Exception
     */
    public CmdExecutionResult ping(CommandMessage cmd) throws Exception {
        logger.debug("PING command executed");
        return new CmdExecutionResult(CommandExitStatus.OK);
    }

    /**
     * The default implementation of the SET_LOG_LEVEL.
     *
     * The first parameter of the command is a string representing the new log level i.e. one of the
     * following:
     * - OFF
     * - ERROR
     * - WARN
     * - INFO
     * - DEBUG
     * - TRACE
     * - ALL
     *
     * @param cmd The PING command received from the command topic
     * @return The result of the execution of the command
     * @throws Exception
     */
    public CmdExecutionResult setLogLevel(CommandMessage cmd) throws Exception {
        List<String> params = cmd.getParams();
        if (params == null || params.isEmpty()) {
            throw new IllegalArgumentException("Missing log level in command");
        }
        // The first parameter is a string with the requested log level
        String levelStr = params.get(0);
        logger.debug("Setting log level to {}", levelStr);


        ILoggerFactory loggerFactory = LoggerFactory.getILoggerFactory();
        ch.qos.logback.classic.Logger rootLogger = (ch.qos.logback.classic.Logger) loggerFactory.getLogger("org.eso.ias");

        // Get the log level form the parameter: it defaults to DEBUG if no match is found
        Level level = Level.valueOf(levelStr);
        rootLogger.setLevel(level);
        logger.info("Log level set to {}", level.toString());
        Map<String, String> props = new HashMap<>();
        props.put("LogLevel", level.toString());
        return new CmdExecutionResult(CommandExitStatus.OK, props,false,false);
    }

    /**
     * The method called when a RESTART command has been executed.
     *
     * The implementation of this method must not effectively restart the process but ask
     * for the restarting in the returned {@link org.eso.ias.command.CommandListener.CmdExecutionResult
     * }
     * The implementation can add here additional code to be executed before the process is shutdown
     *
     * @param cmd The RESTART command received from the command topic
     * @return The result of the execution of the command
     * @throws Exception
     */
    public CmdExecutionResult restart(CommandMessage cmd) throws Exception {
        return new CmdExecutionResult(CommandExitStatus.OK, null,false,true);
    }

    /**
     * The method called when a SHUTDOWN command has been executed.
     *
     * The implementation of this method must not effectively restart the process but ask
     * for the restarting in the returned {@link org.eso.ias.command.CommandListener.CmdExecutionResult
     * }.
     * The implementation can add here additional code to be executed before the process is shutdown
     *
     * @param cmd The RESTART command received from the command topic
     * @return The result of the execution of the command
     * @throws Exception
     */
    public CmdExecutionResult shutdown(CommandMessage cmd) throws Exception {
        return new CmdExecutionResult(CommandExitStatus.OK, null,true,false);
    }
}
