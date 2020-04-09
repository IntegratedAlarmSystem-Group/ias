package org.eso.ias.cdb;

import org.eso.ias.cdb.pojos.*;

import java.util.Set;

/**
 * Interface to flush the content of the CDB pojos
 * in the configuration database
 *
 * Life cycle:
 * <UL>
 *     <LI>{@link #init()} must be called to acquire the resources before invoking methods of this ineterface</LI>
 *     <LI>{@link #shutdown()} must be invoked to free the allocated resource when done using this interface</LI>
 * </UL>
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
	 *  Write the transfer function to the CDB
	 *  
	 *  @param transferFunction The TF configuration to write in the file
	 *  @throws IasCdbException In case of error writing the TF
	 */
	public void writeTransferFunction(TransferFunctionDao transferFunction) throws IasCdbException;
	
	/**
	 *  Write the passed template to the CDB
	 *  
	 *  @param templateDao The template DAO to write in the file
	 *  @throws IasCdbException In case of error writing the TF
	 */
	public void writeTemplate(TemplateDao templateDao) throws IasCdbException;
	
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

	/**
	 * Write the configuration of the passed plugin
	 *
	 * @param pluginConfigDao the configuraton of the plugin
	 * @throws IasCdbException In case of error writing the configuration
	 */
	public void writePluginConfig(PluginConfigDao pluginConfigDao) throws IasCdbException;

	/**
	 * Write the configuration of the passed client
	 *
	 * @param clientConfigDao the configuraton of the client
	 * @throws IasCdbException In case of error writing the configuration
	 */
	public void writeClientConfig(ClientConfigDao clientConfigDao) throws IasCdbException;

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
