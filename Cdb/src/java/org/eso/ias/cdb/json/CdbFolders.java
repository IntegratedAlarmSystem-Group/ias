package org.eso.ias.cdb.json;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Objects;

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
 *     }
 * </pre>
 * 
 * Note that <code>CdbFolders</code> only deals with the folders of the CDB 
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
	 * Delete the subfolder, if it exists.
	 * 
	 * @param cdbParentPath: The path to the parent of the CDB
	 * @return <code>true</code> if the folder has been deleted,
	 * 	       <code>false</code> otherwise
	 */
	public boolean delete(Path cdbParentPath) {
		if (cdbParentPath==null) {
			throw new NullPointerException("The parent path of the CDB can't be null");
		}
		Path folderPath = buildPath(cdbParentPath);
		if (!exists(cdbParentPath)) {
			// The folder does not exist ==> nothing to do
			return false;
		}
		
		deleteFolder(folderPath.toFile());
		return true;
	}
	
	/**
	 * Recursively delete a folder from the file system.
	 * 
	 * @param folderToDelete
	 */
	private void deleteFolder(File folderToDelete) {
		Objects.requireNonNull(folderToDelete, "Cannot delete a NULL folder");
		
		File[] files = folderToDelete.listFiles();
	    if(files!=null) { //some JVMs return null for empty dirs
	        for(File f: files) {
	            if(f.isDirectory()) {
	                deleteFolder(f);
	            } else {
	                f.delete();
	            }
	        }
	    }
	    folderToDelete.delete();
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
	 * Get the path of the CDB folder and if it is the case, creates it.
	 * 
	 * @param cdbParentPath: The path to the parent of the CDB
	 * @param create: if <code>true</code> and the subfolder does not exist, then create it
	 * @return the path to the subfolder.
	 * @throws IOException If the folder is not writeable
	 */
	public Path getFolder(Path cdbParentPath, boolean create) throws IOException {
		if (cdbParentPath==null) {
			throw new NullPointerException("The parent path of the CDB can't be null");
		}
		Path folderPath=buildPath(cdbParentPath);
		File f = folderPath.toFile();
		if (exists(cdbParentPath)) {
			if (f.isDirectory() && f.canWrite()) {
				// No need to create the folder
				return folderPath;
			} else {
				throw new IOException(f.toString()+" exists but unusable: check permissions and type");
			}
		} else {
			f.mkdirs();
			return folderPath;
		}
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
	 * @param create: if <code>true</code> and the subfolder does not exist, then create it
	 * @return the path to the subfolder.
	 * @throws IOException In case of error getting the path
	 */
	public static Path getSubfolder(Path cdbParentPath, CdbFolders folder, boolean create) throws IOException {
		if (folder==null) {
			throw new NullPointerException("CDB subfolder can't be null");
		}
		return folder.getFolder(cdbParentPath, create);
	}
	
	/**
	 * Create all the subfolders of the CDB.
	 * 
	 * If a subfolder already exists then nothing is done
	 * 
	 * @param cdbParentPath: The path to the parent of the CDB
	 * @throws IOException in case of error creating the folders
	 */
	public static void createFolders(Path cdbParentPath) throws IOException {
		for (CdbFolders folder: CdbFolders.values()) {
			folder.getFolder(cdbParentPath, true);
		}
	}
}
