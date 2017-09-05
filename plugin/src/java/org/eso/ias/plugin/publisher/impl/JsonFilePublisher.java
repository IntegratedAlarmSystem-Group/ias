package org.eso.ias.plugin.publisher.impl;

import java.io.BufferedWriter;
import java.util.concurrent.ScheduledExecutorService;

import org.eso.ias.plugin.publisher.BufferedMonitoredSystemData;
import org.eso.ias.plugin.publisher.PublisherException;
/**
 * A publisher that dumps JSON messages in the passed writer.
 * <P>
 * This implementation allows to investigate the sending of monitor points
 * to the core of the IAS by looking at the JSON file where they are all dumped.
 * <P>
 * The data written in the file are the JSON representation of each message
 * i.e. a sequence of {@link BufferedMonitoredSystemData}.
 * As such the file is not JSON specification compliant. 
 * 
 * @author acaproni
 *
 */
public class JsonFilePublisher extends BufferedFilePublisherBase {
	
	/**
	 * Constructor
	 * 
	 * @param pluginId The identifier of the plugin
	 * @param monitoredSystemId The identifier of the system monitored by the plugin
	 * @param serverName The name of the server
	 * @param port The port of the server
	 * @param executorSvc The executor service
	 * @param outWriter The output writer to write JSON strings into
	 */
	public JsonFilePublisher(
			String pluginId, 
			String monitoredSystemId,
			String serverName, 
			int port, 
			ScheduledExecutorService executorSvc, 
			BufferedWriter outWriter) {
		super(pluginId, monitoredSystemId,serverName, port, executorSvc,outWriter);
	}
	
	/**
	 * Build and return a JSON string representing the data
	 * 
	 * @return the JSON string representing the data
	 * @see BufferedFilePublisherBase#buildString(BufferedMonitoredSystemData)
	 */
	@Override
	protected String buildString(BufferedMonitoredSystemData data) throws PublisherException {
		return data.toJsonString();
	}
}
