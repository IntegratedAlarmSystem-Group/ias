package org.eso.ias.cdb.json;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

import org.eso.ias.cdb.CdbReader;
import org.eso.ias.cdb.IasCdbException;
import org.eso.ias.cdb.json.pojos.JsonAsceDao;
import org.eso.ias.cdb.json.pojos.JsonDasuDao;
import org.eso.ias.cdb.json.pojos.JsonDasuToDeployDao;
import org.eso.ias.cdb.json.pojos.JsonSupervisorDao;
import org.eso.ias.cdb.pojos.AsceDao;
import org.eso.ias.cdb.pojos.DasuDao;
import org.eso.ias.cdb.pojos.DasuToDeployDao;
import org.eso.ias.cdb.pojos.IasDao;
import org.eso.ias.cdb.pojos.IasioDao;
import org.eso.ias.cdb.pojos.SupervisorDao;
import org.eso.ias.cdb.pojos.TemplateDao;
import org.eso.ias.cdb.pojos.TransferFunctionDao;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

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
public class JsonReader implements CdbReader {
	
	/**
	 * A holder to rebuild the objects from their IDs.
	 * 
	 * @author acaproni
	 *
	 */
	private class ObjectsHolder {
		public final Map<String,AsceDao> asces = new HashMap<>();
		public final Map<String,DasuDao> dasus = new HashMap<>();
		public final Map<String, TransferFunctionDao> transferFunctions = new HashMap<>();
		public final Map<String, DasuToDeployDao> dasusToDeploy= new HashMap<>();
	}
	
	/**
	 * <code>cdbFileNames</code> return the names of the files to read
	 */
	private final CdbFiles cdbFileNames;
	
	/**
	 * Constructor
	 * 
	 * @param cdbFileNames CdbFile to get the name of the file to red
	 */
	public JsonReader(CdbFiles cdbFileNames) {
		Objects.requireNonNull(cdbFileNames, "cdbFileNames can't be null");
		this.cdbFileNames=cdbFileNames;
	}

	/**
	 * Get the Ias configuration from JSON file.
	 * 
	 * @return The ias configuration red from the JSON file 
	 * @see CdbReader#getIas()
	 * @throws IasCdbException In case of error getting the IAS
	 */
	@Override
	public Optional<IasDao> getIas() throws IasCdbException {
		File f;
		try {
			f= cdbFileNames.getIasFilePath().toFile();
		} catch (IOException ioe) {
			throw new IasCdbException("Error getting file",ioe);
		}
		if (!canReadFromFile(f)) {
			return Optional.empty();
		} else {
			// Parse the file in a JSON pojo
			ObjectMapper mapper = new ObjectMapper();
			try {
				IasDao ias = mapper.readValue(f, IasDao.class);
				return Optional.of(ias);
			} catch (Exception e) {
				throw new IasCdbException("Error reading IAS from "+f.getAbsolutePath(),e);
			}
		}
	}
	
	/**
	 * Get the IASIOs from the file.
	 * 
	 * @return The IASIOs read from the configuration file
	 * @see CdbReader#getIasios()
	 * @throws IasCdbException In case of error getting the IASIOs
	 */
	@Override
	public Optional<Set<IasioDao>> getIasios() throws IasCdbException {
		File f;
		try {
			// The ID is not used for JSON: we pass a whatever sting
			f = cdbFileNames.getIasioFilePath("UnusedID").toFile();
		} catch (IOException ioe) {
			throw new IasCdbException("Error getting IASIOs file",ioe);
		}
		if (!canReadFromFile(f)) {
			return Optional.empty();
		} else {
			// Parse the file in a JSON pojo
			ObjectMapper mapper = new ObjectMapper();
			try {
				Set<IasioDao> iasios = mapper.readValue(f, new TypeReference<Set<IasioDao>>(){});
				return Optional.of(iasios);
			} catch (Throwable t) {
				System.err.println("Error reading IASIOs from "+f.getAbsolutePath()+ ": "+t.getMessage());
				t.printStackTrace();
				return Optional.empty();
			}
		}
	}
	
	/**
	 * Get all the TFs.
	 * 
	 * @return The TFs read from the configuration file
	 * @throws IasCdbException In case of error getting the IASIOs
	 */
	private Optional<Set<TransferFunctionDao>> getTransferFunctions() throws IasCdbException {
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
	private Optional<Set<TemplateDao>> getTemplates() throws IasCdbException {
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
	 * Get the IASIO with the given ID
	 * 
	 * The implementation is not optimized for performances and memory usage because 
	 * it gets all the IASIOs delegating to {@link #getIasios()} then look for the
	 * one with the given ID if present.
	 * <BR>It should be ok for a testing CDB but can be an issue importing/exporting
	 * the production CDB.
	 * 
	 * 
	 * @param id The ID of the IASIO to read the configuration
	 * @return The IASIO red from the file
	 * @throws IasCdbException In case of error getting the IASIO
	 */
	@Override
	public Optional<IasioDao> getIasio(String id) throws IasCdbException {
		Objects.requireNonNull(id, "The IASIO identifier cannot be null");
		String cleanedID = id.trim();
		if (cleanedID.isEmpty()) {
			throw new IllegalArgumentException("The identifier can't be an empty string");
		}
		Optional<Set<IasioDao>> iasios = getIasios();
		if (iasios.isPresent()) {
			for (IasioDao i : iasios.get()) {
				if (i.getId().equals(id)) {
					return Optional.of(i);
				}
			}

		}
		return Optional.empty();
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
	 * Check if the passed file is readable
	 *  
	 * @param inF The file to check
	 * @return true if the file exists and is readable
	 */
	private boolean canReadFromFile(File inF) {
		return inF.exists() && inF.isFile() && inF.canRead();
	}
	
	/**
	 * Read the ASCE configuration from the file. 
	 * 
	 * @param id The not null nor empty ASCE identifier
	 * @throws IasCdbException In case of error getting the ASCE
	 */
	@Override
	public Optional<AsceDao> getAsce(String id) throws IasCdbException {
		Objects.requireNonNull(id, "The ASCE identifier cannot be null");
		String cleanedID = id.trim();
		if (cleanedID.isEmpty()) {
			throw new IllegalArgumentException("The identifier can't be an empty string");
		}
		
		Optional<JsonAsceDao> jAsceOpt;
		try {
			jAsceOpt = getJsonAsce(cleanedID);
		} catch (IOException ioe) {
			throw new IasCdbException("Error getting the JSON ASCE",ioe);
		}
		ObjectsHolder holder = new ObjectsHolder();
		if (jAsceOpt.isPresent()) {
			try {
				updateAsceObjects(jAsceOpt.get(), holder);
			} catch (IOException ioe) {
				throw new IasCdbException("Error updating ASCE objects",ioe);
			}
		}
		return jAsceOpt.map(x -> x.toAsceDao());
	}
	
	/**
	 * Read the DASU configuration from the file. 
	 * 
	 * @param id The not null nor empty DASU identifier
	 * @throws IasCdbException In case of error getting the DASU
	 */
	@Override
	public Optional<DasuDao> getDasu(String id) throws IasCdbException {
		Objects.requireNonNull(id, "The DASU identifier cannot be null");
		String cleanedID = id.trim();
		if (cleanedID.isEmpty()) {
			throw new IllegalArgumentException("The identifier can't be an empty string");
		}
		Optional<JsonDasuDao> jDasuOpt;
		try {
			jDasuOpt = getJsonDasu(cleanedID);
		}  catch (IOException ioe) {
			throw new IasCdbException("Error getting JSON DASU",ioe);
		}
		ObjectsHolder holder = new ObjectsHolder();
		if (jDasuOpt.isPresent()) {
			try {
				updateDasuObjects(jDasuOpt.get(), holder);
			} catch (IOException ioe) {
				throw new IasCdbException("Error updating DASU objects",ioe);
			}
		}
		Optional<DasuDao> ret = jDasuOpt.map(x -> x.toDasuDao());
		
		// If the DASU is templated then the ID of the templates of ASCEs
		// must be equal to that of the DASU
		if (ret.isPresent()) {
			Set<AsceDao> asces = ret.get().getAsces();
			String templateOfDasu = ret.get().getTemplateId(); 
			if (templateOfDasu==null) {
				// No template in the DASU: ASCEs must have no template
				if (!asces.stream().allMatch(asce -> asce.getTemplateId()==null)) {
					throw new IasCdbException("Template mismatch between DASU ["+cleanedID+"] and its ASCEs");
				}
			} else {
				if (!asces.stream().allMatch(asce -> asce.getTemplateId().equals(templateOfDasu))) {
					throw new IasCdbException("Template mismatch between DASU ["+cleanedID+"] and its ASCEs");
				}
			}
		}
		
		
		return ret;
	}
	
	/**
	 * Read the supervisor configuration from the JSON file. 
	 * 
	 * This methods reads the JsonSupervisorDao and convert it to SupervisorDao.
	 * The tricky part is the conversion of the JsonDasuToDeployDao of the JsonSupervisorDao
	 * to a DasuToDeployDao that needs to replace the ID of the DASU to deploy with a DasuDao.
	 * The DasuDao can be easily retrieved by calling {@link #getDasu(String)}.
	 * In the same way, the instance of TemplateDao can be easily retrieved
	 * by calling {@link #getTemplate(String)}
	 * 
	 * @param id The not <code>null</code> nor empty supervisor identifier
	 * @throws IasCdbException if the supervisor file does not exist or is unreadable
	 */
	@Override
	public Optional<SupervisorDao> getSupervisor(String id) throws IasCdbException {
		Objects.requireNonNull(id, "The supervisor identifier cannot be null");
		String cleanedID = id.trim();
		if (cleanedID.isEmpty()) {
			throw new IllegalArgumentException("The identifier can't be an empty string");
		}
		
		// Read the JsonSupervisorDao of the given id
		Optional<JsonSupervisorDao> jSupervOpt;
		try {
			jSupervOpt = getJsonSupervisor(cleanedID);
		} catch (IOException ioe) {
			throw new IasCdbException("Error getting JSON SUpervisor",ioe);
		}
		
		if (jSupervOpt.isPresent()) { 
			
			JsonSupervisorDao jSuperv = jSupervOpt.get();
			
			// Build the SupervisorDao
			SupervisorDao ret = new SupervisorDao();
			ret.setId(jSuperv.getId());
			ret.setHostName(jSuperv.getHostName());
			ret.setLogLevel(jSuperv.getLogLevel());
			
			// Convert each JsonDasuToDeployDao in a DasuToDeployDao
			for (JsonDasuToDeployDao jdtd: jSupervOpt.get().getDasusToDeploy()) {
				// get the dasuDao with the ID in the JsonDasuToDeployDao
				Optional<DasuDao> optDasu = getDasu(jdtd.getDasuId());
				if (!optDasu.isPresent()) {
					throw new IasCdbException("DASU ["+jdtd.getDasuId()+"] not found for supevisor ["+id+"]");
				}
				
				TemplateDao template = null;
				Integer instance = jdtd.getInstance();
				
				// If the JsonDasuToDeployDao is templated, then
				// read the template from the id in the JsonDasuToDeployDao
				if (jdtd.getTemplateId()!=null) {
					Optional<TemplateDao> optTemplate = getTemplate(jdtd.getTemplateId());
					if (!optTemplate.isPresent()) {
						throw new IasCdbException("Template ["+jdtd.getTemplateId()+"] not found for supevisor ["+id+"]");
					}
					template = optTemplate.get();
					if (instance==null) {
						throw new IasCdbException("Instance nr. not found for DTD ["+jdtd.getDasuId()+"] of supevisor ["+id+"]");
					}
				} 
				
				// Add the DasuToDeployDao to the SupervisorDao
				ret.addDasuToDeploy(new DasuToDeployDao(optDasu.get(), template, instance));
				
			}
			return Optional.of(ret);
		} else {
			return Optional.empty();
		}
	}
	
	/**
	 * Get the JSonDasu configuration from JSON files.
	 * 
	 * @param id The id of the DASU
	 * @return The JSON configuration of the DASU if found; 
	 *         empty otherwise
	 * @throws IOException In case of error getting the file
	 */
	private Optional<JsonDasuDao>getJsonDasu(String id) throws IOException {
		File dasuFile =  cdbFileNames.getDasuFilePath(id).toFile();
		if (!canReadFromFile(dasuFile)) {
			return Optional.empty();
		} else {
			ObjectMapper mapper = new ObjectMapper();
			return Optional.of(mapper.readValue(dasuFile, JsonDasuDao.class));
		}
	}
	
	/**
	 * Get the JsonAsce configuration from JSON files.
	 * 
	 * @param id The id of the ASCE
	 * @return The JSON configuration of the ASCE if found; 
	 *         empty otherwise
	 */
	private Optional<JsonAsceDao>getJsonAsce(String id) throws IOException {
		File asceFile =  cdbFileNames.getAsceFilePath(id).toFile();
		if (!canReadFromFile(asceFile)) {
			return Optional.empty();
		} else {
			ObjectMapper mapper = new ObjectMapper();
			return Optional.of(mapper.readValue(asceFile, JsonAsceDao.class));
		}
	}
	
	/**
	 * Get the JSonDasu configuration from JSON files.
	 * 
	 * @param id The id of the DASU
	 * @return The JSON configuration of the DASU if found; 
	 *         empty otherwise
	 */
	private Optional<JsonSupervisorDao>getJsonSupervisor(String id) throws IOException {
		File supervFile =  cdbFileNames.getSuperivisorFilePath(id).toFile();
		if (!canReadFromFile(supervFile)) {
			return Optional.empty();
		} else {
			ObjectMapper mapper = new ObjectMapper();
			return Optional.of(mapper.readValue(supervFile, JsonSupervisorDao.class));
		}
	}
	
	/**
	 * Update the the objects included in the JSON DASU
	 * 
	 * @param jDasuDao The JSON DASU pojo
	 * @param holder The holder to rebuild objects just once
	 * @throws IOException
	 * @throws IasCdbException
	 */
	private void updateDasuObjects(JsonDasuDao jDasuDao, ObjectsHolder holder) throws IOException, IasCdbException {
		Objects.requireNonNull(holder, "The holder cannot be null");
		Objects.requireNonNull(jDasuDao, "The jDASU cannot be null");
		DasuDao dasu = jDasuDao.toDasuDao();
		if (!holder.dasus.containsKey(dasu.getId())) {
			holder.dasus.put(dasu.getId(), dasu);
		}
		
		// Fix the supervisor 
//		if (dasu.getSupervisor()==null) {
//			Optional<SupervisorDao> sup;
//			if (holder.supervisors.containsKey(jDasuDao.getSupervisorID())) {
//				sup = Optional.of(holder.supervisors.get(jDasuDao.getSupervisorID()));
//			} else {
//				Optional<JsonSupervisorDao> jSup = getJsonSupervisor(jDasuDao.getSupervisorID());
//				if (jSup.isPresent()) {
//					updateSupervisorObjects(jSup.get(), holder);
//				} else {
//					throw new IasCdbException("Inconsistent DASU record: Supervisor ["+jDasuDao.getSupervisorID()+"] not found in CDB");
//				}
//				sup = jSup.map(x -> x.toSupervisorDao());
//				sup.ifPresent( s -> holder.supervisors.put(jDasuDao.getSupervisorID(), s));
//			}
//			sup.ifPresent(s -> dasu.setSupervisor(s));
//		}
		
		// Fix included ASCEs
		for (String asceID: jDasuDao.getAsceIDs()) {
			Optional<AsceDao> asce;
			if (holder.asces.containsKey(asceID)) {
				asce=Optional.of(holder.asces.get(asceID));
			} else {
				Optional<JsonAsceDao> jAsce = getJsonAsce(asceID);
				if (jAsce.isPresent()) {
					updateAsceObjects(jAsce.get(), holder);
				} else {
					throw new IasCdbException("Inconsistent DASU record: ASCE ["+asceID+"] not found in CDB");
				}
				asce= jAsce.map( a -> a.toAsceDao());
				asce.ifPresent(a -> holder.asces.put(asceID, a));
			}
			asce.ifPresent(a -> dasu.addAsce(a));
		}
		
		// Fix output IASIO
		getIasio(jDasuDao.getOutputId()).ifPresent(o -> dasu.setOutput(o));
	}
	
	/**
	 * Update the the objects included in the JSON 
	 * 
	 * @param jAsceDao The JSON ASCE pojo
	 * @param holder The holder to rebuild objects just once
	 * @throws IOException
	 * @throws IasCdbException
	 */
	private void updateAsceObjects(JsonAsceDao jAsceDao, ObjectsHolder holder) throws IOException, IasCdbException {
		Objects.requireNonNull(jAsceDao, "The jASCE cannot be null");
		Objects.requireNonNull(holder, "The holder cannot be null");
		AsceDao asce = jAsceDao.toAsceDao();
		if (!holder.asces.containsKey(jAsceDao.getId())) {
			holder.asces.put(asce.getId(), asce);
		}
		
		// Fix output IASIO
		getIasio(jAsceDao.getOutputID()).ifPresent(o -> asce.setOutput(o));
		
		// Fix the inputs
		for (String inId: jAsceDao.getInputIDs()) {
			Optional<IasioDao> iasio = getIasio(inId);
			if (iasio.isPresent()) { 
				asce.addInput(iasio.get(), true);
			} else {
				throw new IasCdbException("Inconsistent ASCE record: IASIO ["+inId+"] not found in CDB");
			}
		} 
		
		// Fix the DASU
		Optional<DasuDao> optDasu = Optional.ofNullable(holder.dasus.get(jAsceDao.getDasuID()));
		if (!optDasu.isPresent()) {
			Optional<JsonDasuDao> jDasu=getJsonDasu(jAsceDao.getDasuID());
			if (jDasu.isPresent()) {
				updateDasuObjects(jDasu.get(), holder);
			} else {
				throw new IasCdbException("Inconsistent ASCE record: DASU ["+jAsceDao.getDasuID()+"] not found in CDB");
			}
			optDasu = jDasu.map(x -> x.toDasuDao());
			optDasu.ifPresent(d -> holder.dasus.put(jAsceDao.getDasuID(), d));
		}
		optDasu.ifPresent(d -> asce.setDasu(d));
		
		// Fix the transfer function
		String tfID= jAsceDao.getTransferFunctionID();
		Optional<TransferFunctionDao> tfOpt = Optional.ofNullable(holder.transferFunctions.get(tfID));
		if (!tfOpt.isPresent()) {
			Optional<TransferFunctionDao> jTF = getTransferFunction(tfID);
			if (jTF.isPresent()) {
				asce.setTransferFunction(jTF.get());
				holder.transferFunctions.put(jAsceDao.getTransferFunctionID(), jTF.get());
			} else {
				throw new IasCdbException("Inconsistent ASCE record: TF ["+jAsceDao.getTransferFunctionID()+"] not found in CDB");
			}
		} else {
			asce.setTransferFunction(tfOpt.get());
		}
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
	public Collection<IasioDao> getIasiosForAsce(String id) throws IasCdbException {
		Objects.requireNonNull(id, "The ID cant't be null");
		if (id.isEmpty()) {
			throw new IllegalArgumentException("Invalid empty ID");
		}
		Optional<AsceDao> asce = getAsce(id);
		Collection<IasioDao> ret = asce.orElseThrow(() -> new IasCdbException("ASCE ["+id+"] not dound")).getInputs();
		return (ret==null)? new ArrayList<>() : ret;
	}

	@Override
	public Optional<TransferFunctionDao> getTransferFunction(String tf_id) throws IasCdbException {
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
	 * Initialize the CDB
	 */
	@Override
	public void init() throws IasCdbException {}
	
	/**
	 * Close the CDB and release the associated resources
	 * @throws IasCdbException
	 */
	@Override
	public void shutdown() throws IasCdbException {}
}
