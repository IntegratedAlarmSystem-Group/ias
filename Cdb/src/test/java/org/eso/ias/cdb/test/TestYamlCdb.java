package org.eso.ias.cdb.test;

import org.eso.ias.cdb.CdbReader;
import org.eso.ias.cdb.CdbWriter;
import org.eso.ias.cdb.structuredtext.TextFileType;
import org.eso.ias.cdb.pojos.*;
import org.eso.ias.cdb.structuredtext.CdbFiles;
import org.eso.ias.cdb.structuredtext.CdbFolders;
import org.eso.ias.cdb.structuredtext.CdbTxtFiles;
import org.eso.ias.cdb.structuredtext.StructuredTextWriter;
import org.eso.ias.cdb.structuredtext.StructuredTextReader;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test reading and writing of structured text CDB.
 * <P>
 * Some tests, write the CDB and then read data out of it.
 * <BR>Other tests instead uses the CDB contained in testYamlCdb
 * whose description is in testYamlCdb/ReadMe.txt
 *
 * This test is the same of the JSON test for YAML.
 * 
 * @author acaproni
 *
 */
public class TestYamlCdb {
	
	/**
	 * The files helper
	 */
	private CdbFiles cdbFiles;
	
	/**
	 * The files writer
	 */
	private CdbWriter cdbWriter;
	
	/**
	 * The files reader
	 */
	private CdbReader cdbReader;
	
	/**
	 * The parent folder is the actual folder
	 */
	public static final Path cdbParentPath =  FileSystems.getDefault().getPath(".");

	@BeforeEach
	public void setUp() throws Exception {
		// Remove any CDB folder if present
		CdbFoldersTestHelper.deleteFolder(CdbFolders.ROOT.getFolder(cdbParentPath));
		assertFalse(CdbFolders.ROOT.exists(cdbParentPath));
		
		cdbFiles = new CdbTxtFiles(cdbParentPath, TextFileType.YAML);
		assertNotNull(cdbFiles);
		cdbWriter = new StructuredTextWriter(cdbFiles);
		assertNotNull(cdbWriter);
		cdbReader = new StructuredTextReader(cdbFiles);
		assertNotNull(cdbReader);
		
		cdbReader.init();
		cdbWriter.init();
	}

	@AfterEach
	public void tearDown() throws Exception {
		CdbFoldersTestHelper.deleteFolder(CdbFolders.ROOT.getFolder(cdbParentPath));
		assertFalse(CdbFolders.ROOT.exists(cdbParentPath));
		
		cdbReader.shutdown();
		cdbWriter.shutdown();
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
		ias.setValidityThreshold(6);

		ias.setHbFrequency(5);

		ias.setBsdbUrl("bsdb-server:9092");
		ias.setSmtp("acaproni:inorpaca@another.smtp.org");

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
	 * Test the reading of ias.yaml
	 * <P>
	 * This test runs against the YAML CDB contained in testYamlCdb
	 *
	 * @throws Exception
	 */
	@Test
	public void testGetIasFromFile() throws Exception {
		Path path = FileSystems.getDefault().getPath("./src/test/testYamlCdb");
		cdbFiles = new CdbTxtFiles(path, TextFileType.YAML);
		cdbReader = new StructuredTextReader(cdbFiles);
		cdbReader.init();

		Optional<IasDao> iasOpt = cdbReader.getIas();
		assertTrue(iasOpt.isPresent());
		IasDao ias = iasOpt.get();

		assertEquals(LogLevelDao.INFO,ias.getLogLevel());
		assertEquals(5,ias.getRefreshRate());
		assertEquals(11,ias.getValidityThreshold());
		assertEquals(10,ias.getHbFrequency());
		assertEquals("127.0.0.1:9092",ias.getBsdbUrl());
		assertEquals("acaproni:pswd@smtp.test.org",ias.getSmtp());

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
		dasu1.setLogLevel(LogLevelDao.TRACE);
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
		dasu.setLogLevel(LogLevelDao.TRACE);

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

	    System.out.println("testWriteAsce...");

		SupervisorDao superv = new SupervisorDao();
		superv.setId("Supervisor-ID");
		superv.setHostName("almaias.eso.org");
		superv.setLogLevel(LogLevelDao.INFO);

		// The DASU that owns the ASCE to test
		DasuDao dasu = new DasuDao();
		dasu.setId("DasuID1");
		dasu.setLogLevel(LogLevelDao.TRACE);

		IasioDao dasuOutIasio = new IasioDao("DASU_OUTPUT", "desc-dasu-out", IasTypeDao.ALARM,"http://www.eso.org");
		dasu.setOutput(dasuOutIasio);

		TransferFunctionDao tfDao1 = new TransferFunctionDao();
		tfDao1.setClassName("org.eso.ias.tf.Threshold");
		tfDao1.setImplLang(TFLanguageDao.JAVA);

		// The template of the ASCE
		TemplateDao templateForAsce = new TemplateDao("TemplateID",2,7);

		// The template for templated inputs
        TemplateDao templateForTemplatedInputs = new TemplateDao("TemplateForTempInputsID",1,3);

		// The ASCE to test
		AsceDao asce = new AsceDao();
		asce.setId("ASCE-ID-For-Testing");
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

		// Templated INPUT definition in IASIO
        IasioDao templatedInputIasio = new IasioDao(
                "TEMPL-INPUT-ID",
                "Templated IASIO",
                IasTypeDao.ALARM,
                "http://www.eso.org");
        templatedInputIasio.setTemplateId("TemplateForTempInputsID");
        cdbWriter.writeIasio(templatedInputIasio,true);

		// Adds 2 template inputs
        TemplateInstanceIasioDao templatedInput1 = new TemplateInstanceIasioDao();
        templatedInput1.setIasio(templatedInputIasio);
        templatedInput1.setTemplateId("TemplateForTempInputsID");
        templatedInput1.setInstance(1);
        asce.addTemplatedInstanceInput(templatedInput1,true);

        TemplateInstanceIasioDao templatedInput2 = new TemplateInstanceIasioDao();
        templatedInput2.setIasio(templatedInputIasio);
        templatedInput2.setTemplateId("TemplateForTempInputsID");
        templatedInput2.setInstance(3);
        asce.addTemplatedInstanceInput(templatedInput2,true);


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
		cdbWriter.writeTemplate(templateForAsce);
        cdbWriter.writeTemplate(templateForTemplatedInputs);

		cdbWriter.writeAsce(asce);
		assertTrue(cdbFiles.getAsceFilePath(asce.getId()).toFile().exists());

		Optional<AsceDao> optAsce = cdbReader.getAsce(asce.getId());
		assertTrue(optAsce.isPresent(),"Got a null ASCE with ID "+asce.getId()+" from CDB");
		assertEquals(asce.getOutput(),optAsce.get().getOutput());
		assertEquals(asce,optAsce.get(),"The ASCEs differ!");

		assertEquals(2,optAsce.get().getTemplatedInstanceInputs().size());

        System.out.println("testWriteAsce done.");
	}

	/**
	 * Check if the writing of one IASIO at a time works
	 *
	 * @throws Exception
	 */
	@Test
	public void testWriteIasio() throws Exception {
		// Is the default value saved for IasioDao#canShelve?
		IasioDao iasioDefaultShelve = new IasioDao(
				"ioID2",
				"IASIO description",
				IasTypeDao.ALARM,
				"http://www.eso.org");

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
				"templateID",
				SoundTypeDao.TYPE2,
				"addr1@eso.org; addr@alma.cl");
		cdbWriter.writeIasio(iasio, false);

		System.out.println(iasio.toString());
		System.out.println(iasio.isCanShelve());

		assertTrue(cdbFiles.getIasioFilePath(iasio.getId()).toFile().exists());

		Optional<IasioDao> optIasio = cdbReader.getIasio(iasio.getId());
		assertTrue( optIasio.isPresent(),"Got an empty IASIO!");
		assertTrue(optIasio.get().isCanShelve(),"Should be possible to shelve");
		assertEquals(iasio, optIasio.get(),"The IASIOs differ!");

		// Check if there is one and only one IASIO in the CDB
		Optional<Set<IasioDao>> optSet = cdbReader.getIasios();
		assertTrue( optSet.isPresent(),"Got an empty set of IASIOs!");
		assertEquals(1,optSet.get().size(),"Size of set mismatch");

		// Append another IASIO
		IasioDao iasio2 = new IasioDao("ioID2", "Another IASIO", IasTypeDao.BOOLEAN,null);
		iasio2.setSound(SoundTypeDao.TYPE3);
		iasio2.setEmails("emailaddr@site.com");
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
		System.out.println("===+++===> "+iasio);
		cdbWriter.writeIasio(iasio, true);
		// There must be still 2 IASIOs in the CDB
		optSet = cdbReader.getIasios();
		assertTrue( optSet.isPresent(),"Got an empty set of IASIOs!");

		for (IasioDao i: optSet.get()) {
			System.out.println("==> "+i);
		}

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
			iasio.setSound(SoundTypeDao.TYPE1);
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
			iasio.setSound(SoundTypeDao.TYPE3);
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
	 * This test runs against the CDB contained in testYamlCdb
	 *
	 * @throws Exception
	 */
	@Test
	public void testGetDasusOfSupervisor() throws Exception {
		Path path = FileSystems.getDefault().getPath("./src/test/testYamlCdb");
		cdbFiles = new CdbTxtFiles(path, TextFileType.YAML);
		cdbReader = new StructuredTextReader(cdbFiles);
		cdbReader.init();
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
	 * This test runs against the CDB contained in testYamlCdb
	 *
	 * @throws Exception
	 */
	@Test
	public void testGetAscesOfDasu() throws Exception {
		Path path = FileSystems.getDefault().getPath("./src/test/testYamlCdb");
		cdbFiles = new CdbTxtFiles(path, TextFileType.YAML);
		cdbReader = new StructuredTextReader(cdbFiles);
		cdbReader.init();

		// Get the ASCE of DasuID1 that has no ASCE
		Set<AsceDao> asces = cdbReader.getAscesForDasu("DasuID1");
		assertTrue(asces.isEmpty());
		// Get the ASCEs of DasuID2 that contains ASCE-ID1
		asces = cdbReader.getAscesForDasu("DasuID2");
		assertEquals(2, asces.size());
		asces.forEach( asce -> assertTrue("ASCE-ID1".equals(asce.getId()) || "ASCE-WITH-TEMPLATED-INPUTS".equals(asce.getId())));

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
	 * This test runs against the CDB contained in testYamlCdb
	 *
	 * @throws Exception
	 */
	@Test
	public void testGetIasiosOfAsce() throws Exception {
		Path path = FileSystems.getDefault().getPath("./src/test/testYamlCdb");
		cdbFiles = new CdbTxtFiles(path, TextFileType.YAML);
		cdbReader = new StructuredTextReader(cdbFiles);
		cdbReader.init();

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
	 * This test runs against the CDB contained in testYamlCdb and
	 * gets Supervisor-ID4.
	 *
	 * @throws Exception
	 */
	@Test
	public void testGetSupervWithTemplatedDASUs() throws Exception {
		Path path = FileSystems.getDefault().getPath("./src/test/testYamlCdb");
		cdbFiles = new CdbTxtFiles(path, TextFileType.YAML);
		cdbReader = new StructuredTextReader(cdbFiles);
		cdbReader.init();

		Optional<SupervisorDao> superv4 = cdbReader.getSupervisor("Supervisor-ID4");
		assertTrue(superv4.isPresent());
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
		// Get templates from the CDB in testYamlCdb
		Path cdbPath =  FileSystems.getDefault().getPath("src/test/testYamlCdb");
		CdbFiles cdbFiles = new CdbTxtFiles(cdbPath, TextFileType.YAML);
		CdbReader jcdbReader = new StructuredTextReader(cdbFiles);
		jcdbReader.init();

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

    /**
     * Test the getter if IDs of Supervisors
     * @throws Exception
     */
    @Test
    public void testGetIdsOfSupervisor() throws Exception {
        Path path = FileSystems.getDefault().getPath("./src/test/testYamlCdb");
        cdbFiles = new CdbTxtFiles(path, TextFileType.YAML);
        cdbReader = new StructuredTextReader(cdbFiles);
        cdbReader.init();

        Optional<Set<String>> idsOpt= cdbReader.getSupervisorIds();
        assertTrue(idsOpt.isPresent());
        Set<String> ids = idsOpt.get();
        assertEquals(4,ids.size());
        for (int i=1; i<=ids.size(); i++) assertTrue(ids.contains("Supervisor-ID"+i));
    }


    /**
     * Test the getter if IDs of DASUs
     * @throws Exception
     */
    @Test
    public void testGetIdsOfDasus() throws Exception {
        Path path = FileSystems.getDefault().getPath("./src/test/testYamlCdb");
        cdbFiles = new CdbTxtFiles(path, TextFileType.YAML);
        cdbReader = new StructuredTextReader(cdbFiles);
        cdbReader.init();

        Optional<Set<String>> idsOpt= cdbReader.getDasuIds();
        assertTrue(idsOpt.isPresent());
        Set<String> ids = idsOpt.get();
        assertEquals(6,ids.size());
        for (int i=1; i<=ids.size(); i++) assertTrue(ids.contains("DasuID"+i));
    }

    /**
     * Test the getter if IDs of ASCEs
     * @throws Exception
     */
    @Test
    public void testGetIdsOfAsces() throws Exception {
        Path path = FileSystems.getDefault().getPath("./src/test/testYamlCdb");
        cdbFiles = new CdbTxtFiles(path, TextFileType.YAML);
        cdbReader = new StructuredTextReader(cdbFiles);
        cdbReader.init();

        Optional<Set<String>> idsOpt= cdbReader.getAsceIds();
        assertTrue(idsOpt.isPresent());
        Set<String> ids = idsOpt.get();
        assertEquals(8,ids.size());
        for (int i=1; i<=6; i++) assertTrue(ids.contains("ASCE-ID"+i));
		assertTrue(ids.contains("ASCE-WITH-TEMPLATED-INPUTS"));
		assertTrue(ids.contains("ASCE-WITH-TEMPLATED-INPUTS-ONLY"));

    }

    /**
     * Test the getting of templated inputs of an ASCE
     * <P>
     * This test runs against the CDB contained in testYamlCdb
     *
     * @throws Exception
     */
    @Test
    public void testTemplatedInputsOfAsce() throws Exception {
        Path path = FileSystems.getDefault().getPath("./src/test/testYamlCdb");
        cdbFiles = new CdbTxtFiles(path, TextFileType.YAML);
        cdbReader = new StructuredTextReader(cdbFiles);
        cdbReader.init();

        // Get on ASCE without templated inputs
        Optional<AsceDao> asce = cdbReader.getAsce("ASCE-ID4");
        assertTrue(asce.isPresent());
        assertTrue(asce.get().getTemplatedInstanceInputs().isEmpty());

        // Get one ASCE with templated inputs
        asce = cdbReader.getAsce("ASCE-WITH-TEMPLATED-INPUTS");
        assertTrue(asce.isPresent());

        Set<TemplateInstanceIasioDao> templInstances= asce.get().getTemplatedInstanceInputs();
        assertEquals(3,templInstances.size());

        templInstances.forEach( ti -> {
            assertEquals("templated-inputs",ti.getTemplateId());
            assertEquals("TempInput",ti.getIasio().getId());
            assertTrue(ti.getInstance()>=3 && ti.getInstance()<=5);
        });
    }

    /**
     * Test the ASCE with templated inputs
     *
     * @throws Exception
     */
	@Test
	public void testGetAsceWithTemplatedInputs() throws Exception {
		Path path = FileSystems.getDefault().getPath("./src/test/testYamlCdb");
		cdbFiles = new CdbTxtFiles(path, TextFileType.YAML);
		cdbReader = new StructuredTextReader(cdbFiles);
		cdbReader.init();

		Optional<AsceDao> asceWithTemplatedInputs = cdbReader.getAsce("ASCE-WITH-TEMPLATED-INPUTS");
		assertTrue(asceWithTemplatedInputs.isPresent());
		assertEquals(3,asceWithTemplatedInputs.get().getTemplatedInstanceInputs().size());
		assertEquals(2,asceWithTemplatedInputs.get().getInputs().size());
        Optional<AsceDao> asceWithTemplatedInputsOnly = cdbReader.getAsce("ASCE-WITH-TEMPLATED-INPUTS-ONLY");
        assertTrue(asceWithTemplatedInputsOnly.isPresent());
        assertTrue(asceWithTemplatedInputsOnly.get().getInputs().isEmpty());
        assertEquals(3,asceWithTemplatedInputsOnly.get().getTemplatedInstanceInputs().size());
	}

	/**
     * Test the getting of an IASIO form file
     *
     * @throws Exception
     */
	@Test
	public void testGetIasio() throws Exception {
		Path path = FileSystems.getDefault().getPath("./src/test/testYamlCdb");
		cdbFiles = new CdbTxtFiles(path, TextFileType.YAML);
		cdbReader = new StructuredTextReader(cdbFiles);
		cdbReader.init();

		Optional<IasioDao> iDao=cdbReader.getIasio("SoundInput");
		assertTrue(iDao.isPresent());
		assertEquals(iDao.get().getSound(),SoundTypeDao.TYPE2);
		assertTrue(iDao.get().isCanShelve());
	}

	/**
	 * Test getting the PluginConfig from a file
	 * @throws Exception
	 */
	@Test
	public void testGetPlugin() throws Exception {
		Path path = FileSystems.getDefault().getPath("./src/test/testYamlCdb");
		cdbFiles = new CdbTxtFiles(path, TextFileType.YAML);
		cdbReader = new StructuredTextReader(cdbFiles);
		cdbReader.init();

		String pluginId = "PluginIDForTesting";

		Optional<PluginConfigDao> pCOnfDao = cdbReader.getPlugin(pluginId);
		assertTrue(pCOnfDao.isPresent());
		PluginConfigDao pConf = pCOnfDao.get();
		System.out.println("PluginConfig:\n"+pConf.toString());
		assertEquals("ACS",pConf.getMonitoredSystemId());

		Map<String,String> props = new HashMap<>();
		for (PropertyDao p: pConf.getProps()) {
			props.put(p.getName(),p.getValue());
		}
		assertEquals(2,props.size());
		assertEquals("itsValue",props.get("a-key"));
		assertEquals("AnotherValue",props.get("Anotherkey"));

		Map<String,ValueDao> values = new HashMap<>();
		for (ValueDao v:pConf.getValues() ) {
			values.put(v.getId(),v);

		}
		assertEquals(2, values.size());
		ValueDao v1 = values.get("AlarmID");
		assertNotNull(v1);
		assertEquals(500,v1.getRefreshTime());
		assertEquals("TheFilter",v1.getFilter());
		assertEquals("Options",v1.getFilterOptions());
		ValueDao v2 = values.get("TempID");
		assertEquals(1500,v2.getRefreshTime());
		assertEquals("Average",v2.getFilter());
		assertEquals("1, 150, 5",v2.getFilterOptions());
	}


	 /**
	 * Test the writing and reading of the configuration of clients
	 * @throws Exception
	 */
	@Test
	public void testClientConfig() throws Exception {

		ClientConfigDao configDao1 = new ClientConfigDao("ID1", "Configuration 1");
		ClientConfigDao configDao2 = new ClientConfigDao("ID2", "Configuration 2");

		cdbWriter.writeClientConfig(configDao1);
		cdbWriter.writeClientConfig(configDao2);

		Optional<ClientConfigDao> c1 = cdbReader.getClientConfig("ID1");
		assertTrue(c1.isPresent());
		System.out.println("configDao1="+configDao1.toString());
		System.out.println("c1="+c1.get().toString());
		assertEquals(configDao1,c1.get());
		Optional<ClientConfigDao> c2 = cdbReader.getClientConfig("ID2");
		assertTrue(c2.isPresent());
		assertEquals(configDao2,c2.get());

		// Now try with a big config
		StringBuilder builder = new StringBuilder();
		for (int t=0; builder.length()<100000; t++) {
			builder.append(t);
			builder.append (' ');
		}
		ClientConfigDao longConfig = new ClientConfigDao("LongConfigId", builder.toString());
		cdbWriter.writeClientConfig(longConfig);

		Optional<ClientConfigDao> c3 = cdbReader.getClientConfig("LongConfigId");
		assertTrue(c3.isPresent());
		assertEquals(longConfig,c3.get());
	}

	/**
	 * Test reading and writing of plugin configurations
	 *
	 * @throws Exception
	 */
	@Test
	public void testWritePluginConfig() throws Exception {
		PluginConfigDao pConf = new PluginConfigDao();
		pConf.setId("GeneratedPluginConfig");
		pConf.setDefaultFilter("Mean");
		pConf.setDefaultFilterOptions("100");
		pConf.setMonitoredSystemId("MSys");

		PropertyDao prop = new PropertyDao();
		prop.setName("pName");
		prop.setValue("pVal");
		Set<PropertyDao> arrayProps = pConf.getProps();
		arrayProps.add(prop);

		ValueDao v1 = new ValueDao();
		v1.setId("V1");
		v1.setRefreshTime(1250);

		ValueDao v2 = new ValueDao();
		v2.setId("Value-2");
		v2.setRefreshTime(750);
		v2.setFilter("Avg");
		v2.setFilterOptions("10");
		Set<ValueDao> values = new HashSet<>();
		values.add(v1);
		values.add(v2);
		pConf.setValues(values);

		cdbWriter.writePluginConfig(pConf);
		assertTrue(cdbFiles.getPluginFilePath(pConf.getId()).toFile().exists(),"Plugin "+pConf.getId()+" configuration file does NOT exist");


		Optional<PluginConfigDao> pConfFromCdb = cdbReader.getPlugin(pConf.getId());
		assertTrue(pConfFromCdb.isPresent());

		assertEquals(pConf,pConfFromCdb.get());
	}

	/**
	 * Test {@link StructuredTextReader#getPluginIds()}
	 * @throws Exception
	 */
	@Test
	public void testGetPluginIds() throws Exception {
		Path path = FileSystems.getDefault().getPath("./src/test/testYamlCdb");
		cdbFiles = new CdbTxtFiles(path, TextFileType.YAML);
		cdbReader = new StructuredTextReader(cdbFiles);
		cdbReader.init();
		Optional<Set<String>> idsOpt = cdbReader.getPluginIds();
		assertTrue(idsOpt.isPresent());
		assertEquals(2,idsOpt.get().size());
		assertTrue(idsOpt.get().contains("PluginIDForTesting"));
		assertTrue(idsOpt.get().contains("PluginID2"));
		cdbReader.shutdown();
	}

	/**
	 * Test {@link StructuredTextReader#getClientIds()}
	 * @throws Exception
	 */
	@Test
	public void testGetClientIds() throws Exception {
		Path path = FileSystems.getDefault().getPath("./src/test/testYamlCdb");
		cdbFiles = new CdbTxtFiles(path, TextFileType.YAML);
		cdbReader = new StructuredTextReader(cdbFiles);
		cdbReader.init();
		Optional<Set<String>> idsOpt = cdbReader.getClientIds();
		assertTrue(idsOpt.isPresent());
		assertEquals(1,idsOpt.get().size());
		assertTrue(idsOpt.get().contains("test"));
		cdbReader.shutdown();
	}

}

