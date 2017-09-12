package org.eso.ias.converter.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;

import java.nio.file.FileSystems;
import java.nio.file.Path;

import org.eso.ias.cdb.CdbReader;
import org.eso.ias.cdb.json.CdbFiles;
import org.eso.ias.cdb.json.CdbFolders;
import org.eso.ias.cdb.json.CdbJsonFiles;
import org.eso.ias.cdb.json.JsonReader;
import org.eso.ias.cdb.pojos.IasTypeDao;
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
	 * The folder struct of the CDB
	 */
	private CdbFiles cdbFiles;
	
	private IasioConfigurationDaoImpl configDao;
	
	@Before
	public void setUp() throws Exception {
		// Remove any CDB folder if present
		CdbFolders.ROOT.delete(CommonHelpers.cdbParentPath);
		assertFalse(CdbFolders.ROOT.exists(CommonHelpers.cdbParentPath));
		cdbFiles = new CdbJsonFiles(CommonHelpers.cdbParentPath);
		CdbReader cdbReader = new JsonReader(cdbFiles);
		configDao = new IasioConfigurationDaoImpl(cdbReader);
		
	}
	
	@After
	public void tearDown() throws Exception{
		configDao.tearDown();
		CdbFolders.ROOT.delete(CommonHelpers.cdbParentPath);
		assertFalse(CdbFolders.ROOT.exists(CommonHelpers.cdbParentPath));
	}
	
	/**
	 * Check if the DAO stores all the IASIOs.
	 */
	@Test
	public void testNumberOfIasios() throws Exception {
		int mpPointsToCreate=1500;
		CommonHelpers.populateCDB(mpPointsToCreate,cdbFiles);
		configDao.setUp();
		int found=0;
		for (int t=0; t<mpPointsToCreate; t++) {
			if (configDao.getConfiguration(CommonHelpers.buildIasId(t))!=null) {
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
		CommonHelpers.populateCDB(mpPointsToCreate,cdbFiles);
		configDao.setUp();
		
		for (int t=0; t<mpPointsToCreate; t++) {
			IasTypeDao iasType = CommonHelpers.buildIasType(t);
			MonitorPointConfiguration mpConf=configDao.getConfiguration(CommonHelpers.buildIasId(t));
			assertNotNull(mpConf);
			assertEquals(IASTypes.valueOf(iasType.toString()), mpConf.mpType);
		}
	}
}
