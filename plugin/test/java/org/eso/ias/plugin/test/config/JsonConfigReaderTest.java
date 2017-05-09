package org.eso.ias.plugin.test.config;

import static org.junit.Assert.*;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Optional;

import org.eso.ias.plugin.config.PluginConfig;
import org.eso.ias.plugin.config.Value;
import org.junit.Test;

import com.fasterxml.jackson.databind.ObjectMapper;

public class JsonConfigReaderTest {
	
	/**
	 * The path from the resources where JSON files for
	 * testing have been saved
	 */
	private static final String resourcePath="/org/eso/iasplugin/config/test/jsonfiles/";
	
	/**
	 * Get a JSON file for testing from the jar
	 * 
	 * @param fileName The name of the file to get
	 * @return
	 * @throws Exception
	 */
	private BufferedReader getReader(String fileName) throws Exception{
		String resource=resourcePath+fileName;
		InputStream inStream = getClass().getResourceAsStream(resource);
		assertNotNull("Resource not found: "+resource, inStream);
		// Open the file from reading
		return new BufferedReader(new InputStreamReader(inStream));
	}

	/**
	 * Read a valid configuration and check the correctness of the
	 * values of the java pojo.
	 * 
	 * @throws Exception
	 */
	@Test
	public void testReadConfiguration() throws Exception {
		
		ObjectMapper jackson2Mapper = new ObjectMapper();
		PluginConfig config = jackson2Mapper.readValue(getReader("configOk.json"), PluginConfig.class);
		
		assertTrue("The passed configuration is valid",config.isValid());
		
		assertEquals("Plugin-ID", config.getId());
		assertEquals("iasdevel.hq.eso.org",config.getSinkServer());
		assertEquals(8192,config.getSinkPort());
		assertEquals(2, config.getValues().length);
		
		Optional<Value> v1 = config.getValue("AlarmID");
		assertTrue(v1.isPresent());
		assertEquals(500, v1.get().getRefreshTime());
		assertEquals("",v1.get().getFilter());
		assertEquals("",v1.get().getFilterOptions());
		
		Optional<Value> v2 = config.getValue("TempID");
		assertTrue(v2.isPresent());
		assertEquals(1500, v2.get().getRefreshTime());
		assertEquals("Average",v2.get().getFilter());
		assertEquals("1, 150, 5",v2.get().getFilterOptions());
	}
	
	/**
	 * Read a non-valid configuration and check the non-correctness of the
	 * values of the java pojo.
	 * 
	 * @throws Exception
	 */
	@Test
	public void testInvalidConf() throws Exception {
		ObjectMapper jackson2Mapper = new ObjectMapper();
		PluginConfig config = jackson2Mapper.readValue(getReader("configInvalidValues.json"), PluginConfig.class);
		assertFalse(config.isValid());
		
		PluginConfig config2 = jackson2Mapper.readValue(getReader("configInvalidValues2.json"), PluginConfig.class);
		assertFalse(config2.isValid());
		
		PluginConfig config3 = jackson2Mapper.readValue(getReader("configInvalidValues3.json"), PluginConfig.class);
		assertFalse(config3.isValid());
		
		PluginConfig config4 = jackson2Mapper.readValue(getReader("configInvalidValues4.json"), PluginConfig.class);
		assertFalse(config4.isValid());
		
		PluginConfig config5 = jackson2Mapper.readValue(getReader("configInvalidValues5.json"), PluginConfig.class);
		assertFalse(config5.isValid());
		
		PluginConfig config6 = jackson2Mapper.readValue(getReader("configInvalidValues6.json"), PluginConfig.class);
		assertFalse(config6.isValid());
		
	}

}
