package org.eso.ias.cdb.structuredtext.yaml;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import org.eso.ias.cdb.CdbWriter;
import org.eso.ias.cdb.IasCdbException;
import org.eso.ias.cdb.structuredtext.StructuredTextWriter;
import org.eso.ias.cdb.structuredtext.json.CdbFiles;
import org.eso.ias.cdb.pojos.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

public class YamlWriter extends StructuredTextWriter {

    /**
     * The logger
     */
    private static final Logger logger = LoggerFactory.getLogger(YamlWriter.class);

    /**
     * Constructor
     *
     * @param cdbFileNames CdbFile to get the name of the file to red
     */
    public YamlWriter(CdbFiles cdbFileNames) {
        super(cdbFileNames);
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

