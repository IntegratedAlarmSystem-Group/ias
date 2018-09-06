package org.eso.ias.plugin.publisher.impl;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.Objects;
import java.util.concurrent.ScheduledExecutorService;

import org.eso.ias.plugin.publisher.BufferedMonitoredSystemData;
import org.eso.ias.plugin.publisher.BufferedPublisherBase;
import org.eso.ias.plugin.publisher.PublisherException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Base class for the publishers that write strings (with different format)
 * on a file.
 *  
 * @author acaproni
 *
 */
public abstract class BufferedFilePublisherBase extends BufferedPublisherBase {
	
	/**
	 * The writer to dump monitor point values into
	 */
	private final BufferedWriter outWriter;
	
	/**
	 * The logger
	 */
	private static final Logger logger = LoggerFactory.getLogger(BufferedFilePublisherBase.class);

	/**
	 * Constructor
	 * 
	 * @param pluginId The identifier of the plugin
	 * @param monitoredSystemId The identifier of the system monitored by the plugin
	 * @param serverName The name of the server
	 * @param port The port of the server
	 * @param executorSvc The executor service
	 * @param outWriter The output writer
	 */
	public BufferedFilePublisherBase(
			String pluginId,
			String monitoredSystemId,
			String serverName, 
			int port, 
			ScheduledExecutorService executorSvc, 
			BufferedWriter outWriter) {
		super(pluginId, monitoredSystemId, serverName, port, executorSvc);
		Objects.requireNonNull(outWriter,"The output stream can't be null");
		this.outWriter=outWriter;
	}
	
	/**
	 * Builds the string to be written in the file.
	 * <P>
	 * It can be a simple string, XML, JSON and so on.
	 * 
	 * @param data The data to be sent to core of the IAS
	 * @return The string to write in the file
	 * @throws PublisherException In case of error building the string
	 */
	protected abstract String buildString(BufferedMonitoredSystemData data) throws PublisherException;
	
	/**
	 * Send the passed data to the core of the IAS.
	 * 
	 * @param data The data to send to the core of the IAS
	 */
	@Override
	protected long publish(BufferedMonitoredSystemData data) throws PublisherException {
		String strToWriteOnFile=buildString(data);
		if (strToWriteOnFile==null || strToWriteOnFile.isEmpty()) {
			return 0L;
		}
		try {
			outWriter.write(strToWriteOnFile);
			outWriter.flush();
		} catch (IOException ioe) {
			throw new PublisherException("Error writing dat",ioe);
		}
		return strToWriteOnFile.length();
	}

	/**
	 * Performs the initialization.
	 * 
	 * @throws PublisherException never
	 */
	@Override
	protected void start() throws PublisherException {
		logger.info("Started");
	}

	/**
	 * Performs the cleanup.
	 * 
	 * @throws PublisherException in case of error releasing the writer
	 */
	@Override
	protected void shutdown() throws PublisherException {
		logger.info("Shutted down");
		try {
			outWriter.flush();
			outWriter.close();
		} catch (IOException ioe) {
			throw new PublisherException("Error closing the file",ioe);
		}
	}

}
