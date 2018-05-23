package org.eso.ias.cdb.test.json;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
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
import org.eso.ias.cdb.pojos.DasuToDeployDao;
import org.eso.ias.cdb.pojos.IasDao;
import org.eso.ias.cdb.pojos.IasTypeDao;
import org.eso.ias.cdb.pojos.IasioDao;
import org.eso.ias.cdb.pojos.LogLevelDao;
import org.eso.ias.cdb.pojos.PropertyDao;
import org.eso.ias.cdb.pojos.SupervisorDao;
import org.eso.ias.cdb.pojos.TFLanguageDao;
import org.eso.ias.cdb.pojos.TemplateDao;
import org.eso.ias.cdb.pojos.TransferFunctionDao;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

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

	@BeforeEach
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
		
		cdbReader.init();
	}

	@AfterEach
	public void tearDown() throws Exception {
		CdbFolders.ROOT.delete(cdbParentPath);
		assertFalse(CdbFolders.ROOT.exists(cdbParentPath));
		
		cdbReader.shutdown();
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
		
		ias.setRefreshRate(4);
		ias.setTolerance(3);
		
		ias.setHbFrequency(5);
		
		ias.setBsdbUrl("bsdb-server:9092");
		
		// Write the IAS
		cdbWriter.writeIas(ias);
		
		// Does it exist?
		assertTrue(cdbFiles.getIasFilePath().toFile().exists());
		
		// Read the IAS from the CDB and check if it is
		// what we just wrote
		Optional<IasDao> optIas = cdbReader.getIas();
		assertTrue( optIas.isPresent(),"Got an empty IAS!");
		assertEquals(ias, optIas.get(),"The IASs differ!");
	}
	

	
	/**
	 * Test the reading of ias.json
	 * <P>
	 * This test runs against the JSON CDB contained in testCdb
	 * 
	 * @throws Exception
	 */
	@Test
	public void testGetIasFromFile() throws Exception {
		Path path = FileSystems.getDefault().getPath("./testCdb");
		cdbFiles = new CdbJsonFiles(path);
		cdbReader = new JsonReader(cdbFiles);
		
		Optional<IasDao> iasOpt = cdbReader.getIas();
		assertTrue(iasOpt.isPresent());
		IasDao ias = iasOpt.get();
		
		assertEquals(LogLevelDao.INFO,ias.getLogLevel());
		assertEquals(5,ias.getRefreshRate());
		assertEquals(1,ias.getTolerance());
		assertEquals(10,ias.getHbFrequency());
		assertEquals("127.0.0.1:9092",ias.getBsdbUrl());
		
		Set<PropertyDao> props = ias.getProps();
		assertEquals(2,props.size());
		props.forEach( p -> {
			if (p.getName().equals("PropName1")) {
				assertEquals("PropValue1",p.getValue());
			} else if (p.getName().equals("PropName2")) {
				assertEquals("PropValue2",p.getValue());
			} 
		});
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
		dasu1.setLogLevel(LogLevelDao.FATAL);
		IasioDao dasuOutIasio1 = new IasioDao("DASU_OUTPUT1", "desc-dasu-out", IasTypeDao.ALARM,"http://www.eso.org");
		dasu1.setOutput(dasuOutIasio1);
		
		DasuDao dasu2 = new DasuDao();
		dasu2.setId("DasuID2");
		dasu1.setLogLevel(LogLevelDao.WARN);
		IasioDao dasuOutIasio2 = new IasioDao("DASU_OUTPUT2", "desc-dasu-out", IasTypeDao.LONG,"http://www.eso.org");
		dasu2.setOutput(dasuOutIasio2);
		
		cdbWriter.writeIasio(dasuOutIasio1, false);
		cdbWriter.writeIasio(dasuOutIasio2, true);
		
		// DASUs must be in the CDB as well otherwise
		// included objects cannot be rebuilt.
		cdbWriter.writeDasu(dasu1);
		cdbWriter.writeDasu(dasu2);
		
		DasuToDeployDao dtd1 = new DasuToDeployDao(dasu1, null, null);
		DasuToDeployDao dtd2 = new DasuToDeployDao(dasu2, null, null);
		superv.addDasuToDeploy(dtd1);
		superv.addDasuToDeploy(dtd2);
		
		cdbWriter.writeSupervisor(superv);
		assertTrue(cdbFiles.getSuperivisorFilePath(superv.getId()).toFile().exists());
		
		Optional<SupervisorDao> optSuperv = cdbReader.getSupervisor(superv.getId());
		assertTrue( optSuperv.isPresent(),"Got an empty Supervisor!");
		assertEquals(superv, optSuperv.get(),"The Supervisors differ!");
		
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
		dasu.setLogLevel(LogLevelDao.FATAL);
		
		TransferFunctionDao tfDao = new TransferFunctionDao();
		tfDao.setClassName("org.eso.ias.tf.Threshold");
		tfDao.setImplLang(TFLanguageDao.SCALA);
		
		AsceDao asce1= new AsceDao();
		asce1.setId("ASCE1");
		asce1.setDasu(dasu);
		asce1.setTransferFunction(tfDao);
		IasioDao output1 = new IasioDao("OUT-ID1", "description", IasTypeDao.CHAR,"http://www.eso.org");
		asce1.setOutput(output1);
		
		TransferFunctionDao tfDao2 = new TransferFunctionDao();
		tfDao2.setClassName("org.eso.ias.tf.Min");
		tfDao2.setImplLang(TFLanguageDao.SCALA);
		
		AsceDao asce2= new AsceDao();
		asce2.setId("ASCE-2");
		asce2.setDasu(dasu);
		IasioDao output2 = new IasioDao("ID2", "description", IasTypeDao.DOUBLE,"http://www.eso.org");
		asce2.setOutput(output2);
		asce2.setTransferFunction(tfDao2);
		
		TransferFunctionDao tfDao3 = new TransferFunctionDao();
		tfDao3.setClassName("org.eso.ias.tf.Max");
		tfDao3.setImplLang(TFLanguageDao.JAVA);
		
		AsceDao asce3= new AsceDao();
		asce3.setId("ASCE-ID-3");
		asce3.setDasu(dasu);
		IasioDao output3 = new IasioDao("OUT-ID3", "desc3", IasTypeDao.SHORT,"http://www.eso.org");
		asce3.setOutput(output2);
		asce3.setTransferFunction(tfDao3);
		
		IasioDao dasuOutIasio = new IasioDao("DASU_OUTPUT", "desc-dasu-out", IasTypeDao.ALARM,"http://www.eso.org");
		dasu.setOutput(dasuOutIasio);
		
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
		cdbWriter.writeIasio(dasuOutIasio, true);
		
		cdbWriter.writeTransferFunction(tfDao);
		cdbWriter.writeTransferFunction(tfDao2);
		cdbWriter.writeTransferFunction(tfDao3);
		
		dasu.addAsce(asce1);
		dasu.addAsce(asce2);
		dasu.addAsce(asce3);
		
		// Write the DASU
		cdbWriter.writeDasu(dasu);
		assertTrue(cdbFiles.getDasuFilePath(dasu.getId()).toFile().exists());
		
		// Read the DASU from CDB and compare with what we just wrote
		Optional<DasuDao> theDasuFromCdb = cdbReader.getDasu(dasu.getId());
		assertTrue(theDasuFromCdb.isPresent(),"Got a null DASU with ID "+dasu.getId()+" from CDB");
		assertEquals(dasu,theDasuFromCdb.get(),"The DASUs differ!");
		assertEquals(theDasuFromCdb.get().getOutput().getId(), dasuOutIasio.getId());
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

		IasioDao dasuOutIasio = new IasioDao("DASU_OUTPUT", "desc-dasu-out", IasTypeDao.ALARM,"http://www.eso.org");
		dasu.setOutput(dasuOutIasio);
		
		TransferFunctionDao tfDao1 = new TransferFunctionDao();
		tfDao1.setClassName("org.eso.ias.tf.Threshold");
		tfDao1.setImplLang(TFLanguageDao.JAVA);
		
		// The ASCE to test
		AsceDao asce = new AsceDao();
		asce.setId("ASCE-ID-For-Testsing");
		asce.setDasu(dasu);
		asce.setTransferFunction(tfDao1);
		
		IasioDao output = new IasioDao("OUTPUT-ID", "One IASIO in output", IasTypeDao.BYTE,"http://www.eso.org");
		asce.setOutput(output);
		
		// A set of inputs (one for each possible type
		for (int t=0; t<IasTypeDao.values().length; t++) {
			IasioDao input = new IasioDao("INPUT-ID"+t, "IASIO "+t+" in input", IasTypeDao.values()[t],"http://www.eso.org");
			asce.addInput(input, true);
			cdbWriter.writeIasio(input, true);
		}
		assertEquals(IasTypeDao.values().length,asce.getInputs().size(),"Not all the inputs have bene added to the ASCE");
		
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
		cdbWriter.writeIasio(dasuOutIasio, true);
		cdbWriter.writeSupervisor(superv);
		cdbWriter.writeDasu(dasu);
		cdbWriter.writeTransferFunction(tfDao1);
		
		
		cdbWriter.writeAsce(asce);
		assertTrue(cdbFiles.getAsceFilePath(asce.getId()).toFile().exists());
		
		Optional<AsceDao> optAsce = cdbReader.getAsce(asce.getId());
		assertTrue(optAsce.isPresent(),"Got a null ASCE with ID "+asce.getId()+" from CDB");
		assertEquals(asce.getOutput(),optAsce.get().getOutput());
		assertEquals(asce,optAsce.get(),"The ASCEs differ!");
	}

	/**
	 * Check if the writing of one IASIO at a time works
	 * 
	 * @throws Exception
	 */
	@Test
	public void testWriteIasio() throws Exception {
		// Is the default value saved for IasioDao#canShelve?
		IasioDao iasioDefaultShelve = new IasioDao("ioID2", "IASIO description", IasTypeDao.ALARM,"http://www.eso.org");
		cdbWriter.writeIasio(iasioDefaultShelve, false);
		Optional<IasioDao> optIasioDefShelve = cdbReader.getIasio(iasioDefaultShelve.getId());
		assertTrue(optIasioDefShelve.isPresent(),"Got an empty IASIO!");
		assertEquals(iasioDefaultShelve, optIasioDefShelve.get(),"The IASIOs differ!");
		assertEquals(IasioDao.canSheveDefault,optIasioDefShelve.get().isCanShelve());
		
		IasioDao iasio = new IasioDao(
				"ioID", 
				"IASIO description", 
				IasTypeDao.ALARM,"http://wiki.alma.cl/ioID",
				true,
				"templateID");
		cdbWriter.writeIasio(iasio, false);
		
		assertTrue(cdbFiles.getIasioFilePath(iasio.getId()).toFile().exists());
		
		Optional<IasioDao> optIasio = cdbReader.getIasio(iasio.getId());
		assertTrue( optIasio.isPresent(),"Got an empty IASIO!");
		assertEquals(iasio, optIasio.get(),"The IASIOs differ!");
		
		// Check if there is one and only one IASIO in the CDB
		Optional<Set<IasioDao>> optSet = cdbReader.getIasios();
		assertTrue( optSet.isPresent(),"Got an empty set of IASIOs!");
		assertEquals(1,optSet.get().size(),"Size of set mismatch");

		// Append another IASIO
		IasioDao iasio2 = new IasioDao("ioID2", "Another IASIO", IasTypeDao.BOOLEAN,null);
		cdbWriter.writeIasio(iasio2, true);
		Optional<Set<IasioDao>> optSet2 = cdbReader.getIasios();
		assertTrue(optSet2.isPresent(),"Got an empty set of IASIOs!");
		assertEquals(2,optSet2.get().size(),"Size of set mismatch");
		
		// Is the second IASIO ok?
		Optional<IasioDao> optIasio2 = cdbReader.getIasio(iasio2.getId());
		assertTrue(optIasio2.isPresent(),"Got an empty IASIO!");
		assertEquals(iasio2, optIasio2.get(),"The IASIOs differ!");
		
		// Check if updating a IASIO replaces the old one
		iasio.setShortDesc("A new Descripton");
		cdbWriter.writeIasio(iasio, true);
		// There must be still 2 IASIOs in the CDB
		optSet = cdbReader.getIasios();
		assertTrue( optSet.isPresent(),"Got an empty set of IASIOs!");
		assertEquals(2,optSet.get().size(),"Size of set mismatch");
		// Check if the IASIO in the CDB has been updated
		optIasio = cdbReader.getIasio(iasio.getId());
		assertTrue( optIasio.isPresent(),"Got an empty IASIO!");
		assertEquals(iasio, optIasio.get(),"The IASIOs differ!");
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
			IasioDao iasio = new IasioDao("iasioID-"+t, "IASIO description "+t, IasTypeDao.values()[t],"http://www.eso.org");
			set.add(iasio);
		}
		cdbWriter.writeIasios(set, false);
		assertTrue(cdbFiles.getIasioFilePath("PlaceHolderID").toFile().exists());
		
		Optional<Set<IasioDao>> optSet = cdbReader.getIasios();
		assertTrue( optSet.isPresent(),"Got an empty set of IASIOs!");
		assertEquals(set.size(),optSet.get().size(),"Size of set mismatch");
		
		// Check if appending works
		Set<IasioDao> set2 = new HashSet<>();
		for (int t=0; t<6; t++) {
			IasioDao iasio = new IasioDao("2ndset-iasioID-"+t, "IASIO descr "+t*2, IasTypeDao.values()[t],"http://www.eso.org");
			set2.add(iasio);
		}
		cdbWriter.writeIasios(set2, true);
		Optional<Set<IasioDao>> optSet2 = cdbReader.getIasios();
		assertTrue(optSet2.isPresent(),"Got an empty set of IASIOs!");
		assertEquals(set.size()+set2.size(),optSet2.get().size(),"Size of set mismatch");
		
		// Update the first iasios
		set.clear();
		for (int t=0; t<5; t++) {
			IasioDao iasio = new IasioDao("iasioID-"+t, "IASIO "+t, IasTypeDao.values()[t],"http://www.eso.org");
			set.add(iasio);
		}
		cdbWriter.writeIasios(set, true);
		// Size must be the same
		Optional<Set<IasioDao>> optSet3 = cdbReader.getIasios();
		assertTrue(optSet3.isPresent(),"Got an empty set of IASIOs!");
		assertEquals(set.size()+set2.size(),optSet3.get().size(),"Size of set mismatch");
		
		// Check if all IASIOs match with the ones in the sets
		set.stream().forEach(x -> assertTrue(optSet3.get().contains(x)));
		set2.stream().forEach(x -> assertTrue(optSet3.get().contains(x)));
		
		// Finally check if saving without appending really remove the right IASIOs
		// by writing again the IASIOs in set
		cdbWriter.writeIasios(set, false);
		// Size must be the same
		Optional<Set<IasioDao>> optSet4 = cdbReader.getIasios();
		assertTrue(optSet4.isPresent(),"Got an empty set of IASIOs!");
		assertEquals(set.size(),optSet4.get().size(),"Size of set mismatch");
		
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
		Set<DasuToDeployDao> dasus = cdbReader.getDasusToDeployInSupervisor("Supervisor-ID2");
		assertTrue(dasus.isEmpty());
		// Get the DASUs of a Supervisor that has one
		dasus = cdbReader.getDasusToDeployInSupervisor("Supervisor-ID1");
		assertEquals(1,dasus.size());
		Iterator<DasuToDeployDao> iter = dasus.iterator();
		DasuToDeployDao dtd = iter.next();
		assertEquals("DasuID1",dtd.getDasu().getId());
		// Get the DASUs of a Supervisor that has three
		dasus = cdbReader.getDasusToDeployInSupervisor("Supervisor-ID3");
		assertEquals(3,dasus.size());
		Set<String> dasuIds = dasus.stream().map(d -> d.getDasu().getId()).collect(Collectors.toSet());
		assertEquals(3,dasuIds.size());
		dasus.forEach( d -> assertTrue(d.getDasu().getId().equals("DasuID2") || d.getDasu().getId().equals("DasuID3")||d.getDasu().getId().equals("DasuID4")));
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
		
		// Check the transfer functions
		for (AsceDao asce: asces) {
			String asceId = asce.getId();
			TransferFunctionDao tfDao = asce.getTransferFunction();
			assertNotNull(tfDao);
			String className = tfDao.getClassName();
			assertNotNull(className);
			TFLanguageDao language = tfDao.getImplLang();
			assertNotNull(language);
			if (asceId.equals("ASCE-ID2") || asceId.equals("ASCE-ID3")) {
				assertEquals("org.eso.ias.tf.AnotherFunction",className);
				assertEquals(TFLanguageDao.SCALA, language);
			} else if (asceId.equals("ASCE-ID4")) {
				assertEquals("org.eso.ias.tf.TestMinMax",className);
				assertEquals(TFLanguageDao.JAVA, language);
			}
		}
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
	
	/**
	 * Test the getting of Supervisor that deploys templated DASUs
	 * <P>
	 * This test runs against the JSON CDB contained in testCdb and
	 * gets Supervisor-ID4.
	 * 
	 * @throws Exception
	 */
	@Test
	public void testGetSupervWithTemplatedDASUs() throws Exception {
		Path path = FileSystems.getDefault().getPath("./testCdb");
		cdbFiles = new CdbJsonFiles(path);
		cdbReader = new JsonReader(cdbFiles);
		
		Optional<SupervisorDao> superv4 = cdbReader.getSupervisor("Supervisor-ID4");
		assert(superv4.isPresent());
		SupervisorDao superv = superv4.get();
		assertEquals(2,superv.getDasusToDeploy().size());
		Map<String , DasuToDeployDao> dasusToDeploy= new HashMap<>();
		for (DasuToDeployDao dtd: superv.getDasusToDeploy()) {
			dasusToDeploy.put(dtd.getDasu().getId(), dtd);
		}
		DasuToDeployDao dtd5 = dasusToDeploy.get("DasuID5");
		assertNotNull(dtd5);
		assertEquals("template1-ID",dtd5.getTemplate().getId());
		assertEquals(3, dtd5.getInstance().intValue());
		DasuToDeployDao dtd6 = dasusToDeploy.get("DasuID6");
		assertNotNull(dtd6);
		assertEquals("template3-ID",dtd6.getTemplate().getId());
		assertEquals(5, dtd6.getInstance().intValue());
	}
	
	/**
	 * Test the writing and reading of the transfer function
	 * 
	 * @throws Exception
	 */
	@Test
	public void testWriteTansferFunction() throws Exception {
		TransferFunctionDao tfDao1 = new TransferFunctionDao("org.eso.ias.tf.Test",TFLanguageDao.SCALA);
		TransferFunctionDao tfDao2 = new TransferFunctionDao("org.eso.ias.tf.MinMax",TFLanguageDao.JAVA);
		
		cdbWriter.writeTransferFunction(tfDao1);
		cdbWriter.writeTransferFunction(tfDao2);
		
		Optional<TransferFunctionDao> optTF1 = cdbReader.getTransferFunction(tfDao1.getClassName());
		assertTrue(optTF1.isPresent());
		assertEquals(tfDao1, optTF1.get());
		Optional<TransferFunctionDao> optTF2 = cdbReader.getTransferFunction(tfDao2.getClassName());
		assertTrue(optTF2.isPresent());
		assertEquals(tfDao2, optTF2.get());
	}
	
	/**
	 * Test the retrieval of templates
	 * 
	 * @throws Exception
	 */
	@Test
	public void testGetTemplates() throws Exception {
		// Get templates from the CDB in testCdb
		Path cdbPath =  FileSystems.getDefault().getPath("testCdb");
		CdbFiles cdbFiles = new CdbJsonFiles(cdbPath);
		CdbReader jcdbReader = new JsonReader(cdbFiles);
		
		Optional<TemplateDao> template2 = jcdbReader.getTemplate("template2-ID");
		assertTrue(template2.isPresent());
		assertEquals(0,template2.get().getMin());
		assertEquals(10,template2.get().getMax());
		
		Optional<TemplateDao> template1 = jcdbReader.getTemplate("template1-ID");
		assertTrue(template1.isPresent());
		Optional<TemplateDao> template3 = jcdbReader.getTemplate("template3-ID");
		assertTrue(template3.isPresent());
	}
	
	/**
	 * Test the writing and reading of the template
	 * 
	 * @throws Exception
	 */
	@Test
	public void testWriteTemplate() throws Exception {
		TemplateDao tDao1 = new TemplateDao("tID1",3,9);
		TemplateDao tDao2 = new TemplateDao("tID2",1,25);
		
		cdbWriter.writeTemplate(tDao1);
		cdbWriter.writeTemplate(tDao2);
		
		Optional<TemplateDao> optT1 = cdbReader.getTemplate(tDao1.getId());
		assertTrue(optT1.isPresent());
		assertEquals(tDao1, optT1.get());
		Optional<TemplateDao> optT2 = cdbReader.getTemplate(tDao2.getId());
		assertTrue(optT2.isPresent());
		assertEquals(tDao2, optT2.get());
	}

}
