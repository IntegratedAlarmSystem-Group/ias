package org.eso.ias.plugin;

import java.util.Collection;
import java.util.concurrent.ThreadFactory;

import org.eso.ias.plugin.config.PluginConfig;
import org.eso.ias.plugin.config.Value;
import org.eso.ias.plugin.thread.PluginThreadFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The main class to write IAS plugins.
 * <P>
 * The plugin takes care of collecting, filtering and sending to the IAS
 * core of all the monitored values.
 * <P>
 * <EM>Life cycle</EM>:
 *  
 * @author acaproni
 */
public class Plugin {
	
	/**
	 * The thread group to which all the threads
	 * created by the plugin belong
	 */
	private final ThreadGroup threadGroup = new ThreadGroup("Plugin thread group");
	
	/**
	 * The factory to get new threads
	 */
	private final ThreadFactory threadFactory = new PluginThreadFactory(threadGroup);
	
	/**
	 * The logger
	 */
	private final Logger logger = LoggerFactory.getLogger(Plugin.class);

	/**
	 * Build a plugin with the passed parameters
	 * 
	 * @param id The Identifier of the plugin
	 * @param sinkServerName The server to send 
	 * 						 monitor point values and alarms to
	 * @param sinkServerPort The IP port number 
	 * @param values The list of monitor point values and alarms
	 */
	public Plugin(
			String id, 
			String sinkServerName,
			int sinkServerPort,
			Collection<Value> values) {
		
	}
	
	/**
	 * Build a plugin from the passed configuration
	 * @param config
	 */
	public Plugin(PluginConfig config) {
		this(
				config.getId(),
				config.getSinkServer(),
				config.getSinkPort(),
				config.getValuesAsCollection());
	}
	
	/**
	 * This method must be called at the beginning
	 * to acquire the needed resources.
	 */
	public void start() {
		
	}
	
	/**
	 * This method must be called when finished using the object 
	 * to free the resources. 
	 */
	public void shutdown() {
		
	}
}
