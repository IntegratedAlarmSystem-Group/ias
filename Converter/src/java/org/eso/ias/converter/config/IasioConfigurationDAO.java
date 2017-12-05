package org.eso.ias.converter.config;

/**
 * The DAO interface to get the configuration of the DAO.
 * <P>
 * Implementers of this interface get the configuration of the
 * monitor point from the configuration database: calling this method
 * can considerably slow down the conversion task.
 * <P>
 * It is recommended to reduce the accesses to the configuration database for example
 * with a cache. 
 * 
 * @author acaproni
 */
public interface IasioConfigurationDAO {
	
	/**
	 * Initialize the DAO
	 * 
	 * @throws ConfigurationException in case of error initializing
	 */
	public void initialize() throws ConfigurationException;
	
	/**
	 * 
	 * @return <code>true</code> if the DAO has been initialized
	 */
	public boolean isInitialized();
	
	
	/**
	 * Get the configuration of the the monitor point
	 * with the passed ID.
	 * 
	 * @param mpId The not <code>null</code> nor empty ID of the MP 
	 * @return The configuration of the MP with the passed ID
	 *         or <code>null</code> if such configuration does not exist
	 */
	public MonitorPointConfiguration getConfiguration(String mpId);
	
	/**
	 * Close the DAO freeing all the acquired resources
	 * 
	 * @throws ConfigurationException in case of  error closing
	 */
	public void close() throws ConfigurationException;

	/**
	 * 
	 * @return <code>true</code> if the DAO has been closed
	 */
	public boolean isClosed();
}
