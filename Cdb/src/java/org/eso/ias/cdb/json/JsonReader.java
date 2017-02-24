package org.eso.ias.cdb.json;

import java.io.File;
import java.util.Optional;
import java.util.Set;

import org.eso.ias.cdb.pojos.IasDao;
import org.eso.ias.cdb.pojos.IasioDao;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Read CDB configuration from JSON files
 * 
 * <P>JSON writing and parsing is done with Jackson2 
 * (http://wiki.fasterxml.com/JacksonStreamingApi)
 * 
 * @see CdbFileReader
 * @author acaproni
 */
public class JsonReader implements CdbFileReader {

	/**
	 * Get the Ias configuration from JSON file.
	 * 
	 * @param f: The JSON file to read IAS configuration from
	 * @return The ias configuration read from the JSON file 
	 * @see CdbFileReader#getIas(File)
	 */
	public Optional<IasDao> getIas(File f) {
		if (!canReadFromFile(f)) {
			return Optional.empty();
		} else {
			// Parse the file in a JSON pojo
			ObjectMapper mapper = new ObjectMapper();
			try {
				IasDao ias = mapper.readValue(f, IasDao.class);
				return Optional.of(ias);
			} catch (Throwable t) {
				System.out.println("Error reading IAS from "+f.getAbsolutePath()+ ": "+t.getMessage());
				t.printStackTrace();
				return Optional.empty();
			}
		}
	}
	
	/**
	 * Get the IASIOs from the passed file.
	 * 
	 * @param f: The file to read IAS configuration from
	 * @return The IASIOs read from the file
	 * @see CdbFileReader#getIasios(File)
	 */
	public Optional<Set<IasioDao>> getIasios(File f) {
		if (!canReadFromFile(f)) {
			return Optional.empty();
		} else {
			// Parse the file in a JSON pojo
			ObjectMapper mapper = new ObjectMapper();
			try {
				Set<IasioDao> iasios = mapper.readValue(f, new TypeReference<Set<IasioDao>>(){});
				return Optional.of(iasios);
			} catch (Throwable t) {
				System.out.println("Error reading IAS from "+f.getAbsolutePath()+ ": "+t.getMessage());
				t.printStackTrace();
				return Optional.empty();
			}
		}
	}
	
	/**
	 * Check if the passed file is readable
	 *  
	 * @param inF The file to check
	 * @return true if the file exists and is readable
	 */
	private boolean canReadFromFile(File inF) {
		return inF.exists() && inF.isFile() && inF.canRead();
	}
}
