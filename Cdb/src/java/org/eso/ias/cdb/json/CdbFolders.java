package org.eso.ias.cdb.json;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;

/**
 * <P>The structure of the folders and the names of the files 
 * of the CDB are described in the 
 * <A href="https://github.com/IntegratedAlarmSystem-Group/ias/wiki/ConfigurationDatabase">CDB wiki</A>:
 * 
 * <VERBATIM>
 * +CDB
 * 	| ias.json
 *  |+ Supervisor
 *     | <SupID-1>.json
 *     |
 *     | <SupID-n>.json
 *  |+ DASU
 *     | <DasuID-1>.json
 *     | ...
 *     | <DasuID-m>.json
 *  |+ ASCE
 *     | <AsceID-1>.json
 *     | ...
 *     | <AsceID-t>.json
 *  |+ IASIO
 *     | IASIO.json
 * </VERBATIM>
 * 
 * @author acaproni
 *
 */
public enum CdbFolders {
	
	ROOT("CDB"),
	SUPERVISOR("Supervisor"),
	DASU("DASU"),
	ASCE("ASCE"),
	IASIO("IASIO");
	
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
	 * Get the path of a CDB folder and if it is the case, creates the folder.
	 * 
	 * @param cdbParentPath: The path to the parent of the CDB
	 * @param folder: the CDB folder to create 
	 * @param append: if <code>true</code> and the subfolder does not exist, then create it
	 * @return the path to the subfolder.
	 */
	public static Path getSubfolder(Path cdbParentPath, CdbFolders folder, boolean create) throws IOException {
		if (cdbParentPath==null) {
			throw new NullPointerException("The parent path of the CDB can't be null");
		}
		if (folder==null) {
			throw new NullPointerException("CDB subfolder can't be null");
		}
		Path newPath;
		if (folder==ROOT) {
			newPath=cdbParentPath.resolve(folder.folderName);
		} else {
			newPath=cdbParentPath.resolve(ROOT.folderName).resolve(folder.folderName);
		}
		File f = newPath.toFile();
		if (f.exists()) {
			if (f.isDirectory() && f.canWrite()) {
				// No need to create the folder
				return newPath;
			} else {
				throw new IOException(f.toString()+" exists but unusable: check permissions and type");
			}
		} else {
			f.mkdirs();
			return newPath;
		}
	}
}
