package org.eso.ias.cdb.test.json;

import static org.junit.Assert.*;

import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.eso.ias.cdb.CdbReader;
import org.eso.ias.cdb.CdbWriter;
import org.eso.ias.cdb.json.CdbFiles;
import org.eso.ias.cdb.json.CdbFolders;
import org.eso.ias.cdb.json.CdbJsonFiles;
import org.eso.ias.cdb.json.JsonReader;
import org.eso.ias.cdb.json.JsonWriter;
import org.eso.ias.cdb.pojos.AsceDao;
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
 * <P>
 * Some tests, write the CDB and then read data out of it.
 * <BR>Other tests instead uses the JSON CDB contained in testCdb
 * whose description is in testCdb/ReadMe.txt
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
	 * Test reading and writing of Supervisor
	 * @throws Exception
	 */
	@Test
	public void testWriteSupervisor() throws Exception {
		
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
		
		// DASUs must be in the CDB as well otherwise
		// included objects cannot be rebuilt.
		cdbWriter.writeDasu(dasu1);
		cdbWriter.writeDasu(dasu2);
		
		cdbWriter.writeSupervisor(superv);
		assertTrue(cdbFiles.getSuperivisorFilePath(superv.getId()).toFile().exists());
		
		Optional<SupervisorDao> optSuperv = cdbReader.getSupervisor(superv.getId());
		assertTrue("Got an empty Supervisor!", optSuperv.isPresent());
		assertEquals("The Supervisors differ!", superv, optSuperv.get());
		
	}

	@Test
	public void testWriteDasu() throws Exception {
		SupervisorDao superv = new SupervisorDao();
		superv.setId("Supervisor-ID");
		superv.setHostName("almadev2.alma.cl");
		superv.setLogLevel(LogLevelDao.INFO);
		
		// The DASU to test
		DasuDao dasu = new DasuDao();
		dasu.setId("DasuID1");
		dasu.setSupervisor(superv);
		dasu.setLogLevel(LogLevelDao.FATAL);
		superv.addDasu(dasu);
		
		AsceDao asce1= new AsceDao();
		asce1.setId("ASCE1");
		asce1.setDasu(dasu);
		asce1.setTfClass("org.eso.ias.tf.EqualTransferFunction");
		IasioDao output1 = new IasioDao("OUT-ID1", "description", 456, IasTypeDao.CHAR);
		asce1.setOutput(output1);
		
		AsceDao asce2= new AsceDao();
		asce2.setId("ASCE-2");
		asce2.setDasu(dasu);
		IasioDao output2 = new IasioDao("ID2", "description", 1000, IasTypeDao.DOUBLE);
		asce2.setOutput(output2);
		asce2.setTfClass("org.eso.ias.tf.MinMaxTransferFunction");
		AsceDao asce3= new AsceDao();
		asce3.setId("ASCE-ID-3");
		asce3.setDasu(dasu);
		IasioDao output3 = new IasioDao("OUT-ID3", "desc3", 456, IasTypeDao.SHORT);
		asce3.setOutput(output2);
		asce3.setTfClass("org.eso.ias.tf.MaxTransferFunction");
		
		// Supervisor must be in the CDB as well otherwise
		// included objects cannot be rebuilt.
		cdbWriter.writeSupervisor(superv);
		
		// ASCE must be in the CDB as well otherwise
		// included objects cannot be rebuilt.
		cdbWriter.writeAsce(asce1);
		cdbWriter.writeAsce(asce2);
		cdbWriter.writeAsce(asce3);
		
		cdbWriter.writeIasio(output1, false);
		cdbWriter.writeIasio(output2, true);
		cdbWriter.writeIasio(output3, true);
		
		dasu.addAsce(asce1);
		dasu.addAsce(asce2);
		dasu.addAsce(asce3);
		
		// Write the DASU
		cdbWriter.writeDasu(dasu);
		assertTrue(cdbFiles.getDasuFilePath(dasu.getId()).toFile().exists());
		
		// Read the DASU from CDB and compare with what we just wrote
		Optional<DasuDao> theDasuFromCdb = cdbReader.getDasu(dasu.getId());
		assertTrue("Got a null DASU with ID "+dasu.getId()+" from CDB",theDasuFromCdb.isPresent());
		assertEquals("The DAUSs differ!", dasu,theDasuFromCdb.get());
	}

	@Test
	public void testWriteAsce() throws Exception {
		
		SupervisorDao superv = new SupervisorDao();
		superv.setId("Supervisor-ID");
		superv.setHostName("almaias.eso.org");
		superv.setLogLevel(LogLevelDao.INFO);
		
		// The DASU that owns the ASCE to test
		DasuDao dasu = new DasuDao();
		dasu.setId("DasuID1");
		dasu.setLogLevel(LogLevelDao.FATAL);
		dasu.setSupervisor(superv);
		
		// The ASCE to test
		AsceDao asce = new AsceDao();
		asce.setId("ASCE-ID-For-Testsing");
		asce.setDasu(dasu);
		asce.setTfClass("org.eso.ias.tf.AnotherFunction");
		
		IasioDao output = new IasioDao("OUTPUT-ID", "One IASIO in output", 8193, IasTypeDao.BYTE);
		asce.setOutput(output);
		
		// A set of inputs (one for each possible type
		for (int t=0; t<IasTypeDao.values().length; t++) {
			IasioDao input = new IasioDao("INPUT-ID"+t, "IASIO "+t+" in input", 1000+t*128, IasTypeDao.values()[t]);
			asce.addInput(input, true);
			cdbWriter.writeIasio(input, true);
		}
		assertEquals("Not all the inputs have bene added to the ASCE",IasTypeDao.values().length,asce.getInputs().size());
		
		// Adds few properties
		PropertyDao p1 = new PropertyDao();
		p1.setName("Prop1-Name");
		p1.setValue("The value of P1");
		PropertyDao p2 = new PropertyDao();
		p2.setName("Prop2-Name");
		p2.setValue("The value of P2");
		PropertyDao p3 = new PropertyDao();
		p3.setName("Prop3-Name");
		p3.setValue("The value of P3");
		asce.getProps().add(p1);
		asce.getProps().add(p2);
		asce.getProps().add(p3);
		
		// Included objects must be in the CDB as well otherwise
		// included objects cannot be rebuilt in the ASCE
		cdbWriter.writeIasio(output, true);
		cdbWriter.writeSupervisor(superv);
		cdbWriter.writeDasu(dasu);
		
		
		cdbWriter.writeAsce(asce);
		assertTrue(cdbFiles.getAsceFilePath(asce.getId()).toFile().exists());
		
		Optional<AsceDao> optAsce = cdbReader.getAsce(asce.getId());
		assertTrue("Got a null ASCE with ID "+asce.getId()+" from CDB",optAsce.isPresent());
		assertEquals("The ASCEs differ!", asce,optAsce.get());
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
	
	/**
	 * Test the getting of DASUs belonging to a given supervisor
	 * <P>
	 * This test runs against the JSON CDB contained in testCdb
	 * 
	 * @throws Exception
	 */
	@Test
	public void testGetDasusOfSupervisor() throws Exception {
		Path path = FileSystems.getDefault().getPath("./testCdb");
		cdbFiles = new CdbJsonFiles(path);
		cdbReader = new JsonReader(cdbFiles);
		// Get the DASUs of a Supervisor that has none
		Set<DasuDao> dasus = cdbReader.getDasusForSupervisor("Supervisor-ID2");
		assertTrue(dasus.isEmpty());
		// Get the DASUs of a Supervisor that has one
		dasus = cdbReader.getDasusForSupervisor("Supervisor-ID1");
		assertEquals(1,dasus.size());
		Iterator<DasuDao> iter = dasus.iterator();
		DasuDao dasu = iter.next();
		assertEquals("DasuID1",dasu.getId());
		// Get the DASUs of a Supervisor that has three
		dasus = cdbReader.getDasusForSupervisor("Supervisor-ID3");
		assertEquals(3,dasus.size());
		Set<String> dasuIds = dasus.stream().map(d -> d.getId()).collect(Collectors.toSet());
		assertEquals(3,dasuIds.size());
		dasus.forEach( d -> assertTrue(d.getId().equals("DasuID2") || d.getId().equals("DasuID3")||d.getId().equals("DasuID4")));
	}
	
	/**
	 * Test the getting of ASCEs belonging to a given DASU
	 * <P>
	 * This test runs against the JSON CDB contained in testCdb
	 * 
	 * @throws Exception
	 */
	@Test
	public void testGetAscesOfDasu() throws Exception {
		Path path = FileSystems.getDefault().getPath("./testCdb");
		cdbFiles = new CdbJsonFiles(path);
		cdbReader = new JsonReader(cdbFiles);
		
		// Get the ASCE of DasuID1 that has no ASCE
		Set<AsceDao> asces = cdbReader.getAscesForDasu("DasuID1");
		assertTrue(asces.isEmpty());
		// Get the ASCEs of DasuID2 that contains ASCE-ID1
		asces = cdbReader.getAscesForDasu("DasuID2");
		assertEquals(1, asces.size());
		asces.forEach( asce -> assertEquals("ASCE-ID1",asce.getId()));
		
		// Get the ASCE of DasuID3 that has 3 ASCEs
		asces = cdbReader.getAscesForDasu("DasuID3");
		assertEquals(3, asces.size());
		Set<String> asceIds = asces.stream().map(d -> d.getId()).collect(Collectors.toSet());
		assertEquals(3, asceIds.size());
		asces.forEach( a -> assertTrue(a.getId().equals("ASCE-ID2") || a.getId().equals("ASCE-ID3") || a.getId().equals("ASCE-ID4")));
	}
	
	/**
	 * Test the getting of IASIOs belonging to a given ASCE
	 * <P>
	 * This test runs against the JSON CDB contained in testCdb
	 * 
	 * @throws Exception
	 */
	@Test
	public void testGetIasiosOfAsce() throws Exception {
		Path path = FileSystems.getDefault().getPath("./testCdb");
		cdbFiles = new CdbJsonFiles(path);
		cdbReader = new JsonReader(cdbFiles);
		
		// Get the IASIOs of ASCE-ID4 that has 3 inputs
		Collection<IasioDao> iasios = cdbReader.getIasiosForAsce("ASCE-ID4");
		assertEquals(3,iasios.size());
		Set<String> iosIds = iasios.stream().map(d -> d.getId()).collect(Collectors.toSet());
		assertEquals(3,iosIds.size());
		iosIds.forEach( a -> assertTrue(a.equals("iasioID-2") || a.equals("iasioID-3") || a.equals("iasioID-4")));
		
		// Get the IASIOs of ASCE-ID3 that has 2 inputs
		iasios = cdbReader.getIasiosForAsce("ASCE-ID3");
		assertEquals(2,iasios.size());
	}

}
