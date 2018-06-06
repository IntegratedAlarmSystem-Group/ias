package org.eso.ias.cdb;

import java.util.Collection;
import java.util.Optional;
import java.util.Set;

import org.eso.ias.cdb.pojos.AsceDao;
import org.eso.ias.cdb.pojos.DasuDao;
import org.eso.ias.cdb.pojos.DasuToDeployDao;
import org.eso.ias.cdb.pojos.IasDao;
import org.eso.ias.cdb.pojos.IasioDao;
import org.eso.ias.cdb.pojos.SupervisorDao;
import org.eso.ias.cdb.pojos.TemplateDao;
import org.eso.ias.cdb.pojos.TransferFunctionDao;

/**
 * Interface to build CDB pojos from the configuration database.
 * 
 * @author acaproni
 */
public interface CdbReader {
	
	/**
	 * Get the Ias configuration from a CDB.
	 * 
	 * @return The ias configuration read from the CDB 
	 * @throws IasCdbException In case of error getting the IAS
	 */
	Optional<IasDao> getIas() throws IasCdbException;
	
	/**
	 * Get the IASIOs.
	 * 
	 * @return The IASIOs red from the CDB
	 * @throws IasCdbException In case of error getting the IASIOs
	 */
	public Optional<Set<IasioDao>> getIasios() throws IasCdbException;
	
	/**
	 * Get the IASIO with the given ID
	 * 
	 * @param id The ID of the IASIO to read the configuration
	 * @return The IASIO red from the CDB
	 * @throws IasCdbException In case of error getting the IASIO
	 */
	public Optional<IasioDao> getIasio(String id) throws IasCdbException;
	
	/**
	 * Read the supervisor configuration from the CDB. 
	 * 
	 * @param id The not null nor empty supervisor identifier
	 * @return The Supervisor red from the CDB
	 * @throws IasCdbException In case of error getting the Supervisor
	 */
	public Optional<SupervisorDao> getSupervisor(String id) throws IasCdbException;
	
	/**
	 * Read the transfer function configuration from the CDB. 
	 * 
	 * @param tf_id The not <code>null</code> nor empty transfer function identifier
	 * @return The transfer function read from the CDB
	 * @throws IasCdbException in case of error reading from the CDB
	 */
	public Optional<TransferFunctionDao> getTransferFunction(String tf_id) throws IasCdbException;
	
	/**
	 * Read the ttemplate configuration from the CDB. 
	 * 
	 * @param template_id The not <code>null</code> nor empty identifier of the template
	 * @return The template read from the CDB
	 * @throws IasCdbException in case of error reading from the CDB
	 */
	public Optional<TemplateDao> getTemplate(String template_id) throws IasCdbException;
	
	/**
	 * Read the ASCE configuration from the CDB. 
	 * 
	 * @param id The not null nor empty ASCE identifier
	 * @return The ASCE red from the file
	 * @throws IasCdbException In case of error getting the ASCE
	 */
	public Optional<AsceDao> getAsce(String id) throws IasCdbException;
	
	/**
	 * Read the DASU configuration from the CDB. 
	 * 
	 * @param id The not null nor empty DASU identifier
	 * @return The DASU red from the file
	 * @throws IasCdbException In case of error getting the DASU
	 */
	public Optional<DasuDao> getDasu(String id) throws IasCdbException;
	
	/**
	 * Return the DASUs to deploy in the Supervisor with the given identifier
	 * 
	 * @param id The not <code>null</code> nor empty identifier of the supervisor
	 * @return A set of DASUs to deploy in the supervisor with the passed id
	 * @throws IasCdbException in case of error reading CDB or if the 
	 *                         supervisor with the give identifier does not exist
	 */
	public Set<DasuToDeployDao> getDasusToDeployInSupervisor(String id) throws IasCdbException;
	

	
	/**
	 * Return the ASCEs belonging to the given DASU.
	 * 
	 * @param id The not <code>null</code> nor empty identifier of the DASU
	 * @return A set of ASCEs running in the DASU with the passed id
	 * @throws IasCdbException in case of error reading CDB or if the 
	 *                         DASU with the give identifier does not exist
	 */
	public Set<AsceDao> getAscesForDasu(String id) throws IasCdbException;
	
	/**
	 * Return the IASIOs in input to the given ASCE.
	 * 
	 * @param id The not <code>null</code> nor empty identifier of the ASCE
	 * @return A set of IASIOs running in the ASCE with the passed id
	 * @throws IasCdbException in case of error reading CDB or if the 
	 *                         ASCE with the give identifier does not exist
	 */
	public Collection<IasioDao> getIasiosForAsce(String id) throws IasCdbException;
	
	/**
	 * Initialize the CDB
	 */
	public void init() throws IasCdbException;
	
	/**
	 * Close the CDB and release the associated resources
	 * @throws IasCdbException
	 */
	public void shutdown() throws IasCdbException;

}
