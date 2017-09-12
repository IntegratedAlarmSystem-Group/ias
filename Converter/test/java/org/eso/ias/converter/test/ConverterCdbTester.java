package org.eso.ias.converter.test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;

import org.eso.ias.cdb.CdbReader;
import org.eso.ias.cdb.CdbWriter;
import org.eso.ias.cdb.json.CdbFiles;
import org.eso.ias.cdb.json.CdbFolders;
import org.eso.ias.cdb.json.CdbJsonFiles;
import org.eso.ias.cdb.json.JsonReader;
import org.eso.ias.cdb.json.JsonWriter;
import org.eso.ias.cdb.pojos.IasDao;
import org.eso.ias.cdb.pojos.IasTypeDao;
import org.eso.ias.cdb.pojos.IasioDao;
import org.eso.ias.cdb.pojos.LogLevelDao;
import org.eso.ias.converter.config.IasioConfigurationDaoImpl;
import org.eso.ias.converter.config.MonitorPointConfiguration;
import org.eso.ias.prototype.input.java.IASTypes;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * Test the CDB DAO of the converter,
 * i,e, the {@link IasioConfigurationDaoImpl}.
 * 
 * @author acaproni
 *
 */
public class ConverterCdbTester {
	
	/**
	 * The parent folder of the CDB is the actual folder
	 */
	public static final Path cdbParentPath =  FileSystems.getDefault().getPath(".");
	
	/**
	 * The folder struct of the CDB
	 */
	private CdbFiles cdbFiles;
	
	/**
	 * The prefix of the IDs of the IASIOs written in the config file
	 */
	private final String IasioIdPrefix="IoID-";
	
	private IasioConfigurationDaoImpl configDao;
	
	@Before
	public void setUp() throws Exception {
		// Remove any CDB folder if present
		CdbFolders.ROOT.delete(cdbParentPath);
		assertFalse(CdbFolders.ROOT.exists(cdbParentPath));
		cdbFiles = new CdbJsonFiles(cdbParentPath);
		CdbReader cdbReader = new JsonReader(cdbFiles);
		configDao = new IasioConfigurationDaoImpl(cdbReader);
		
	}
	
	/**
	 * Create a IasTypeDao from the given index
	 * 
	 * @param n The index
	 * @return The IasTypeDao
	 */
	private IasTypeDao buildIasType(int n) {
		return IasTypeDao.values()[n%IasTypeDao.values().length];
	}
	
	/**
	 * Populate the CDB with the passed number of IASIO
	 * 
	 * @param numOfIasio the number of IASIOs to write in the configurations
	 * @throws Exception
	 */
	private void populateCDB(int numOfIasios) throws Exception {
		if (numOfIasios<=0) {
			throw new IllegalArgumentException("Invalid number of IASIOs to write in the CDB");
		}
		CdbWriter cdbWriter = new JsonWriter(cdbFiles);
		Set<IasioDao> iasios = new HashSet<>(numOfIasios);
		for (int t=0; t<numOfIasios; t++) {
			IasTypeDao iasType = buildIasType(t);
			IasioDao iasio = new IasioDao(IasioIdPrefix+t, "IASIO description", 1500, iasType);
			iasios.add(iasio);
		}
		cdbWriter.writeIasios(iasios, false);
	}
	
	@After
	public void tearDown() throws Exception{
		configDao.tearDown();
		CdbFolders.ROOT.delete(cdbParentPath);
		assertFalse(CdbFolders.ROOT.exists(cdbParentPath));
	}
	
	/**
	 * Check if the DAO stores all the IASIOs.
	 */
	@Test
	public void testNumberOfIasios() throws Exception{
		int mpPointsToCreate=1500;
		populateCDB(mpPointsToCreate);
		configDao.setUp();
		int found=0;
		for (int t=0; t<mpPointsToCreate; t++) {
			if (configDao.getConfiguration(IasioIdPrefix+t)!=null) {
				found++;
			}
		}
		assertEquals(mpPointsToCreate,found);
	}


	/**
	 * Check if the DAO correctly associates the configuration
	 * to a IASIO id
	 */
	@Test
	public void testIasiosDataIntegrity() throws Exception {
		int mpPointsToCreate=2000;
		populateCDB(mpPointsToCreate);
		configDao.setUp();
		
		for (int t=0; t<mpPointsToCreate; t++) {
			IasTypeDao iasType = buildIasType(t);
			MonitorPointConfiguration mpConf=configDao.getConfiguration(IasioIdPrefix+t);
			assertNotNull(mpConf);
			assertEquals(IASTypes.valueOf(iasType.toString()), mpConf.mpType);
		}
	}
}
