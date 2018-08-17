package org.eso.ias.plugin.config;

import java.util.concurrent.Future;

/**
 * 
 * Plugin configuration DAO.
 * <P>
 * As reading the configuration can be arbitrarily slow (especially if red
 * from the network or a database), {@link #getPluginConfig()} returns
 * a {@link Future} to allow reading the configuration concurrently.
 * 
 * @author acaproni
 *
 */
public interface PluginConfigDao {

	/**
	 * Get and return the java pojo with the configuration of the plugin.
	 * 
	 * @return the java pojo with the configuration of the plugin
	 * @throws PluginConfigException in case of error getting the configuration
	 */
	Future<PluginConfig> getPluginConfig() throws PluginConfigException;
}
