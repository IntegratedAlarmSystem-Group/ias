package org.eso.ias.cdb.structuredtext;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import org.eso.ias.cdb.CdbWriter;
import org.eso.ias.cdb.IasCdbException;
import org.eso.ias.cdb.pojos.*;
import org.eso.ias.cdb.structuredtext.json.CdbFiles;
import org.eso.ias.cdb.structuredtext.json.pojos.JsonSupervisorDao;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;
import java.util.stream.Collectors;

public abstract class StructuredTextWriter implements CdbWriter {

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
     * cdbFileNames return the names of the files to read
     */
    protected final CdbFiles cdbFileNames;

    /**
     * Constructor
     * @param cdbFileNames  The files of the CDB
     */
    public StructuredTextWriter(CdbFiles cdbFileNames) {
        Objects.requireNonNull(cdbFileNames, "cdbFileNames can't be null");
        this.cdbFileNames=cdbFileNames;
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
            f = cdbFileNames.getSuperivisorFilePath(superv.getId()).toFile();
        }catch (IOException ioe) {
            throw new IasCdbException("Error getting Supervisor file",ioe);
        }

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
            f = cdbFileNames.getIasioFilePath("UnusedID").toFile();
        }catch (IOException ioe) {
            throw new IasCdbException("Error getting IASIOs file",ioe);
        }
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
                throw new IasCdbException("Error writing JSON IASIOs",t);
            }
        }
    }

    /**
     * Get the next IasioDao from the passed parser, if it exists
     *
     * @param jp The jackson parser
     * @return The IasioDao read from the parser if found
     * @throws IOException In case of error getting the next IASIO
     */
    private IasioDao getNextIasio(JsonParser jp) throws IOException {
        String iasioId=null;
        String iasioDesc=null;
        String iasioType=null;
        String iasioUrl=null;
        String templateId=null;
        String emails = null;
        String soundType=null;
        while(jp.nextToken() != JsonToken.END_OBJECT){
            String name = jp.getCurrentName();
            if ("id".equals(name)) {
                jp.nextToken();
                iasioId=jp.getText();
            }
            if ("shortDesc".equals(name)) {
                jp.nextToken();
                iasioDesc=jp.getText();
            }
            if ("iasType".equals(name)) {
                jp.nextToken();
                iasioType=jp.getText();
            }
            if ("docUrl".equals(name)) {
                jp.nextToken();
                iasioUrl=jp.getText();
            }
            if ("templateId".equals(name)) {
                jp.nextToken();
                templateId=jp.getText();
            }
            if ("emails".equals(name)) {
                jp.nextToken();
                String temp = jp.getText();
                if (temp!=null) emails=temp;
            }
            if ("sound".equals(name)) {
                jp.nextToken();
                soundType=jp.getText();
            }
        }

        IasioDao ret = new IasioDao(iasioId,iasioDesc, IasTypeDao.valueOf(iasioType),iasioUrl);
        if (templateId!=null && !templateId.trim().isEmpty()) {
            ret.setTemplateId(templateId);
        }
        if (emails!=null && !emails.trim().isEmpty()) {
            ret.setEmails(emails);
        }
        if (soundType!=null && !soundType.trim().isEmpty()) {
            ret.setSound(SoundTypeDao.valueOf(soundType));
        }
        return ret;
    }
}
