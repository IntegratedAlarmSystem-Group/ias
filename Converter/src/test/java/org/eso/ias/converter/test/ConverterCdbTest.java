package org.eso.ias.converter.test;

import org.eso.ias.cdb.CdbReader;
import org.eso.ias.cdb.CdbWriter;
import org.eso.ias.cdb.pojos.IasTypeDao;
import org.eso.ias.cdb.pojos.IasioDao;
import org.eso.ias.cdb.pojos.TemplateDao;
import org.eso.ias.cdb.structuredtext.*;
import org.eso.ias.converter.config.Cache;
import org.eso.ias.converter.config.MonitorPointConfiguration;
import org.eso.ias.types.IASTypes;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.HashSet;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test the CDB DAO of the converter,
 * i,e, the {@link IasioConfigurationDaoImpl}.
 * 
 * @author acaproni
 *
 */
public class ConverterCdbTest {

    /**
     * The logger
     */
    private static final Logger logger = LoggerFactory.getLogger(ConverterCdbTest.class);
	
	/**
	 * The parent folder of the CDB is the actual folder
	 */
    public static final Path cdbParentPath =  FileSystems.getDefault().getPath(".");
	
	/**
	 * The prefix of the IDs of the IASIOs written in the config file
	 */
	public static final String IasioIdPrefix="IoID-";

    /**
     * The ID of the template
     */
	public static String templateId = "TemplateId";

	/** The ID of the tmeplated IASIO */
	public static String idOfTemplatedIasio = "TempaltedIasioId";
	
	/**
	 * The folder struct of the CDB
	 */
	private CdbFiles cdbFiles;
	
	private Cache configDao;

	/**
	 * Create a Iasio ID from the given index
	 * 
	 * @param n The index
	 * @return The ID
	 */
    private String buildIasId(int n) {
		return IasioIdPrefix+n;
	}
	
	/**
	 * Create a IasTypeDao from the given index
	 * 
	 * @param n The index
	 * @return The IasTypeDao
	 */
    private static IasTypeDao buildIasType(int n) {
		return IasTypeDao.values()[n%IasTypeDao.values().length];
	}

	public static void deleteFolder(Path folderPath) throws IOException {
		if (!Files.exists(folderPath)) {
			return;
		}
        Files.walkFileTree(folderPath, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                Files.delete(file); // delete file
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                Files.delete(dir); // delete directory after its contents
                return FileVisitResult.CONTINUE;
            }
        });
    }
	
	/**
	 * Populate the CDB with the passed number of IASIO
	 * 
	 * @param numOfIasios the number of IASIOs to write in the configurations
	 * @param cdbFiles The folder struct of the CDB
	 * @throws Exception
	 */
    private void populateCDB(int numOfIasios,CdbFiles cdbFiles) throws Exception {
		Objects.requireNonNull(cdbFiles);
		if (numOfIasios<=0) {
			throw new IllegalArgumentException("Invalid number of IASIOs to write in the CDB");
		}
		logger.info("Populating JSON CDB in {} with {} IASIOS",cdbParentPath.toAbsolutePath().toString(),numOfIasios);
		CdbWriter cdbWriter = new StructuredTextWriter(cdbFiles);
		cdbWriter.init();
		createTemplate(cdbWriter);
		populateCDB(numOfIasios, cdbWriter);
		logger.info("CDB created");
		cdbWriter.shutdown();
	}
	
	/**
	 * Populate the CDB with the passed number of IASIO
	 * 
	 * @param numOfIasios the number of IASIOs to write in the configurations
	 * @param cdbWriter The writer of the CDB
	 * @throws Exception
	 */
    private void populateCDB(int numOfIasios,CdbWriter cdbWriter) throws Exception {
		Objects.requireNonNull(cdbWriter);
		if (numOfIasios<=0) {
			throw new IllegalArgumentException("Invalid number of IASIOs to write in the CDB");
		}
		Set<IasioDao> iasios = buildIasios(numOfIasios);
		// Adds one templated IASIO
        IasTypeDao iasType = buildIasType(2);
        IasioDao iasio = new IasioDao(idOfTemplatedIasio, "IASIO description", iasType,"http://www.eso.org/almm/alarms");
        iasio.setTemplateId(templateId);
        iasios.add(iasio);

        iasios.forEach( i -> logger.debug("IASIO to write {} of type {}",i.getId(),iasio.getIasType()));

		cdbWriter.writeIasios(iasios, false);
	}
	
	/**
	 * Build the set of IASIOs configuration to write in the CDB
	 * 
	 * @param numOfIasios the number of IASIOs to write in the configurations
	 * @return the set of IASIOs configuration to write in the CDB
	 */
    private Set<IasioDao> buildIasios(int numOfIasios) {
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

    /**
     * Write a template in the CDB
     *
     * @param cdbWriter the CDB writer
     * @throws Exception
     */
    private void createTemplate(CdbWriter cdbWriter) throws Exception {
	    logger.info("Adding template");
        TemplateDao tDao = new TemplateDao(templateId, 5,15);
        cdbWriter.writeTemplate(tDao);
    }

    @BeforeEach
    public void setUp() throws Exception {
        // Create the CDB folder
		new File(cdbParentPath.toFile().getAbsolutePath()+"/CDB").mkdirs();
		cdbFiles = new CdbTxtFiles(cdbParentPath.toFile().getAbsolutePath(), TextFileType.JSON);
        CdbReader cdbReader = new StructuredTextReader(cdbFiles);
        cdbReader.init();
        configDao = new Cache(cdbReader);

    }
	
	@AfterEach
	public void tearDown() throws Exception{
		configDao.close();
		ConverterCdbTest.deleteFolder(CdbFolders.ROOT.getFolder(cdbParentPath));
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
		logger.info("testIasiosDataIntegrity started");
		int mpPointsToCreate=2000;
		populateCDB(mpPointsToCreate,cdbFiles);
		configDao.initialize();
		
		for (int t=0; t<mpPointsToCreate; t++) {
			IasTypeDao iasType = buildIasType(t);
			String id = buildIasId(t);
			Optional<MonitorPointConfiguration> mpConf=configDao.getConfiguration(id);
			assertTrue(mpConf.isPresent(),id+" NOT found");
			assertEquals(IASTypes.valueOf(iasType.toString()),
					mpConf.get().mpType,
					"Type of "+id+" mismatch");
		}
		logger.info("testIasiosDataIntegrity done");
	}

	@Test
    public void testTemplatesIasio() throws  Exception {
		logger.info("testTemplatesIasio started");
       int mpPointsToCreate=5;
       populateCDB(mpPointsToCreate,cdbFiles);
       populateCDB(mpPointsToCreate,cdbFiles);
       configDao.initialize();
        Optional<MonitorPointConfiguration> mpConf=configDao.getConfiguration(idOfTemplatedIasio);
       assertTrue(mpConf.isPresent());
       assertTrue(mpConf.get().minTemplateIndex.isPresent());
       assertTrue(mpConf.get().maxTemplateIndex.isPresent());
       assertTrue(5==mpConf.get().minTemplateIndex.get());
       assertTrue(15==mpConf.get().maxTemplateIndex.get());
		logger.info("testTemplatesIasio done");
    }
}


