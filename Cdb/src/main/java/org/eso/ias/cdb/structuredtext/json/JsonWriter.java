package org.eso.ias.cdb.structuredtext.json;

import org.eso.ias.cdb.CdbWriter;
import org.eso.ias.cdb.structuredtext.CdbFiles;
import org.eso.ias.cdb.structuredtext.StructuredTextWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Writes IAS structures into JSON files.
 * 
 * To avoid recursion, objects included into pojos (for example ASCEs in a DASU)
 * are represented by their IDs only. This is done, in practice,
 * writing a different set of pojos (i.e. those in the org.eso.ias.cdb.pojos)
 * instead of those in the org.eso.ias.pojos. 
 * 
 * <P>JSON writing and parsing is done with Jackson2 
 * (http://wiki.fasterxml.com/JacksonStreamingApi)
 * 
 * @see CdbWriter
 * @author acaproni
 */
public class JsonWriter extends StructuredTextWriter {

	/**
     * The logger
     */
    private static final Logger logger = LoggerFactory.getLogger(JsonWriter.class);

	/**
	 * Constructor
	 * 
	 * @param cdbFileNames CdbFile to get the name of the file to red
	 */
	public JsonWriter(CdbFiles cdbFileNames) {
		super(cdbFileNames);
	}

}
