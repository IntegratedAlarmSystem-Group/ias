package org.eso.ias.cdb.structuredtext.json;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.eso.ias.cdb.CdbReader;
import org.eso.ias.cdb.IasCdbException;
import org.eso.ias.cdb.structuredtext.StructuredTextReader;
import org.eso.ias.cdb.structuredtext.json.pojos.*;
import org.eso.ias.cdb.pojos.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

/**
 * Read CDB configuration from JSON files.
 * 
 * As {@link JsonWriter} replaces objects included into other objects 
 * (like ACSEs into a DASU), the reader must reconstruct 
 * the entire tree of objects inclusion before returning the
 * DAO pojos to the caller.
 * 
 * <P>JSON writing and parsing is done with Jackson2 
 * (http://wiki.fasterxml.com/JacksonStreamingApi)
 * 
 * @see CdbReader
 * @author acaproni
 */
public class JsonReader extends StructuredTextReader {

	 /**
     * The logger
     */
    private static final Logger logger = LoggerFactory.getLogger(JsonReader.class);

	/**
	 * Constructor
	 * 
	 * @param cdbFileNames CdbFile to get the name of the file to red
	 */
	public JsonReader(CdbFiles cdbFileNames) {
		super(cdbFileNames);
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
	public Collection<TemplateInstanceIasioDao> getTemplateInstancesIasiosForAsce(String id)
            throws IasCdbException {
		if (closed.get()) {
			throw new IasCdbException("The reader is shut down");
		}
		if (!initialized.get()) {
			throw new IasCdbException("The reader is not initialized");
		}

        if (id ==null || id.isEmpty()) {
            throw new IllegalArgumentException("Invalid null or empty ID");
        }
        Optional<AsceDao> asce = getAsce(id);
        Collection<TemplateInstanceIasioDao> ret = asce.orElseThrow(() -> new IasCdbException("ASCE ["+id+"] not dound")).getTemplatedInstanceInputs();
        return (ret==null)? new ArrayList<>() : ret;
    }
















}
