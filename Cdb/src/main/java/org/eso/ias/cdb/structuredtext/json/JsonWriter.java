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
		
		ObjectMapper mapper = new ObjectMapper();
		mapper.setDefaultPrettyPrinter(new DefaultPrettyPrinter());
		if (!f.exists() || !append) {
			try {
				mapper.writerWithDefaultPrettyPrinter().writeValue(f, templates);
			}catch (Throwable t) {
				throw new IasCdbException("Error writing JSON TFss",t);
			}
		} else {
			Path path = FileSystems.getDefault().getPath(f.getAbsolutePath());
			Path parent  = path.getParent();
			File tempF;
			BufferedOutputStream outS;
			try {
				if (parent==null) {
					tempF = File.createTempFile("templates", "tmp", null);
				} else {
					tempF = File.createTempFile("templates", "tmp", parent.toFile());
				}
				outS = new BufferedOutputStream(new FileOutputStream(tempF));
			} catch (IOException ioe) {
				throw new IasCdbException("Error creating temporary file",ioe);
			}
			
			JsonFactory jsonFactory = new JsonFactory(); 
			JsonParser jp;
			try {
				jp = jsonFactory.createParser(f);
			} catch (Throwable t) {
				try {
					outS.close();
				} catch (IOException ioe) {}
				throw new IasCdbException("Error creating the JSON parser", t);
			} 
			JsonGenerator jg;
			try { 
				jg = jsonFactory.createGenerator(outS);
			} catch (IOException ioe) {
				try {
					outS.close();
				} catch (IOException nestedIOE) {}
				throw new IasCdbException("Error creating the JSON generator", ioe);
			} 
			
			jg.useDefaultPrettyPrinter();

			// Builds a map of templates to replace existing Templates 
			Map<String,TemplateDao> templatesMap = templates.stream().collect(Collectors.toMap(
					new Function<TemplateDao,String>() {
						public String apply(TemplateDao i) { return i.getId(); }
					},
					Function.<TemplateDao>identity()));
			try {
				
				while(jp.nextToken() != JsonToken.END_ARRAY){
					JsonToken curToken = jp.getCurrentToken();
					if (curToken==JsonToken.START_ARRAY) {
						jg.writeStartArray();
					}
					if (curToken==JsonToken.START_OBJECT) {
						TemplateDao templateInFile = getNextTemplate(jp);
						if (templatesMap.containsKey(templateInFile.getId())) {
							// The template in the set replaces the one in the file
							putNextTemplate(templatesMap.get(templateInFile.getId()),jg);
							templatesMap.remove(templateInFile.getId());
						} else {
							putNextTemplate(templateInFile,jg);
						}
					}
				}
				// Flushes the remaining TFs from the set into the file
				for (String key: templatesMap.keySet()) {
					putNextTemplate(templatesMap.get(key),jg);
				}
				
			} catch (IOException ioe) {
				throw new IasCdbException("I/O Error processing JSON files",ioe);
			} finally {
				// Done... close everything
				try {
					jp.close();
					jg.writeEndArray();
					jg.flush();
					jg.close();
				} catch (IOException ioe) {
					throw new IasCdbException("I/O Error closing JSON parser and generator",ioe);
				}
			}
			
			
			// Remove the original file and rename the temporary file
			try {
				Files.delete(path);
			} catch (IOException ioe) {
				throw new IasCdbException("Error deleting temporary file "+path,ioe);
			}
			tempF.renameTo(f);
		}
	}
	

	

	
	/**
	 * Write a template in the JSON file.
	 * 
	 * @param tDao The template to write in the file
	 * @param jg The Jakson2 generator
	 * @throws IOException In case of error writing the TF
	 */
	private void putNextTemplate(TemplateDao tDao, JsonGenerator jg) throws IOException {
		Objects.requireNonNull(tDao);
        jg.useDefaultPrettyPrinter();
		jg.writeStartObject();
		jg.writeStringField("id",tDao.getId());
		jg.writeStringField("min",Integer.valueOf(tDao.getMin()).toString());
		jg.writeStringField("max",Integer.valueOf(tDao.getMax()).toString());
		jg.writeEndObject();
	}

	/**
	 * Get the next Template from the passed parser, if it exists
	 * 
	 * @param jp The jason parser
	 * @return The TemplateDao read from the parser if found
	 * @throws IOException In case of error getting the next TF
	 */
	private TemplateDao getNextTemplate(JsonParser jp) throws IOException {
		String id=null;
		String min=null;
		String max=null;
		while(jp.nextToken() != JsonToken.END_OBJECT){
			String name = jp.getCurrentName();
			if ("id".equals(name)) {
				jp.nextToken();
				id=jp.getText();
			}
			if ("min".equals(name)) {
				jp.nextToken();
				min=jp.getText();
			}
			if ("max".equals(name)) {
				jp.nextToken();
				max=jp.getText();
			}
		}
		TemplateDao ret = new TemplateDao(id,Integer.parseInt(min),Integer.parseInt(max));
		return ret;
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
		Set<TemplateDao> templates = new HashSet<>();
		templates.add(templateDao);
		writeTemplates(templates, true);
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
