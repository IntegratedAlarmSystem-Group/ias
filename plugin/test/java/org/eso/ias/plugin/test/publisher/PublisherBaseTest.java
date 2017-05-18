package org.eso.ias.plugin.test.publisher;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;

import org.eso.ias.plugin.publisher.MonitoredSystemData;
import org.eso.ias.plugin.publisher.PublisherBase;
import org.eso.ias.plugin.thread.PluginThreadFactory;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertEquals;

/**
 * Test the {@link PublisherBase}.
 * 
 * @author acaproni
 *
 */
public class PublisherBaseTest {
	/**
	 * The concrete PublisherBase to test
	 * 
	 * @author acaproni
	 */
	private class PublisherTest extends PublisherBase {

		/**
		 * Constructor
		 * @param pluginId
		 * @param serverName
		 * @param port
		 * @param executorSvc
		 */
		public PublisherTest(String pluginId, String serverName, int port, ScheduledExecutorService executorSvc) {
			super(pluginId, serverName, port, executorSvc);
			
		}

		@Override
		protected void publish(MonitoredSystemData data) {
			
		}

		/**
		 * @see org.eso.ias.plugin.publisher.PublisherBase#setUp()
		 */
		@Override
		protected void setUp() throws Exception {
			logger.info("Initialized");
			
		}

		/**
		 * @see org.eso.ias.plugin.publisher.PublisherBase#tearDown()
		 */
		@Override
		protected void tearDown() throws Exception {
			logger.info("Cleaned up");
			
		}
		
	}
	
	/**
	 * The logger
	 */
	private final static Logger logger = LoggerFactory.getLogger(PublisherTest.class);
	
	/**
	 * The ID of the plugin for testing
	 */
	private final String pluginId = "IAS-Publisher-Test-ID";
	
	/**
	 * The name of a server of the plugin for testing
	 */
	private final String pluginServerName = "iasdev.hq.eso.org";
	
	/**
	 * The port of the server of the plugin for testing
	 */
	private final int pluginServerPort = 12345;
	
	/**
	 * The object to test
	 */
	private PublisherTest publisher;
	
	@Before
	public void setUp() {
		// Build the publisher
		ThreadGroup threadGroup = new ThreadGroup("Plugin thread group");
		int poolSize = Runtime.getRuntime().availableProcessors()/2;
		ScheduledExecutorService schedExecutorSvc= Executors.newScheduledThreadPool(poolSize, PluginThreadFactory.getThreadFactory());
		publisher = new PublisherTest(pluginId, pluginServerName, pluginServerPort, schedExecutorSvc);
	}
	
	@After
	public void tearDown() {
		// Shuts down the scheduled executor service
	}
	
	@Test
	public void testBasicData() {
		assertNotNull(publisher);
		assertEquals("Plugin-IDs differ",pluginId,publisher.pluginId);
		assertEquals("Servers differ",pluginServerName,publisher.serverName);
		assertEquals("Servers ports",pluginServerPort,publisher.serverPort);
	}

	public PublisherBaseTest() {
		// TODO Auto-generated constructor stub
	}

}
