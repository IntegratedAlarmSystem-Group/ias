package org.eso.ias.cdb.test;

import org.eso.ias.cdb.IasCdbException;
import org.eso.ias.cdb.TextFileType;
import org.eso.ias.cdb.structuredtext.StructuredTextReader;
import org.junit.jupiter.api.Test;

import java.io.File;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test the {@link org.eso.ias.cdb.structuredtext.StructuredTextReader}.
 *
 * The <code>StructuredTextReader</code> is tested against the YAML and JSON tyoes
 * in {@link TestJsonCdb} and {@link TestYamlCdb}.
 * This class only tests methods that are not specific to the implementations.
 */
public class TestStructTextReader {

    /**
     * Checks if the empty constructor correctly detects the type of the
     * CDb in the folder
     *
     * @throws Exception
     */
    @Test
    public void testCdbTypeDetection() throws Exception {
        String yamlFolder = "src/test/testYamlCdb";
        StructuredTextReader yamlReader = new StructuredTextReader(new File(yamlFolder));
        assertEquals(TextFileType.YAML, yamlReader.cdbFilesType);

        String jsonFolder = "src/test/testJsonCdb";
        StructuredTextReader jsonReader = new StructuredTextReader(new File(jsonFolder));
        assertEquals(TextFileType.JSON, jsonReader.cdbFilesType);

        String unknownTypeFolder = "src/test/unknownCdbType";
        assertThrows(IasCdbException.class, () -> new StructuredTextReader(new File(unknownTypeFolder)));
    }
}
