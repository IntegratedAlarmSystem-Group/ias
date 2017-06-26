package org.eso.ias.cdb;

import java.util.Optional;
import java.util.Set;

import org.eso.ias.cdb.pojos.AsceDao;
import org.eso.ias.cdb.pojos.DasuDao;
import org.eso.ias.cdb.pojos.IasDao;
import org.eso.ias.cdb.pojos.IasioDao;
import org.eso.ias.cdb.pojos.SupervisorDao;

/**
 * Interface to build CDB pojos from the configuration database.
 * 
 * @author acaproni
 */
public interface CdbReader {
	
	/**
	 * Get the Ias configuration from a file.
	 * 
	 * @return The ias configuration read from the file 
	 * @throws IasCdbException In case of error getting the IAS
	 */
	Optional<IasDao> getIas() throws IasCdbException;
	
	/**
	 * Get the IASIOs.
	 * 
	 * @return The IASIOs red from the file
	 * @throws IasCdbException In case of error getting the IASIOs
	 */
	public Optional<Set<IasioDao>> getIasios() throws IasCdbException;
	
	/**
	 * Get the IASIO with the given ID
	 * 
	 * @param id The ID of the IASIO to read the congiuration
	 * @return The IASIO red from the file
	 * @throws IasCdbException In case of error getting the IASIO
	 */
	public Optional<IasioDao> getIasio(String id) throws IasCdbException;
	
	/**
	 * Read the supervisor configuration from the file. 
	 * 
	 * @param id The not null nor empty supervisor identifier
	 * @return The Supervisor red from the file
	 * @throws IasCdbException In case of error getting the Supervisor
	 */
	public Optional<SupervisorDao> getSupervisor(String id) throws IasCdbException;
	
	/**
	 * Read the ASCE configuration from the file. 
	 * 
	 * @param id The not null nor empty ASCE identifier
	 * @return The ASCE red from the file
	 * @throws IasCdbException In case of error getting the ASCE
	 */
	public Optional<AsceDao> getAsce(String id) throws IasCdbException;
	
	/**
	 * Read the DASU configuration from the file. 
	 * 
	 * @param id The not null nor empty DASU identifier
	 * @return The DASU red from the file
	 * @throws IasCdbException In case of error getting the DASU
	 */
	public Optional<DasuDao> getDasu(String id) throws IasCdbException;

}
