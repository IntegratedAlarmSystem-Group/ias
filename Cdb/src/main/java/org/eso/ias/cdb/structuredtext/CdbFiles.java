package org.eso.ias.cdb.structuredtext;

import org.eso.ias.cdb.structuredtext.TextFileType;

import java.io.IOException;
import java.nio.file.Path;

/**
 * An interface to get the name of a file of the CDB
 * independently of the adopted implementation. 
 * 
 * @author acaproni
 *
 */
public interface CdbFiles {

	/**
	 * @return The type of files in the CDB
	 */
	public TextFileType getCdbFileType();
	
	/**
	 * @return The path of ias global configuration
	 * @throws IOException In case of IO error getting the path
	 */
	public Path getIasFilePath() throws IOException;
	
	/**
	 * 
	 * @param supervisorID The ID of the supervisor
	 * 
	 * @return The path for the configuration of the supervisor
	 * 		with the passed ID  
	 * @throws IOException In case of IO error getting the path
	 */
	public Path getSuperivisorFilePath(String supervisorID) throws IOException;
	
	/**
	 * 
	 * @param dasuID The ID of the DASU
	 * 
	 * @return The path for the configuration of the DASU
	 * 		with the passed ID  
	 * @throws IOException In case of IO error getting the path
	 */
	public Path getDasuFilePath(String dasuID) throws IOException;
	
	/**
	 * 
	 * @param tfID The ID of the transfer function
	 * 
	 * @return The path for the configuration of the TF
	 * 		with the passed ID 
	 * @throws IOException In case of IO error getting the path 
	 */
	public Path getTFFilePath(String tfID) throws IOException;
	
	/**
	 * 
	 * @param templateID The ID of the template
	 * 
	 * @return The path for the configuration of the template
	 * 		with the passed ID 
	 * @throws IOException In case of IO error getting the path 
	 */
	public Path getTemplateFilePath(String templateID) throws IOException;
	
	/**
	 * 
	 * @param asceID The ID of the ASCE
	 * 
	 * @return The path for the configuration of the ASCE
	 * 		with the passed ID 
	 * @throws IOException In case of IO error getting the path 
	 */
	public Path getAsceFilePath(String asceID) throws IOException;
	
	/**
	 * 
	 * @param iasioID The ID of the IASIO
	 * 
	 * @return The path for the configuration of the IASIO
	 * 		with the passed ID
	 * @throws IOException In case of IO error getting the path 
	 */
	public Path getIasioFilePath(String iasioID) throws IOException;

	/**
	 *
	 * @param clientID The identifier of the client
	 * @return The path for the configuration of the client
	 *         with the passed ID
	 * @throws IOException In case of IO error getting the path
	 */
	public Path getClientFilePath(String clientID) throws IOException;

	/**
	 *
	 * @param clientID The identifier of the plugin
	 * @return The path for the configuration of the plugin
	 *         with the passed ID
	 * @throws IOException In case of IO error getting the path
	 */
	public Path getPluginFilePath(String clientID) throws IOException;

}
