package org.eso.ias.plugin.publisher.impl;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.Objects;
import java.util.concurrent.ScheduledExecutorService;

import org.eso.ias.plugin.publisher.MonitoredSystemData;
import org.eso.ias.plugin.publisher.PublisherBase;
import org.eso.ias.plugin.publisher.PublisherException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A publisher that dumps messages in the passed writer.
 * <P>
 * This implementation allows to investigate the sending of monitor points
 * to the core of the IAS by looking at the file where they are all dumped.
 * 
 * @author acaproni
 *
 */
public class FilePublisher extends PublisherBase {
	
	/**
	 * The writer to dump monitor point values into
	 */
	private final BufferedWriter outWriter;
	
	/**
	 * The logger
	 */
	private static final Logger logger = LoggerFactory.getLogger(FilePublisher.class);

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
		super(pluginId, serverName, port, executorSvc);
		Objects.requireNonNull(outWriter,"The output stream can't be null");
		this.outWriter=outWriter;
	}

	@Override
	protected void publish(MonitoredSystemData data) {
		try {
			outWriter.write(data.toString());
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
