package org.eso.ias.cdb.json;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.eso.ias.cdb.CdbWriter;
import org.eso.ias.cdb.IasCdbException;
import org.eso.ias.cdb.json.pojos.JsonAsceDao;
import org.eso.ias.cdb.json.pojos.JsonDasuDao;
import org.eso.ias.cdb.json.pojos.JsonSupervisorDao;
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
public class JsonWriter implements CdbWriter {

	/**
     * The logger
     */
    private static final Logger logger = LoggerFactory.getLogger(JsonWriter.class);

	/**
	 * Signal if the reader has been initialized
	 */
	private final AtomicBoolean initialized = new AtomicBoolean(false);

	/**
	 * Signal if the reader has been closed
	 */
	private final AtomicBoolean closed = new AtomicBoolean(false);
	
	/**
	 * cdbFileNames return the names of the files to read
	 */
	private final CdbFiles cdbFileNames;
	
	/**
	 * Constructor
	 * 
	 * @param cdbFileNames CdbFile to get the name of the file to red
	 */
	public JsonWriter(CdbFiles cdbFileNames) {
		Objects.requireNonNull(cdbFileNames, "cdbFileNames can't be null");
		this.cdbFileNames=cdbFileNames;
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
		ObjectMapper mapper = new ObjectMapper();
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
		ObjectMapper mapper = new ObjectMapper();
		mapper.setDefaultPrettyPrinter(new DefaultPrettyPrinter());
		try {
			mapper.writerWithDefaultPrettyPrinter().writeValue(f, jsonSup);
		}catch (Throwable t) {
			throw new IasCdbException("Error writing JSON Supervisor",t);
		}
	}
	
	/**
	 * Serialize the DASU in the JSON file.
	 * 
	 * @param dasu The DASU configuration to write in the file
	 */
	@Override
	public void writeDasu(DasuDao dasu) throws IasCdbException {
		if (closed.get()) {
			throw new IasCdbException("The writer is shut down");
		}
		if (!initialized.get()) {
			throw new IasCdbException("The writer is not initialized");
		}

		Objects.requireNonNull(dasu);
		File f;
		try { 
			f = cdbFileNames.getDasuFilePath(dasu.getId()).toFile();
		}catch (IOException ioe) {
			throw new IasCdbException("Error getting DASU file",ioe);
		}
		JsonDasuDao jsonDasu = new JsonDasuDao(dasu);
		ObjectMapper mapper = new ObjectMapper();
		mapper.setDefaultPrettyPrinter(new DefaultPrettyPrinter());
		try {
			mapper.writerWithDefaultPrettyPrinter().writeValue(f, jsonDasu);
		}catch (Throwable t) {
			throw new IasCdbException("Error writing JSON DASU",t);
		}
	}
	
	/**
	 * Serialize the ASCE in the JSON file.
	 * 
	 * @param asce The ASCE configuration to write in the file
	 */
	@Override
	public void writeAsce(AsceDao asce) throws IasCdbException {
		if (closed.get()) {
			throw new IasCdbException("The writer is shut down");
		}
		if (!initialized.get()) {
			throw new IasCdbException("The writer is not initialized");
		}
		Objects.requireNonNull(asce);

		File f;
		try {
			f = cdbFileNames.getAsceFilePath(asce.getId()).toFile();
		}catch (IOException ioe) {
			throw new IasCdbException("Error getting ASCE file",ioe);
		}
		JsonAsceDao jsonAsce = new JsonAsceDao(asce);
		ObjectMapper mapper = new ObjectMapper();
		mapper.setDefaultPrettyPrinter(new DefaultPrettyPrinter());
		try {
			mapper.writerWithDefaultPrettyPrinter().writeValue(f, jsonAsce);
		}catch (Throwable t) {
			throw new IasCdbException("Error writing JSON ASCE",t);
		}
	}
	
	/**
	 * Serialize the IASIO in the JSON file.
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
		ObjectMapper mapper = new ObjectMapper();
		mapper.setDefaultPrettyPrinter(new DefaultPrettyPrinter());
		if (!f.exists() || !append) {
			try {
				mapper.writerWithDefaultPrettyPrinter().writeValue(f, iasios);
			}catch (Throwable t) {
				throw new IasCdbException("Error writing JSON IASIOs",t);
			}
		} else {
			Path path = FileSystems.getDefault().getPath(f.getAbsolutePath());
			Path parent  = path.getParent();
			File tempF;
			BufferedOutputStream outS;
			try {
				if (parent==null) {
					tempF = File.createTempFile("iasio", "tmp", null);
				} else {
					tempF = File.createTempFile("iasio", "tmp", parent.toFile());
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

			// Builds a map of IASIOs to replace existing IASIOs 
			Map<String,IasioDao> iasiosMap = iasios.stream().collect(Collectors.toMap(
					new Function<IasioDao,String>() {
						public String apply(IasioDao i) { return i.getId(); }
					},
					Function.<IasioDao>identity()));
			try {
				
				while(jp.nextToken() != JsonToken.END_ARRAY){
					JsonToken curToken = jp.getCurrentToken();
					if (curToken==JsonToken.START_ARRAY) {
						jg.writeStartArray();
					}
					if (curToken==JsonToken.START_OBJECT) {
						IasioDao iasioinFile = getNextIasio(jp);

						if (iasiosMap.containsKey(iasioinFile.getId())) {
							// The IASIO in the set replaces the one in the file
							putNextIasio(iasiosMap.get(iasioinFile.getId()),jg);
							iasiosMap.remove(iasioinFile.getId());
						} else {
							putNextIasio(iasioinFile,jg);
						}
					}
				}
				// Flushes the remaining IASIOs from the set into the file
				for (String key: iasiosMap.keySet()) {
					putNextIasio(iasiosMap.get(key),jg);
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
	 * Serialize the TFs in the JSON file.
	 * 
	 * <P>If <code>append</code> is <code>false</code> then a new file is created otherwise
	 * the TFs in the passed files are written at the end of the file.
	 * <BR>If a TF in <code>tfs</code> already exists in the file, the latter
	 * is replaced by that in the set.
	 * 
	 * @param tfs The TFs to write in the file
	 * @param append: if <code>true</code> the passed TFs are appended to the file
	 *                otherwise a new file is created
	 */
	private void writeTransferFunctions(Set<TransferFunctionDao> tfs, boolean append) throws IasCdbException {
		File f;
		try { 
			// The ID is unused in getTFFilePath
			f = cdbFileNames.getTFFilePath("UnusedID").toFile();
		}catch (IOException ioe) {
			throw new IasCdbException("Error getting TFs file",ioe);
		}
		
		ObjectMapper mapper = new ObjectMapper();
		mapper.setDefaultPrettyPrinter(new DefaultPrettyPrinter());
		if (!f.exists() || !append) {
			try {
				mapper.writerWithDefaultPrettyPrinter().writeValue(f, tfs);
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
					tempF = File.createTempFile("tfs", "tmp", null);
				} else {
					tempF = File.createTempFile("tfs", "tmp", parent.toFile());
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

			// Builds a map of TFs to replace existing TFs 
			Map<String,TransferFunctionDao> tfsMap = tfs.stream().collect(Collectors.toMap(
					new Function<TransferFunctionDao,String>() {
						public String apply(TransferFunctionDao i) { return i.getClassName(); }
					},
					Function.<TransferFunctionDao>identity()));
			try {
				
				while(jp.nextToken() != JsonToken.END_ARRAY){
					JsonToken curToken = jp.getCurrentToken();
					if (curToken==JsonToken.START_ARRAY) {
						jg.writeStartArray();
					}
					if (curToken==JsonToken.START_OBJECT) {
						TransferFunctionDao tfInFile = getNextTF(jp);
						if (tfsMap.containsKey(tfInFile.getClassName())) {
							// The TF in the set replaces the one in the file
							putNextTF(tfsMap.get(tfInFile.getClassName()),jg);
							tfsMap.remove(tfInFile.getClassName());
						} else {
							putNextTF(tfInFile,jg);
						}
					}
				}
				// Flushes the remaining TFs from the set into the file
				for (String key: tfsMap.keySet()) {
					putNextTF(tfsMap.get(key),jg);
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
	 * Write a IasioDao in the JSON file.
	 * 
	 * @param iasio The IASIO to write in the file
	 * @param jg The Jakson2 generator
	 * @throws IOException In case of error writing the IASIO
	 */
	private void putNextIasio(IasioDao iasio, JsonGenerator jg) throws IOException {
		Objects.requireNonNull(iasio);
		jg.useDefaultPrettyPrinter();
		jg.writeStartObject();
		jg.writeStringField("id",iasio.getId());
		if (iasio.getShortDesc()!=null && !iasio.getShortDesc().trim().isEmpty()) {
			jg.writeStringField("shortDesc",iasio.getShortDesc());
		}
		jg.writeStringField("iasType",iasio.getIasType().toString());
		if (iasio.getDocUrl()!=null && !iasio.getDocUrl().trim().isEmpty()) {
			jg.writeStringField("docUrl",iasio.getDocUrl());
		}
		if (iasio.getTemplateId()!=null && !iasio.getTemplateId().trim().isEmpty()) {
			jg.writeStringField("templateId",iasio.getTemplateId());
		}
		if (iasio.getEmails()!=null && !iasio.getEmails().trim().isEmpty()) {
			jg.writeStringField("emails",iasio.getEmails());
		}
		if (iasio.getSound()!=null) {
			jg.writeStringField("sound",iasio.getSound().toString());
		}
		if (iasio.isCanShelve()!=iasio.canSheveDefault) {
			jg.writeStringField("canShelve", Boolean.valueOf(iasio.isCanShelve()).toString());
		}
		jg.writeEndObject();
	}
	
	/**
	 * Write a TransferFunctionDao in the JSON file.
	 * 
	 * @param tf The transfer function to write in the file
	 * @param jg The Jakson2 generator
	 * @throws IOException In case of error writing the TF
	 */
	private void putNextTF(TransferFunctionDao tf, JsonGenerator jg) throws IOException {
		Objects.requireNonNull(tf);
        jg.useDefaultPrettyPrinter();
		jg.writeStartObject();
		jg.writeStringField("className",tf.getClassName());
		jg.writeStringField("implLang",tf.getImplLang().toString());
		jg.writeEndObject();
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
	 * Get the next IasioDao from the passed parser, if it exists
	 * 
	 * @param jp The jason parser
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

		IasioDao ret = new IasioDao(iasioId,iasioDesc,IasTypeDao.valueOf(iasioType),iasioUrl);
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
	
	/**
	 * Get the next TransferFunctionDao from the passed parser, if it exists
	 * 
	 * @param jp The jason parser
	 * @return The TransferFunctionDao read from the parser if found
	 * @throws IOException In case of error getting the next TF
	 */
	private TransferFunctionDao getNextTF(JsonParser jp) throws IOException {
		String tfClassName=null;
		String tfImplLang=null;
		while(jp.nextToken() != JsonToken.END_OBJECT){
			String name = jp.getCurrentName();
			if ("className".equals(name)) {
				jp.nextToken();
				tfClassName=jp.getText();
			}
			if ("implLang".equals(name)) {
				jp.nextToken();
				tfImplLang=jp.getText();
			}
		}
		TransferFunctionDao ret = new TransferFunctionDao(tfClassName,TFLanguageDao.valueOf(tfImplLang));
		return ret;
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
	 *  Write the transfer function to the CDB
	 *  
	 *  @param transferFunction The TF configuration to write in the file
	 *  @throws IasCdbException In case of error writing the TF
	 */
	@Override
	public void writeTransferFunction(TransferFunctionDao transferFunction) throws IasCdbException {
		if (closed.get()) {
			throw new IasCdbException("The writer is shut down");
		}
		if (!initialized.get()) {
			throw new IasCdbException("The writer is not initialized");
		}
		Objects.requireNonNull(transferFunction);
		Set<TransferFunctionDao> tfs = new HashSet<>();
		tfs.add(transferFunction);
		writeTransferFunctions(tfs, true);
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
