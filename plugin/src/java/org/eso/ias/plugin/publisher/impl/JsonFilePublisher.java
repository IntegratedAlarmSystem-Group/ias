package org.eso.ias.plugin.publisher.impl;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.concurrent.ScheduledExecutorService;

import org.eso.ias.plugin.publisher.MonitoredSystemData;
import org.eso.ias.plugin.publisher.PublisherBase;
import org.eso.ias.plugin.publisher.PublisherException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
/**
 * A publisher that dumps JSON messages in the passed writer.
 * <P>
 * This implementation allows to investigate the sending of monitor points
 * to the core of the IAS by looking at the JSON file where they are all dumped.
 * <P>
 * The data written in the file are the JSON representation of each message
 * i.e. a sequence of {@link MonitoredSystemData}.
 * As such the file is not JSON specification compliant. 
 * 
 * @author acaproni
 *
 */
public class JsonFilePublisher extends PublisherBase {
	
	/**
	 * The writer to dump monitor point values into
	 */
	private final BufferedWriter outWriter;
	
	/**
	 * The logger
	 */
	private static final Logger logger = LoggerFactory.getLogger(JsonFilePublisher.class);

	/**
	 * Constructor
	 * 
	 * @param pluginId The identifier of the plugin
	 * @param serverName The name of the server
	 * @param port The port of the server
	 * @param executorSvc The executor service
	 * @param outStream The output stream to write JSON strings into
	 */
	public JsonFilePublisher(String pluginId, String serverName, int port, ScheduledExecutorService executorSvc, BufferedWriter outWriter) {
		super(pluginId, serverName, port, executorSvc);
		if (outWriter==null) {
			throw new IllegalArgumentException("The output stream can't be null");
		}
		this.outWriter=outWriter;
	}

	@Override
	protected void publish(MonitoredSystemData data) {
		String jsonString;
		try {
			jsonString = data.toJsonString();
		} catch (PublisherException pe) {
			logger.error("Error getting the JSON string: {}",pe.getMessage());
			pe.printStackTrace(System.err);
			return;
		}
		try {
			outWriter.write(jsonString);
			outWriter.flush();
		} catch (IOException ioe) {
			logger.error("Error writing data: {}",ioe.getMessage());
			ioe.printStackTrace(System.err);
		}
	}

	@Override
	protected void start() throws PublisherException {
		logger.info("Started");

	}

	@Override
	protected void shutdown() throws PublisherException {
		logger.info("Shutted down");
	}

}
