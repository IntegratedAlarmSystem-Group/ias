package org.eso.ias.cdb.structuredtext.yaml;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import org.eso.ias.cdb.CdbReader;
import org.eso.ias.cdb.IasCdbException;

import org.eso.ias.cdb.structuredtext.json.CdbFiles;
import org.eso.ias.cdb.pojos.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

public class YamlReader implements CdbReader {

    /**
     * The logger
     */
    private static final Logger logger = LoggerFactory.getLogger(YamlReader.class);

	/**
	 * Signal if the reader has been initialized
	 */
	private final AtomicBoolean initialized = new AtomicBoolean(false);

	/**
	 * <code>cdbFileNames</code> return the names of the files to read
	 */
	private final CdbFiles cdbFileNames;

	/**
	 * Signal if the reader has been closed
	 */
	private final AtomicBoolean closed = new AtomicBoolean(false);

	public YamlReader(CdbFiles cdbFileNames) {
		Objects.requireNonNull(cdbFileNames, "cdbFileNames can't be null");
		this.cdbFileNames=cdbFileNames;
	}

	/**
	 * Check if the passed file is readable
	 *
	 * @param inF The file to check
	 * @return true if the file exists and is readable
	 */
	private boolean canReadFromFile(File inF) {
		return inF.exists() && inF.isFile() && inF.canRead();
	}

    /**
	 * Get the Ias configuration from a CDB.
	 * 
	 * @return The ias configuration read from the CDB 
	 * @throws IasCdbException In case of error getting the IAS
	 */
	public Optional<IasDao> getIas() throws IasCdbException {
		if (closed.get()) {
			throw new IasCdbException("The reader is shut down");
		}
		if (!initialized.get()) {
			throw new IasCdbException("The reader is not initialized");
		}

		File f;
		try {
			f= cdbFileNames.getIasFilePath().toFile();
		} catch (IOException ioe) {
			throw new IasCdbException("Error getting file",ioe);
		}
		if (!canReadFromFile(f)) {
			return Optional.empty();
		} else {
			// Parse the YAML file in a pojo
			ObjectMapper mapper = new YAMLMapper();
			try {
				IasDao ias = mapper.readValue(f, IasDao.class);
				return Optional.of(ias);
			} catch (Exception e) {
				throw new IasCdbException("Error reading IAS from "+f.getAbsolutePath(),e);
			}
		}
    }
	
	/**
	 * Get the IASIOs.
	 * 
	 * @return The IASIOs read from the CDB
	 * @throws IasCdbException In case of error getting the IASIOs
	 */
	public Optional<Set<IasioDao>> getIasios() throws IasCdbException {
        throw new IasCdbException("Unsupported operation");
    }
	
	/**
	 * Get the IASIO with the given ID
	 * 
	 * @param id The ID of the IASIO to read the configuration
	 * @return The IASIO read from the CDB
	 * @throws IasCdbException In case of error getting the IASIO
	 */
	public Optional<IasioDao> getIasio(String id) throws IasCdbException {
        throw new IasCdbException("Unsupported operation");
    }
	
	/**
	 * Read the supervisor configuration from the CDB. 
	 * 
	 * @param id The not null nor empty supervisor identifier
	 * @return The Supervisor read from the CDB
	 * @throws IasCdbException In case of error getting the Supervisor
	 */
	public Optional<SupervisorDao> getSupervisor(String id) throws IasCdbException {
        throw new IasCdbException("Unsupported operation");
    }
	
	/**
	 * Read the transfer function configuration from the CDB. 
	 * 
	 * @param tf_id The not <code>null</code> nor empty transfer function identifier
	 * @return The transfer function read from the CDB
	 * @throws IasCdbException in case of error reading from the CDB
	 */
	public Optional<TransferFunctionDao> getTransferFunction(String tf_id) throws IasCdbException {
        throw new IasCdbException("Unsupported operation");
    }

	/**
	 * Get the transfer functions.
	 *
	 * @return The transfer functions read from the CDB
	 * @throws IasCdbException In case of error getting the transfer functions
	 */
	public Optional<Set<TransferFunctionDao>> getTransferFunctions() throws IasCdbException {
        throw new IasCdbException("Unsupported operation");
    }

	/**
	 * Read the ttemplate configuration from the CDB. 
	 * 
	 * @param template_id The not <code>null</code> nor empty identifier of the template
	 * @return The template read from the CDB
	 * @throws IasCdbException in case of error reading from the CDB
	 */
	public Optional<TemplateDao> getTemplate(String template_id) throws IasCdbException {
        throw new IasCdbException("Unsupported operation");
    }

	/**
	 * Get the templates.
	 *
	 * @return The templates read from the CDB
	 * @throws IasCdbException In case of error getting the templates
	 */
	public Optional<Set<TemplateDao>> getTemplates() throws IasCdbException {
        throw new IasCdbException("Unsupported operation");
    }
	
	/**
	 * Read the ASCE configuration from the CDB. 
	 * 
	 * @param id The not null nor empty ASCE identifier
	 * @return The ASCE read from the file
	 * @throws IasCdbException In case of error getting the ASCE
	 */
	public Optional<AsceDao> getAsce(String id) throws IasCdbException {
        throw new IasCdbException("Unsupported operation");
    }
	
	/**
	 * Read the DASU configuration from the CDB. 
	 * 
	 * @param id The not null nor empty DASU identifier
	 * @return The DASU read from the file
	 * @throws IasCdbException In case of error getting the DASU
	 */
	public Optional<DasuDao> getDasu(String id) throws IasCdbException {
        throw new IasCdbException("Unsupported operation");
    }
	
	/**
	 * Return the DASUs to deploy in the Supervisor with the given identifier
	 * 
	 * @param id The not <code>null</code> nor empty identifier of the supervisor
	 * @return A set of DASUs to deploy in the supervisor with the passed id
	 * @throws IasCdbException in case of error reading CDB or if the 
	 *                         supervisor with the give identifier does not exist
	 */
	public Set<DasuToDeployDao> getDasusToDeployInSupervisor(String id) throws IasCdbException {
        throw new IasCdbException("Unsupported operation");
    }
	
	/**
	 * Return the ASCEs belonging to the given DASU.
	 * 
	 * @param id The not <code>null</code> nor empty identifier of the DASU
	 * @return A set of ASCEs running in the DASU with the passed id
	 * @throws IasCdbException in case of error reading CDB or if the 
	 *                         DASU with the give identifier does not exist
	 */
	public Set<AsceDao> getAscesForDasu(String id) throws IasCdbException {
        throw new IasCdbException("Unsupported operation");
    }
	
	/**
	 * Return the IASIOs in input to the given ASCE.
	 * 
	 * @param id The not <code>null</code> nor empty identifier of the ASCE
	 * @return A set of IASIOs in input to the ASCE
	 * @throws IasCdbException in case of error reading CDB or if the 
	 *                         ASCE with the give identifier does not exist
	 */
	public Collection<IasioDao> getIasiosForAsce(String id) throws IasCdbException {
        throw new IasCdbException("Unsupported operation");
    }

    /**
     * Get the IDs of the Supervisors.
     *
     * This method is useful to deploy the supervisors
     *
     * @return The the IDs of the supervisors read from the CDB
     * @throws IasCdbException In case of error getting the IDs of the supervisors
     */
    public Optional<Set<String>> getSupervisorIds() throws IasCdbException {
        throw new IasCdbException("Unsupported operation");
    }

    /**
     * Get the IDs of the DASUs.
     *
     * @return The IDs of the DASUs read from the CDB
     * @throws IasCdbException In case of error getting the IDs of the DASUs
     */
    public Optional<Set<String>> getDasuIds() throws IasCdbException {
        throw new IasCdbException("Unsupported operation");
    }

    /**
     * Get the IDs of the ASCEs.
     *
     * @return The IDs of the ASCEs read from the CDB
     * @throws IasCdbException In case of error getting the IDs of the ASCEs
     */
    public Optional<Set<String>> getAsceIds() throws IasCdbException {
        throw new IasCdbException("Unsupported operation");
    }

	/**
	 * Return the templated IASIOs in input to the given ASCE.
	 *
	 * These inputs are the one generated by a different template than
	 * that of the ASCE
	 * (@see <A href="https://github.com/IntegratedAlarmSystem-Group/ias/issues/124">#124</A>)
	 *
	 * @param id The not <code>null</code> nor empty identifier of the ASCE
	 * @return A set of template instance of IASIOs in input to the ASCE
	 * @throws IasCdbException in case of error reading CDB or if the
	 *                         ASCE with the give identifier does not exist
	 */
	public Collection<TemplateInstanceIasioDao> getTemplateInstancesIasiosForAsce(String id) throws IasCdbException {
        throw new IasCdbException("Unsupported operation");
    }

	/**
	 * Get the configuration of the client with the passed identifier.
	 *
	 * The configuration is passed as a string whose format depends
	 * on the client implementation.
	 *
	 * @param id The not null nor empty ID of the IAS client
	 * @return The configuration of the client
	 * @throws IasCdbException In case of error getting the configuration of the client
	 */
	public Optional<ClientConfigDao> getClientConfig(String id) throws IasCdbException {
        throw new IasCdbException("Unsupported operation");
    }

	/**
	 * Get the configuration of the plugin with the passed identifier.
	 *
	 * The configuration of the plugin can be read from a file or from the CDB.
	 * In both cases, the configuration is returned as #PluginConfigDao.
	 * This m,ethod returns the configuration from the CDB; reading from file is
	 * not implemented here.
	 *
	 * @param id The not null nor empty ID of the IAS plugin
	 * @return The configuration of the plugin
	 * @throws IasCdbException In case of error getting the configuration of the plugin
	 */
	public Optional<PluginConfigDao> getPlugin(String id) throws IasCdbException {
        throw new IasCdbException("Unsupported operation");
    }

	/**
	 * @return The IDs of all the plugins in the CDB
	 * @throws IasCdbException In case of error getting the IDs of the plugins
	 */
	public Optional<Set<String>> getPluginIds() throws IasCdbException {
        throw new IasCdbException("Unsupported operation");
    }

	/**
	 * @return The IDs of all the plugins in the CDB
	 * @throws IasCdbException In case of error getting the IDs of the clients
	 */
	public Optional<Set<String>> getClientIds() throws IasCdbException {
        throw new IasCdbException("Unsupported operation");
    }

	/**
	 * Initialize the CDB
	 */
	public void init() throws IasCdbException {
		if (closed.get()) {
			throw new IasCdbException("Cannot initialize: already closed");
		}
		if(!initialized.get()) {
			logger.debug("Initialized");
			initialized.set(true);
		} else {
			logger.warn("Already initialized: skipping initialization");
		}
	}
	
	/**
	 * Close the CDB and release the associated resources
	 * @throws IasCdbException
	 */
	public void shutdown() throws IasCdbException {
		if (!initialized.get()) {
			throw new IasCdbException("Cannot shutdown a reader that has not been initialized");
		}
		if (!closed.get()) {
			logger.debug("Closed");
			closed.set(true);
		} else {
			logger.warn("Already closed!");
		}
	}

}
