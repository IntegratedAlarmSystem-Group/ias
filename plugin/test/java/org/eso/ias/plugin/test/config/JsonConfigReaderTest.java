package org.eso.ias.plugin.test.config;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.eso.ias.plugin.config.PluginConfig;
import org.eso.ias.plugin.config.PluginConfigException;
import org.eso.ias.plugin.config.PluginConfigFileReader;
import org.eso.ias.plugin.config.Value;
import org.eso.ias.plugin.thread.PluginThreadFactory;
import org.junit.Test;

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
	 * The thread factory for testing
	 */
	private static final ThreadFactory threadFactory = new PluginThreadFactory(threadGroup);	


	/**
	 * Read a valid configuration and check the correctness of the
	 * values of the java pojo.
	 * 
	 * @throws Exception
	 */
	@Test
	public void testReadConfiguration() throws PluginConfigException, TimeoutException, ExecutionException, InterruptedException {
		
		PluginConfigFileReader jsonFileReader = new PluginConfigFileReader(resourcePath+"configOk.json", threadFactory);
		assertNotNull(jsonFileReader);
		Future<PluginConfig> futurePluginConfig = jsonFileReader.getPluginConfig();
		assertNotNull(futurePluginConfig);
		PluginConfig config = futurePluginConfig.get(1, TimeUnit.MINUTES);
		assertNotNull(config);
		
		assertTrue("The passed configuration is valid",config.isValid());
		
		assertEquals("Plugin-ID", config.getId());
		assertEquals("iasdevel.hq.eso.org",config.getSinkServer());
		assertEquals(8192,config.getSinkPort());
		assertEquals(2, config.getValues().length);
		
		Optional<Value> v1Opt = config.getValue("AlarmID");
		assertTrue(v1Opt.isPresent());
		assertEquals(500, v1Opt.get().getRefreshTime());
		assertEquals("",v1Opt.get().getFilter());
		assertEquals("",v1Opt.get().getFilterOptions());
		
		Optional<Value> v2Opt = config.getValue("TempID");
		assertTrue(v2Opt.isPresent());
		assertEquals(1500, v2Opt.get().getRefreshTime());
		assertEquals("Average",v2Opt.get().getFilter());
		assertEquals("1, 150, 5",v2Opt.get().getFilterOptions());
	}
	
	/**
	 * Read a non-valid configuration and check the non-correctness of the
	 * values of the java pojo.
	 * 
	 * @throws Exception
	 */
	@Test
	public void testInvalidConf() throws PluginConfigException, InterruptedException, ExecutionException, TimeoutException {
		
		PluginConfigFileReader jsonFileReader = new PluginConfigFileReader(resourcePath+"configInvalidValues.json", threadFactory);
		PluginConfig config = jsonFileReader.getPluginConfig().get(1,TimeUnit.MINUTES);
		assertFalse(config.isValid());
		
		PluginConfigFileReader jsonFileReader2 = new PluginConfigFileReader(resourcePath+"configInvalidValues2.json", threadFactory);
		PluginConfig config2 = jsonFileReader2.getPluginConfig().get(1,TimeUnit.MINUTES);
		assertFalse(config2.isValid());
		
		PluginConfigFileReader jsonFileReader3 = new PluginConfigFileReader(resourcePath+"configInvalidValues3.json", threadFactory);
		PluginConfig config3 = jsonFileReader3.getPluginConfig().get(1,TimeUnit.MINUTES);
		assertFalse(config3.isValid());
		
		PluginConfigFileReader jsonFileReader4 = new PluginConfigFileReader(resourcePath+"configInvalidValues4.json", threadFactory);
		PluginConfig config4 = jsonFileReader4.getPluginConfig().get(1,TimeUnit.MINUTES);
		assertFalse(config4.isValid());
		
		PluginConfigFileReader jsonFileReader5 = new PluginConfigFileReader(resourcePath+"configInvalidValues5.json", threadFactory);
		PluginConfig config5 = jsonFileReader5.getPluginConfig().get(1,TimeUnit.MINUTES);
		assertFalse(config5.isValid());
		
		PluginConfigFileReader jsonFileReader6 = new PluginConfigFileReader(resourcePath+"configInvalidValues6.json", threadFactory);
		PluginConfig config6 = jsonFileReader6.getPluginConfig().get(1,TimeUnit.MINUTES);
		assertFalse(config6.isValid());
		
	}

}
