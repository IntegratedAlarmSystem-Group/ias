package org.eso.ias.cdb.structuredtext;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import org.eso.ias.cdb.CdbReader;
import org.eso.ias.cdb.IasCdbException;
import org.eso.ias.cdb.TextFileType;
import org.eso.ias.cdb.pojos.IasDao;
import org.eso.ias.cdb.structuredtext.json.CdbFiles;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.Objects;
import java.util.Optional;
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

}
