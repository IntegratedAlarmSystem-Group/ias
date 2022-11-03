package org.eso.ias.cdb.test;

import org.eso.ias.cdb.CdbReader;
import org.eso.ias.cdb.CdbReaderFactory;
import org.eso.ias.cdb.json.JsonReader;
import org.eso.ias.cdb.pojos.IasDao;
import org.eso.ias.cdb.rdb.RdbReader;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

import static org.eso.ias.cdb.CdbReaderFactory.cdbClassCmdLineParam;
import static org.eso.ias.cdb.CdbReaderFactory.jsonCdbCmdLineParamShort;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Test if {@link org.eso.ias.cdb.CdbReaderFactory} correctly returns the proper CDB
 * implementation.
 *
 * The test checks the instantiation of the external CDB reader whose jar is in the
 * folder pointed by the IAS_EXTERNAL_JARS environment variable.
 * The Mock implementation is provided by org.eso.ias.cdb.test.extreader.ExtCdbReader whose jar
 * is in the IAS_EXTERNAL_JARS.
 * The external CDB reader must be instantiated when the -cdbClass parameter is in the command line.
 *
 * The test checks if the JSON CDB implementation is instantiated when the -jCdb parameter is
 * in the command line.
 *
 * The test checks if the RDB implementation is used when none of the others apply.
 */
public class CdbReaderFactoryTest {

    /**
     * The logger
     */
    private static final Logger logger = LoggerFactory.getLogger(CdbReaderFactoryTest.class);

    /**
     * The class of the external CDB reader
     */
    private static final String externalClass="org.eso.ias.cdb.test.extreader.ExtCdbReader";

    @BeforeEach
    public void setUp() throws Exception {}

    @AfterEach
    public void tearDown() throws Exception {}

    @Test
    void testRdbInstantiation() throws Exception {
        logger.info("Checking if the RDB implementation is correctly built");
        String[] args = {"first", "second", "third"};
        CdbReader cdbReader = CdbReaderFactory.getCdbReader(args);
        assertNotNull(cdbReader);
        assertThrows(ClassCastException.class, () -> {JsonReader jReader=(JsonReader)cdbReader;});
        RdbReader rdbReader = (RdbReader)cdbReader;
    }

    @Test
    void testJsonInstantiation() throws Exception {
        logger.info("Checking if the JSON implementation is correctly built");
        String[] args = {"first", "second", jsonCdbCmdLineParamShort, "./src/test/testCdb"};
        CdbReader cdbReader = CdbReaderFactory.getCdbReader(args);
        JsonReader jReader=(JsonReader)cdbReader;
        cdbReader.init();
        Optional<IasDao> iasDaoOptional = cdbReader.getIas();
        assertTrue(iasDaoOptional.isPresent());
        IasDao iasDao = iasDaoOptional.get();
        assertEquals(10,iasDao.getHbFrequency());
        assertEquals("127.0.0.1:9092",iasDao.getBsdbUrl());
        cdbReader.shutdown();
    }

    @Test
    void testExtReaderInstantiation() throws Exception {
        logger.info("Checking if the external reader is correctly built");
        String[] args = {"first", "second", cdbClassCmdLineParam, externalClass};
        CdbReader cdbReader = CdbReaderFactory.getCdbReader(args);
        cdbReader.init();
        Optional<IasDao> iasDaoOptional = cdbReader.getIas();
        assertTrue(iasDaoOptional.isPresent());
        IasDao iasDao = iasDaoOptional.get();
        assertEquals(5,iasDao.getHbFrequency());
        assertEquals(3,iasDao.getRefreshRate());
        cdbReader.shutdown();
    }
}
