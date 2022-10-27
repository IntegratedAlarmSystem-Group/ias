package org.eso.ias.converter.config;

import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.eso.ias.converter.config.ConfigurationDaoBase;
import org.eso.ias.cdb.CdbReader;
import org.eso.ias.cdb.pojos.IasioDao;
import org.eso.ias.converter.ValueMapper;
import org.eso.ias.utils.pcache.PCache;

/**
 * A cache of IasioDao for the {@link ValueMapper}.
 * This class replaces IasioConfigurationDaoImpl with {@link PCache}
 * 
 * This class stores IasiosDao and templates.
 */
public class Cache extends ConfigurationDaoBase {

    /**
	 * The logger
	 */
	private static final Logger logger = LoggerFactory.getLogger(Cache.class);

   /**
	 * The default value for the max number of items to keep in the volatile
	 * memory of PCache
	 * @see {@link PCache}
	 */
	private static final int DEFAULT_MAXCACHESIZE = 10000;

	/**
	 * The java property to customize the max size of the in-memory cache
	 */
	private static final String MAXCACHESIZE_PROPNAME = "org.eso.ias.converter.maxcachesize";

	/** 
	 * The CDB reader to get IasioDaos and TemplateDaos
	 * 
	 * This property is initialized in the constructor and freed (i.e. set to null)
	 * after initialization beacuse there will be no further need to access the CDB 
	 */
	private CdbReader cdbReader=null;

	/** 
	 * The cache of {@link MonitorPointConfiguration}
	 */
	private final PCache pcache = new PCache(Integer.getInteger(MAXCACHESIZE_PROPNAME, DEFAULT_MAXCACHESIZE),0); 

	public Cache(CdbReader cdbReader) {
		Objects.requireNonNull(cdbReader);
		this.cdbReader=cdbReader;
	}

    /**
	 * Initialize the DAO.
	 * 
	 * setUP storres in the cache the IasioDaos and TemplateDaos read from the CDB
	 * 
	 * @throws ConfigurationException The exception returned in case of error initializing
	 */
	protected void setUp() throws ConfigurationException {
		Objects.requireNonNull(cdbReader);
		Optional<Set<IasioDao>> iasiosOpt = cdbReader.getIasios();
		if (iasiosOpt.isEmpty()) {
			logger.warn("No IASIO definitions read from CDB: nothing to do for this converter!");
			return;
		}
		Set<IasioDao> iasios = iasiosOpt.get();
		if (iasios.isEmpty()) {
			logger.warn("Empty list of IASIO definitions read from CDB: nothing to do for this converter!");
			return;
		}
		
	}

    /**
	 * Shuts down the DAO
	 * 
	 * @throws ConfigurationException in case of error shutting down
	 */
	protected  void tearDown() throws ConfigurationException {}

    /**
	 * Get the configuration of the the monitor point
	 * with the passed ID.
	 * 
	 * @param mpId The not <code>null</code> nor empty ID of the MP 
	 * @return The configuration of the MP with the passed ID
	 *         or <code>null</code> if such configuration does not exist
	 */
	public Optional<MonitorPointConfiguration> getConfiguration(String mpId) {}
}


