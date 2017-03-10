package org.eso.ias.cdb.test.json;

import static org.junit.Assert.*;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.nio.file.FileSystems;
import java.nio.file.Path;

import org.eso.ias.cdb.json.CdbFolders;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * Test creation and deletion of CDB folders
 * 
 * @see CdbFolders
 * @author acaproni
 *
 */
public class CdbFoldersTest {
	
	/**
	 * The parent folder is the actual folder
	 */
	public static final Path cdbParentPath =  FileSystems.getDefault().getPath(".");

	@Before
	public void setUp() throws Exception {
		// Remove any CDB folder if present
		CdbFolders.ROOT.delete(cdbParentPath);
		assertFalse(CdbFolders.ROOT.exists(cdbParentPath));
	}

	@After
	public void tearDown() throws Exception {
		CdbFolders.ROOT.delete(cdbParentPath);
		assertFalse(CdbFolders.ROOT.exists(cdbParentPath));
	}

	/**
	 * Check the creation of the folders 
	 * by {@link CdbFolders#getFolder(Path, boolean)}
	 * 
	 * @throws Exception
	 */
	@Test
	public void testFoldersCreation() throws Exception {
		for (CdbFolders folder: CdbFolders.values()) {
			folder.getFolder(cdbParentPath, true);
			assertTrue(folder.toString()+"Creation of folder failed!", folder.exists(cdbParentPath));
		}
	}
	
	/**
	 * Test the creation of one folder 
	 * by {@link CdbFolders#getFolder(Path, boolean)}
	 * 
	 * @throws Exception
	 */
	@Test
	public void testFolderCreation() throws Exception {
		// Check if the folder is created when does not exists
		assertFalse(CdbFolders.DASU.exists(cdbParentPath));
		CdbFolders.DASU.getFolder(cdbParentPath, true);
		assertTrue(CdbFolders.DASU.exists(cdbParentPath));
		
		// Check if the folder is NOT created when does not exists
		// but we do not ask to create
		assertFalse(CdbFolders.ASCE.exists(cdbParentPath));
		CdbFolders.ASCE.getFolder(cdbParentPath, false);
		assertTrue(CdbFolders.ASCE.exists(cdbParentPath));
	}
	
	/**
	 * Check if all the folders are created
	 * 
	 * @throws Exception
	 */
	@Test
	public void testCreateFoldersStruct() throws Exception {
		CdbFolders.createFolders(cdbParentPath);
		for (CdbFolders folder: CdbFolders.values()) {
			File f = folder.getFolder(cdbParentPath, false).toFile();
			assertTrue(folder.toString()+"Creation of folder failed!", f.exists());
		}
	}
	
	/**
	 * Test the deletion of a folder
	 * 
	 * @throws Exception
	 */
	@Test
	public void testDelete() throws Exception {
		// Check if an empty folder is delete
		
		// Check if the folder is created when does not exists
		assertFalse(CdbFolders.SUPERVISOR.exists(cdbParentPath));
		CdbFolders.SUPERVISOR.getFolder(cdbParentPath, true);
		assertTrue(CdbFolders.SUPERVISOR.exists(cdbParentPath));
		
		CdbFolders.SUPERVISOR.delete(cdbParentPath);
		assertFalse(CdbFolders.SUPERVISOR.exists(cdbParentPath));
		
		// Check if a non-empty folder is created as well 
		
		// Create the DASU folder
		Path p = CdbFolders.SUPERVISOR.getFolder(cdbParentPath, true);
		assertTrue(CdbFolders.SUPERVISOR.exists(cdbParentPath));
		// Create text file in the DASU folder
		File txtFile = p.resolve("Test.log").toFile();
		BufferedWriter writer = null;
        try {
            writer = new BufferedWriter(new FileWriter(txtFile));
            writer.write("Hello world!");
        } finally {
            try {
                // Close the writer regardless of what happens...
                writer.close();
            } catch (Exception e) { }
        }
		assertTrue("The file does not exists!", txtFile.exists());
		// Delete the folder
		CdbFolders.SUPERVISOR.delete(cdbParentPath);
		assertFalse(CdbFolders.SUPERVISOR.exists(cdbParentPath));
	}
}
