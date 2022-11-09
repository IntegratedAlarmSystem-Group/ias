package org.eso.ias.cdb.structuredtext.json;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.eso.ias.cdb.CdbWriter;
import org.eso.ias.cdb.IasCdbException;
import org.eso.ias.cdb.structuredtext.StructuredTextWriter;
import org.eso.ias.cdb.structuredtext.json.pojos.JsonAsceDao;
import org.eso.ias.cdb.structuredtext.json.pojos.JsonDasuDao;
import org.eso.ias.cdb.structuredtext.json.pojos.JsonSupervisorDao;
import org.eso.ias.cdb.pojos.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;
import java.util.stream.Collectors;

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
			f = cdbFileNames.getPluginFilePath(pluginConfigDao.getId()).toFile();
		} catch (IOException ioe) {
			throw new IasCdbException("Error getting plugin file "+pluginConfigDao.getId(),ioe);
		}

		ObjectMapper mapper = new ObjectMapper();
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
			f = cdbFileNames.getClientFilePath(clientConfigDao.getId());
		} catch (IOException ioe) {
			throw new IasCdbException("Error getting client file "+clientConfigDao.getId(),ioe);
		}

		List<String> strings = new ArrayList();
		strings.add(clientConfigDao.getConfig());
		try {
			Files.write(f,strings, StandardOpenOption.WRITE, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
		} catch (IOException ioe) {
			throw new IasCdbException("Error reading client file "+f.toString(),ioe);
		}
	}
}
