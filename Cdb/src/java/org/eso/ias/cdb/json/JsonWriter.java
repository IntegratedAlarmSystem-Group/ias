package org.eso.ias.cdb.json;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.eso.ias.cdb.json.pojos.JsonAcseDao;
import org.eso.ias.cdb.json.pojos.JsonDasuDao;
import org.eso.ias.cdb.json.pojos.JsonSupervisorDao;
import org.eso.ias.cdb.pojos.AsceDao;
import org.eso.ias.cdb.pojos.DasuDao;
import org.eso.ias.cdb.pojos.IasDao;
import org.eso.ias.cdb.pojos.IasTypeDao;
import org.eso.ias.cdb.pojos.IasioDao;
import org.eso.ias.cdb.pojos.SupervisorDao;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Writes IAS structures into JSON files.
 * 
 * <P>JSON writing and parsing is done with Jackson2 
 * (http://wiki.fasterxml.com/JacksonStreamingApi)
 * 
 * @see CdbFileWriter
 * @author acaproni
 */
public class JsonWriter implements CdbFileWriter {
	
	/**
	 * Serialize the ias in the JSON file.
	 * 
	 * @param ias The IAS configuration to write in the file
	 * @param f: the file to write
	 */
	@Override
	public void writeIas(IasDao ias, File f) throws IOException {
		ObjectMapper mapper = new ObjectMapper();
		mapper.setDefaultPrettyPrinter(new DefaultPrettyPrinter());
		mapper.writeValue(f, ias);
	}
	
	/**
	 * Serialize the Supervisor in the JSON file.
	 * 
	 * @param superv The Supervisor configuration to write in the file
	 * @param f: the file to write
	 */
	@Override
	public void writeSupervisor(SupervisorDao superv, File f) throws IOException  {
		JsonSupervisorDao jsonSup = new JsonSupervisorDao(superv);
		ObjectMapper mapper = new ObjectMapper();
		mapper.setDefaultPrettyPrinter(new DefaultPrettyPrinter());
		mapper.writeValue(f, jsonSup);
	}
	
	/**
	 * Serialize the DASU in the JSON file.
	 * 
	 * @param dasu The DASU configuration to write in the file
	 * @param f: the file to write
	 */
	@Override
	public void writeDasu(DasuDao dasu, File f) throws IOException {
		JsonDasuDao jsonDasu = new JsonDasuDao(dasu);
		ObjectMapper mapper = new ObjectMapper();
		mapper.setDefaultPrettyPrinter(new DefaultPrettyPrinter());
		mapper.writeValue(f, jsonDasu);
	}
	
	/**
	 * Serialize the ASCE in the JSON file.
	 * 
	 * @param asce The ASCE configuration to write in the file
	 * @param f: the file to write
	 */
	@Override
	public void writeAsce(AsceDao asce, File f) throws IOException {
		JsonAcseDao jsonAsce = new JsonAcseDao(asce);
		ObjectMapper mapper = new ObjectMapper();
		mapper.setDefaultPrettyPrinter(new DefaultPrettyPrinter());
		mapper.writeValue(f, jsonAsce);
	}
	
	/**
	 * Serialize the IASIO in the JSON file.
	 * 
	 * @param iasio The IASIO configuration to write in the file
	 * @param f: the file to write
	 * @param append: if <code>true</code> the passed iasio is appended to the file
	 *                otherwise a new file is created
	 *                
	 * @see org.eso.ias.cdb.json.CdbFileWriter#writeIasio(org.eso.ias.cdb.pojos.IasioDao, java.io.File, boolean)
	 * @see #writeIasios(Set, File, boolean)
	 */
	@Override
	public void writeIasio(IasioDao iasio, File f, boolean append) throws IOException {
		Set<IasioDao> iasios = new HashSet<>();
		iasios.add(iasio);
		writeIasios(iasios,f,append);
	}

	/**
	 * Serialize the IASIOs in the JSON file.
	 * 
	 * <P>If <code>append</code> is <code>false</code> then a new file is created otherwise
	 * the IASIOs in the passed files are written at the end of the file.
	 * <BR>If a IASIO in <code>iasios</code> already exists in the file, the latter
	 * is replaced by that in the set.
	 * 
	 * @param iasio The IASIOs to write in the file
	 * @param f: the file to write
	 * @param append: if <code>true</code> the passed iasios are appended to the file
	 *                otherwise a new file is created
	 *                
	 * @see org.eso.ias.cdb.json.CdbFileWriter#writeIasios(java.util.Set, java.io.File, boolean)
	 */
	@Override
	public void writeIasios(Set<IasioDao> iasios, File f, boolean append) throws IOException {
		ObjectMapper mapper = new ObjectMapper();
		mapper.setDefaultPrettyPrinter(new DefaultPrettyPrinter());
		if (!f.exists() || !append) {
			mapper.writeValue(f, iasios);
		} else {
			Path path = FileSystems.getDefault().getPath(f.getAbsolutePath());
			Path parent  = path.getParent();
			File tempF = File.createTempFile("iasio", "tmp", parent.toFile());
			
			JsonFactory jsonFactory = new JsonFactory(); 
			JsonParser jp = jsonFactory.createParser(f);
			
			BufferedOutputStream outS = new BufferedOutputStream(new FileOutputStream(tempF));
			JsonGenerator jg = jsonFactory.createGenerator(outS);
			jg.setPrettyPrinter(new DefaultPrettyPrinter());

			// Builds a map of IASIOs to replace existing IASIOs 
			Map<String,IasioDao> iasiosMap = iasios.stream().collect(Collectors.toMap(
					new Function<IasioDao,String>() {
						public String apply(IasioDao i) { return i.getId(); }
					},
					Function.<IasioDao>identity()));
			
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
			// Done... close everything
			jp.close();
			jg.writeEndArray();
			jg.flush();
			jg.close();
			// Remove the original file and rename the temporary file
			Files.delete(path);
			tempF.renameTo(f);
		}
	}
	
	/**
	 * Write a IasioDao in the JSON file.
	 * 
	 * @param iasio The IASIO to write in the file
	 * @param jg The Jakson2 generator
	 */
	private void putNextIasio(IasioDao iasio, JsonGenerator jg) throws IOException {
		Objects.requireNonNull(iasio);
		jg.writeStartObject();
		jg.writeStringField("id",iasio.getId());
		if (iasio.getShortDesc()!=null && !iasio.getShortDesc().isEmpty()) {
			jg.writeStringField("shortDesc",iasio.getShortDesc());
		}
		jg.writeStringField("iasType",iasio.getIasType().toString());
		jg.writeNumberField("refreshRate",Integer.valueOf(iasio.getRefreshRate()));
		jg.writeEndObject();
	}
	
	/**
	 * Get the next IasioDao from the passed parser, if it exists
	 * 
	 * @param jp The jason parser
	 * @return The IasioDao read from the parser if found
	 */
	private IasioDao getNextIasio(JsonParser jp) throws IOException {
		String iasioId=null;
		String iasioDesc=null;
		int iasioRate=-1;
		String iasioType=null;
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
			if ("refreshRate".equals(name)) {
				jp.nextToken();
				iasioRate=jp.getIntValue();
			}
		}
		IasioDao ret = new IasioDao(iasioId,iasioDesc,iasioRate,IasTypeDao.valueOf(iasioType));
		return ret;
	}
}
