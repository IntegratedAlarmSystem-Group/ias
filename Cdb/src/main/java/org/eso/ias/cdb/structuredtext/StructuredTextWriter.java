package org.eso.ias.cdb.structuredtext;

import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import org.eso.ias.cdb.CdbWriter;
import org.eso.ias.cdb.IasCdbException;
import org.eso.ias.cdb.pojos.IasDao;
import org.eso.ias.cdb.structuredtext.json.CdbFiles;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

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
}
