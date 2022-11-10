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



}

