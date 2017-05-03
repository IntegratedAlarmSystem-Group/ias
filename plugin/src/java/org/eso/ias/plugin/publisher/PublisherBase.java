package org.eso.ias.plugin.publisher;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Base class for publishing data to the IAS core.
 * 
 * @author acaproni
 *
 */
public abstract class PublisherBase implements MonitorPointSender {
	
	/**
	 * The logger
	 */
	private final Logger logger = LoggerFactory.getLogger(PublisherBase.class);
	
	/**
	 * The name of the server to send monitor points to
	 */
	public final String serverName; 
	
	/**
	 * The port of the server to send monitor points to
	 */
	public final int serverPort;
	
	/**
	 * The ID of of the plugin.
	 */
	public final String pluginId;

	/**
	 * Constructor
	 * 
	 * @param serverName The name of the server
	 * @param port The port of the server
	 */
	public PublisherBase(String pluginId, String serverName, int port) {
		if (pluginId==null || pluginId.isEmpty()) {
			throw new IllegalArgumentException("The ID can't be null nor empty");
		}
		this.pluginId=pluginId;
		if (serverName==null || serverName.isEmpty()) {
			throw new IllegalArgumentException("The sink server name can't be null nor empty");
		}
		this.serverName=serverName;
		if (port<-0) {
			throw new IllegalArgumentException("Invalid port number: "+port);
		}
		this.serverPort=port;
		logger.info("This plugn "+pluginId+" will send monitor points to "+serverName+"@"+serverPort);
	}
}
