package org.eso.ias.cdb.structuredtext;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;

/**
 * <P>The structure of the folders and the names of the files 
 * of the CDB are described in the 
 * <A href="https://github.com/IntegratedAlarmSystem-Group/ias/wiki/ConfigurationDatabase">CDB wiki</A>:
 * 
 * <pre>
* {@code
 * +CDB
 * 	| ias.json
 *  |+ Supervisor
 *     | <SupID-1>.json
 *     | ...
 *     | <SupID-n>.json
 *  |+ DASU
 *     | <DasuID-1>.json
 *     | ...
 *     | <DasuID-m>.json
 *  |+ TF
 *     | tfs.json
 *  |+ ASCE
 *     | <AsceID-1>.json
 *     | ...
 *     | <AsceID-t>.json
 *  |+ IASIO
 *     | IASIO.json
 *  |+ TEMPLATE
 *     | templates.json
 *  |+ CLIENT
 *     | <ClientId-1>.conf
 *     | ...
 *     | <ClientId-x>.conf
 * }
 * <code>CdbFolders</code> returns the path and the names of the folders of the IAS CDB.
 * 
 * <code>CdbFolders</code> only deals with the folders of the CDB 
 * but not with the name of the files that ultimately depends on the selected format.
 * The example shown above is for JSON files.
 * 
 * @author acaproni
 *
 */
public enum CdbFolders {
	
	ROOT("CDB"),
	SUPERVISOR("Supervisor"),
	DASU("DASU"),
	ASCE("ASCE"),
	IASIO("IASIO"),
	TF("TF"),
	TEMPLATE("TEMPLATE"),
	PLUGIN("PLUGIN"),
	CLIENT("CLIENT");
	
	/**
	 *  The name of the folder
	 */
	public final String folderName;
	
	/**
	 * Constructor 
	 * 
	 * @param folderName The name of the folder in the CDB
	 */
	private CdbFolders(String folderName) {
		this.folderName=folderName;
	}
	
	/**
	 * Build the path of the subfolder
	 * 
	 * @param cdbParentPath: The path to the parent of the CDB
	 * @return The path to the subfolder
	 */
	private Path buildPath(Path cdbParentPath) {
		if (cdbParentPath==null) {
			throw new NullPointerException("The parent path of the CDB can't be null");
		}
		Path folderPath;
		if (this==ROOT) {
			folderPath=cdbParentPath.resolve(ROOT.folderName);
		} else {
			folderPath=cdbParentPath.resolve(ROOT.folderName).resolve(this.folderName);
		}
		return folderPath;
	}
	
	/**
	 * Get the path of the CDB folder.
	 * 
	 * @param cdbParentPath: The path to the parent of the CDB
	 * @return the path to the subfolder.
	 * @throws IOException If the folder is not writeable
	 */
	public Path getFolder(Path cdbParentPath) throws IOException {
		if (cdbParentPath==null) {
			throw new NullPointerException("The parent path of the CDB can't be null");
		}
		return buildPath(cdbParentPath);
	}
	
	/**
	 * Check if the subfolder exists
	 * 
	 * @param cdbParentPath The path of the folder
	 * @return <code>true</code> if the folder exists,
	 * 		   <code>false</code> otherwise.
	 */
	public boolean exists(Path cdbParentPath) {
		Path path = buildPath(cdbParentPath);
		File f = path.toFile();
		return f.exists();
	}
	
	/**
	 * Get the path of a CDB folder and if it is the case, creates the folder.
	 * 
	 * @param cdbParentPath: The path to the parent of the CDB
	 * @param folder: the CDB folder to create 
	 * @return the path to the subfolder.
	 * @throws IOException In case of error getting the path
	 */
	public static Path getSubfolder(Path cdbParentPath, CdbFolders folder) throws IOException {
		if (folder==null) {
			throw new NullPointerException("CDB subfolder can't be null");
		}
		return folder.getFolder(cdbParentPath);
	}
}
