package org.eso.ias.cdb.test.json;

import static org.junit.Assert.*;

import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import org.eso.ias.cdb.CdbReader;
import org.eso.ias.cdb.CdbWriter;
import org.eso.ias.cdb.json.CdbFiles;
import org.eso.ias.cdb.json.CdbFolders;
import org.eso.ias.cdb.json.CdbJsonFiles;
import org.eso.ias.cdb.json.JsonReader;
import org.eso.ias.cdb.json.JsonWriter;
import org.eso.ias.cdb.pojos.DasuDao;
import org.eso.ias.cdb.pojos.IasDao;
import org.eso.ias.cdb.pojos.IasTypeDao;
import org.eso.ias.cdb.pojos.IasioDao;
import org.eso.ias.cdb.pojos.LogLevelDao;
import org.eso.ias.cdb.pojos.PropertyDao;
import org.eso.ias.cdb.pojos.SupervisorDao;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * Test reading and writing of JSON CDB.
 * 
 * @author acaproni
 *
 */
public class TestJsonCdb {
	
	/**
	 * JSON files helper
	 */
	private CdbFiles cdbFiles;
	
	/**
	 * JSON files writer
	 */
	private CdbWriter cdbWriter;
	
	/**
	 * JSON files reader
	 */
	private CdbReader cdbReader;
	
	/**
	 * The parent folder is the actual folder
	 */
	public static final Path cdbParentPath =  FileSystems.getDefault().getPath(".");

	@Before
	public void setUp() throws Exception {
		// Remove any CDB folder if present
		CdbFolders.ROOT.delete(cdbParentPath);
		assertFalse(CdbFolders.ROOT.exists(cdbParentPath));
		
		cdbFiles = new CdbJsonFiles(cdbParentPath);
		assertNotNull(cdbFiles);
		cdbWriter = new JsonWriter(cdbFiles);
		assertNotNull(cdbWriter);
		cdbReader = new JsonReader(cdbFiles);
		assertNotNull(cdbReader);
	}

	@After
	public void tearDown() throws Exception {
		CdbFolders.ROOT.delete(cdbParentPath);
		assertFalse(CdbFolders.ROOT.exists(cdbParentPath));
	}

	@Test
	public void testWriteIas() throws Exception {
		
		// Build a IAS with a couple of properties
		IasDao ias = new IasDao();
		ias.setLogLevel(LogLevelDao.INFO);
		Set<PropertyDao> props = ias.getProps();
		
		PropertyDao p1 = new PropertyDao();
		p1.setName("P1-Name");
		p1.setValue("A value for property 1");
		props.add(p1);
		PropertyDao p2 = new PropertyDao();
		p2.setName("P2-Name");
		p2.setValue("A value for another property");
		props.add(p2);
		
		// Write the IAS
		cdbWriter.writeIas(ias);
		
		// Does it exist?
		assertTrue(cdbFiles.getIasFilePath().toFile().exists());
		
		// Read the IAS from the CDB and check if it is
		// what we just wrote
		Optional<IasDao> optIas = cdbReader.getIas();
		assertTrue("Got an empty IAS!", optIas.isPresent());
		assertEquals("The IASs differ!", ias, optIas.get());
	}

	/**
	 * Test reading and writing of Supervisor without Dasu
	 * @throws Exception
	 */
	@Test
	public void testWriteSupervisorNoDasus() throws Exception {
		
		SupervisorDao superv = new SupervisorDao();
		superv.setId("Supervisor-ID");
		superv.setHostName("almadev1.alma.cl");
		superv.setLogLevel(LogLevelDao.DEBUG);
		
		cdbWriter.writeSupervisor(superv);
		assertTrue(cdbFiles.getSuperivisorFilePath(superv.getId()).toFile().exists());
		
		Optional<SupervisorDao> optSuperv = cdbReader.getSupervisor(superv.getId());
		assertTrue("Got an empty Supervisor!", optSuperv.isPresent());
		assertEquals("The Supervisors differ!", superv, optSuperv.get());
	}
	
	/**
	 * Test reading and writing of Supervisor without Dasu
	 * @throws Exception
	 */
	@Test
	public void testWriteSupervisorWithDasus() throws Exception {
		
		SupervisorDao superv = new SupervisorDao();
		superv.setId("Supervisor-ID");
		superv.setHostName("almadev2.alma.cl");
		superv.setLogLevel(LogLevelDao.INFO);
		
		// Adds the DASUs
		DasuDao dasu1 = new DasuDao();
		dasu1.setId("DasuID1");
		dasu1.setSupervisor(superv);
		dasu1.setLogLevel(LogLevelDao.FATAL);
		superv.addDasu(dasu1);
		
		DasuDao dasu2 = new DasuDao();
		dasu2.setId("DasuID2");
		dasu2.setSupervisor(superv);
		dasu1.setLogLevel(LogLevelDao.WARN);
		superv.addDasu(dasu2);
		
		cdbWriter.writeSupervisor(superv);
		assertTrue(cdbFiles.getSuperivisorFilePath(superv.getId()).toFile().exists());
		Optional<SupervisorDao> optSuperv = cdbReader.getSupervisor(superv.getId());
		assertTrue("Got an empty Supervisor!", optSuperv.isPresent());
		System.out.println(superv);
		System.out.println(optSuperv.get());
		assertEquals("The Supervisors differ!", superv, optSuperv.get());
		
	}

	@Test
	public void testWriteDasu() {
		fail("Not yet implemented");
	}

	@Test
	public void testWriteAsce() {
		fail("Not yet implemented");
	}

	/**
	 * Check if the writing of one IASIO at a time works
	 * 
	 * @throws Exception
	 */
	@Test
	public void testWriteIasio() throws Exception {
		IasioDao iasio = new IasioDao("ioID", "IASIO description", 1500, IasTypeDao.ALARM);
		cdbWriter.writeIasio(iasio, false);
		
		assertTrue(cdbFiles.getIasioFilePath(iasio.getId()).toFile().exists());
		
		Optional<IasioDao> optIasio = cdbReader.getIasio(iasio.getId());
		assertTrue("Got an empty IASIO!", optIasio.isPresent());
		assertEquals("The IASIOs differ!", iasio, optIasio.get());
		
		// Check if there is one and only one IASIO in the CDB
		Optional<Set<IasioDao>> optSet = cdbReader.getIasios();
		assertTrue("Got an empty set of IASIOs!", optSet.isPresent());
		assertEquals("Size of set mismatch",1,optSet.get().size());

		// Append another IASIO
		IasioDao iasio2 = new IasioDao("ioID2", "Another IASIO", 2150, IasTypeDao.BOOLEAN);
		cdbWriter.writeIasio(iasio2, true);
		Optional<Set<IasioDao>> optSet2 = cdbReader.getIasios();
		assertTrue("Got an empty set of IASIOs!", optSet2.isPresent());
		assertEquals("Size of set mismatch",2,optSet2.get().size());
		
		// Is the second IASIO ok?
		Optional<IasioDao> optIasio2 = cdbReader.getIasio(iasio2.getId());
		assertTrue("Got an empty IASIO!", optIasio2.isPresent());
		assertEquals("The IASIOs differ!", iasio2, optIasio2.get());
		
		// Check if updating a IASIO replaces the old one
		iasio.setRefreshRate(100);
		iasio.setShortDesc("A new Desccripton");
		cdbWriter.writeIasio(iasio, true);
		// There must be still 2 IASIOs in the CDB
		optSet = cdbReader.getIasios();
		assertTrue("Got an empty set of IASIOs!", optSet.isPresent());
		assertEquals("Size of set mismatch",2,optSet.get().size());
		// Check if the IASIO in the CDB has been updated
		optIasio = cdbReader.getIasio(iasio.getId());
		assertTrue("Got an empty IASIO!", optIasio.isPresent());
		assertEquals("The IASIOs differ!", iasio, optIasio.get());
	}

	/**
	 * Check if the writing of set of IASIOs at once works
	 * 
	 * @throws Exception
	 */
	@Test
	public void testWriteIasios() throws Exception {
		Set<IasioDao> set = new HashSet<>();
		for (int t=0; t<5; t++) {
			IasioDao iasio = new IasioDao("iasioID-"+t, "IASIO description "+t, 1500+t*250, IasTypeDao.values()[t]);
			set.add(iasio);
		}
		cdbWriter.writeIasios(set, false);
		assertTrue(cdbFiles.getIasioFilePath("PlaceHolderID").toFile().exists());
		
		Optional<Set<IasioDao>> optSet = cdbReader.getIasios();
		assertTrue("Got an empty set of IASIOs!", optSet.isPresent());
		assertEquals("Size of set mismatch",set.size(),optSet.get().size());
		
		// Check if appending works
		Set<IasioDao> set2 = new HashSet<>();
		for (int t=0; t<6; t++) {
			IasioDao iasio = new IasioDao("2ndset-iasioID-"+t, "IASIO descr "+t*2, 1550+t*225, IasTypeDao.values()[t]);
			set2.add(iasio);
		}
		cdbWriter.writeIasios(set2, true);
		Optional<Set<IasioDao>> optSet2 = cdbReader.getIasios();
		assertTrue("Got an empty set of IASIOs!", optSet2.isPresent());
		assertEquals("Size of set mismatch",set.size()+set2.size(),optSet2.get().size());
		
		// Update the first iasios
		set.clear();
		for (int t=0; t<5; t++) {
			IasioDao iasio = new IasioDao("iasioID-"+t, "IASIO "+t, 100+t*250, IasTypeDao.values()[t]);
			set.add(iasio);
		}
		cdbWriter.writeIasios(set, true);
		// Size must be the same
		Optional<Set<IasioDao>> optSet3 = cdbReader.getIasios();
		assertTrue("Got an empty set of IASIOs!", optSet3.isPresent());
		assertEquals("Size of set mismatch",set.size()+set2.size(),optSet3.get().size());
		
		// Check if all IASIOs match with the ones in the sets
		set.stream().forEach(x -> assertTrue(optSet3.get().contains(x)));
		set2.stream().forEach(x -> assertTrue(optSet3.get().contains(x)));
		
		// Finally check if saving without appending really remove the right IASIOs
		// by writing again the IASIOs in set
		cdbWriter.writeIasios(set, false);
		// Size must be the same
		Optional<Set<IasioDao>> optSet4 = cdbReader.getIasios();
		assertTrue("Got an empty set of IASIOs!", optSet4.isPresent());
		assertEquals("Size of set mismatch",set.size(),optSet4.get().size());
		
		// Check if all IASIOs match with the ones in the sets
		set.stream().forEach(x -> assertTrue(optSet4.get().contains(x)));
		set2.stream().forEach(x -> assertFalse(optSet4.get().contains(x)));
	}

}
