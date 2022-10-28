package org.eso.ias.converter.config;

import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.eso.ias.cdb.IasCdbException;
import org.eso.ias.cdb.pojos.TemplateDao;
import org.eso.ias.types.IASTypes;
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

	/**
	 * Templates are stored in a map
	 */
	private final Map<String, TemplateDao> templates = new HashMap<>();

	public Cache(CdbReader cdbReader) {
		Objects.requireNonNull(cdbReader);
		this.cdbReader=cdbReader;
	}

    /**
	 * Initialize the DAO.
	 * 
	 * Stores in the cache the IasioDaos and TemplateDaos read from the CDB
	 * 
	 * @throws ConfigurationException The exception returned in case of error initializing
	 */
	protected void setUp() throws ConfigurationException {
		Objects.requireNonNull(cdbReader);
		Optional<Set<IasioDao>> iasiosOpt;
		try {
			iasiosOpt = cdbReader.getIasios();
		} catch (IasCdbException e) {
			throw new ConfigurationException("Error geting the IASIOs from the CDB", e);
		}
		if (iasiosOpt.isEmpty()) {
			logger.warn("No IASIO definitions read from CDB: nothing to do for this converter!");
			return;
		}
		Set<IasioDao> iasios = iasiosOpt.get();
		if (iasios.isEmpty()) {
			logger.warn("Empty list of IASIO definitions read from CDB: nothing to do for this converter!");
			return;
		}
		ObjectMapper objectMapper = new ObjectMapper();
		// Tries to get as many IASIOs as possible: errors are logged
		iasios.forEach(iasio -> {
			StringWriter writer = new StringWriter();
			try {
				objectMapper.writeValue(writer, iasio);
				pcache.put(iasio.getId(), writer.toString());
				getTemplate(iasio.getTemplateId());
			} catch (Exception e) {
				logger.error("Error converting {} to JSON",iasio.toString(),e);
			}
		});
	}

	/**
	 * Get and add in the map the template with given ID.
	 *
	 * If the templateid is null or an empty string, this method returns
	 * without adding anything
	 *
	 * @param templateId the template to get (can be null or empty)
	 * @throws ConfigurationException If the template is not found in the CDB
	 */
	private void getTemplate(String templateId) throws ConfigurationException {
		if (templateId==null || templateId.isEmpty()) { // No template to get
			return;
		}
		Optional<TemplateDao> tDaoOpt;
		try {
			tDaoOpt = cdbReader.getTemplate(templateId);
		} catch (Exception e) {
			throw new ConfigurationException("Error getting the template ["+templateId+"] from CDB",e);
		}
		if (tDaoOpt.isEmpty()) {
			throw new ConfigurationException("Template "+templateId+" NOT found in CDB");
		}
		templates.put(templateId,tDaoOpt.get());
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
	public Optional<MonitorPointConfiguration> getConfiguration(String mpId) {
		Optional<String> jsonStrOpt = pcache.jget(mpId);
		if (jsonStrOpt.isEmpty()) {
			return Optional.empty();
		} else {
			ObjectMapper objectMapper = new ObjectMapper();
			IasioDao iasioDao=null;
			try {
				iasioDao = objectMapper.readValue(jsonStrOpt.get(), IasioDao.class);
			} catch (Exception e) {
				logger.error("Error poarsing [{}] MP lost",jsonStrOpt.get(),e);
				return Optional.empty();
			}

			// Get the min and max if the monitor point is templated
			Optional<Integer> min = Optional.empty();
			Optional<Integer> max = Optional.empty();
			String templateId = iasioDao.getTemplateId();
			if (templateId!=null && !templateId.isEmpty()) {
				TemplateDao template = templates.get(templateId);
				if (template==null) {
					logger.error("Template "+templateId+" NOT found in the map: monitor point "+mpId+ " lost!!");
					return Optional.empty();
				}
				min = Optional.of(template.getMin());
				max = Optional.of(template.getMax());
			}
			MonitorPointConfiguration mpc = new MonitorPointConfiguration(IASTypes.fromIasioDaoType(iasioDao.getIasType()), min, max);
			return Optional.of(mpc);
		}
	}
}


