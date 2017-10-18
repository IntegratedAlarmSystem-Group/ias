package org.eso.ias.converter.config;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import org.eso.ias.cdb.CdbReader;
import org.eso.ias.cdb.IasCdbException;
import org.eso.ias.cdb.pojos.IasioDao;
import org.eso.ias.prototype.input.java.IASTypes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This DAO keeps in memory the configuration of the IASIOs defined in the CDB.
 * The only strategy to save memory is to keep only the snapshot of configuration 
 * strictly needed to reduce the memory usage.
 * This means to save a minimum data set of all the IASIOs defined in the CDB
 * and not a reduced set of IASIOs than those defined in the CDB.
 * <P>
 * Ideally all the configuration is red at startup and kept in a map 
 * to used as a cache, avoiding to read the configuration from the CDB at run time.
 * <BR>This implementation could trigger a OOM if the configuration database
 * contains too many IOASIOs and in that case we will have to adopt different strategies
 * like for example
 * <UL>
 * 	<LI>partition the data to be converted by more converters that
 *      also means to use more the one queue for the raw data produced by
 *      the plugins
 * 	<LI>access the CDB many times during the life cycle of the converter
 * 	<LI>...
 * </UL>  
 * 
 * @author acaproni
 *
 */
public class IasioConfigurationDaoImpl extends  ConfigurationDaoBase {
	
	/**
	 * The logger
	 */
	private static final Logger logger = LoggerFactory.getLogger(IasioConfigurationDaoImpl.class);
	
	/**
	 * The map with the configuration.
	 * <P>
	 * The key is the ID of a monitor point and the vlue its configuration.
	 */
	private final Map<String,MonitorPointConfiguration> configuration = new HashMap<>();
	
	/**
	 * The DAO to read the configuration from the CDB
	 */
	private final CdbReader cdbReader;
	
	/**
	 * Constructor
	 * 
	 * @param cdbReader The DAO reader
	 */
	public IasioConfigurationDaoImpl(CdbReader cdbReader) {
		Objects.requireNonNull(cdbReader);
		this.cdbReader=cdbReader;
	}
	
	/**
	 * Read the configuration of all the IASIOs and build an internal
	 * representation aiming to use as less memory as possible.
	 * 
	 * @param cdbReader The DAO to get the configurations
	 * @param config  The configuration
	 * @param append if <code>true</code> the data read from the DAO are appended
	 *               to the existing ones, otherwise the newly read configuration
	 *               replaces the old one
	 * @throws IasCdbException In case of error getting the configuration from the DAO
	 */
	private void buildConfigurationMap(CdbReader cdbReader,Map<String,MonitorPointConfiguration> config, boolean append) throws IasCdbException {
		if (append) {
			logger.debug("Clearing the cache of IASIOs");
			config.clear();
		}
		logger.info("Reading IASIOs configuration from CDB...");
		Optional<Set<IasioDao>> iasios = cdbReader.getIasios();
		logger.info("Got the configuration of {} IASIOS",iasios.map( Set::size).orElse(0));
		iasios.ifPresent( s -> s.forEach(iasioDao -> addConfiguration(iasioDao)));
		logger.info("{} IASIO configurations in cache",configuration.size());
	}
	
	/**
	 * Add the configuration of the passed IASIO to the map.
	 * 
	 * @param iasio The IASIO whose configuration must be added to the map
	 */
	private void addConfiguration(IasioDao iasio) {
		MonitorPointConfiguration mpConf = new MonitorPointConfiguration(IASTypes.fromIasioDaoType(iasio.getIasType()));
		configuration.put(iasio.getId(), mpConf);
		logger.debug("[{}] IASIO configuration added in cache; {} IASIOs in cache",iasio.getId(),configuration.size());
	}

	@Override
	protected void setUp() throws ConfigurationException {
		logger.debug("Setting up...");
		try {
			buildConfigurationMap(cdbReader,configuration,true);
		} catch (IasCdbException ice) {
			throw new ConfigurationException("Esception building the configuration map",ice);
		}
		logger.debug("Ready");
	}

	@Override
	protected void tearDown() throws ConfigurationException {
		logger.debug("Shutting down...");
		configuration.clear();
		logger.debug("Closed");
	}

	@Override
	public MonitorPointConfiguration getConfiguration(String mpId) {
		Objects.requireNonNull(mpId);
		return configuration.get(mpId);
	}

}
