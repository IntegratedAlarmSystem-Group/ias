package org.eso.ias.plugin.test.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.eso.ias.plugin.config.PluginConfig;
import org.eso.ias.plugin.config.PluginConfigException;
import org.eso.ias.plugin.config.PluginConfigFileReader;
import org.eso.ias.plugin.config.Property;
import org.eso.ias.plugin.config.Value;
import org.junit.jupiter.api.Test;

public class JsonConfigReaderTest {
	
	/**
	 * The path from the resources where JSON files for
	 * testing have been saved
	 */
	private static final String resourcePath="/org/eso/iasplugin/config/test/jsonfiles/";
	
	/**
	 * The thread group for testing
	 */
	private static final ThreadGroup threadGroup = new ThreadGroup("JSONReaderThreadGroup");

	/**
	 * Read a valid configuration and check the correctness of the
	 * values of the java pojo.
	 * 
	 * @throws Exception
	 */
	@Test
	public void testReadConfiguration() throws PluginConfigException, TimeoutException, ExecutionException, InterruptedException {
		
		PluginConfigFileReader jsonFileReader = new PluginConfigFileReader(resourcePath+"configOkGlobalFilter.json");
		assertNotNull(jsonFileReader);
		Future<PluginConfig> futurePluginConfig = jsonFileReader.getPluginConfig();
		assertNotNull(futurePluginConfig);
		PluginConfig config = futurePluginConfig.get(1, TimeUnit.MINUTES);
		assertNotNull(config);
		
		assertTrue(config.isValid(),"The passed configuration is valid");
		
		assertEquals("Plugin-ID", config.getId());
		assertEquals("iasdevel.hq.eso.org",config.getSinkServer());
		assertEquals(8192,config.getSinkPort());
		assertEquals(2, config.getValues().length);
		assertEquals(3, config.getAutoSendTimeInterval());
		assertEquals(9, config.getHbFrequency());
		assertEquals("Average",config.getDefaultFilter());
		assertEquals("5,10,15",config.getDefaultFilterOptions());
		
		// Check the properties
		assertEquals(2, config.getProperties().length);
		assertEquals(2, config.getProps().size());
		Optional<Property> p1 = config.getProperty("a-key");
		assertTrue(p1.isPresent());
		assertEquals("a-key",p1.get().getKey());
		assertEquals("itsValue",p1.get().getValue());
		
		Optional<Property> p2 = config.getProperty("Anotherkey");
		assertTrue(p1.isPresent());
		assertEquals("Anotherkey",p2.get().getKey());
		assertEquals("AnotherValue",p2.get().getValue());
		
		// This property does not exist
		Optional<Property> p3 = config.getProperty("MissingKey");
		assertFalse(p3.isPresent());
		
		// Check the value with: filter, filterOptions, refreshTime.
		// Add the GlobalFilter called defaultFilter and GlobalFilterOptions called defaultFilterOptions.
		Optional<Value> v1Opt = config.getValue("AlarmID");
		assertTrue(v1Opt.isPresent());
		assertEquals(500, v1Opt.get().getRefreshTime());
		assertEquals("Average",v1Opt.get().getFilter());
		assertEquals("10, 5, 4",v1Opt.get().getFilterOptions());

		// Check if the filterOptions taked is not the global, it take the local filterOption.
		assertNotEquals("5,10,15",v1Opt.get().getFilterOptions());
		
		Optional<Value> v2Opt = config.getValue("TempID");
		assertTrue(v2Opt.isPresent());
		assertEquals(1500, v2Opt.get().getRefreshTime());

		assertNull(v2Opt.get().getFilter());
		assertNull(v2Opt.get().getFilterOptions());
	}
	
	/**
	 * Read a non-valid configuration and check the non-correctness of the
	 * values of the java pojo.
	 * 
	 * @throws Exception
	 */
	@Test
	public void testInvalidConf() throws PluginConfigException, InterruptedException, ExecutionException, TimeoutException {
		
		PluginConfigFileReader jsonFileReader = new PluginConfigFileReader(resourcePath+"configInvalidValues.json");
		PluginConfig config = jsonFileReader.getPluginConfig().get(1,TimeUnit.MINUTES);
		assertFalse(config.isValid());
		
		PluginConfigFileReader jsonFileReader2 = new PluginConfigFileReader(resourcePath+"configInvalidValues2.json");
		PluginConfig config2 = jsonFileReader2.getPluginConfig().get(1,TimeUnit.MINUTES);
		assertFalse(config2.isValid());
		
		PluginConfigFileReader jsonFileReader3 = new PluginConfigFileReader(resourcePath+"configInvalidValues3.json");
		PluginConfig config3 = jsonFileReader3.getPluginConfig().get(1,TimeUnit.MINUTES);
		assertFalse(config3.isValid());
		
		PluginConfigFileReader jsonFileReader4 = new PluginConfigFileReader(resourcePath+"configInvalidValues4.json");
		PluginConfig config4 = jsonFileReader4.getPluginConfig().get(1,TimeUnit.MINUTES);
		assertFalse(config4.isValid());
		
		PluginConfigFileReader jsonFileReader5 = new PluginConfigFileReader(resourcePath+"configInvalidValues5.json");
		PluginConfig config5 = jsonFileReader5.getPluginConfig().get(1,TimeUnit.MINUTES);
		assertFalse(config5.isValid());
		
		PluginConfigFileReader jsonFileReader6 = new PluginConfigFileReader(resourcePath+"configInvalidValues6.json");
		PluginConfig config6 = jsonFileReader6.getPluginConfig().get(1,TimeUnit.MINUTES);
		assertFalse(config6.isValid());
		
		PluginConfigFileReader jsonFileReader7 = new PluginConfigFileReader(resourcePath+"configInvalidValues7.json");
		PluginConfig config7 = jsonFileReader7.getPluginConfig().get(1,TimeUnit.MINUTES);
		assertFalse(config7.isValid());
		
		PluginConfigFileReader jsonFileReader8 = new PluginConfigFileReader(resourcePath+"configInvalidValues8.json");
		PluginConfig config8 = jsonFileReader8.getPluginConfig().get(1,TimeUnit.MINUTES);
		assertFalse(config8.isValid());
		
		PluginConfigFileReader jsonFileReader9 = new PluginConfigFileReader(resourcePath+"configInvalidValues9.json");
		PluginConfig config9 = jsonFileReader9.getPluginConfig().get(1,TimeUnit.MINUTES);
		assertFalse(config9.isValid());
		
	}
	/**
	 * Read a non-valid configuration and check the non-correctness of the
	 * values of the java pojo.
	 * 
	 * @throws Exception
	 */
	@Test
	public void testValidConf() throws PluginConfigException, InterruptedException, ExecutionException, TimeoutException {
	PluginConfigFileReader jsonFileReader9 = new PluginConfigFileReader(resourcePath+"configOk.json");
		PluginConfig config9 = jsonFileReader9.getPluginConfig().get(1,TimeUnit.MINUTES);
		assertTrue(config9.isValid());

		PluginConfigFileReader jsonFileReader10 = new PluginConfigFileReader(resourcePath+"configOkGlobalFilter.json");
		PluginConfig config10 = jsonFileReader10.getPluginConfig().get(1,TimeUnit.MINUTES);
		assertTrue(config10.isValid());
}


}
