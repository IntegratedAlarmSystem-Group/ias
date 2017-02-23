package org.eso.ias.cdb.json;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import org.eso.ias.cdb.json.pojos.JsonAcseDao;
import org.eso.ias.cdb.json.pojos.JsonDasuDao;
import org.eso.ias.cdb.json.pojos.JsonSupervisorDao;
import org.eso.ias.cdb.pojos.AsceDao;
import org.eso.ias.cdb.pojos.DasuDao;
import org.eso.ias.cdb.pojos.IasDao;
import org.eso.ias.cdb.pojos.IasioDao;
import org.eso.ias.cdb.pojos.SupervisorDao;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Writes IAS structures into JSON files.
 * 
 * <P>JSON writing and parsing is done with the streaming feature 
 * of Jackson2 (http://wiki.fasterxml.com/JacksonStreamingApi)
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
		mapper.writeValue(f, jsonAsce);
	}
	
	/**
	 * Create/open the file to write.
	 *  
	 * @param f The file to write IAS data structures into.
	 * @return The file to write
	 * @throws IOException In case of error creating the output file
	 */
	private BufferedOutputStream creteFileForOutput(File f) throws IOException {
		// Tons of controls...
		if (f==null) {
			throw new NullPointerException("The file to write into can't be null");
		}
		if (f.exists()) {
			throw new IOException(f.getAbsolutePath()+" already exists");
		}
		if (f.isDirectory()) {
			throw new IOException(f.getAbsolutePath()+" cannot be a directory");
		}
		if (!f.canWrite()) {
			throw new IOException("Cannot write "+f.getAbsolutePath()+": check permissions");
		}
		FileOutputStream fOutStrem = new FileOutputStream(f, false);
		return new BufferedOutputStream(fOutStrem);
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
		mapper.writeValue(f, iasios);
	}
}
