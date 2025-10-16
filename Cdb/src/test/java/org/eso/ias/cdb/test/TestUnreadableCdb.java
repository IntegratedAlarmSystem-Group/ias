package org.eso.ias.cdb.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.Collection;
import java.util.Optional;
import java.util.Set;

import org.eso.ias.cdb.CdbReader;
import org.eso.ias.cdb.pojos.AsceDao;
import org.eso.ias.cdb.pojos.DasuToDeployDao;
import org.eso.ias.cdb.pojos.IasDao;
import org.eso.ias.cdb.pojos.IasioDao;
import org.eso.ias.cdb.pojos.PluginConfigDao;
import org.eso.ias.cdb.pojos.TemplateDao;
import org.eso.ias.cdb.structuredtext.CdbFiles;
import org.eso.ias.cdb.structuredtext.CdbTxtFiles;
import org.eso.ias.cdb.structuredtext.StructuredTextReader;
import org.eso.ias.cdb.structuredtext.TextFileType;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Check that an unreadbale CDB can be read (i.e. no IAS CDB object tries
 * to write the CDB)
 * 
 * Regression test for #262: it does not check the correctness of the data in the CDB
 * but if it can read a read-only CDB 
 * 
 * TestUnreadableCdb changes the permissions of TestUnreadableCdb before and after executing the tests
 */
public class TestUnreadableCdb {
	
	/**
	 * JSON files reader
	 */
	private CdbReader cdbReader;

    /**
     * The folder that contains the CDB that is not writable
     */
    private static final String cdbFolderName = "./src/test/testReadOnlyCdb";

	@BeforeAll
	public static void setUpClass() throws Exception {
        Path path = FileSystems.getDefault().getPath(TestUnreadableCdb.cdbFolderName);
		TestUnreadableCdb.setPermissions(path, true);
	}

	@AfterAll
	public static void tearDownClass() throws Exception {
		Path path = FileSystems.getDefault().getPath(TestUnreadableCdb.cdbFolderName);
		TestUnreadableCdb.setPermissions(path, false);
	}

    @BeforeEach
	public void setUp() throws Exception {
        Path path = FileSystems.getDefault().getPath(TestUnreadableCdb.cdbFolderName);

		// Checks that fuiles and folders are read-only
		assertTrue(TestUnreadableCdb.areAllFilesAndFoldersNotWritable(path));

		CdbFiles cdbFiles = new CdbTxtFiles(path, TextFileType.JSON);
		cdbReader = new StructuredTextReader(cdbFiles);
		cdbReader.init();
    }

    @AfterEach
	public void tearDown() throws Exception {
        cdbReader.shutdown();
    }
    /**
	 * Test the reading of ias.json
	 *
	 * @throws Exception
	 */
	@Test
	public void testGetIasFromFile() throws Exception {
		Optional<IasDao> iasOpt = cdbReader.getIas();
		assertTrue(iasOpt.isPresent());
	}

	
	private static void setPermissions(Path rootDir, boolean readOnly) throws IOException {
		Set<PosixFilePermission> folderPerms;
		Set<PosixFilePermission> filePerms;
		if (readOnly) {
			folderPerms = PosixFilePermissions.fromString("r-xr-xr-x");
			filePerms =   PosixFilePermissions.fromString("r--r--r--");
		 } else {
			folderPerms = PosixFilePermissions.fromString("rwxr-xr-x");
			filePerms =   PosixFilePermissions.fromString("rw-r--r--");
		 }
		 
        
		Files.walkFileTree(rootDir, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                Files.setPosixFilePermissions(dir, folderPerms);
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                Files.setPosixFilePermissions(file, filePerms);
                return FileVisitResult.CONTINUE;
            }
        });

		System.out.println("CDB permission set to " + (readOnly ? "read-only" : "read-write"));

    }

	
	public static boolean areAllFilesAndFoldersNotWritable(Path rootDir) throws IOException {
        final boolean[] allNotWritable = {true};

        Files.walkFileTree(rootDir, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
                if (Files.isWritable(dir)) {
                    allNotWritable[0] = false;
                    return FileVisitResult.TERMINATE;
                }
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                if (Files.isWritable(file)) {
                    allNotWritable[0] = false;
                    return FileVisitResult.TERMINATE;
                }
                return FileVisitResult.CONTINUE;
            }
        });

        return allNotWritable[0];
    }



    /**
	 * Test the retrieval of templates
	 *
	 * @throws Exception
	 */
	@Test
	public void testGetTemplates() throws Exception {
		Optional<TemplateDao> template2 = cdbReader.getTemplate("template2-ID");
		assertTrue(template2.isPresent());
		Optional<TemplateDao> template1 = cdbReader.getTemplate("template1-ID");
		assertTrue(template1.isPresent());
		Optional<TemplateDao> template3 = cdbReader.getTemplate("template3-ID");
		assertTrue(template3.isPresent());
	}

    /**
	 * Test {@link StructuredTextReader#getPluginIds()}
	 * @throws Exception
	 */
	@Test
	public void testGetPluginIds() throws Exception {
		Optional<Set<String>> idsOpt = cdbReader.getPluginIds();
		assertTrue(idsOpt.isPresent());
		assertEquals(1,idsOpt.get().size());
		assertTrue(idsOpt.get().contains("PluginID2"));
	}

	/**
	 * Test {@link StructuredTextReader#getClientIds()}
	 * @throws Exception
	 */
	@Test
	public void testGetClientIds() throws Exception {
		Optional<Set<String>> idsOpt = cdbReader.getClientIds();
		assertTrue(idsOpt.isPresent());
		assertEquals(1,idsOpt.get().size());
		assertTrue(idsOpt.get().contains("test"));
	}

    /**
	 * Test getting the PluginConfig from a JSON file
	 * @throws Exception
	 */
	@Test
	public void testGetPlugin() throws Exception {

		String pluginId = "PluginID2";

		Optional<PluginConfigDao> pCOnfDao = cdbReader.getPlugin(pluginId);
		assertTrue(pCOnfDao.isPresent());
	}

    /**
     * Test the getting of an IASIO form file
     *
     * @throws Exception
     */
	@Test
	public void testGetIasio() throws Exception {
		Optional<IasioDao> iDao=cdbReader.getIasio("iasioID-7");
		assertTrue(iDao.isPresent());
	}

    /**
     * Test the ASCE with templated inputs
     *
     * @throws Exception
     */
	@Test
	public void testGetAsce() throws Exception {
		Optional<AsceDao> asceWithTemplatedInputs = cdbReader.getAsce("ASCE-ID1");
		assertTrue(asceWithTemplatedInputs.isPresent());
	}

    /**
     * Test the getter if IDs of ASCEs
     * @throws Exception
     */
    @Test
    public void testGetIdsOfAsces() throws Exception {

        Optional<Set<String>> idsOpt= cdbReader.getAsceIds();
        assertTrue(idsOpt.isPresent());
        Set<String> ids = idsOpt.get();
        assertEquals(1,ids.size());
    }

    /**
     * Test the getter if IDs of DASUs
     * @throws Exception
     */
    @Test
    public void testGetIdsOfDasus() throws Exception {
        Optional<Set<String>> idsOpt= cdbReader.getDasuIds();
        assertTrue(idsOpt.isPresent());
        Set<String> ids = idsOpt.get();
        assertEquals(1,ids.size());
    }

     /**
     * Test the getter if IDs of Supervisors
     * @throws Exception
     */
    @Test
    public void testGetIdsOfSupervisor() throws Exception {
        Optional<Set<String>> idsOpt= cdbReader.getSupervisorIds();
        assertTrue(idsOpt.isPresent());
        Set<String> ids = idsOpt.get();
        assertEquals(1,ids.size());
    }

    /**
	 * Test the getting of IASIOs belonging to a given ASCE
	 * <P>
	 * This test runs against the JSON CDB contained in testJsonCdb
	 *
	 * @throws Exception
	 */
	@Test
	public void testGetIasiosOfAsce() throws Exception {
		// Get the IASIOs of ASCE-ID4 that has 3 inputs
		Collection<IasioDao> iasios = cdbReader.getIasiosForAsce("ASCE-ID1");
		assertEquals(2,iasios.size());
	}

    /**
	 * Test the getting of ASCEs belonging to a given DASU
	 * <P>
	 * This test runs against the JSON CDB contained in testJsonCdb
	 *
	 * @throws Exception
	 */
	@Test
	public void testGetAscesOfDasu() throws Exception {
		// Get the ASCE of DasuID1 that has no ASCE
		Set<AsceDao> asces = cdbReader.getAscesForDasu("DasuID1");
		assertTrue(asces.isEmpty());
	}

    /**
	 * Test the getting of DASUs belonging to a given supervisor
	 * <P>
	 * This test runs against the JSON CDB contained in testJsonCdb
	 *
	 * @throws Exception
	 */
	@Test
	public void testGetDasusOfSupervisor() throws Exception {
		// Get the DASUs of a Supervisor that has one
		Set<DasuToDeployDao>dasus = cdbReader.getDasusToDeployInSupervisor("Supervisor-ID1");
		assertEquals(1,dasus.size());
	}
    
}
