package org.eso.ias.cdb.json;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Objects;

public class CdbJsonFiles implements CdbFiles {
	
	/**
	 * The extension of json file names
	 */
	public static final String jsonFileExtension=".json";
	
	/**
	 * The name of the file containing all the templates
	 */
	public static final String iasFileName="ias"+jsonFileExtension;
	
	/**
	 * The name of the file containing all the templates
	 */
	public static final String templatesFileName="templates"+jsonFileExtension;
	
	/**
	 * The name of the file containing all the IASIOs
	 */
	public static final String iasiosFileName="iasios"+jsonFileExtension;
	
	/**
	 * The name of the file containing all the transfer functions
	 */
	public static final String transferFunsFileName="tfs"+jsonFileExtension;
	
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
	 * @throws IOException If the passed folder i snot valid
	 */
	public CdbJsonFiles(String parentFolder) throws IOException {
		Objects.requireNonNull(parentFolder,"The parent folder can't be null");
		if (parentFolder.trim().isEmpty()) {
			throw new IllegalArgumentException("Invalid empty parent folder");
		}
		Path p = Paths.get(parentFolder);
		if (checkParentFolder(p)) {
			this.cdbParentFolder=p;
		} else {
			throw new IOException("Check folder permission "+parentFolder);
		}
	}
	
	/**
	 * Constructor
	 * 
	 * @param parentFolder The path of the parent folder of the CDB
	 * @throws IOException If the passed folder is not valid
	 */
	public CdbJsonFiles(Path parentFolder) throws IOException {
		Objects.requireNonNull(parentFolder,"The parent folder can't be null");
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
	 * @throws IOException If the passed folder is not valid
	 */
	public CdbJsonFiles(File parentFolder) throws IOException {
		Objects.requireNonNull(parentFolder,"The parent folder can't be null");
		Path p = parentFolder.toPath();
		if (checkParentFolder(p)) {
			this.cdbParentFolder=p;
		} else {
			throw new IOException("Check folder permission "+parentFolder.getAbsolutePath());
		}
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
	 * @see org.eso.ias.cdb.json.CdbFiles#getIasFilePath()
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
		return CdbFolders.getSubfolder(cdbParentFolder, CdbFolders.SUPERVISOR, true).resolve(supervisorID+jsonFileExtension);
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
		return CdbFolders.getSubfolder(cdbParentFolder, CdbFolders.DASU, true).resolve(dasuID+jsonFileExtension);
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
		return CdbFolders.getSubfolder(cdbParentFolder, CdbFolders.ASCE, true).resolve(asceID+jsonFileExtension);
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
}
