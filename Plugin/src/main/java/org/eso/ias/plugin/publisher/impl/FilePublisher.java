package org.eso.ias.plugin.publisher.impl;

import org.eso.ias.plugin.publisher.BufferedMonitoredSystemData;
import org.eso.ias.plugin.publisher.PublisherException;

import java.io.BufferedWriter;
import java.util.concurrent.ScheduledExecutorService;

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
	 * @param monitoredSystemId The identifier of the system monitored by the plugin
	 * @param executorSvc The executor service
	 * @param outWriter The output writer
	 */
	public FilePublisher(String pluginId, String monitoredSystemId, ScheduledExecutorService executorSvc, BufferedWriter outWriter) {
		super(pluginId, monitoredSystemId, executorSvc,outWriter);
	}

	/**
	 * Build and return a string representing the data
	 * 
	 * @return  a string representing the data
	 * @see BufferedFilePublisherBase#buildString(BufferedMonitoredSystemData)
	 */
	@Override
	protected String buildString(BufferedMonitoredSystemData data) throws PublisherException {
		return data.toString();
	}
}
