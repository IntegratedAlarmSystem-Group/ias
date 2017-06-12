package org.eso.ias.plugin.publisher.impl;

import java.io.BufferedWriter;
import java.util.concurrent.ScheduledExecutorService;

import org.eso.ias.plugin.publisher.BufferedMonitoredSystemData;
import org.eso.ias.plugin.publisher.PublisherException;

/**
 * A publisher that dumps messages in the passed writer.
 * <P>
 * This implementation allows to investigate the sending of monitor points
 * to the core of the IAS by looking at the file where they are all dumped.
 * 
 * @author acaproni
 *
 */
public class FilePublisher extends BufferedFilePublisherBase {
	/**
	 * Constructor
	 * 
	 * @param pluginId The identifier of the plugin
	 * @param serverName The name of the server
	 * @param port The port of the server
	 * @param executorSvc The executor service
	 * @param outStream The output stream
	 */
	public FilePublisher(String pluginId, String serverName, int port, ScheduledExecutorService executorSvc, BufferedWriter outWriter) {
		super(pluginId, serverName, port, executorSvc,outWriter);
	}

	/**
	 * @see org.eso.ias.plugin.publisher.impl.BufferedFilePublisherBase#buildString(org.eso.ias.plugin.publisher.BufferedMonitoredSystemData)
	 */
	@Override
	protected String buildString(BufferedMonitoredSystemData data) throws PublisherException {
		return data.toString();
	}
}
