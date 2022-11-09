package org.eso.ias.cdb.structuredtext.json;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.eso.ias.cdb.CdbReader;
import org.eso.ias.cdb.IasCdbException;
import org.eso.ias.cdb.structuredtext.StructuredTextReader;
import org.eso.ias.cdb.structuredtext.json.pojos.*;
import org.eso.ias.cdb.pojos.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

/**
 * Read CDB configuration from JSON files.
 * 
 * As {@link JsonWriter} replaces objects included into other objects 
 * (like ACSEs into a DASU), the reader must reconstruct 
 * the entire tree of objects inclusion before returning the
 * DAO pojos to the caller.
 * 
 * <P>JSON writing and parsing is done with Jackson2 
 * (http://wiki.fasterxml.com/JacksonStreamingApi)
 * 
 * @see CdbReader
 * @author acaproni
 */
public class JsonReader extends StructuredTextReader {

	 /**
     * The logger
     */
    private static final Logger logger = LoggerFactory.getLogger(JsonReader.class);

	/**
	 * Constructor
	 * 
	 * @param cdbFileNames CdbFile to get the name of the file to red
	 */
	public JsonReader(CdbFiles cdbFileNames) {
		super(cdbFileNames);
	}



	/**
	 * Get all the TFs.
	 * 
	 * @return The TFs read from the configuration file
	 * @throws IasCdbException In case of error getting the IASIOs
	 */
	@Override
	public Optional<Set<TransferFunctionDao>> getTransferFunctions() throws IasCdbException {
		if (closed.get()) {
			throw new IasCdbException("The reader is shut down");
		}
		if (!initialized.get()) {
			throw new IasCdbException("The reader is not initialized");
		}

		File f;
		try {
			// The ID is not used for JSON: we pass a whatever sting
			f = cdbFileNames.getTFFilePath("UnusedID").toFile();
		} catch (IOException ioe) {
			throw new IasCdbException("Error getting TFs file",ioe);
		}
		if (!canReadFromFile(f)) {
			return Optional.empty();
		} else {
			// Parse the file in a JSON pojo
			ObjectMapper mapper = new ObjectMapper();
			try {
				Set<TransferFunctionDao> tfs = mapper.readValue(f, new TypeReference<Set<TransferFunctionDao>>(){});
				return Optional.of(tfs);
			} catch (Throwable t) {
				System.err.println("Error reading TFs from "+f.getAbsolutePath()+ ": "+t.getMessage());
				t.printStackTrace();
				return Optional.empty();
			}
		}
	}
	
	/**
	 * Get the all the templates from the file.
	 * 
	 * @return The templates read from the configuration file
	 * @throws IasCdbException In case of error getting the IASIOs
	 */
	@Override
	public Optional<Set<TemplateDao>> getTemplates() throws IasCdbException {
		if (closed.get()) {
			throw new IasCdbException("The reader is shut down");
		}
		if (!initialized.get()) {
			throw new IasCdbException("The reader is not initialized");
		}

		File f;
		try {
			// The ID is not used for JSON: we pass a whatever sting
			f = cdbFileNames.getTemplateFilePath("UnusedID").toFile();
		} catch (IOException ioe) {
			throw new IasCdbException("Error getting the template file",ioe);
		}
		if (!canReadFromFile(f)) {
			return Optional.empty();
		} else {
			// Parse the file in a JSON pojo
			ObjectMapper mapper = new ObjectMapper();
			try {
				Set<TemplateDao> templates = mapper.readValue(f, new TypeReference<Set<TemplateDao>>(){});
				return Optional.of(templates);
			} catch (Throwable t) {
				System.err.println("Error reading templates from "+f.getAbsolutePath()+ ": "+t.getMessage());
				t.printStackTrace();
				return Optional.empty();
			}
		}
	}
	

	
	/**
	 * Read the template configuration from the CDB. 
	 * 
	 * @param template_id The not <code>null</code> nor empty identifier of the template
	 * @return The template read from the CDB
	 * @throws IasCdbException in case of error reading from the CDB
	 */
	@Override
	public Optional<TemplateDao> getTemplate(String template_id) throws IasCdbException {
		if (closed.get()) {
			throw new IasCdbException("The reader is shut down");
		}
		if (!initialized.get()) {
			throw new IasCdbException("The reader is not initialized");
		}

		if (template_id==null || template_id.isEmpty()) {
			throw new IllegalArgumentException("The ID of the template cannot be null nor empty");
		}
		Optional<Set<TemplateDao>> templates = getTemplates();
		if (!templates.isPresent()) {
			return Optional.empty();
		}
		return templates.get().stream().filter(template -> template.getId().equals(template_id)).findFirst();
	}

	/**
	 * Return the DASUs belonging to the given Supervisor.
	 * 
	 * @param id The not <code>null</code> nor empty identifier of the supervisor
	 * @return A set of DASUs running in the supervisor with the passed id
	 * @throws IasCdbException in case of error reading CDB or if the 
	 *                         supervisor with the give identifier does not exist
	 */
	public Set<DasuToDeployDao> getDasusToDeployInSupervisor(String id) throws IasCdbException {
		if (closed.get()) {
			throw new IasCdbException("The reader is shut down");
		}
		if (!initialized.get()) {
			throw new IasCdbException("The reader is not initialized");
		}

		Objects.requireNonNull(id, "The ID cant't be null");
		if (id.isEmpty()) {
			throw new IllegalArgumentException("Invalid empty ID");
		}
		Optional<SupervisorDao> superv = getSupervisor(id);
		Set<DasuToDeployDao> ret = superv.orElseThrow(() -> new IasCdbException("Supervisor ["+id+"] not dound")).getDasusToDeploy();
		return (ret==null)? new HashSet<>() : ret;
	}
	
	/**
	 * Return the ASCEs belonging to the given DASU.
	 * 
	 * @param id The not <code>null</code> nor empty identifier of the supervisor
	 * @return A set of DASUs running in the supervisor with the passed id
	 * @throws IasCdbException in case of error reading CDB or if the 
	 *                         DASU with the give identifier does not exist
	 */
	public Set<AsceDao> getAscesForDasu(String id) throws IasCdbException {
		if (closed.get()) {
			throw new IasCdbException("The reader is shut down");
		}
		if (!initialized.get()) {
			throw new IasCdbException("The reader is not initialized");
		}

		Objects.requireNonNull(id, "The ID cant't be null");
		if (id.isEmpty()) {
			throw new IllegalArgumentException("Invalid empty ID");
		}
		Optional<DasuDao> dasu = getDasu(id);
		Set<AsceDao> ret = dasu.orElseThrow(() -> new IasCdbException("DASU ["+id+"] not found")).getAsces();
		return (ret==null)? new HashSet<>() : ret;
	}
	
	/**
	 * Return the IASIOs in input to the given ASCE.
	 * 
	 * @param id The not <code>null</code> nor empty identifier of the ASCE
	 * @return A set of IASIOs running in the ASCE with the passed id
	 * @throws IasCdbException in case of error reading CDB or if the 
	 *                         ASCE with the give identifier does not exist
	 */
	@Override
	public Collection<IasioDao> getIasiosForAsce(String id) throws IasCdbException {
		if (closed.get()) {
			throw new IasCdbException("The reader is shut down");
		}
		if (!initialized.get()) {
			throw new IasCdbException("The reader is not initialized");
		}

		if (id ==null || id.isEmpty()) {
			throw new IllegalArgumentException("Invalid null or empty ID");
		}
		Optional<AsceDao> asce = getAsce(id);
		Collection<IasioDao> ret = asce.orElseThrow(() -> new IasCdbException("ASCE ["+id+"] not dound")).getInputs();
		return (ret==null)? new ArrayList<>() : ret;
	}

	/**
	 * Return the templated IASIOs in input to the given ASCE.
	 *
	 * These inputs are the one generated by a different template than
	 * that of the ASCE
	 * (@see <A href="https://github.com/IntegratedAlarmSystem-Group/ias/issues/124">#!@$</A>)
	 *
	 * @param id The not <code>null</code> nor empty identifier of the ASCE
	 * @return A set of template instance of IASIOs in input to the ASCE
	 * @throws IasCdbException in case of error reading CDB or if the
	 *                         ASCE with the give identifier does not exist
	 */
	@Override
	public Collection<TemplateInstanceIasioDao> getTemplateInstancesIasiosForAsce(String id)
            throws IasCdbException {
		if (closed.get()) {
			throw new IasCdbException("The reader is shut down");
		}
		if (!initialized.get()) {
			throw new IasCdbException("The reader is not initialized");
		}

        if (id ==null || id.isEmpty()) {
            throw new IllegalArgumentException("Invalid null or empty ID");
        }
        Optional<AsceDao> asce = getAsce(id);
        Collection<TemplateInstanceIasioDao> ret = asce.orElseThrow(() -> new IasCdbException("ASCE ["+id+"] not dound")).getTemplatedInstanceInputs();
        return (ret==null)? new ArrayList<>() : ret;
    }


	@Override
	public Optional<TransferFunctionDao> getTransferFunction(String tf_id) throws IasCdbException {
		if (closed.get()) {
			throw new IasCdbException("The reader is shut down");
		}
		if (!initialized.get()) {
			throw new IasCdbException("The reader is not initialized");
		}

		Objects.requireNonNull(tf_id);
		String cleanedID = tf_id.trim();
		if (cleanedID.isEmpty()) {
			throw new IllegalArgumentException("Invalid empty TF ID");
		}
		Optional<Set<TransferFunctionDao>> tfs = getTransferFunctions();
		if (tfs.isPresent()) {
			for (TransferFunctionDao tf : tfs.get()) {
				if (tf.getClassName().equals(cleanedID)) {
					return Optional.of(tf);
				}
			}
		}
		return Optional.empty();
	}

    /**
     * Retrun teh IDs in the passed folders.
     *
     * For Supetrvisors, DASUs and ASCEs, the Ids are teh names of the
     * json files in the folder.
     *
     * This method is to avoid replication as the same alghoritm works
     * for Supervisors, DASUs and ASCEs.
     *
     * @param placeHolderFilename A place holder for a file in the folder
     * @return
     */
	private Set<String> getIdsInFolder(File placeHolderFilename) throws IasCdbException {
		if (closed.get()) {
			throw new IasCdbException("The reader is shut down");
		}
		if (!initialized.get()) {
			throw new IasCdbException("The reader is not initialized");
		}

        String fName = placeHolderFilename.toString();
        int pos = fName.lastIndexOf(File.separatorChar);
        if (pos <=0) throw new IasCdbException("Invalid file name!");

        FilenameFilter filter = new FilenameFilter() {
            @Override
            public boolean accept(File file, String s) {
                return s.toLowerCase().endsWith(".json");
            }
        };
        String folderName= fName.substring(0,pos);
        File folder = new File(folderName);
        File[] filesInFolder = folder.listFiles(filter);

        Set<String> ret = new HashSet<>();
        for (File file: filesInFolder) {
            String jSonfFileName = file.toString();
            int i = jSonfFileName.lastIndexOf('.');
            String fileNameWithoutExtension=jSonfFileName.substring(0,i);
            i = fileNameWithoutExtension.lastIndexOf(File.separatorChar);
            if (i==-1) {
                ret.add(fileNameWithoutExtension);
            } else {
                String cleanedFile = fileNameWithoutExtension.substring(i+1);
                ret.add(cleanedFile);
            }
        }
        return ret;
    }

	/**
	 * Get the IDs of the Supervisors.
	 *
	 * This method is useful to deploy the supervisors
	 *
	 * @return The the IDs of the supervisors read from the CDB
	 * @throws IasCdbException In case of error getting the IDs of the supervisors
	 */
	@Override
	public Optional<Set<String>> getSupervisorIds() throws IasCdbException {
		if (closed.get()) {
			throw new IasCdbException("The reader is shut down");
		}
		if (!initialized.get()) {
			throw new IasCdbException("The reader is not initialized");
		}

	    // We do not care if this supervisor exists or not as we need the
        // to scan the folder for the names of all the files if contains
        File supervFile;
        try {
            supervFile = cdbFileNames.getSuperivisorFilePath("PlaceHolder").toFile();
        } catch (IOException ioe) {
            throw new IasCdbException("Error getting path",ioe);
        }

        return Optional.of(getIdsInFolder(supervFile));
    }

	/**
	 * Get the IDs of the DASUs.
	 *
	 * @return The IDs of the DASUs read from the CDB
	 * @throws IasCdbException In case of error getting the IDs of the DASUs
	 */
	@Override
	public Optional<Set<String>> getDasuIds() throws IasCdbException {
		if (closed.get()) {
			throw new IasCdbException("The reader is shut down");
		}
		if (!initialized.get()) {
			throw new IasCdbException("The reader is not initialized");
		}

        // We do not care if this DASU exists or not as we need the
        // to scan the folder for the names of all the files if contains
        File dasuFile;
        try {
            dasuFile = cdbFileNames.getDasuFilePath("PlaceHolder").toFile();
        } catch (IOException ioe) {
            throw new IasCdbException("Error getting path",ioe);
        }

        return Optional.of(getIdsInFolder(dasuFile));
    }

	/**
	 * Get the IDs of the ASCEs.
	 *
	 * @return The IDs of the ASCEs read from the CDB
	 * @throws IasCdbException In case of error getting the IDs of the ASCEs
	 */
	@Override
	public Optional<Set<String>> getAsceIds() throws IasCdbException {
		if (closed.get()) {
			throw new IasCdbException("The reader is shut down");
		}
		if (!initialized.get()) {
			throw new IasCdbException("The reader is not initialized");
		}
	   // We do not care if this ASCE exists or not as we need the
        // to scan the folder for the names of all the files if contains
        File asceFile;
        try {
            asceFile = cdbFileNames.getAsceFilePath("PlaceHolder").toFile();
        } catch (IOException ioe) {
            throw new IasCdbException("Error getting path",ioe);
        }

        return Optional.of(getIdsInFolder(asceFile));
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
	@Override
	public Optional<ClientConfigDao> getClientConfig(String id) throws IasCdbException {
		if (closed.get()) {
			throw new IasCdbException("The reader is shut down");
		}
		if (!initialized.get()) {
			throw new IasCdbException("The reader is not initialized");
		}

	    Objects.requireNonNull(id,"The identifier can't be an null");
        String cleanedID = id.trim();
        if (cleanedID.isEmpty()) {
            throw new IllegalArgumentException("The identifier can't be an empty string");
        }

        try {
            Path configFilePath =  cdbFileNames.getClientFilePath(id);
            if (!canReadFromFile(configFilePath.toFile())) {
                return Optional.empty();
            } else {
                List<String> lines = Files.readAllLines(configFilePath);
                StringBuilder builder = new StringBuilder();
                lines.forEach(l -> {
                    builder.append(l);
                });
                return Optional.of(new ClientConfigDao(cleanedID,builder.toString()));
            }
        } catch (IOException ioe) {
            throw new IasCdbException("Error getting the configuration of the client "+id,ioe);
        }

	}

	/**
	 * Get the configuration of the plugin with the passed identifier.
	 * <p>
	 * The configuration of the plugin can be read from a file or from the CDB.
	 * In both cases, the configuration is returned as #PluginConfigDao.
	 * This m,ethod returns the configuration from the CDB; reading from file is
	 * not implemented here.
	 *
	 * @param id The not null nor empty ID of the IAS plugin
	 * @return The configuration of the plugin
	 * @throws IasCdbException In case of error getting the configuration of the plugin
	 */
	@Override
	public Optional<PluginConfigDao> getPlugin(String id) throws IasCdbException {
		if (closed.get()) {
			throw new IasCdbException("The reader is shut down");
		}
		if (!initialized.get()) {
			throw new IasCdbException("The reader is not initialized");
		}

		Objects.requireNonNull(id,"The identifier  of the plugin can't be an null");
		String cleanedID = id.trim();
		if (cleanedID.isEmpty()) {
			throw new IllegalArgumentException("The identifier of the plugin can't be an empty string");
		}
		logger.debug("Getting plugin config {}",cleanedID);
		if (!cleanedID.equals(id)) {
			logger.warn("CThe passed plugin ID contains blank chars: searching for [{}] instead of [{}]",cleanedID,id);
		}

		try {
			Path pluginFilePath = cdbFileNames.getPluginFilePath(id);
			logger.debug("Getting plugin config from {}",pluginFilePath.toFile().getAbsolutePath());
			if (!canReadFromFile(pluginFilePath.toFile())) {
				logger.error("{} is unreadable",pluginFilePath.toFile().getAbsolutePath());
				return Optional.empty();
			} else {
				// Parse the file in a JSON pojo
				ObjectMapper mapper = new ObjectMapper();
				try {
					PluginConfigDao plConfig = mapper.readValue(pluginFilePath.toFile(), PluginConfigDao.class);
					if (!plConfig.getId().equals(cleanedID)) {
						throw new IasCdbException("CDB ID vs File name misconfiguration for PLUGIN with ID=["+id+"]");
					}
					return Optional.of(plConfig);
				} catch (Exception e) {
					throw new IasCdbException("Error reading parsing plugin configuration from file " + pluginFilePath.toAbsolutePath(), e);
				}
			}
		} catch (Exception e) {
			throw new IasCdbException("Error reading config of plugin " + id, e);
		}
	}

	/**
	 * @return The IDs of all the plugins in the CDB
	 * @throws IasCdbException In case of error getting the IDs of the plugins
	 */
	@Override
	public Optional<Set<String>> getPluginIds() throws IasCdbException {
		if (closed.get()) {
			throw new IasCdbException("The reader is shut down");
		}
		if (!initialized.get()) {
			throw new IasCdbException("The reader is not initialized");
		}

		// We do not care if this plugin exists or not as we need the
		// to scan the folder for the names of all the files if contains
		File pluginFile;
		try {
			pluginFile = cdbFileNames.getPluginFilePath("PlaceHolder").toFile();
		} catch (IOException ioe) {
			throw new IasCdbException("Error getting path",ioe);
		}

		return Optional.of(getIdsInFolder(pluginFile));
	}

	/**
	 * @return The IDs of all the plugins in the CDB
	 * @throws IasCdbException In case of error getting the IDs of the clients
	 */
	@Override
	public Optional<Set<String>> getClientIds() throws IasCdbException {
		if (closed.get()) {
			throw new IasCdbException("The reader is shut down");
		}
		if (!initialized.get()) {
			throw new IasCdbException("The reader is not initialized");
		}

		// We do not care if this plugin exists or not as we need the
		// to scan the folder for the names of all the files if contains
		File clientFile;
		try {
			clientFile = cdbFileNames.getClientFilePath("PlaceHolder").toFile();
		} catch (IOException ioe) {
			throw new IasCdbException("Error getting path",ioe);
		}

		return Optional.of(getIdsInFolder(clientFile));
	}

}
