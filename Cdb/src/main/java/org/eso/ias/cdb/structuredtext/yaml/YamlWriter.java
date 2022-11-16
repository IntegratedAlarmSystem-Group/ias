package org.eso.ias.cdb.structuredtext.yaml;

import org.eso.ias.cdb.structuredtext.StructuredTextWriter;
import org.eso.ias.cdb.structuredtext.CdbFiles;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

