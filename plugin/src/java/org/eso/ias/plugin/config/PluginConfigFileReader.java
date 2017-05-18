package org.eso.ias.plugin.config;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.eso.ias.plugin.thread.PluginThreadFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Read the plugin configuration from a JSON file.
 * 
 * @author acaproni
 *
 */
public class PluginConfigFileReader implements PluginConfigDao {
	
	/**
	 * The callable that concurrently gets the configuration.
	 * 
	 * @author acaproni
	 *
	 */
	private class ConfigCallable implements Callable<PluginConfig> {
		
		/**
		 * The reader to read the JSON from.
		 */
		private final BufferedReader reader;
		
		/**
		 * Contructor
		 * 
		 * @param reader The reader to read the JSON file
		 */
		public ConfigCallable(BufferedReader reader) {
			super();
			this.reader = reader;
			assert(this.reader!=null);
		}

		/**
		 * Reads and parse the JSON file from the reader and return
		 * the plugin configuration
		 */
		@Override
		public PluginConfig call() throws Exception {
			try {
				ObjectMapper jackson2Mapper = new ObjectMapper();
				PluginConfig config = jackson2Mapper.readValue(reader, PluginConfig.class);
				logger.info("JSON Plugin configuration successfully red from {}",srcName);
				return config;
			} finally {
				reader.close();
			}
		}
	}
	
	/**
	 * The logger
	 */
	private final Logger logger = LoggerFactory.getLogger(PluginConfigFileReader.class);
	
	/**
	 * The name of the resource or file to read the JSON configuration from.
	 */
	private final String srcName;
	
	/**
	 * The executor service to asynchronously read and parse the JSON file.
	 */
	private final ExecutorService exService;
	
	/**
	 * The plugin config returned by async. reading the JSON file.
	 */
	private final Future<PluginConfig> futurePluginConfig;
	
	/**
	 * Constructor.
	 * <P>
	 * Read the configuration from the passed reader.
	 * 
	 * @param reader The reader to read the JSON file with the configuration
	 * @param srcName The name of the resource
	 */
	public PluginConfigFileReader(BufferedReader reader, String srcName) {
		if (reader==null) {
			throw new IllegalArgumentException("The reader can't be null");
		}
		if (srcName==null || srcName.isEmpty()) {
			throw new IllegalArgumentException("The resource name can't be null nor empty");
		}
		this.srcName=srcName;
		exService = Executors.newSingleThreadExecutor(PluginThreadFactory.getThreadFactory());
		
		futurePluginConfig = startAsyncReading(reader);
	}

	/**
	 * Constructor.
	 * <P>
	 * The passed parameter is the resource (i.e. a file in a jar) with the configuration. 
	 * 
	 * @param resourceName The name of the resource containing the JSON of the configuration
	 * @throws PluginConfigException in case of error accessing the resource
	 */
	public PluginConfigFileReader(String resourceName) throws PluginConfigException {
		this(getReader(resourceName),resourceName);
	}
	
	/**
	 * Constructor.
	 * <P>
	 * Read the configuration from the passed file.
	 * 
	 * @param fileName The name of the JSON file containing the configuration
	 * @throws PluginConfigException in case of error opening the file
	 */
	public PluginConfigFileReader(File fileName) throws PluginConfigException {
		this(getReader(fileName),fileName.getAbsolutePath());
	}
	
	/**
	 * Submit the callable to read the JSON file asynchronously.
	 * 
	 * @param reader The not null {@link BufferedReader} to read the JSON from 
	 * @return The future to get the PluginConfig from
	 */
	private Future<PluginConfig> startAsyncReading(BufferedReader reader) {
		logger.debug("Starting the async. reading of the JSON configuration");
		ConfigCallable callable = new ConfigCallable(reader);
		return exService.submit(callable);
	}
	
	/**
	 * Get a reader from the passed resource 
	 * 
	 * @param resourceName The name of the file to get
	 * @return The reader to read the configuration from
	 * @throws PluginConfigException If it was not possible to get the stream form the resource
	 */
	private static BufferedReader getReader(String resourceName) throws PluginConfigException {
		InputStream inStream = PluginConfigFileReader.class.getResourceAsStream(resourceName);
		if (inStream==null) {
			throw new PluginConfigException("Resource not found: "+resourceName);
		}
		// Open the file from reading
		return new BufferedReader(new InputStreamReader(inStream));
	}
	
	/**
	 * Get a reader from the passed file 
	 * 
	 * @param file The name of the file to get
	 * @return The reader to read the configuration from
	 * @throws PluginConfigException If it was not possible to get the stream form the resource
	 */
	private static BufferedReader getReader(File file) throws PluginConfigException {
		InputStream inStream;
		try {
			inStream=new FileInputStream(file);
		} catch (FileNotFoundException fnfe) {
			throw new PluginConfigException("Cannot read: "+file.getAbsolutePath(),fnfe);
		}
		// Open the file from reading
		return new BufferedReader(new InputStreamReader(inStream));
	}

	/**
	 * Get and return the configuration of the plugin.
	 * 
	 * @see PluginConfigDao
	 */
	@Override
	public Future<PluginConfig> getPluginConfig() throws PluginConfigException {
		return futurePluginConfig;
	}

}
