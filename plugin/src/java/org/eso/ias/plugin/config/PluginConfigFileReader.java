package org.eso.ias.plugin.config;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStreamReader;
import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadFactory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Read the plugin configuration from a JSON file.
 * <P>
 * The configuration is read asynchronously by a dedicated thread.
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
	private final ExecutorService exService = Executors.newSingleThreadExecutor(new ThreadFactory() {
		@Override
		public Thread newThread(Runnable r) {
			Thread ret = new Thread(r);
			ret.setName(this.getClass().getName());
			ret.setDaemon(true);
		    return ret;
		}
	});
	
	/**
	 * The reader
	 */
	private final BufferedReader reader;
	
	/**
	 * Constructor.
	 * 
	 * @param reader The reader to read the JSON file with the configuration
	 * @param srcName The name of the resource
	 */
	public PluginConfigFileReader(BufferedReader reader, String srcName) {
		Objects.requireNonNull(reader,"The reader can't be null");
		Objects.requireNonNull(srcName,"The name can't be null");
		if (srcName.isEmpty()) {
			throw new IllegalArgumentException("The resource name can't be an empty string");
		}
		this.srcName=srcName;
		this.reader=reader;
	}
	

	/**
	 * Constructor.
	 * 
	 * @param resourceName The name of the resource containing the JSON of the configuration
	 * @throws PluginConfigException in case of error accessing the resource
	 */
	public PluginConfigFileReader(String resourceName) throws PluginConfigException {
		this(new BufferedReader(
				new InputStreamReader(PluginConfigFileReader.class.getResourceAsStream(resourceName))),
				resourceName);
	}
	
	/**
	 * Constructor.
	 * 
	 * @param file The file with the JSON configuration
	 * @throws PluginConfigException in case of error opening the file
	 * @throws FileNotFoundException If the passed file is not found 
	 */
	public PluginConfigFileReader(File file) throws PluginConfigException, FileNotFoundException {
		this(new BufferedReader(
				new InputStreamReader(new FileInputStream(file))),
				file.getAbsolutePath());
	}

	/**
	 * Submit the callable to read the JSON file asynchronously.
	 * 
	 * @return the Future to get the configuration produced by the thread
	 * @see PluginConfigDao
	 */
	@Override
	public Future<PluginConfig> getPluginConfig() throws PluginConfigException {
		logger.debug("Starting the async. reading of the JSON configuration");
		ConfigCallable callable = new ConfigCallable(reader);
		return exService.submit(callable);
	}

}
