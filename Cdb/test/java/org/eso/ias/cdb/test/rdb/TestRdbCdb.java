package org.eso.ias.cdb.test.rdb;

import static org.junit.Assert.*;

import java.util.Optional;

import org.eso.ias.cdb.CdbReader;
import org.eso.ias.cdb.CdbWriter;
import org.eso.ias.cdb.pojos.DasuDao;
import org.eso.ias.cdb.pojos.IasDao;
import org.eso.ias.cdb.pojos.LogLevelDao;
import org.eso.ias.cdb.pojos.PropertyDao;
import org.eso.ias.cdb.pojos.SupervisorDao;
import org.eso.ias.cdb.rdb.RdbReader;
import org.eso.ias.cdb.rdb.RdbUtils;
import org.eso.ias.cdb.rdb.RdbWriter;
import org.eso.ias.cdb.test.json.TestJsonCdb;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.Test;

/**
 * Test reading and writing data from/to 
 * the relational database.
 * <P>
 * Reading and writing is done by {@link CdbReader} and {@link CdbWriter}
 * implementators as it is done in {@link TestJsonCdb} so, in principle the
 * same test can be run for text files and relational database.
 * <BR>The reason to have a separate test is because with hibernate there is 
 * no need to explicitly store objects contained in other objects as needed
 * by the CDB on files.  Actually, this test should be shorter and easier to read.
 * 
 * 
 * <EM>Note</em>: with the current implementation, 
 *                running this test will clear the content of the production
 *                database
 * 
 * @author acaproni
 *
 */
public class TestRdbCdb {
	
	/**
	 * Helper object to read and write the RDB
	 */
	private static final RdbUtils rdbUtils =  RdbUtils.getRdbUtils();
	
	/**
	 * The reader for the CDB RDB
	 */
	private final CdbReader cdbReader = new RdbReader();
	
	/**
	 * The reader for the CDB RDB
	 */
	private final CdbWriter cdbWriter = new RdbWriter();

	/**
	 * @throws java.lang.Exception
	 */
	@Before
	public void setUp() throws Exception {
		// Clear the content of the DB
		
		// Remove all the tables
		rdbUtils.dropTables();
		
		// The create empty tables
		rdbUtils.createTables();
	}

	/**
	 * @throws java.lang.Exception
	 */
	@After
	public void tearDown() throws Exception {
	}
	
	@AfterClass public static void logout() {
        rdbUtils.close();
  }

	/**
	 * Test reading and writing the IAS
	 */
	@Test
	public void testIas() throws Exception {
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
		
		// Write the IAS
		cdbWriter.writeIas(ias);
		
		// Get the IAS from the reader
		Optional<IasDao> optIas = cdbReader.getIas();
		assertTrue("Got an empty IAS!", optIas.isPresent());
		assertEquals("The IASs differ!", ias, optIas.get());
		
		System.out.println(optIas.get());
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
		
		
		cdbWriter.writeSupervisor(superv);
		
		Optional<SupervisorDao> optSuperv = cdbReader.getSupervisor(superv.getId());
		assertTrue("Got an empty Supervisor!", optSuperv.isPresent());
		assertEquals("The Supervisors differ!", superv, optSuperv.get());
		
	}
}
