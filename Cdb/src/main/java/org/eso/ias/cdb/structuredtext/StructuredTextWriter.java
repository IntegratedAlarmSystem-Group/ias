package org.eso.ias.cdb.structuredtext;

import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import org.eso.ias.cdb.CdbWriter;
import org.eso.ias.cdb.IasCdbException;
import org.eso.ias.cdb.pojos.*;
import org.eso.ias.cdb.structuredtext.pojos.JsonAsceDao;
import org.eso.ias.cdb.structuredtext.pojos.JsonDasuDao;
import org.eso.ias.cdb.structuredtext.pojos.JsonSupervisorDao;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

public class StructuredTextWriter implements CdbWriter {

    /**
     * The logger
     */
    private static final Logger logger = LoggerFactory.getLogger(StructuredTextWriter.class);

    /**
     * Signal if the reader has been initialized
     */
    protected final AtomicBoolean initialized = new AtomicBoolean(false);

    /**
     * Signal if the reader has been closed
     */
    protected final AtomicBoolean closed = new AtomicBoolean(false);

    /**
     * The CdbFiles to get the names of the files to write
     */
    protected final CdbFiles cdbFiles;

    /**
     * The type of the files of the CDB
     */
    public final TextFileType cdbFilesType;

    /**
     * Constructor
     * @param cdbFileNames  The files of the CDB
     */
    public StructuredTextWriter(CdbFiles cdbFiles) {
        Objects.requireNonNull(cdbFiles, "cdbFileNames can't be null");
        this.cdbFiles=cdbFiles;
        cdbFilesType = cdbFiles.getCdbFileType();
    }

    public StructuredTextWriter(File parentFolder) throws IasCdbException {
        Objects.requireNonNull(parentFolder, "Parent folder can't be null");
        Optional<TextFileType> cdbTypeOpt = TextFileType.getCdbType(parentFolder);
        if (cdbTypeOpt.isEmpty()) {
            throw new IasCdbException("Could not get the type of the CDB from "+parentFolder.getAbsolutePath());
        }
        try {
            this.cdbFiles=new CdbTxtFiles(parentFolder, cdbTypeOpt.get());
            cdbFilesType = cdbFiles.getCdbFileType();
            logger.info("{} CDB writer from CDB in {}", cdbFilesType, parentFolder.getAbsolutePath());
        } catch (Exception e) {
            throw new IasCdbException("Error building the CdbFiles for folder "+parentFolder.getAbsolutePath(), e);
        }
    }

    /**
     * Initialize the CDB
     */
    @Override
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
    @Override
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
     * Check if the file containing the given file exists and is writable.
     * It creates the directory if it does not exist.
     * 
     * @param file The file whose parent directory must be checked and,
     *             if it is the case, created
     * @throws IasCdbException In case of error creating the folder or if it is not writable
     */
    void checkFolderOfFile(Path file) throws IasCdbException {
        Path parentFolder = file.getParent();
        if (parentFolder==null) {
            throw new IasCdbException("Cannot get the parent folder of file "+file);
        }
        if (!Files.exists(parentFolder)) {
            try {
                Files.createDirectories(parentFolder);
            } catch (IOException ioe) {
                throw new IasCdbException("Error creating folder "+parentFolder.toAbsolutePath(),ioe);
            }
        }
    }

    /**
     * Get the {@link ObjectMapper} depending on the type of
     * files in the CDB
     *
     * @return The {@link ObjectMapper} to parse the files
     */
    protected ObjectMapper getObjectMapper() {
        switch (cdbFiles.getCdbFileType()) {
            case JSON: return new ObjectMapper();
            case YAML: return new YAMLMapper();
            default: throw new UnsupportedOperationException("Unsupported CDB file type "+cdbFiles.getCdbFileType());
        }
    }

    /**
     * Serialize the ias in the JSON file.
     *
     * @param ias The IAS configuration to write in the file
     */
    @Override
    public void writeIas(IasDao ias) throws IasCdbException {
        if (closed.get()) {
            throw new IasCdbException("The writer is shut down");
        }
        if (!initialized.get()) {
            throw new IasCdbException("The writer is not initialized");
        }

        Objects.requireNonNull(ias);
        File f;
        try {
            f= cdbFiles.getIasFilePath().toFile();
        }catch (IOException ioe) {
            throw new IasCdbException("Error getting IAS file",ioe);
        }
        checkFolderOfFile(f.toPath());
        ObjectMapper mapper = getObjectMapper();
        mapper.setDefaultPrettyPrinter(new DefaultPrettyPrinter());
        try {
            mapper.writerWithDefaultPrettyPrinter().writeValue(f, ias);
        } catch (Throwable t) {
            throw new IasCdbException("Error writing JSON IAS",t);
        }
    }

    /**
     * Serialize the Supervisor in the JSON file.
     *
     * @param superv The Supervisor configuration to write in the file
     */
    @Override
    public void writeSupervisor(SupervisorDao superv) throws IasCdbException  {
        if (closed.get()) {
            throw new IasCdbException("The writer is shut down");
        }
        if (!initialized.get()) {
            throw new IasCdbException("The writer is not initialized");
        }

        Objects.requireNonNull(superv);
        File f;
        try {
            f = cdbFiles.getSuperivisorFilePath(superv.getId()).toFile();
        }catch (IOException ioe) {
            throw new IasCdbException("Error getting Supervisor file",ioe);
        }
        checkFolderOfFile(f.toPath());

        JsonSupervisorDao jsonSup = new JsonSupervisorDao(superv);
        ObjectMapper mapper = getObjectMapper();
        mapper.setDefaultPrettyPrinter(new DefaultPrettyPrinter());
        try {
            mapper.writerWithDefaultPrettyPrinter().writeValue(f, jsonSup);
        }catch (Throwable t) {
            throw new IasCdbException("Error writing JSON Supervisor",t);
        }
    }

    /**
     * Serialize the IASIO in the structured text file
     * by delegation to {@link #writeIasios(Set, boolean)}
     *
     * @param iasio The IASIO configuration to write in the file
     * @param append: if <code>true</code> the passed iasio is appended to the file
     *                otherwise a new file is created
     *
     * @see CdbWriter#writeIasio(IasioDao, boolean)
     */
    @Override
    public void writeIasio(IasioDao iasio, boolean append) throws IasCdbException {
        if (closed.get()) {
            throw new IasCdbException("The writer is shut down");
        }
        if (!initialized.get()) {
            throw new IasCdbException("The writer is not initialized");
        }

        Set<IasioDao> iasios = new HashSet<>();
        iasios.add(iasio);

        writeIasios(iasios,append);
    }

    /**
     * Serialize the IASIOs in the JSON file.
     *
     * <P>If <code>append</code> is <code>false</code> then a new file is created otherwise
     * the IASIOs in the passed files are written at the end of the file.
     * <BR>If a IASIO in <code>iasios</code> already exists in the file, the latter
     * is replaced by that in the set.
     *
     * @param iasios The IASIOs to write in the file
     * @param append: if <code>true</code> the passed iasios are appended to the file
     *                otherwise a new file is created
     *
     * @see CdbWriter#writeIasios(Set, boolean)
     */
    @Override
    public void writeIasios(Set<IasioDao> iasios, boolean append) throws IasCdbException {
        if (closed.get()) {
            throw new IasCdbException("The writer is shut down");
        }
        if (!initialized.get()) {
            throw new IasCdbException("The writer is not initialized");
        }
        Objects.requireNonNull(iasios);

        File f;
        try {
            f = cdbFiles.getIasioFilePath("UnusedID").toFile();
        } catch (IOException ioe) {
            throw new IasCdbException("Error getting IASIOs file",ioe);
        }
        checkFolderOfFile(f.toPath());
        ObjectMapper mapper = getObjectMapper();
        mapper.setDefaultPrettyPrinter(new DefaultPrettyPrinter());
        if (!f.exists() || !append) {
            try {
                mapper.writerWithDefaultPrettyPrinter().writeValue(f, iasios);
            }catch (Throwable t) {
                throw new IasCdbException("Error writing JSON IASIOs",t);
            }
        } else {

            // Reads the IASIOs from the file
            IasioDao[] iasioDaos;
            try {
                iasioDaos = mapper.readValue(f, IasioDao[].class);
            } catch (Exception e) {
                throw new IasCdbException("Error reading IASIOs from" + f.getAbsolutePath(), e);
            }

            // The IASIOs to write are those read from the file plus those passed
            // to this method, without repetitions
            Map<String, IasioDao> iasiosToWrite = new HashMap<>();
            for (IasioDao iDao: iasioDaos) {
                iasiosToWrite.put(iDao.getId(), iDao);
            }
            // Adds the IASIOs read from the files (the map revoes repetitions)
            for (IasioDao iDao: iasios) {
                 iasiosToWrite.put(iDao.getId(), iDao);
            }
            try {
                mapper.writerWithDefaultPrettyPrinter().writeValue(f, iasiosToWrite.values());
            }catch (Throwable t) {
                throw new IasCdbException("Error writing IASIOs to "+f.getAbsolutePath(),t);
            }
        }
    }

    /**
     * Serialize the DASU in the JSON file.
     *
     * @param dasu The DASU configuration to write in the file
     */
    @Override
    public void writeDasu(DasuDao dasu) throws IasCdbException {
        if (closed.get()) {
            throw new IasCdbException("The writer is shut down");
        }
        if (!initialized.get()) {
            throw new IasCdbException("The writer is not initialized");
        }

        Objects.requireNonNull(dasu);
        File f;
        try {
            f = cdbFiles.getDasuFilePath(dasu.getId()).toFile();
        }catch (IOException ioe) {
            throw new IasCdbException("Error getting DASU file",ioe);
        }
        checkFolderOfFile(f.toPath());
        JsonDasuDao jsonDasu = new JsonDasuDao(dasu);
        ObjectMapper mapper = getObjectMapper();
        mapper.setDefaultPrettyPrinter(new DefaultPrettyPrinter());
        try {
            mapper.writerWithDefaultPrettyPrinter().writeValue(f, jsonDasu);
        }catch (Throwable t) {
            throw new IasCdbException("Error writing JSON DASU",t);
        }
    }

    /**
     * Serialize the ASCE in the JSON file.
     *
     * @param asce The ASCE configuration to write in the file
     */
    @Override
    public void writeAsce(AsceDao asce) throws IasCdbException {
        if (closed.get()) {
            throw new IasCdbException("The writer is shut down");
        }
        if (!initialized.get()) {
            throw new IasCdbException("The writer is not initialized");
        }
        Objects.requireNonNull(asce);

        File f;
        try {
            f = cdbFiles.getAsceFilePath(asce.getId()).toFile();
        }catch (IOException ioe) {
            throw new IasCdbException("Error getting ASCE file",ioe);
        }
        checkFolderOfFile(f.toPath());
        JsonAsceDao jsonAsce = new JsonAsceDao(asce);
        ObjectMapper mapper = getObjectMapper();
        mapper.setDefaultPrettyPrinter(new DefaultPrettyPrinter());
        try {
            mapper.writerWithDefaultPrettyPrinter().writeValue(f, jsonAsce);
        }catch (Throwable t) {
            throw new IasCdbException("Error writing JSON ASCE",t);
        }
    }

    /**
     *  Write the transfer function to the CDB
     *
     *  @param transferFunction The TF configuration to write in the file
     *  @throws IasCdbException In case of error writing the TF
     */
    @Override
    public void writeTransferFunction(TransferFunctionDao transferFunction) throws IasCdbException {
        if (closed.get()) {
            throw new IasCdbException("The writer is shut down");
        }
        if (!initialized.get()) {
            throw new IasCdbException("The writer is not initialized");
        }
        Objects.requireNonNull(transferFunction);
        Set<TransferFunctionDao> tfs = new HashSet<>();
        tfs.add(transferFunction);
        writeTransferFunctions(tfs, true);
    }

    /**
     * Serialize the TFs in the JSON file.
     *
     * <P>If <code>append</code> is <code>false</code> then a new file is created otherwise
     * the TFs in the passed files are written at the end of the file.
     * <BR>If a TF in <code>tfs</code> already exists in the file, the latter
     * is replaced by that in the set.
     *
     * @param tfs The TFs to write in the file
     * @param append: if <code>true</code> the passed TFs are appended to the file
     *                otherwise a new file is created
     */
    private void writeTransferFunctions(Set<TransferFunctionDao> tfs, boolean append) throws IasCdbException {
        File f;
        try {
            // The ID is unused in getTFFilePath
            f = cdbFiles.getTFFilePath("UnusedID").toFile();
        } catch (IOException ioe) {
            throw new IasCdbException("Error getting TFs file",ioe);
        }
        checkFolderOfFile(f.toPath());

        ObjectMapper mapper = getObjectMapper();
        mapper.setDefaultPrettyPrinter(new DefaultPrettyPrinter());
        if (!f.exists() || !append) {
            try {
                mapper.writerWithDefaultPrettyPrinter().writeValue(f, tfs);
            } catch (Throwable t) {
                throw new IasCdbException("Error writing JSON TFss",t);
            }
        } else {

            TransferFunctionDao[] tfsFromFile;
            try {
                tfsFromFile = mapper.readValue(f, TransferFunctionDao[].class);
            } catch (Exception e) {
                throw new IasCdbException("Exception reading TFs from "+f.getAbsolutePath(),e);
            }
            Map<String, TransferFunctionDao> tfsMap = new HashMap<>();
            for (TransferFunctionDao tDao: tfsFromFile) {
                tfsMap.put(tDao.getClassName(), tDao);
            }
            for (TransferFunctionDao tDao: tfs) {
                tfsMap.put(tDao.getClassName(), tDao);
            }
            try {
                mapper.writerWithDefaultPrettyPrinter().writeValue(f, tfsMap.values());
            } catch  (Exception e) {
                throw new IasCdbException("Exception writing TFs to "+f.getAbsolutePath(),e);
            }
        }
    }

    /**
     *  Write the passed template to the CDB
     *
     *  @param templateDao The template DAO to write in the file
     *  @throws IasCdbException In case of error writing the TF
     */
    @Override
    public void writeTemplate(TemplateDao templateDao) throws IasCdbException {
        if (closed.get()) {
            throw new IasCdbException("The writer is shut down");
        }
        if (!initialized.get()) {
            throw new IasCdbException("The writer is not initialized");
        }

        Objects.requireNonNull(templateDao);
        Set<TemplateDao> templates = new HashSet<>();
        templates.add(templateDao);
        writeTemplates(templates, true);
    }

    /**
     * Serialize the templates in the JSON file.
     *
     * <P>If <code>append</code> is <code>false</code> then a new file is created otherwise
     * the templates in the passed files are appended at the end of the file.
     * <BR>If a emplate in <code>templates</code> already exists in the file, the latter
     * is replaced by that in the set.
     *
     * @param templates The templates to write in the file
     * @param append: if <code>true</code> the passed TFs are appended to the file
     *                otherwise a new file is created
     */
    private void writeTemplates(Set<TemplateDao> templates, boolean append) throws IasCdbException {
        File f;
        try {
            // The ID is unused in getTFFilePath
            f = cdbFiles.getTemplateFilePath("UnusedID").toFile();
        }catch (IOException ioe) {
            throw new IasCdbException("Error getting TFs file",ioe);
        }
        checkFolderOfFile(f.toPath());

        ObjectMapper mapper = new ObjectMapper();
        mapper.setDefaultPrettyPrinter(new DefaultPrettyPrinter());
        if (!f.exists() || !append) {
            try {
                mapper.writerWithDefaultPrettyPrinter().writeValue(f, templates);
            } catch (Throwable t) {
                throw new IasCdbException("Error writing templates",t);
            }
        } else {

            TemplateDao[] templatesOnFile;
            try {
                templatesOnFile = mapper.readValue(f, TemplateDao[].class);
            } catch (Exception e) {
                throw new IasCdbException("Error reading templates from" + f.getAbsolutePath(), e);
            }
            Map<String, TemplateDao> templatesMap = new HashMap<>();
            for (TemplateDao tDao: templatesOnFile) {
                templatesMap.put(tDao.getId(), tDao);
            }
            for (TemplateDao tDao: templates) {
                templatesMap.put(tDao.getId(), tDao);
            }
            try {
                mapper.writerWithDefaultPrettyPrinter().writeValue(f, templatesMap.values());
            } catch (Throwable t) {
                throw new IasCdbException("Error writing templates on file "+f.getAbsolutePath(),t);
            }
        }
    }

    /**
     * Write the configuration of the passed plugin
     *
     * @param pluginConfigDao the configuraton of the plugin
     * @throws IasCdbException In case of error writing the configuration
     */
    @Override
    public void writePluginConfig(PluginConfigDao pluginConfigDao) throws IasCdbException {
        if (closed.get()) {
            throw new IasCdbException("The writer is shut down");
        }
        if (!initialized.get()) {
            throw new IasCdbException("The writer is not initialized");
        }

        Objects.requireNonNull(pluginConfigDao);
        File f;
        try {
            f = cdbFiles.getPluginFilePath(pluginConfigDao.getId()).toFile();
        } catch (IOException ioe) {
            throw new IasCdbException("Error getting plugin file "+pluginConfigDao.getId(),ioe);
        }
        checkFolderOfFile(f.toPath());

        ObjectMapper mapper = getObjectMapper();
        mapper.setDefaultPrettyPrinter(new DefaultPrettyPrinter());
        try {
            mapper.writerWithDefaultPrettyPrinter().writeValue(f, pluginConfigDao);
        } catch (Throwable t) {
            throw new IasCdbException("Error writing JSON plugin configuration",t);
        }
    }

    /**
     * Write the configuration of the client with the passed identifier.
     *
     * The configuration is written as it is i.e. without converting to JSON
     * because the format of the config is defined by each client
     *
     * @param clientConfigDao the configuraton of the client
     * @throws IasCdbException In case of error writing the configuration
     */
    @Override
    public void writeClientConfig(ClientConfigDao clientConfigDao) throws IasCdbException {
        if (closed.get()) {
            throw new IasCdbException("The writer is shut down");
        }
        if (!initialized.get()) {
            throw new IasCdbException("The writer is not initialized");
        }

        Objects.requireNonNull(clientConfigDao);
        Path f;
        try {
            f = cdbFiles.getClientFilePath(clientConfigDao.getId());
        } catch (IOException ioe) {
            throw new IasCdbException("Error getting client file "+clientConfigDao.getId(),ioe);
        }
        checkFolderOfFile(f);

        List<String> strings = new ArrayList<>();
        strings.add(clientConfigDao.getConfig());
        try {
            Files.write(f,strings, StandardOpenOption.WRITE, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        } catch (IOException ioe) {
            throw new IasCdbException("Error reading client file "+f.toString(),ioe);
        }
    }

}
