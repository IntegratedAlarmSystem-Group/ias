package org.eso.ias.cdb.test;

import org.eso.ias.cdb.TextFileType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

public class TestTextFileType {
    /**
     * The logger
     */
    private static final Logger logger = LoggerFactory.getLogger(TestTextFileType.class);

    private static final String folderNamePrefix = "src/test/";

    @BeforeEach
    public void setUp() throws Exception {}

    @AfterEach
    public void tearDown() throws Exception {}

    @Test
    void testTypeByFilename() throws Exception {
        Optional<TextFileType> jTypeOpt = TextFileType.fromFile("test/file.name.json");
        assertFalse(jTypeOpt.isEmpty());
        assertEquals(jTypeOpt.get(), TextFileType.JSON);
        Optional<TextFileType> yTypeOpt = TextFileType.fromFile("test/file/name.yaml");
        assertFalse(yTypeOpt.isEmpty());
        assertEquals(yTypeOpt.get(), TextFileType.YAML);
        Optional<TextFileType> uTypeOpt = TextFileType.fromFile("test/file/name.pippo");
        assertTrue(uTypeOpt.isEmpty());

    }

    @Test
    void testTypeByFolder() throws Exception {
        File jFolder = new File(folderNamePrefix+"testJsonCdb");
        Optional<TextFileType> jTypeOpt = TextFileType.getCdbType(jFolder);
        assertFalse(jTypeOpt.isEmpty());
        assertEquals(jTypeOpt.get(), TextFileType.JSON);
        File yFolder = new File(folderNamePrefix+"testYamlCdb");
        Optional<TextFileType> yTypeOpt = TextFileType.getCdbType(yFolder);
        assertFalse(yTypeOpt.isEmpty());
        assertEquals(yTypeOpt.get(), TextFileType.YAML);
        // eFolder does not contains CDB so it files with an exception
        File eFolder = new File(".");
        assertThrows(java.lang.IllegalArgumentException.class, () -> {TextFileType.getCdbType(eFolder);});
        // A CDB with an unknown file type
        File uFolder = new File(folderNamePrefix+"unknownCdbType");
        Optional<TextFileType> uTypeOpt = TextFileType.getCdbType(uFolder);
        assertTrue(uTypeOpt.isEmpty());
    }
}
