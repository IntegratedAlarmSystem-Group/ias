package org.eso.ias.plugin.network.test;

import static org.junit.Assert.assertNotNull;

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
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * test the {@link UdpPlugin}
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
	private static final int udpPort = 5000;
	
	/**
	 * The publisher of monitor points
	 */
	private MonitorPointSender mpSender;
	
	/**
	 * The object to test
	 */
	private UdpPlugin udpPlugin;
	
	/**
	 * The latch used by the UdpPlugin to signal termination
	 */
	private CountDownLatch udpPluginLatch;
	
	@Before
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
				"localhost", 
				10000, 
				Plugin.getScheduledExecutorService(), 
				this);
		
		HbProducer hbProd = new HbLogProducer(new HbJsonSerializer());
		
		udpPlugin = new UdpPlugin(config, mpSender, hbProd, udpPort);
		
		udpPluginLatch = udpPlugin.setUp();
		assertNotNull(udpPluginLatch);
	}
	
	@After
	public void tearDown() throws Exception {
		logger.debug("Shutting down");
		udpPlugin.shutdown();
	}
	
	@Test
	public void test() throws Exception {
		logger.debug("Leaving the plugin time to run");
		udpPluginLatch.await(1, TimeUnit.MINUTES);
		logger.debug("test terminated");
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
		try {
			logger.info("Data published {}",mpData.toJsonString());
		} catch (PublisherException pe) {
			logger.error("Error translating the MonitorPointData into a JSON string",pe);
		}
	}
}
