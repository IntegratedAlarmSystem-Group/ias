package org.eso.ias.cdb.json;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

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
		ObjectMapper mapper = new ObjectMapper();
		mapper.writeValue(f, superv);
	}
	
	/**
	 * Serialize the DASU in the JSON file.
	 * 
	 * @param dasu The DASU configuration to write in the file
	 * @param f: the file to write
	 */
	@Override
	public void writeDasu(DasuDao dasu, File f) throws IOException {
		ObjectMapper mapper = new ObjectMapper();
		mapper.writeValue(f, dasu);
	}
	
	/**
	 * Serialize the ASCE in the JSON file.
	 * 
	 * @param asce The ASCE configuration to write in the file
	 * @param f: the file to write
	 */
	@Override
	public void writeAsce(AsceDao asce, File f) throws IOException {
		ObjectMapper mapper = new ObjectMapper();
		mapper.writeValue(f, asce);
	}
	
	/**
	 * Serialize the IASIO in the JSON file.
	 * 
	 * @param iasio The IASIO configuration to write in the file
	 * @param f: the file to write
	 */
	@Override
	public void writeIasio(IasioDao iasio, File f) throws IOException {
		ObjectMapper mapper = new ObjectMapper();
		mapper.writeValue(f, iasio);
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
}
