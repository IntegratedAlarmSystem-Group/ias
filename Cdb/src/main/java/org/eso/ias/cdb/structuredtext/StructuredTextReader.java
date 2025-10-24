package org.eso.ias.cdb.structuredtext;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import org.eso.ias.cdb.CdbReader;
import org.eso.ias.cdb.IasCdbException;
import org.eso.ias.cdb.structuredtext.TextFileType;
import org.eso.ias.cdb.pojos.*;
import org.eso.ias.cdb.structuredtext.pojos.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

public class StructuredTextReader implements CdbReader {

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
    private final CdbFiles cdbFileNames;

    /**
     * The type of the files of the CDB
     */
    public final TextFileType cdbFilesType;

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
        cdbFilesType = cdbFileNames.getCdbFileType();
    }

    /**
     * Constructor that guesses the type of the folder from the
     * content of the CDB folder from the content of the parentFolder.
     *
     * @param parentFolder the parent folder of the CDB
     * @throws IasCdbException if it was not possible to get the type of CDB
     *                         from the passed parent folder
     */
    public StructuredTextReader(File parentFolder) throws IasCdbException {
        Objects.requireNonNull(parentFolder);
        Optional<TextFileType> cdbTypeOpt = TextFileType.getCdbType(parentFolder);
        if (cdbTypeOpt.isEmpty()) {
            throw new IasCdbException("Could not get the type of the CDB from "+parentFolder.getAbsolutePath());
        }
        try {
            this.cdbFileNames=new CdbTxtFiles(parentFolder, cdbTypeOpt.get());
            cdbFilesType = cdbFileNames.getCdbFileType();
            logger.info("{} CDB reader from CDB in {}", cdbFilesType, parentFolder.getAbsolutePath());
        } catch (Exception e) {
            throw new IasCdbException("Error building the CdbFiles for folder "+parentFolder.getAbsolutePath(), e);
        }

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
            throw new IasCdbException("Error getting " +cdbFileNames.getCdbFileType()+" Supervisor",ioe);
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
            for (JsonDasuToDeployDao jdtd: jSuperv.getDasusToDeploy()) {
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
            ObjectMapper mapper = getObjectMapper();
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
            ObjectMapper mapper = getObjectMapper();
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
     * Return the ASCEs belonging to the given DASU.
     *
     * @param id The not <code>null</code> nor empty identifier of the supervisor
     * @return A set of DASUs running in the supervisor with the passed id
     * @throws IasCdbException in case of error reading CDB or if the
     *                         DASU with the give identifier does not exist
     */
    public Set<AsceDao> getAscesForDasu(String id) throws IasCdbException {
        Optional<DasuDao> dasu = getDasu(id);
        Set<AsceDao> ret = dasu.orElseThrow(() -> new IasCdbException("DASU ["+id+"] not found")).getAsces();
        return (ret==null)? new HashSet<>() : ret;
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
        Optional<SupervisorDao> superv = getSupervisor(id);
        Set<DasuToDeployDao> ret = superv.orElseThrow(() -> new IasCdbException("Supervisor ["+id+"] not dound")).getDasusToDeploy();
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
        Optional<AsceDao> asce = getAsce(id);
        Collection<IasioDao> ret = asce.orElseThrow(() -> new IasCdbException("ASCE ["+id+"] not found")).getInputs();
        return (ret==null)? new ArrayList<>() : ret;
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
     * Return the IDs in the passed folders.
     *
     * For Superrvisors, DASUs, ASCEs, and TFS the Ids match with the names of the
     * files in the folder.
     *
     * @param placeHolderFilename A place holder for a file in the folder
     * @return the IDs in the passed folders.
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
                return s.toLowerCase().endsWith(cdbFileNames.getCdbFileType().ext);
            }
        };
        String folderName= fName.substring(0,pos);
        File folder = new File(folderName);
        File[] filesInFolder = folder.listFiles(filter);

        if (filesInFolder==null) {
            logger.warn(folderName+" NOT found");
            return new HashSet<>();
        }

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
                ObjectMapper mapper = getObjectMapper();
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
	public Collection<TemplateInstanceIasioDao> getTemplateInstancesIasiosForAsce(String id) throws IasCdbException {
        Optional<AsceDao> asce = getAsce(id);
        Collection<TemplateInstanceIasioDao> ret = asce.orElseThrow(() -> new IasCdbException("ASCE ["+id+"] not dound")).getTemplatedInstanceInputs();
        return (ret==null)? new ArrayList<>() : ret;
    }
}
