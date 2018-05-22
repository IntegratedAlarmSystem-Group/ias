
package org.eso.ias.cdb.test.rdb;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Collection;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import org.eso.ias.cdb.CdbReader;
import org.eso.ias.cdb.CdbWriter;
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
import org.eso.ias.cdb.rdb.RdbReader;
import org.eso.ias.cdb.rdb.RdbUtils;
import org.eso.ias.cdb.rdb.RdbWriter;
import org.eso.ias.cdb.test.json.TestJsonCdb;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Test reading and writing data from/to the relational database.
 * <P>
 * Reading and writing is done by {@link CdbReader} and {@link CdbWriter}
 * implementators as it is done in {@link TestJsonCdb} so, in principle the same
 * test can be run for text files and relational database. <BR>
 * The reason to have a separate test is because with hibernate there is no need
 * to explicitly store objects contained in other objects as needed by the CDB
 * on files. Actually, this test should be shorter and easier to read.
 * 
 * 
 * <EM>Note</em>: with the current implementation, running this test will clear
 * the content of the production database
 * 
 * @author acaproni
 *
 */
public class TestRdbCdb {

	/**
	 * Helper object to read and write the RDB
	 */
	private static final RdbUtils rdbUtils = RdbUtils.getRdbUtils();

	/**
	 * The reader for the CDB RDB
	 */
	private final CdbReader cdbReader = new RdbReader();

	/**
	 * The reader for the CDB RDB
	 */
	private final CdbWriter cdbWriter = new RdbWriter();

	/**
	 * The logger
	 */
	private final Logger logger = LoggerFactory.getLogger(TestRdbCdb.class);

	/**
	 * @throws java.lang.Exception
	 */
	@BeforeEach
	public void setUp() throws Exception {
		// Clear the content of the DB

		// Remove all the tables
		try {
			rdbUtils.dropTables();
		} catch (Throwable t) {
			System.out.println("Failure dropping tables. Was the RDB empty?");
			System.out.println("Error " + t.getMessage() + " ignored");
		}

		// The create empty tables
		rdbUtils.createTables();
	}

	/**
	 * @throws java.lang.Exception
	 */
	@AfterEach
	public void tearDown() throws Exception {
	}

	@AfterAll
	public static void logout() {
		rdbUtils.close();
	}

	/**
	 * Test reading and writing the IAS
	 */
	@Test
	public void testIas() throws Exception {
		logger.info("testIas");
		IasDao ias = new IasDao();
		ias.setLogLevel(LogLevelDao.DEBUG);

		PropertyDao p1 = new PropertyDao();
		p1.setName("P1-name");
		p1.setValue("1000");
		PropertyDao p2 = new PropertyDao();
		p2.setName("P2-name");
		p2.setValue("Name of P2");

		ias.getProps().add(p1);
		ias.getProps().add(p2);
		
		ias.setRefreshRate(4);
		ias.setTolerance(3);
		
		ias.setHbFrequency(5);
		
		ias.setBsdbUrl("localhost:9092");

		// Write the IAS
		cdbWriter.writeIas(ias);

		// Get the IAS from the reader
		Optional<IasDao> optIas = cdbReader.getIas();
		assertTrue( optIas.isPresent(),"Got an empty IAS!");
		assertEquals(ias, optIas.get(),"The IASs differ!");

		// Modify the IAS and save it again
		IasDao ias2 = optIas.get();
		ias2.setLogLevel(LogLevelDao.INFO);
		assertTrue(ias2.getProps().remove(p1),"Error removing a property from the IAS");
		
		ias2.setRefreshRate(5);
		ias2.setTolerance(2);
		ias2.setHbFrequency(10);
		ias.setBsdbUrl("bsdb-server:9092");
		cdbWriter.writeIas(ias2);

		// Get the IAS from the reader
		Optional<IasDao> optIas2 = cdbReader.getIas();
		assertTrue(optIas2.isPresent(),"Got an empty IAS!");
		assertEquals(ias2, optIas2.get(),"The IASs differ!");
		assertEquals(1, optIas2.get().getProps().size(),"Wrong number of properties");
	}

	/**
	 * Test reading and writing of Supervisor
	 * 
	 * @throws Exception
	 */
	@Test
	public void testSupervisor() throws Exception {
		logger.info("testSupervisor");
		
		TemplateDao tDao1 = new TemplateDao("tID1",3,9);
		TemplateDao tDao2 = new TemplateDao("tID2",1,25);
		cdbWriter.writeTemplate(tDao1);
		cdbWriter.writeTemplate(tDao2);
		
		SupervisorDao superv = new SupervisorDao();
		superv.setId("Supervisor-ID");
		superv.setHostName("almadev2.alma.cl");
		superv.setLogLevel(LogLevelDao.INFO);

		// Adds the DASU to deploy
		DasuDao dasu1 = new DasuDao();
		dasu1.setId("DasuID1");
		dasu1.setLogLevel(LogLevelDao.FATAL);
		DasuToDeployDao dtd1 = new DasuToDeployDao(dasu1, tDao1, 5);
		superv.addDasuToDeploy(dtd1);

		IasioDao dasuOut1 = new IasioDao("DASU-OUT-1", "descr", IasTypeDao.ALARM,"http://www.eso.org/1");
		cdbWriter.writeIasio(dasuOut1, true);
		dasu1.setOutput(dasuOut1);

		DasuDao dasu2 = new DasuDao();
		dasu2.setId("DasuID2");
		dasu2.setLogLevel(LogLevelDao.WARN);
		DasuToDeployDao dtd2 = new DasuToDeployDao(dasu2, tDao2, 1);
		superv.addDasuToDeploy(dtd2);

		IasioDao dasuOut2 = new IasioDao("DASU-OUT-2", "descr", IasTypeDao.DOUBLE,"http://www.eso.org/2");
		cdbWriter.writeIasio(dasuOut2, true);
		dasu2.setOutput(dasuOut2);

		cdbWriter.writeDasu(dasu1);
		cdbWriter.writeDasu(dasu2);
		cdbWriter.writeSupervisor(superv);
		
		Optional<SupervisorDao> optSuperv = cdbReader.getSupervisor(superv.getId());
		assertTrue(optSuperv.isPresent(),"Got an empty Supervisor!");
		assertEquals(superv, optSuperv.get(),"The Supervisors differ!");

		// Modify the supervisor then save it again
		superv.setHostName("almadev.hq.eso.org");
		superv.removeDasu(dasu2.getId());
		
		DasuDao dasu3 = new DasuDao();
		dasu3.setId("DasuID3");
		dasu3.setLogLevel(LogLevelDao.FATAL);
		DasuToDeployDao dtd3 = new DasuToDeployDao(dasu3, null, null);
		superv.addDasuToDeploy(dtd3);
		
		IasioDao dasuOut3 = new IasioDao("DASU-OUT-3", "descr", IasTypeDao.DOUBLE,"http://www.eso.org/3");
		cdbWriter.writeIasio(dasuOut3, true);
		dasu3.setOutput(dasuOut3);
		
		cdbWriter.writeDasu(dasu3);

		logger.info("Writing the modified supervisor {}",superv);
		cdbWriter.writeSupervisor(superv);

		// Check if it has been updated
		Optional<SupervisorDao> optSuperv2 = cdbReader.getSupervisor(superv.getId());
		assertTrue(optSuperv2.isPresent(),"Got an empty Supervisor!");
		assertEquals(superv, optSuperv2.get(),"The Supervisors differ!");
	}

	/**
	 * Test reading and writing of IASIO
	 * 
	 * @throws Exception
	 */
	@Test
	public void testIasio() throws Exception {
		logger.info("testIasio");
		IasioDao io = new IasioDao(
				"IO-ID", 
				"IASIO description", 
				IasTypeDao.INT,
				"http://www.eso.org",false,
				null);
		cdbWriter.writeIasio(io, true);

		Optional<IasioDao> iasioFromRdb = cdbReader.getIasio("IO-ID");
		assertTrue(iasioFromRdb.isPresent(),"Got an empty IASIO!");
		assertEquals(io, iasioFromRdb.get(),"The IASIOs differ!");
		
		// Is the default value saved for IasioDao#canShelve?
		IasioDao iasioDefaultShelve = new IasioDao("ioID2", "IASIO description", IasTypeDao.ALARM,"http://www.eso.org");
		cdbWriter.writeIasio(iasioDefaultShelve, false);
		Optional<IasioDao> optIasioDefShelve = cdbReader.getIasio(iasioDefaultShelve.getId());
		assertTrue(optIasioDefShelve.isPresent(),"Got an empty IASIO!");
		assertEquals(iasioDefaultShelve, optIasioDefShelve.get(),"The IASIOs differ!");
		assertEquals(IasioDao.canSheveDefault,optIasioDefShelve.get().isCanShelve());
	}
	
	/**
	 * Test reading and writing of IASIO
	 * 
	 * @throws Exception
	 */
	@Test
	public void testTemplatedIasio() throws Exception {
		logger.info("testTemplatedIasio");
		
		TemplateDao template = new TemplateDao("TemplateForTest", 5, 37);
		cdbWriter.writeTemplate(template);
		
		IasioDao io = new IasioDao(
				"T-IO-ID", 
				"IASIO template description", 
				IasTypeDao.ALARM,
				"http://www.eso.org",false,
				template.getId());
		cdbWriter.writeIasio(io, true);

		Optional<IasioDao> iasioFromRdb = cdbReader.getIasio("T-IO-ID");
		assertTrue(iasioFromRdb.isPresent(),"Got an empty templated IASIO!");
		assertEquals(io, iasioFromRdb.get(),"The IASIOs differ!");
		assertEquals(io.getTemplateId(),iasioFromRdb.get().getTemplateId());
	}
	
	/**
	 * Test reading and writing of a set of IASIOs
	 * 
	 * @throws Exception
	 */
	@Test
	public void testIasios() throws Exception {
		logger.info("testIasios");
		IasioDao io1 = new IasioDao("IO-ID1", "IASIO descr1", IasTypeDao.INT,"http://www.eso.org");
		IasioDao io2 = new IasioDao("IO-ID2", "IASIO descr2", IasTypeDao.ALARM,"http://www.eso.org");
		IasioDao io3 = new IasioDao("IO-ID3", "IASIO descr3", IasTypeDao.BOOLEAN,"http://www.eso.org");
		IasioDao io4 = new IasioDao("IO-ID4", "IASIO descr4", IasTypeDao.DOUBLE,"http://www.eso.org");
		IasioDao io5 = new IasioDao("IO-ID5", "IASIO descr5", IasTypeDao.STRING,"http://www.eso.org");
		Set<IasioDao> iasios = new HashSet<>();
		iasios.add(io1);
		iasios.add(io2);
		iasios.add(io3);
		iasios.add(io4);
		iasios.add(io5);

		cdbWriter.writeIasios(iasios, true);

		Optional<Set<IasioDao>> iasiosFromRdb = cdbReader.getIasios();
		assertTrue(iasiosFromRdb.isPresent(),"Got an empty set of IASIOs!");
		assertEquals(iasios.size(), iasiosFromRdb.get().size());
		assertEquals(iasios, iasiosFromRdb.get(),"The sets of IASIOs differ!");
	}

	/**
	 * Test reading and writing of DASU
	 * 
	 * @throws Exception
	 */
	@Test
	public void testDasu() throws Exception {
		logger.info("testDasu");
		// Test the reading/writing od a DASU with no ASCEs
		DasuDao dasuNoASCEs = new DasuDao();
		dasuNoASCEs.setId("A-DASU-For-Testing");
		dasuNoASCEs.setLogLevel(LogLevelDao.DEBUG);

		IasioDao dasuNoASCEsOut = new IasioDao("DASU-OUT-1", "descr", IasTypeDao.ALARM,"http://www.eso.org");
		cdbWriter.writeIasio(dasuNoASCEsOut, true);
		dasuNoASCEs.setOutput(dasuNoASCEsOut);

		cdbWriter.writeDasu(dasuNoASCEs);

		Optional<DasuDao> dasuFromRdb = cdbReader.getDasu("A-DASU-For-Testing");
		assertTrue(dasuFromRdb.isPresent(),"Got an empty DASU!");
		assertEquals(dasuNoASCEs, dasuFromRdb.get(),"The DASUs differ!");

		// Test the reading/writing of a DASU with some ASCEs
		DasuDao dasuWithASCEs = new DasuDao();
		dasuWithASCEs.setId("A-DASU-With-ASCEs");
		dasuWithASCEs.setLogLevel(LogLevelDao.WARN);

		IasioDao dasuWithASCEsOut = new IasioDao("DASU-OUT-1", "descr", IasTypeDao.ALARM,"http://www.eso.org");
		cdbWriter.writeIasio(dasuNoASCEsOut, true);
		dasuWithASCEs.setOutput(dasuWithASCEsOut);

		// Output of ASCE1
		IasioDao ioAsce1Out = new IasioDao("IASIO-OUT-2", "descr", IasTypeDao.DOUBLE,"http://www.eso.org");
		cdbWriter.writeIasio(ioAsce1Out, true);

		TransferFunctionDao tfDao = new TransferFunctionDao();
		tfDao.setClassName("org.eso.ias.tf.Threshold");
		tfDao.setImplLang(TFLanguageDao.SCALA);
		cdbWriter.writeTransferFunction(tfDao);

		// ASCE1
		AsceDao asce1 = new AsceDao();
		asce1.setTransferFunction(tfDao);
		asce1.setId("ASCE1-ID");
		asce1.setDasu(dasuWithASCEs);
		asce1.setOutput(ioAsce1Out);
		dasuWithASCEs.addAsce(asce1);

		// Output of ASCE2
		IasioDao ioAsce2Out = new IasioDao("IASIO-OUT-2", "descr", IasTypeDao.BOOLEAN,"http://www.eso.org");
		cdbWriter.writeIasio(ioAsce2Out, true);

		TransferFunctionDao tfDao2 = new TransferFunctionDao();
		tfDao2.setClassName("org.eso.ias.tf.Min");
		tfDao2.setImplLang(TFLanguageDao.JAVA);
		cdbWriter.writeTransferFunction(tfDao2);

		// ASCE2
		AsceDao asce2 = new AsceDao();
		asce2.setTransferFunction(tfDao2);
		asce2.setId("ASCE2-ID");
		asce2.setDasu(dasuWithASCEs);
		asce2.setOutput(ioAsce2Out);
		dasuWithASCEs.addAsce(asce2);

		cdbWriter.writeDasu(dasuWithASCEs);
		Optional<DasuDao> dasuWithAscesFromRdb = cdbReader.getDasu("A-DASU-With-ASCEs");
		assertTrue(dasuWithAscesFromRdb.isPresent(),"Got an empty DASU!");
		assertEquals(dasuWithASCEs.getOutput().getId(), dasuWithAscesFromRdb.get().getOutput().getId());
		assertEquals(dasuWithASCEs, dasuWithAscesFromRdb.get(),"The DASUs differ!");
		assertEquals(2, dasuWithAscesFromRdb.get().getAsces().size(),"The number of ASCEs in the DASU is wrong");
	}
	
	/**
	 * Test the transfer function
	 */
	@Test
	public void testTransferFunction() throws Exception {
		logger.info("testTransferFunction");
		
		TransferFunctionDao tfDao = new TransferFunctionDao();
		tfDao.setClassName("org.eso.ias.tranfer.functions.MinMaxThreshold");
		tfDao.setImplLang(TFLanguageDao.SCALA);
		
		cdbWriter.writeTransferFunction(tfDao);

		Optional<TransferFunctionDao> tfFromRdb = cdbReader.getTransferFunction(tfDao.getClassName());
		assertTrue(tfFromRdb.isPresent(),"Got an empty TF!");
		assertEquals(tfDao, tfFromRdb.get(),"The TFs differ!");
	}

	/**
	 * Test reading and writing of ASCE
	 * 
	 * @throws Exception
	 */
	@Test
	public void testAsce() throws Exception {
		logger.info("testAsce");
		// The supervisor where the DASU containing the ASCE runs

		// The DAUS where the ASCE runs
		DasuDao dasu = new DasuDao();
		dasu.setId("A-DASU-For-Testing");
		dasu.setLogLevel(LogLevelDao.DEBUG);

		IasioDao dasuOut = new IasioDao("DASU-OUT-1", "descr", IasTypeDao.ALARM,"http://www.eso.org");
		cdbWriter.writeIasio(dasuOut, true);
		dasu.setOutput(dasuOut);

		cdbWriter.writeDasu(dasu);
		

		Set<IasioDao> iasios = new HashSet<>();
		// The output of the ASCE
		IasioDao ioOut = new IasioDao("IASIO-OUT", "description of output", IasTypeDao.ALARM,"http://www.eso.org");
		cdbWriter.writeIasio(ioOut, true);

		// The 5 inputs of the ASCE
		IasioDao ioIn1 = new IasioDao("IASIO-IN1", "input-1", IasTypeDao.DOUBLE,"http://www.eso.org");
		iasios.add(ioIn1);
		IasioDao ioIn2 = new IasioDao("IASIO-IN2", "input-2", IasTypeDao.INT,"http://www.eso.org");
		iasios.add(ioIn2);
		IasioDao ioIn3 = new IasioDao("IASIO-IN3", "input-3", IasTypeDao.BOOLEAN,"http://www.eso.org");
		iasios.add(ioIn3);
		IasioDao ioIn4 = new IasioDao("IASIO-IN4", "input-4", IasTypeDao.ALARM,"http://www.eso.org");
		iasios.add(ioIn4);
		IasioDao ioIn5 = new IasioDao("IASIO-IN5", "input-5", IasTypeDao.STRING,"http://www.eso.org");
		iasios.add(ioIn5);

		cdbWriter.writeIasios(iasios, true);

		// The props of the ASCE
		PropertyDao p1 = new PropertyDao();
		p1.setName("Prop1-Name");
		p1.setValue("Prop1-value");
		PropertyDao p2 = new PropertyDao();
		p2.setName("Prop2-Name");
		p2.setValue("Prop2-value");

		TransferFunctionDao tfDao = new TransferFunctionDao();
		tfDao.setClassName("org.eso.ias.tf.Threshold");
		tfDao.setImplLang(TFLanguageDao.SCALA);
		cdbWriter.writeTransferFunction(tfDao);

		// The ASCE to test
		AsceDao asce = new AsceDao();
		asce.setTransferFunction(tfDao);
		asce.setId("ASCE-ID");
		asce.setDasu(dasu);
		asce.setOutput(ioOut);
		iasios.stream().forEach(io -> asce.addInput(io, true));
		asce.getProps().add(p1);
		asce.getProps().add(p2);
		dasu.addAsce(asce);

		cdbWriter.writeAsce(asce);

		Optional<AsceDao> asceFromRdb = cdbReader.getAsce("ASCE-ID");
		assertTrue( asceFromRdb.isPresent(),"Got an empty ASCE!");
		assertEquals(asce, asceFromRdb.get(),"The ASCEs differ!");
		assertEquals(iasios.size(), asceFromRdb.get().getInputs().size(),"The number of inputs of the ASCE differ!");
		assertEquals(2, asceFromRdb.get().getProps().size(),"The number of properties of the ASCE differ!");
	}

	/**
	 * Build the CDB with the same structure of that defined for the JSON
	 * implementation and described in testCdb/ReadMe.txt
	 * 
	 * @throws Exception
	 */
	private void buildCDB() throws Exception {
		logger.info("Building the CDB");
		// Prepare the CDB

		// First the IASIOs
		Set<IasioDao> iasios = new HashSet<>();
		IasioDao ioOut = new IasioDao("IASIO-OUT", "description of output", IasTypeDao.ALARM,"http://www.eso.org");
		iasios.add(ioOut);
		// The 5 inputs of the ASCEs
		IasioDao ioIn1 = new IasioDao("iasioID-1", "input-1", IasTypeDao.DOUBLE,"http://www.eso.org");
		iasios.add(ioIn1);
		IasioDao ioIn2 = new IasioDao("iasioID-2", "input-2", IasTypeDao.INT,"http://www.eso.org");
		iasios.add(ioIn2);
		IasioDao ioIn3 = new IasioDao("iasioID-3", "input-3", IasTypeDao.BOOLEAN,"http://www.eso.org");
		iasios.add(ioIn3);
		IasioDao ioIn4 = new IasioDao("iasioID-4", "input-4", IasTypeDao.ALARM,"http://www.eso.org");
		iasios.add(ioIn4);
		cdbWriter.writeIasios(iasios, true);

		// A DASU
		DasuDao dasu = new DasuDao();
		dasu.setId("DasuID1");
		dasu.setLogLevel(LogLevelDao.DEBUG);
		dasu.setOutput(ioOut);
		cdbWriter.writeDasu(dasu);
		
		DasuToDeployDao dtd = new DasuToDeployDao(dasu, null, null);
		
		// The supervisor with one DASU
		SupervisorDao superv1 = new SupervisorDao();
		superv1.setId("Supervisor-ID1");
		superv1.setHostName("almaias.hq.eso.org");
		superv1.setLogLevel(LogLevelDao.DEBUG);
		superv1.addDasuToDeploy(dtd);
		cdbWriter.writeSupervisor(superv1);

		
		
		// Another supervisor without DASU
		SupervisorDao superv2 = new SupervisorDao();
		superv2.setId("Supervisor-ID2");
		superv2.setHostName("almaias.hq.eso.org");
		superv2.setLogLevel(LogLevelDao.DEBUG);
		cdbWriter.writeSupervisor(superv2);

		// A DASU
		DasuDao dasu2 = new DasuDao();
		dasu2.setId("DasuID2");
		dasu2.setLogLevel(LogLevelDao.DEBUG);
		dasu2.setOutput(ioIn3);

		TransferFunctionDao tfDao1 = new TransferFunctionDao();
		tfDao1.setClassName("org.eso.ias.tf.Max");
		tfDao1.setImplLang(TFLanguageDao.JAVA);
		cdbWriter.writeTransferFunction(tfDao1);

		// The ASCE for DASU2
		AsceDao asce = new AsceDao();
		asce.setTransferFunction(tfDao1);
		asce.setId("ASCE-ID1");
		asce.setDasu(dasu);
		asce.setOutput(ioOut);
		asce.addInput(ioIn1, false);
		asce.addInput(ioIn2, false);
		dasu2.addAsce(asce);
		cdbWriter.writeDasu(dasu2);

		// A DASU
		DasuDao dasu3 = new DasuDao();
		dasu3.setId("DasuID3");
		dasu3.setLogLevel(LogLevelDao.DEBUG);
		dasu3.setOutput(ioIn3);

		TransferFunctionDao tfDao2 = new TransferFunctionDao();
		tfDao2.setClassName("org.eso.ias.tf.MinMax");
		tfDao2.setImplLang(TFLanguageDao.SCALA);
		cdbWriter.writeTransferFunction(tfDao2);

		// A ASCE for DasuID3
		AsceDao asce2 = new AsceDao();
		asce2.setTransferFunction(tfDao2);
		asce2.setId("ASCE-ID2");
		asce2.setDasu(dasu3);
		asce2.setOutput(ioOut);
		dasu3.addAsce(asce2);

		TransferFunctionDao tfDao3 = new TransferFunctionDao();
		tfDao3.setClassName("org.eso.ias.tf.MaxThreshold");
		tfDao3.setImplLang(TFLanguageDao.SCALA);
		cdbWriter.writeTransferFunction(tfDao3);

		// A ASCE for DasuID3
		AsceDao asce3 = new AsceDao();
		asce3.setTransferFunction(tfDao3);
		asce3.setId("ASCE-ID3");
		asce3.setDasu(dasu3);
		asce3.setOutput(ioOut);
		asce3.addInput(ioIn1, false);
		asce3.addInput(ioIn2, false);
		dasu3.addAsce(asce3);

		TransferFunctionDao tfDao4 = new TransferFunctionDao();
		tfDao4.setClassName("org.eso.ias.tf.AvgMax");
		tfDao4.setImplLang(TFLanguageDao.SCALA);
		cdbWriter.writeTransferFunction(tfDao4);

		// A ASCE for DasuID3
		AsceDao asce4 = new AsceDao();
		asce4.setTransferFunction(tfDao4);
		asce4.setId("ASCE-ID4");
		asce4.setDasu(dasu3);
		asce4.setOutput(ioOut);
		asce4.addInput(ioIn2, false);
		asce4.addInput(ioIn3, false);
		asce4.addInput(ioIn4, false);
		dasu3.addAsce(asce4);

		cdbWriter.writeDasu(dasu3);

		// A DASU
		DasuDao dasu4 = new DasuDao();
		dasu4.setId("DasuID4");
		dasu4.setLogLevel(LogLevelDao.DEBUG);
		dasu4.setOutput(ioIn4);
		cdbWriter.writeDasu(dasu4);
		
		DasuToDeployDao dtd2 = new DasuToDeployDao(dasu2, null, null);
		DasuToDeployDao dtd3 = new DasuToDeployDao(dasu3, null, null);
		DasuToDeployDao dtd4 = new DasuToDeployDao(dasu4, null, null);
		
		// Another supervisor without 3 DASUs
		SupervisorDao superv3 = new SupervisorDao();
		superv3.setId("Supervisor-ID3");
		superv3.setHostName("almaias.hq.eso.org");
		superv3.setLogLevel(LogLevelDao.DEBUG);
		superv3.addDasuToDeploy(dtd2);
		superv3.addDasuToDeploy(dtd3);
		superv3.addDasuToDeploy(dtd4);
		cdbWriter.writeSupervisor(superv3);
		

		logger.info("CDB built");
	}

	/**
	 * Test the getting of the DASU of a supervisor
	 */
	@Test
	public void testGetDasusOfSupervisor() throws Exception {
		logger.info("testGetDasusOfSupervisor");
		buildCDB();

		// The test starts below
		Set<DasuToDeployDao> dtds = cdbReader.getDasusToDeployInSupervisor("Supervisor-ID1");
		assertEquals(1, dtds.size());
		for (DasuToDeployDao dtd : dtds) {
			assertEquals("DasuID1", dtd.getDasu().getId());
		}

		dtds = cdbReader.getDasusToDeployInSupervisor("Supervisor-ID2");
		assertEquals(0, dtds.size());

		dtds = cdbReader.getDasusToDeployInSupervisor("Supervisor-ID3");
		assertEquals(3, dtds.size());
		for (DasuToDeployDao dtd : dtds) {
			assertTrue(
					dtd.getDasu().getId().equals("DasuID2") || dtd.getDasu().getId().equals("DasuID3") || dtd.getDasu().getId().equals("DasuID4"));
		}
	}

	/**
	 * Test the getting of the DAUS of a supervisor
	 */
	@Test
	public void testGetAscesOfDasu() throws Exception {
		logger.info("testGetAscesOfDasu");
		buildCDB();

		// The test starts below
		Set<AsceDao> asces = cdbReader.getAscesForDasu("DasuID1");
		assertEquals(0, asces.size());

		asces = cdbReader.getAscesForDasu("DasuID2");
		assertEquals(1, asces.size());
		for (AsceDao asce : asces) {
			assertTrue(asce.getId().equals("ASCE-ID1"));
		}

		asces = cdbReader.getAscesForDasu("DasuID3");
		assertEquals(3, asces.size());
		for (AsceDao asce : asces) {
			assertTrue(asce.getId().equals("ASCE-ID2") || asce.getId().equals("ASCE-ID3")
					|| asce.getId().equals("ASCE-ID4"));
		}
	}

	/**
	 * Test the getting of the DAUS of a supervisor
	 */
	@Test
	public void testGetInputsOfAsce() throws Exception {
		logger.info("testGetInputsOfAsce");
		buildCDB();

		// The test starts below
		Collection<IasioDao> iasios = cdbReader.getIasiosForAsce("ASCE-ID1");
		assertEquals(2, iasios.size());
		for (IasioDao iasio : iasios) {
			assertTrue(iasio.getId().equals("iasioID-1") || iasio.getId().equals("iasioID-2"));
		}

		iasios = cdbReader.getIasiosForAsce("ASCE-ID3");
		assertEquals(2, iasios.size());
		for (IasioDao iasio : iasios) {
			assertTrue(iasio.getId().equals("iasioID-1") || iasio.getId().equals("iasioID-2"));
		}

		iasios = cdbReader.getIasiosForAsce("ASCE-ID4");
		assertEquals(3, iasios.size());
		for (IasioDao iasio : iasios) {
			assertTrue(iasio.getId().equals("iasioID-2") || iasio.getId().equals("iasioID-3")
					|| iasio.getId().equals("iasioID-4"));
		}
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
