package org.eso.ias.cdb.structuredtext.json;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Objects;

import org.eso.ias.cdb.TextFileType;

public class CdbTxtFiles implements CdbFiles {

	/**
	 * The extension of file names
	 */
	public final String fileExtension;

	/**
	 * The extension of configuration files of the clients
	 */
	public static final String confFileExtension=".conf";
	
	/**
	 * The name of the file containing all the templates
	 */
	public final String iasFileName;
	
	/**
	 * The name of the file containing all the templates
	 */
	public final String templatesFileName;
	
	/**
	 * The name of the file containing all the IASIOs
	 */
	public final String iasiosFileName;
	
	/**
	 * The name of the file containing all the transfer functions
	 */
	public final String transferFunsFileName;
	
	/**
	 * The parent folder of the CDB 
	 * i.e. the folder where we expect to find CDB, CDB/DASU etc.
	 * 
	 * @see CdbFolders
	 */
	private final Path cdbParentFolder;
	
	/**
	 * Constructor
	 * 
	 * @param parentFolder The stringified path of the parent folder of the CDB
	 * @param fType The type of the text files
	 * @throws IOException If the passed folder i snot valid
	 */
	public CdbTxtFiles(String parentFolder, TextFileType fType) throws IOException {
		this(Paths.get(parentFolder), fType);
	}
	
	/**
	 * Constructor
	 * 
	 * @param parentFolder The path of the parent folder of the CDB
     * @param fType The type of the text files
	 * @throws IOException If the passed folder is not valid
	 */
	public CdbTxtFiles(Path parentFolder, TextFileType fType) throws IOException {
		Objects.requireNonNull(parentFolder,"The parent folder can't be null");
		Objects.requireNonNull(fType, "Invalid null file type");
		this.fileExtension=fType.ext;
		this.iasFileName="ias"+ fileExtension;
		this.templatesFileName="templates"+ fileExtension;
		this.iasiosFileName="iasios"+ fileExtension;
		this.transferFunsFileName="tfs"+ fileExtension;
		if (checkParentFolder(parentFolder)) {
			this.cdbParentFolder=parentFolder;
		} else {
			throw new IOException("Check folder permission "+parentFolder.toAbsolutePath());
		}
	}
	
	/**
	 * Constructor
	 * 
	 * @param parentFolder The file pointing to the parent folder of the CDB
	 * @param fType The type of the text files
	 * @throws IOException If the passed folder is not valid
	 */
	public CdbTxtFiles(File parentFolder, TextFileType fType) throws IOException {
		this(parentFolder.toPath(), fType);
	}
	
	/**
	 * Check if the passed path is a writable folder.
	 * 
	 * @param p The path to the folder to check
	 * @return <code>true</code> if the path passed all the checks,
	 *         <code>false</code> otherwise
	 */
	private boolean checkParentFolder(Path p) {
		Objects.requireNonNull(p);
		return Files.exists(p) && Files.isDirectory(p) && Files.isWritable(p);
	}

	/** 
	 * @return The file to store ias configuration
	 * @see CdbFiles#getIasFilePath()
	 */
	@Override
	public Path getIasFilePath()  throws IOException {
		return CdbFolders.getSubfolder(cdbParentFolder, CdbFolders.ROOT, true).resolve(iasFileName);
	}

	/* (non-Javadoc)
	 * @see org.eso.ias.cdb.json.CdbFiles#getSuperivisorFilePath(java.lang.String)
	 */
	@Override
	public Path getSuperivisorFilePath(String supervisorID)  throws IOException{
		Objects.requireNonNull(supervisorID, "Invalid null supervisor ID");
		if (supervisorID.isEmpty()) {
			throw new IllegalArgumentException("Invalid empty supervisor ID");
		}
		return CdbFolders.getSubfolder(cdbParentFolder, CdbFolders.SUPERVISOR, true).resolve(supervisorID+ fileExtension);
	}

	/* (non-Javadoc)
	 * @see org.eso.ias.cdb.json.CdbFiles#getDasuFilePath(java.lang.String)
	 */
	@Override
	public Path getDasuFilePath(String dasuID)  throws IOException{
		Objects.requireNonNull(dasuID, "Invalid null DASU ID");
		if (dasuID.isEmpty()) {
			throw new IllegalArgumentException("Invalid empty DASU ID");
		}
		return CdbFolders.getSubfolder(cdbParentFolder, CdbFolders.DASU, true).resolve(dasuID+ fileExtension);
	}

	/* (non-Javadoc)
	 * @see org.eso.ias.cdb.json.CdbFiles#getAsceFilePath(java.lang.String)
	 */
	@Override
	public Path getAsceFilePath(String asceID)  throws IOException{
		Objects.requireNonNull(asceID, "Invalid null ASCE ID");
		if (asceID.isEmpty()) {
			throw new IllegalArgumentException("Invalid empty ASCE ID");
		}
		return CdbFolders.getSubfolder(cdbParentFolder, CdbFolders.ASCE, true).resolve(asceID+ fileExtension);
	}

	/* (non-Javadoc)
	 * @see org.eso.ias.cdb.json.CdbFiles#getIasioFilePath(java.lang.String)
	 */
	@Override
	public Path getIasioFilePath(String iasioID)  throws IOException{
		return CdbFolders.getSubfolder(cdbParentFolder, CdbFolders.IASIO, true).resolve(iasiosFileName);
	}

	/*
	 * (non-Javadoc)
	 * @see org.eso.ias.cdb.json.CdbFiles#getTFFilePath(java.lang.String)
	 */
	@Override
	public Path getTFFilePath(String tfID) throws IOException {
		return CdbFolders.getSubfolder(cdbParentFolder, CdbFolders.TF, true).resolve(transferFunsFileName);
	}
	
	/*
	 * (non-Javadoc)
	 * @see org.eso.ias.cdb.json.CdbFiles#getTemplateFilePath(java.lang.String)
	 */
	@Override
	public Path getTemplateFilePath(String templateID) throws IOException {
		return CdbFolders.getSubfolder(cdbParentFolder, CdbFolders.TEMPLATE, true).resolve(templatesFileName);
	}

	/**
	 *
	 * @param clientID The identifier of the client
	 * @return The path for the configuration of the client
	 *         with the passed ID
	 * @throws IOException In case of IO error getting the path
	 */
	@Override
	public Path getClientFilePath(String clientID) throws IOException {
		if (clientID==null || clientID.isEmpty()) {
			throw new IllegalArgumentException("Invalid null or empty DASU ID");
		}
		return CdbFolders.getSubfolder(cdbParentFolder, CdbFolders.CLIENT, true).resolve(clientID+confFileExtension);
	}

	/**
	 * @param clientID The identifier of the plugin
	 * @return The path for the configuration of the plugin
	 * with the passed ID
	 * @throws IOException In case of IO error getting the path
	 */
	@Override
	public Path getPluginFilePath(String clientID) throws IOException {
		if (clientID==null || clientID.isEmpty()) {
			throw new IllegalArgumentException("Invalid null or empty DASU ID");
		}
		return CdbFolders.getSubfolder(cdbParentFolder, CdbFolders.PLUGIN, true).resolve(clientID+ fileExtension);
	}
}
