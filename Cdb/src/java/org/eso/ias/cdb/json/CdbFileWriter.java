package org.eso.ias.cdb.json;

import java.io.File;
import java.io.IOException;

import org.eso.ias.cdb.pojos.AsceDao;
import org.eso.ias.cdb.pojos.DasuDao;
import org.eso.ias.cdb.pojos.IasDao;
import org.eso.ias.cdb.pojos.IasioDao;
import org.eso.ias.cdb.pojos.SupervisorDao;

/**
 * Interface to flush the content of the CDB pojos
 * to files.
 * 
 * @author acaproni
 */
public interface CdbFileWriter {
	/**
	 * Write the ias in the passed file.
	 * 
	 * @param ias The IAS configuration to write in the file
	 * @param f: the file to write
	 */
	public void writeIas(IasDao ias, File f) throws IOException;
	
	/**
	 * Write the Supervisor in the passed file.
	 * 
	 * @param superv The Supervisor configuration to write in the file
	 * @param f: the file to write
	 */
	public void writeSupervisor(SupervisorDao superv, File f) throws IOException ;
	
	/**
	 * Write the DASU in the passed file.
	 * 
	 * @param dasu The DASU configuration to write in the file
	 * @param f: the file to write
	 */
	public void writeDasu(DasuDao dasu, File f) throws IOException ;
	
	/**
	 * Write the ASCE in the passed file.
	 * 
	 * @param asce The ASCE configuration to write in the file
	 * @param f: the file to write
	 */
	public void writeAsce(AsceDao asce, File f) throws IOException ;
	
	/**
	 * Write the IASIO in the passed file.
	 * 
	 * @param iasio The IASIO configuration to write in the file
	 * @param f: the file to write
	 */
	public void writeIasio(IasioDao iasio, File f) throws IOException ;
}
