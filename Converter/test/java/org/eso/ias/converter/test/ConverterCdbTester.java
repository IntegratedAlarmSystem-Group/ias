package org.eso.ias.converter.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

import org.eso.ias.cdb.CdbReader;
import org.eso.ias.cdb.CdbWriter;
import org.eso.ias.cdb.json.CdbFiles;
import org.eso.ias.cdb.json.CdbFolders;
import org.eso.ias.cdb.json.CdbJsonFiles;
import org.eso.ias.cdb.json.JsonReader;
import org.eso.ias.cdb.json.JsonWriter;
import org.eso.ias.cdb.pojos.IasTypeDao;
import org.eso.ias.cdb.pojos.IasioDao;
import org.eso.ias.converter.config.IasioConfigurationDaoImpl;
import org.eso.ias.converter.config.MonitorPointConfiguration;
import org.eso.ias.types.IASTypes;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

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
	 * The prefix of the IDs of the IASIOs written in the config file
	 */
	public static final String IasioIdPrefix="IoID-";
	
	/**
	 * The folder struct of the CDB
	 */
	private CdbFiles cdbFiles;
	
	private IasioConfigurationDaoImpl configDao;
	
	@BeforeEach
	public void setUp() throws Exception {
		// Remove any CDB folder if present
		CdbFolders.ROOT.delete(cdbParentPath);
		assertFalse(CdbFolders.ROOT.exists(cdbParentPath));
		cdbFiles = new CdbJsonFiles(cdbParentPath);
		CdbReader cdbReader = new JsonReader(cdbFiles);
		configDao = new IasioConfigurationDaoImpl(cdbReader);
		
	}
	
	/**
	 * Create a Iasio ID from the given index
	 * 
	 * @param n The index
	 * @return The ID
	 */
	public String buildIasId(int n) {
		return IasioIdPrefix+n;
	}
	
	/**
	 * Create a IasTypeDao from the given index
	 * 
	 * @param n The index
	 * @return The IasTypeDao
	 */
	public static IasTypeDao buildIasType(int n) {
		return IasTypeDao.values()[n%IasTypeDao.values().length];
	}
	
	/**
	 * Populate the CDB with the passed number of IASIO
	 * 
	 * @param numOfIasio the number of IASIOs to write in the configurations
	 * @param cdbFiles The folder struct of the CDB
	 * @throws Exception
	 */
	public void populateCDB(int numOfIasios,CdbFiles cdbFiles) throws Exception {
		Objects.requireNonNull(cdbFiles);
		if (numOfIasios<=0) {
			throw new IllegalArgumentException("Invalid number of IASIOs to write in the CDB");
		}
		populateCDB(numOfIasios, new JsonWriter(cdbFiles));
	}
	
	/**
	 * Populate the CDB with the passed number of IASIO
	 * 
	 * @param numOfIasio the number of IASIOs to write in the configurations
	 * @param cdbWriter The writer of the CDB
	 * @throws Exception
	 */
	public void populateCDB(int numOfIasios,CdbWriter cdbWriter) throws Exception {
		Objects.requireNonNull(cdbWriter);
		if (numOfIasios<=0) {
			throw new IllegalArgumentException("Invalid number of IASIOs to write in the CDB");
		}
		Set<IasioDao> iasios = biuldIasios(numOfIasios);
		cdbWriter.writeIasios(iasios, false);
	}
	
	/**
	 * Build the set of IASIOs configuration to write in the CDB
	 * 
	 * @param numOfIasios the number of IASIOs to write in the configurations
	 * @return the set of IASIOs configuration to write in the CDB
	 */
	public Set<IasioDao> biuldIasios(int numOfIasios) {
		if (numOfIasios<=0) {
			throw new IllegalArgumentException("Invalid number of IASIOs to write in the CDB");
		}
		Set<IasioDao> iasios = new HashSet<>(numOfIasios);
		for (int t=0; t<numOfIasios; t++) {
			IasTypeDao iasType = buildIasType(t);
			IasioDao iasio = new IasioDao(buildIasId(t), "IASIO description", iasType,"http://www.eso.org/almm/alarms");
			iasios.add(iasio);
		}
		return iasios;
	}
	
	@AfterEach
	public void tearDown() throws Exception{
		configDao.close();
		CdbFolders.ROOT.delete(cdbParentPath);
		assertFalse(CdbFolders.ROOT.exists(cdbParentPath));
	}
	
	/**
	 * Check if the DAO stores all the IASIOs.
	 */
	@Test
	public void testNumberOfIasios() throws Exception {
		int mpPointsToCreate=1500;
		populateCDB(mpPointsToCreate,cdbFiles);
		configDao.initialize();
		int found=0;
		for (int t=0; t<mpPointsToCreate; t++) {
			if (configDao.getConfiguration(buildIasId(t))!=null) {
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
		populateCDB(mpPointsToCreate,cdbFiles);
		configDao.initialize();
		
		for (int t=0; t<mpPointsToCreate; t++) {
			IasTypeDao iasType = buildIasType(t);
			MonitorPointConfiguration mpConf=configDao.getConfiguration(buildIasId(t));
			assertNotNull(mpConf);
			assertEquals(IASTypes.valueOf(iasType.toString()), mpConf.mpType);
		}
	}
}
