package org.eso.ias.cdb.json;

import java.io.IOException;
import java.util.Optional;
import java.util.Set;

import org.eso.ias.cdb.pojos.AsceDao;
import org.eso.ias.cdb.pojos.DasuDao;
import org.eso.ias.cdb.pojos.IasDao;
import org.eso.ias.cdb.pojos.IasioDao;
import org.eso.ias.cdb.pojos.SupervisorDao;

/**
 * Interface to build CDB pojos from files.
 * 
 * @author acaproni
 */
public interface CdbFileReader {
	
	/**
	 * Get the Ias configuration from a file.
	 * 
	 * @return The ias configuration read from the file 
	 */
	Optional<IasDao> getIas() throws IOException;
	
	/**
	 * Get the IASIOs.
	 * 
	 * @return The IASIOs red from the file
	 */
	public Optional<Set<IasioDao>> getIasios() throws IOException;
	
	/**
	 * Get the IASIO with the given ID
	 * 
	 * @param id The ID of the IASIO to read the congiuration
	 * @return The IASIO red from the file
	 */
	public Optional<IasioDao> getIasio(String id) throws IOException;
	
	/**
	 * Read the supervisor configuration from the file. 
	 * 
	 * @param id The not null nor empty supervisor identifier
	 */
	public Optional<SupervisorDao> getSupervisor(String id) throws IOException;
	
	/**
	 * Read the ASCE configuration from the file. 
	 * 
	 * @param id The not null nor empty ASCE identifier
	 */
	public Optional<AsceDao> getAsce(String id) throws IOException;
	
	/**
	 * Read the DASU configuration from the file. 
	 * 
	 * @param id The not null nor empty DASU identifier
	 */
	public Optional<DasuDao> getDasu(String id) throws IOException;

}
