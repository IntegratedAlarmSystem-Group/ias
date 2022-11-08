package org.eso.ias.cdb.structuredtext.yaml;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import org.eso.ias.cdb.CdbWriter;
import org.eso.ias.cdb.IasCdbException;
import org.eso.ias.cdb.structuredtext.json.CdbFiles;
import org.eso.ias.cdb.pojos.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

public class YamlWriter implements CdbWriter {

    /**
     * The logger
     */
    private static final Logger logger = LoggerFactory.getLogger(YamlWriter.class);

    /**
     * Signal if the reader has been initialized
     */
    private final AtomicBoolean initialized = new AtomicBoolean(false);

    /**
     * Signal if the reader has been closed
     */
    private final AtomicBoolean closed = new AtomicBoolean(false);

    /**
     * cdbFileNames return the names of the files to read
     */
    private final CdbFiles cdbFileNames;

    /**
     * Constructor
     *
     * @param cdbFileNames CdbFile to get the name of the file to red
     */
    public YamlWriter(CdbFiles cdbFileNames) {
        Objects.requireNonNull(cdbFileNames, "cdbFileNames can't be null");
        this.cdbFileNames=cdbFileNames;
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
            f= cdbFileNames.getIasFilePath().toFile();
        }catch (IOException ioe) {
            throw new IasCdbException("Error getting IAS file",ioe);
        }
        ObjectMapper mapper = new YAMLMapper();
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

        throw new IasCdbException("Unsupported operation");
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
        throw new IasCdbException("Unsupported operation");
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

        throw new IasCdbException("Unsupported operation");
    }

    /**
     * Serialize the IASIO in the JSON file.
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

        throw new IasCdbException("Unsupported operation");
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

        throw new IasCdbException("Unsupported operation");
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
        throw new IasCdbException("Unsupported operation");
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
            f = cdbFileNames.getTFFilePath("UnusedID").toFile();
        }catch (IOException ioe) {
            throw new IasCdbException("Error getting TFs file",ioe);
        }

        throw new IasCdbException("Unsupported operation");
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
            f = cdbFileNames.getTemplateFilePath("UnusedID").toFile();
        }catch (IOException ioe) {
            throw new IasCdbException("Error getting TFs file",ioe);
        }

        throw new IasCdbException("Unsupported operation");
    }

    /**
     * Write a IasioDao in the JSON file.
     *
     * @param iasio The IASIO to write in the file
     * @param jg The Jakson2 generator
     * @throws IOException In case of error writing the IASIO
     */
    private void putNextIasio(IasioDao iasio, JsonGenerator jg) throws IOException {

    }

    /**
     * Write a TransferFunctionDao in the JSON file.
     *
     * @param tf The transfer function to write in the file
     * @param jg The Jakson2 generator
     * @throws IOException In case of error writing the TF
     */
    private void putNextTF(TransferFunctionDao tf, JsonGenerator jg) throws IOException {
        Objects.requireNonNull(tf);

    }

    /**
     * Write a template in the JSON file.
     *
     * @param tDao The template to write in the file
     * @param jg The Jakson2 generator
     * @throws IOException In case of error writing the TF
     */
    private void putNextTemplate(TemplateDao tDao, JsonGenerator jg) throws IOException {

    }

    /**
     * Get the next IasioDao from the passed parser, if it exists
     *
     * @param jp The jason parser
     * @return The IasioDao read from the parser if found
     * @throws IOException In case of error getting the next IASIO
     */
    private IasioDao getNextIasio(JsonParser jp) throws IOException {
        return  null;
    }

    /**
     * Get the next TransferFunctionDao from the passed parser, if it exists
     *
     * @param jp The jason parser
     * @return The TransferFunctionDao read from the parser if found
     * @throws IOException In case of error getting the next TF
     */
    private TransferFunctionDao getNextTF(JsonParser jp) throws IOException {
        return null;
    }

    /**
     * Get the next Template from the passed parser, if it exists
     *
     * @param jp The jason parser
     * @return The TemplateDao read from the parser if found
     * @throws IOException In case of error getting the next TF
     */
    private TemplateDao getNextTemplate(JsonParser jp) throws IOException {
        return null;
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
        throw new IasCdbException("Unsupported operation");
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
        throw new IasCdbException("Unsupported operation");
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
        throw new IasCdbException("Unsupported operation");
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

}

