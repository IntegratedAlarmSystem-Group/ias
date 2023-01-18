package org.eso.ias.command;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The CommandManager base class for command executors subscribes as a consumer of the command topic and 
 * as a producer of the reply topic.
 * Its task is to receive the commands for the process where it runs, discarding the commands targeted to
 * other processes.
 *
 * Normally commands are forwarded to the listener for execution and the replies published
 * in the reply topic. SHUTDOWN and RESTART must be executed by the {@link CommandManager} because a
 * reply must be sent before shutting down.
 *
 * Commands are executed in a dedicated thread in FIFO order.
 * Received commands are queued and discarded when the queue is full as we do not expect many commands.
 */
abstract public class CommandManager {

    /** The ID of the process where the object of this class runs */
    public final String id;

    /**
     * The logger
     */
    private static final Logger logger = LoggerFactory.getLogger(CommandManager.class);

    /**
     * Constructor
     *
     * @param id the id of the process
     */
    public CommandManager(String id) {
        if (id==null || id.isEmpty()) {
            throw new IllegalArgumentException("Invalid null/empty ID");
        }
        this.id=id;
    }

    /**
     * Start getting events from the command topic and send them to the passed listener.
     * The listener is usually an instance of {@link DefaultCommandExecutor} or an object
     * extending {@link DefaultCommandExecutor} to customize the commands
     *
     * @param  commandListener The listener of commands that execute all the commands
     * @param closeable The closeable class to free the resources while exiting/restating
     */
    public abstract void start(CommandListener commandListener, AutoCloseable closeable) throws Exception;

    /**
     * Close the producer and the consumer and release all the allocated resources.
     */
    public abstract void close();

}
