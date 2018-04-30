package org.eso.ias.heartbeat;

/**
 * The message sent by the heartbeat reporting the status
 * of the tool.
 * 
 * @author acaproni
 *
 */
public enum HeartbeatStatus {

	/**
	 * The tool is starting up.
	 * 
	 * This phase includes the building of the objects,
	 * acquiringg of resources, initialization etc.
	 */
    STARTING_UP,
    
    /**
     * The tool is up and running
     */
    RUNNING,
    
    /**
     * The tool is running but  not fully operative
     * 
     * For example a Supervisor where a DASU does not run
     * due to a failure of the TF of one of the ASCEs.
     */
    PARTIALLY_RUNNING,
    
    /**
     * The tool is paused i.e. it does not process inputs 
     * neither produces outputs.
     */
    PAUSED,
    
    /**
     * The tool is shutting down
     * 
     * This message includes releasing of resources, closing of threads
     * etc.
     */
    EXITING;
}
