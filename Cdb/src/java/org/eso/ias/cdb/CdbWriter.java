package org.eso.ias.cdb;

import java.util.Set;

import org.eso.ias.cdb.pojos.AsceDao;
import org.eso.ias.cdb.pojos.DasuDao;
import org.eso.ias.cdb.pojos.IasDao;
import org.eso.ias.cdb.pojos.IasioDao;
import org.eso.ias.cdb.pojos.SupervisorDao;

/**
 * Interface to flush the content of the CDB pojos
 * in the configuration database
 * 
 * @author acaproni
 */
public interface CdbWriter {
	/**
	 * Write the ias in the passed file.
	 * 
	 * @param ias The IAS configuration to write in the file
	 * @throws IasCdbException In case of error writing the IAS
	 */
	public void writeIas(IasDao ias) throws IasCdbException;
	
	/**
	 * Write the Supervisor in the passed file.
	 * 
	 * @param superv The Supervisor configuration to write in the file
	 * @throws IasCdbException In case of error writing the Supervisor
	 */
	public void writeSupervisor(SupervisorDao superv) throws IasCdbException;
	
	/**
	 * Write the DASU in the passed file.
	 * 
	 * @param dasu The DASU configuration to write in the file
	 * @throws IasCdbException In case of error writing the DASU
	 */
	public void writeDasu(DasuDao dasu) throws IasCdbException;
	
	/**
	 * Write the ASCE in the passed file.
	 * 
	 * @param asce The ASCE configuration to write in the file
	 * @throws IasCdbException In case of error writing the ASCE
	 */
	public void writeAsce(AsceDao asce) throws IasCdbException;
	
	/**
	 * Write the IASIO in the file.
	 * 
	 * @param iasio The IASIO configuration to write in the file
	 * @param append: if <code>true</code> the passed iasio is appended to the file
	 *                otherwise a new file is created
	 * @throws IasCdbException In case of error writing the IASIO
	 */
	public void writeIasio(IasioDao iasio, boolean append) throws IasCdbException;
	
	/**
	 * Write the IASIOs in the file.
	 * 
	 * @param iasios The IASIOs to write in the file
	 * @param append: if <code>true</code> the passed iasios are appended to the file
	 *                otherwise a new file is created
	 * @throws IasCdbException In case of error writing the IASIOs
	 */
	public void writeIasios(Set<IasioDao> iasios, boolean append) throws IasCdbException;
}
