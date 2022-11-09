package org.eso.ias.cdb.structuredtext;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import org.eso.ias.cdb.CdbReader;
import org.eso.ias.cdb.IasCdbException;
import org.eso.ias.cdb.pojos.*;
import org.eso.ias.cdb.structuredtext.json.CdbFiles;
import org.eso.ias.cdb.structuredtext.json.JsonReader;
import org.eso.ias.cdb.structuredtext.json.pojos.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

public abstract class StructuredTextReader implements CdbReader {

    /**
     * The logger
     */
    private static final Logger logger = LoggerFactory.getLogger(StructuredTextReader.class);

    /**
     * Signal if the reader has been initialized
     */
    protected final AtomicBoolean initialized = new AtomicBoolean(false);

    /**
     * <code>cdbFileNames</code> return the names of the files to read
     */
    protected final CdbFiles cdbFileNames;

    /**
     * Signal if the reader has been closed
     */
    protected final AtomicBoolean closed = new AtomicBoolean(false);

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
     * Constructor
     *
     * @param cdbFiles The files of the CDB
     */
    public StructuredTextReader(CdbFiles cdbFiles) {
        Objects.requireNonNull(cdbFiles);
        this.cdbFileNames=cdbFiles;
    }

    /**
     * Get the {@link ObjectMapper} depending on the type of
     * files in the CDB
     *
     * @return The {@link ObjectMapper} to parse the files
     */
    protected ObjectMapper getObjectMapper() {
        switch (cdbFileNames.getCdbFileType()) {
            case JSON: return new ObjectMapper();
            case YAML: return new YAMLMapper();
            default: throw new UnsupportedOperationException("Unsupported CDB file type "+cdbFileNames.getCdbFileType());
        }
    }

    /**
     * Check if the passed file is readable
     *
     * @param inF The file to check
     * @return true if the file exists and is readable
     */
    protected boolean canReadFromFile(File inF) {
        return inF.exists() && inF.isFile() && inF.canRead();
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
            // Parse the file in a pojo
            ObjectMapper mapper = getObjectMapper();
            try {
                IasDao ias = mapper.readValue(f, IasDao.class);
                return Optional.of(ias);
            } catch (Exception e) {
                throw new IasCdbException("Error reading IAS from "+f.getAbsolutePath(),e);
            }
        }
    }

    /**
     * Get the JsonSupervisor configuration from the structured text file.
     *
     * @param id The id of the Supervisor
     * @return The JSON configuration of the Supervisor if found;
     *         empty otherwise
     */
    private Optional<JsonSupervisorDao> parseSupervisorFile(String id) throws IOException {
        File supervFile =  cdbFileNames.getSuperivisorFilePath(id).toFile();
        if (!canReadFromFile(supervFile)) {
            return Optional.empty();
        } else {
            ObjectMapper mapper = getObjectMapper();
            return Optional.of(mapper.readValue(supervFile, JsonSupervisorDao.class));
        }
    }

    /**
     * Read the supervisor configuration from the CDB.
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
        if (closed.get()) {
            throw new IasCdbException("The reader is shut down");
        }
        if (!initialized.get()) {
            throw new IasCdbException("The reader is not initialized");
        }

        Objects.requireNonNull(id, "The supervisor identifier cannot be null");
        String cleanedID = id.trim();
        if (cleanedID.isEmpty()) {
            throw new IllegalArgumentException("The identifier can't be an empty string");
        }

        // Read the JsonSupervisorDao of the given id
        Optional<JsonSupervisorDao> jSupervOpt;
        try {
            jSupervOpt = parseSupervisorFile(cleanedID);
        } catch (IOException ioe) {
            throw new IasCdbException("Error getting JSON Supervisor",ioe);
        }

        if (jSupervOpt.isPresent()) {

            JsonSupervisorDao jSuperv = jSupervOpt.get();

            if (!jSuperv.getId().equals(id)) {
                throw  new IasCdbException("CDB ID vs File name misconfiguration for Supervisor with ID=["+id+"]");
            }

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
     * Read the DASU configuration from the file.
     *
     * @param id The not null nor empty DASU identifier
     * @throws IasCdbException In case of error getting the DASU
     */
    @Override
    public Optional<DasuDao> getDasu(String id) throws IasCdbException {
        if (closed.get()) {
            throw new IasCdbException("The reader is shut down");
        }
        if (!initialized.get()) {
            throw new IasCdbException("The reader is not initialized");
        }

        Objects.requireNonNull(id, "The DASU identifier cannot be null");
        String cleanedID = id.trim();
        if (cleanedID.isEmpty()) {
            throw new IllegalArgumentException("The identifier can't be an empty string");
        }
        Optional<JsonDasuDao> jDasuOpt;
        try {
            jDasuOpt = parseDasuFile(cleanedID);
        }  catch (IOException ioe) {
            throw new IasCdbException("Error getting JSON DASU "+id,ioe);
        }
        StructuredTextReader.ObjectsHolder holder = new StructuredTextReader.ObjectsHolder();
        if (jDasuOpt.isPresent()) {
            if (!jDasuOpt.get().getId().equals(id)) {
                throw  new IasCdbException("CDB ID vs File name misconfiguration for DASU with ID=["+id+"]");
            }
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
            if (templateOfDasu==null || templateOfDasu.isEmpty()) {
                // No template in the DASU: ASCEs must have no template
                if (!asces.stream().allMatch(asce -> asce.getTemplateId()==null || asce.getTemplateId().isEmpty()) ) {
                    throw new IasCdbException("Template mismatch between DASU ["+cleanedID+"] and its ASCEs");
                }
            } else {
                if (!asces.stream().allMatch(asce -> asce.getTemplateId()!=null && asce.getTemplateId().equals(templateOfDasu))) {
                    String asceIds = Arrays.toString(asces.stream().map(a -> a.getId()).toArray());
                    throw new IasCdbException("Template mismatch between DASU ["+cleanedID+"] and its ASCEs "+asceIds);
                }
            }
        }


        return ret;
    }

    /**
     * Get the JSonDasu configuration from JSON files.
     *
     * @param id The id of the DASU
     * @return The JSON configuration of the DASU if found;
     *         empty otherwise
     * @throws IOException In case of error getting the file
     */
    private Optional<JsonDasuDao> parseDasuFile(String id) throws IOException {
        File dasuFile =  cdbFileNames.getDasuFilePath(id).toFile();
        if (!canReadFromFile(dasuFile)) {
            return Optional.empty();
        } else {
            ObjectMapper mapper = getObjectMapper();
            return Optional.of(mapper.readValue(dasuFile, JsonDasuDao.class));
        }
    }

    /**
     * Update the objects included in the JSON DASU
     *
     * @param jDasuDao The JSON DASU pojo
     * @param holder The holder to rebuild objects just once
     * @throws IOException
     * @throws IasCdbException
     */
    private void updateDasuObjects(JsonDasuDao jDasuDao, StructuredTextReader.ObjectsHolder holder) throws IOException, IasCdbException {
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
                Optional<JsonAsceDao> jAsce = parseAsceFile(asceID);
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
        Optional<IasioDao> outputOpt = getIasio(jDasuDao.getOutputId());
        IasioDao outputIasio = outputOpt.orElseThrow( () -> new IasCdbException("Output "+jDasuDao.getOutputId()+" of DASU "+jDasuDao.getId()+" not found in CDB"));
        dasu.setOutput(outputIasio);
    }

    /**
     * Update the the objects included in the JSON
     *
     * @param jAsceDao The JSON ASCE pojo
     * @param holder The holder to rebuild objects just once
     * @throws IOException
     * @throws IasCdbException
     */
    private void updateAsceObjects(JsonAsceDao jAsceDao, StructuredTextReader.ObjectsHolder holder) throws IOException, IasCdbException {
        Objects.requireNonNull(jAsceDao, "The jASCE cannot be null");
        Objects.requireNonNull(holder, "The holder cannot be null");
        AsceDao asce = jAsceDao.toAsceDao();
        if (!holder.asces.containsKey(jAsceDao.getId())) {
            holder.asces.put(asce.getId(), asce);
        }

        // Fix output IASIO
        Optional<IasioDao> outputOpt = getIasio(jAsceDao.getOutputID());
        IasioDao outputIasio = outputOpt.orElseThrow( () -> new IasCdbException("Output "+jAsceDao.getOutputID()+" of ASCE "+jAsceDao.getId()+" not found in CDB"));
        asce.setOutput(outputIasio);

        // Fix the inputs
        for (String inId: jAsceDao.getInputIDs()) {
            Optional<IasioDao> iasio = getIasio(inId);
            if (iasio.isPresent()) {

                // Check consistency of template
                String templateId = iasio.get().getTemplateId();
                if (templateId!=null && !templateId.isEmpty()) {
                    Optional<TemplateDao> templateDaoOpt = getTemplate(templateId);
                    if (!templateDaoOpt.isPresent()) {
                        throw new IasCdbException("Template "+templateId+" of IASIO "+inId+" NOT found in CDB");
                    }
                }

                asce.addInput(iasio.get(), true);
            } else {
                throw new IasCdbException("Inconsistent ASCE record: IASIO ["+inId+"] NOT found in CDB");
            }
        }

        // Fix templated inputs
        for (JsonTemplatedInputsDao templatedinput: jAsceDao.getTemplatedInputs()) {
            TemplateInstanceIasioDao tiid = new TemplateInstanceIasioDao();

            int instance=templatedinput.getInstanceNum();
            String iasioId= templatedinput.getIasioId();
            String templateId = templatedinput.getTemplateId();

            Optional<TemplateDao> templateDaoOpt = getTemplate(templateId);
            if (!templateDaoOpt.isPresent()) {
                throw new IasCdbException("Template "+templateId+" of IASIO "+iasioId+" NOT found in CDB");
            } else {
                if (instance<templateDaoOpt.get().getMin() || instance>templateDaoOpt.get().getMax()) {
                    throw new IasCdbException("Instance "+instance+" of IASIO "+iasioId+" out of allowed range ["+
                            templateDaoOpt.get().getMin()+","+templateDaoOpt.get().getMax()+"]");
                }
            }

            tiid.setInstance(instance);
            tiid.setTemplateId(templateId);
            Optional<IasioDao> iasio = getIasio(iasioId);
            if (iasio.isPresent()) {
                tiid.setIasio(iasio.get());
                asce.addTemplatedInstanceInput(tiid,true);
            } else {
                throw new IasCdbException("Inconsistent ASCE record: IASIO ["+templatedinput.getIasioId()+"] not found in CDB");
            }
        }

        // Fix the DASU
        Optional<DasuDao> optDasu = Optional.ofNullable(holder.dasus.get(jAsceDao.getDasuID()));
        if (!optDasu.isPresent()) {
            Optional<JsonDasuDao> jDasu=parseDasuFile(jAsceDao.getDasuID());
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
     * Get the JsonAsce configuration from JSON files.
     *
     * @param id The id of the ASCE
     * @return The JSON configuration of the ASCE if found; 
     *         empty otherwise
     */
    private Optional<JsonAsceDao> parseAsceFile(String id) throws IOException {
        File asceFile =  cdbFileNames.getAsceFilePath(id).toFile();
        if (!canReadFromFile(asceFile)) {
            return Optional.empty();
        } else {
            ObjectMapper mapper = getObjectMapper();
            return Optional.of(mapper.readValue(asceFile, JsonAsceDao.class));
        }
    }

    /**
     * Read the ASCE configuration from the file.
     *
     * @param id The not null nor empty ASCE identifier
     * @throws IasCdbException In case of error getting the ASCE
     */
    @Override
    public Optional<AsceDao> getAsce(String id) throws IasCdbException {
        if (closed.get()) {
            throw new IasCdbException("The reader is shut down");
        }
        if (!initialized.get()) {
            throw new IasCdbException("The reader is not initialized");
        }

        Objects.requireNonNull(id, "The ASCE identifier cannot be null");
        String cleanedID = id.trim();
        if (cleanedID.isEmpty()) {
            throw new IllegalArgumentException("The identifier can't be an empty string");
        }

        Optional<JsonAsceDao> jAsceOpt;
        try {
            jAsceOpt = parseAsceFile(cleanedID);
        } catch (IOException ioe) {
            ioe.printStackTrace();
            throw new IasCdbException("Error getting the JSON ASCE "+cleanedID,ioe);
        }
        ObjectsHolder holder = new ObjectsHolder();
        if (jAsceOpt.isPresent()) {
            if (!jAsceOpt.get().getId().equals(id)) {
                throw  new IasCdbException("CDB ID vs File name misconfiguration for ASCE with ID=["+id+"]");
            }
            try {
                updateAsceObjects(jAsceOpt.get(), holder);
            } catch (IOException ioe) {
                ioe.printStackTrace();
                throw new IasCdbException("Error updating ASCE objects",ioe);
            }
        }
        return jAsceOpt.map(x -> x.toAsceDao());
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
        if (closed.get()) {
            throw new IasCdbException("The reader is shut down");
        }
        if (!initialized.get()) {
            throw new IasCdbException("The reader is not initialized");
        }

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
            ObjectMapper mapper = getObjectMapper();
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
        if (closed.get()) {
            throw new IasCdbException("The reader is shut down");
        }
        if (!initialized.get()) {
            throw new IasCdbException("The reader is not initialized");
        }

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
}
