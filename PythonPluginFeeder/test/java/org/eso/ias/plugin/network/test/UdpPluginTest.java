package org.eso.ias.plugin.network.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.eso.ias.heartbeat.HbProducer;
import org.eso.ias.heartbeat.publisher.HbLogProducer;
import org.eso.ias.heartbeat.serializer.HbJsonSerializer;
import org.eso.ias.plugin.Plugin;
import org.eso.ias.plugin.config.PluginConfig;
import org.eso.ias.plugin.config.PluginConfigFileReader;
import org.eso.ias.plugin.network.UdpPlugin;
import org.eso.ias.plugin.publisher.MonitorPointData;
import org.eso.ias.plugin.publisher.MonitorPointSender;
import org.eso.ias.plugin.publisher.PublisherException;
import org.eso.ias.plugin.publisher.impl.ListenerPublisher;
import org.eso.ias.plugin.publisher.impl.ListenerPublisher.PublisherEventsListener;
import org.eso.ias.types.Alarm;
import org.eso.ias.types.OperationalMode;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * test the {@link UdpPlugin} closing the loop from
 * python to the monitor point published
 * by the java plugin:
 * UdpPlugin.py -> UdpPlugin.java -> Plugin.java -> BSDB
 * 
 * The test setup up the java plugin {@link #udpPlugin},
 * the run the python plugin (MockUdpPlugin.py) that sends some monitor points and alarms.
 *  
 *  The test check if the monitor points sent by MockUdpPlugin.py
 *  are finally published by Plugin.java 
 *  (i.e. received in {@link #dataReceived(MonitorPointData)}).
 *  
 *  To avoid the java plugin to continuosly send the same monitor points (auto-send), 
 *  the  autoSendTimeInterval in pyConfig.json is set to 120 seconds
 *  while this test lasts much less
 * 
 * @author acaproni
 *
 */
public class UdpPluginTest implements PublisherEventsListener {
	
	/**
	 * The logger
	 */
	private static final Logger logger = LoggerFactory.getLogger(UdpPluginTest.class);
	
	/**
	 * The path from the resources where JSON files for
	 * testing have been saved
	 */
	private static final String resourcePath="/org/eso/ias.pyplugin/config/test/json/";
	
	/**
	 * The port of the UDP socket
	 */
	private static final int udpPort = 10101;
	
	/**
	 * The publisher of monitor points
	 */
	private MonitorPointSender mpSender;
	
	/**
	 * The object to test
	 */
	private UdpPlugin udpPlugin;
	
	/**
	 * The monitor point published by the java plugin
	 */
	private final Map<String, MonitorPointData> publishedMPoints = new HashMap<>();
	
	/**
	 * The latch used by the UdpPlugin to signal termination
	 */
	private CountDownLatch udpPluginLatch;
	
	/**
	 * The python plugin
	 */
	private Process proc;
	
	@BeforeEach
	public void setUp() throws Exception {
		PluginConfigFileReader jsonFileReader = new PluginConfigFileReader(resourcePath+"pyConfig.json");
		assertNotNull(jsonFileReader);
		Future<PluginConfig> futurePluginConfig = jsonFileReader.getPluginConfig();
		assertNotNull(futurePluginConfig);
		PluginConfig config = futurePluginConfig.get(1, TimeUnit.MINUTES);
		assertNotNull(config);
		
		mpSender = new ListenerPublisher(
				config.getId(), 
				config.getMonitoredSystemId(), 
				"localhost",  // Unused
				10000, // Unused 
				Plugin.getScheduledExecutorService(), 
				this);
		
		// Not interested in HBs here so loggin is enough
		HbProducer hbProd = new HbLogProducer(new HbJsonSerializer());
		
		// The plugin will send data to this process instead of the BSDB
		udpPlugin = new UdpPlugin(config, mpSender, hbProd, udpPort);
		
		udpPluginLatch = udpPlugin.setUp();
		assertNotNull(udpPluginLatch);
		
		Thread.sleep(1000);
		launchPythonPlugin();
		
	}
	
	
	@AfterEach
	public void tearDown() throws Exception {
		logger.debug("Shutting down");
		udpPlugin.shutdown();
	}
	
	@Test
	public void test() throws Exception {
		logger.debug("Leaving the plugin time to run");
		udpPluginLatch.await(30, TimeUnit.SECONDS);
		logger.debug("test terminated");
		
		// Check if the python process terminated without errors
		assertFalse(proc.isAlive(),"Python plugin still running");
		assertEquals(0,proc.exitValue(),"Python plugin terminated with error "+proc.exitValue());
		
		assertEquals(6, publishedMPoints.size());
		
		MonitorPointData mpdDouble = publishedMPoints.get("ID-Double");
		assertEquals(OperationalMode.INITIALIZATION.toString(),mpdDouble.getOperationalMode());
		assertEquals(Double.valueOf(122.54), Double.valueOf(Double.parseDouble(mpdDouble.getValue())));
		
		MonitorPointData mpdLong = publishedMPoints.get("ID-Long");
		assertEquals(OperationalMode.STARTUP.toString(),mpdLong.getOperationalMode());
		assertEquals(Integer.valueOf(1234567), Integer.valueOf(Integer.parseInt(mpdLong.getValue())));
		
		MonitorPointData mpdBool = publishedMPoints.get("ID-Bool");
		assertEquals(OperationalMode.OPERATIONAL.toString(),mpdBool.getOperationalMode());
		assertEquals(Boolean.FALSE, Boolean.valueOf(Boolean.parseBoolean(mpdBool.getValue())));
		
		MonitorPointData mpdChar = publishedMPoints.get("ID-Char");
		assertEquals(OperationalMode.DEGRADED.toString(),mpdChar.getOperationalMode());
		assertTrue(mpdChar.getValue().length()==1);
		assertEquals('X', mpdChar.getValue().charAt(0));
		
		MonitorPointData mpdString = publishedMPoints.get("ID-String");
		assertEquals(OperationalMode.CLOSING.toString(),mpdString.getOperationalMode());
		assertEquals("Testing for test", mpdString.getValue());
		
		MonitorPointData mpdAlarm = publishedMPoints.get("ID-Alarm");
		assertEquals(OperationalMode.UNKNOWN.toString(),mpdAlarm.getOperationalMode());
		assertEquals(Alarm.SET_HIGH.toString(), mpdAlarm.getValue());
	}

	@Override
	public void initialized() {
		logger.info("Initialized");
	}

	@Override
	public void closed() {
		logger.info("Closed");		
	}

	@Override
	public void dataReceived(MonitorPointData mpData) {
		publishedMPoints.put(mpData.getId(), mpData);
		try {
			logger.info("Data published {}",mpData.toJsonString());
		} catch (PublisherException pe) {
			logger.error("Error translating the MonitorPointData into a JSON string",pe);
		}
	}
	
	private void launchPythonPlugin() throws Exception {
		logger.debug("Starting the python plugin");
		ProcessBuilder builder = new ProcessBuilder("MockUdpPlugin");
		proc = builder.start();
		logger.debug("Python plugin running");
	}
}
