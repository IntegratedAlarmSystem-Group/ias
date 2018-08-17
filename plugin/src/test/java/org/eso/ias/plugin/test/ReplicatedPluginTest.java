package org.eso.ias.plugin.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Properties;
import java.util.Vector;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.eso.ias.heartbeat.HbEngine;
import org.eso.ias.heartbeat.HbMsgSerializer;
import org.eso.ias.heartbeat.HbProducer;
import org.eso.ias.heartbeat.serializer.HbJsonSerializer;
import org.eso.ias.plugin.Plugin;
import org.eso.ias.plugin.Sample;
import org.eso.ias.plugin.ValueToSend;
import org.eso.ias.plugin.config.Value;
import org.eso.ias.plugin.filter.FilteredValue;
import org.eso.ias.plugin.filter.Filter.EnrichedSample;
import org.eso.ias.plugin.publisher.MonitorPointSender;
import org.eso.ias.plugin.publisher.PublisherException;
import org.eso.ias.types.IasValidity;
import org.eso.ias.types.Identifier;
import org.eso.ias.types.IdentifierType;
import org.eso.ias.types.OperationalMode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Test the Ids of a replicated plugin
 * 
 * @author acaproni
 *
 */
public class ReplicatedPluginTest {
	
	/**
	 * Sender saves the values published by the Plugin
	 * in the {@link #plubishedValues} list.
	 *  
	 * @author acaproni
	 *
	 */
	public class Sender implements MonitorPointSender {
		
		/**
		 * The values to be sent to the BSDB
		 */
		public final List<ValueToSend> plubishedValues = new ArrayList<>();
		
		/**
		 * Monitor points are added to the list
		 */
		@Override
		public void offer(ValueToSend monitorPoint) {
			ReplicatedPluginTest.logger.info("ValueReceived {}",monitorPoint);
			plubishedValues.add(monitorPoint);
		}

		@Override
		public SenderStats getStats() {
			return null;
		}

		@Override
		public void setUp() throws PublisherException {
			plubishedValues.clear();
		}

		@Override
		public void tearDown() throws PublisherException {	
			plubishedValues.clear();
		}

		@Override
		public void startSending() {}

		@Override
		public void stopSending() {}

		@Override
		public boolean isStopped() {
			return false;
		}

		@Override
		public boolean isClosed() {
			return false;
		}
		
	}
	
	
	
	/**
	 * The plugin to test
	 */
	private Plugin replicatedPlugin;
	
	/**
	 * The instance of the replicated plugins
	 */
	private final int instance = 7;
	
	/**
	 * The sender to get and test the values published by the plugin
	 */
	private Sender monitorPointSender;
	
	private final String monitoredSystemId = "MonSysId";
	private Identifier monSysIdentifier = new Identifier(monitoredSystemId, IdentifierType.MONITORED_SOFTWARE_SYSTEM);
	
	/**
	 * The identifier of the plugin 
	 */
	private final String pluginId = "ReplicatedPluginId";
	private Identifier pluginIdentifier = new Identifier(pluginId, IdentifierType.PLUGIN,monSysIdentifier);
	
	private final Value v1 = new Value();
	
	
	private final Value v2 = new Value();
	
	
	private final Value v3 = new Value();
	
	/**
	 * The logger
	 */
	private static final Logger logger = LoggerFactory.getLogger(ReplicatedPluginTest.class);
	
	@BeforeEach
	public void setUp() {
		monitorPointSender = new Sender();
		
		v1.setId("Value-ID1");
		v1.setRefreshTime(1000);
		v2.setId("Value-ID2");
		v2.setRefreshTime(2000);
		v3.setId("Value-ID3");
		v3.setRefreshTime(3000);
		Collection<Value> values = new ArrayList<Value>();
		values.add(v1);
		values.add(v2);
		values.add(v3);
		
		// Format of the string with the HBs
		HbMsgSerializer hbSerializer = new HbJsonSerializer();
		
		// Build the producer for sending HBs
		HbProducer hbProd = new MockHeartBeatProd(hbSerializer);
		
		replicatedPlugin = new Plugin(
				pluginId, 
				monitoredSystemId, 
				values, 
				new Properties(), 
				monitorPointSender, 
				null, //defaultFilter
				null, //defaultFilterOptions
				2,
				instance,
				1,
				hbProd);
	}
	
	/**
	 * Test the ID of the plugin
	 * 
	 * @throws Exception
	 */
	@Test
	public void testPluginId() throws Exception {
		String id = replicatedPlugin.pluginId;
		assertTrue(Identifier.isTemplatedIdentifier(id));
		assertTrue(Identifier.buildIdFromTemplate(pluginId, instance).equals(id));
	}
	
	/**
	 * Test the IDs of the values sent to the BSDB
	 * 
	 * @throws Exception
	 */
	@Test
	public void testIdsOfValues() throws Exception {
		List<EnrichedSample> samples = new ArrayList<>();
		Sample s = new Sample("Test");
		EnrichedSample eSample = new EnrichedSample(s, true);
		samples.add(eSample);
		
		FilteredValue fv1 = new FilteredValue("", samples, System.currentTimeMillis());
		ValueToSend vts1 = new ValueToSend(
				v1.getId(), 
				fv1, 
				OperationalMode.OPERATIONAL, 
				IasValidity.RELIABLE);
		replicatedPlugin.monitoredValueUpdated(vts1);
		
		assertEquals(1,monitorPointSender.plubishedValues.size());
		ValueToSend sentValue = monitorPointSender.plubishedValues.remove(0);
		assertEquals(0,monitorPointSender.plubishedValues.size());
		
		assertTrue(Identifier.isTemplatedIdentifier(sentValue.id));
		assertTrue(Identifier.buildIdFromTemplate(v1.getId(), instance).equals(sentValue.id));
		
		FilteredValue fv2 = new FilteredValue("", samples, System.currentTimeMillis());
		ValueToSend vts2 = new ValueToSend(
				v2.getId(), 
				fv2, 
				OperationalMode.OPERATIONAL, 
				IasValidity.RELIABLE);
		replicatedPlugin.monitoredValueUpdated(vts2);
		
		assertEquals(1,monitorPointSender.plubishedValues.size());
		sentValue = monitorPointSender.plubishedValues.remove(0);
		assertEquals(0,monitorPointSender.plubishedValues.size());
		
		assertTrue(Identifier.isTemplatedIdentifier(sentValue.id));
		assertTrue(Identifier.buildIdFromTemplate(v2.getId(), instance).equals(sentValue.id));
		
		FilteredValue fv3 = new FilteredValue("", samples, System.currentTimeMillis());
		ValueToSend vts3 = new ValueToSend(
				v3.getId(), 
				fv3, 
				OperationalMode.OPERATIONAL, 
				IasValidity.RELIABLE);
		replicatedPlugin.monitoredValueUpdated(vts3);
		
		assertEquals(1,monitorPointSender.plubishedValues.size());
		sentValue = monitorPointSender.plubishedValues.remove(0);
		assertEquals(0,monitorPointSender.plubishedValues.size());
		
		assertTrue(Identifier.isTemplatedIdentifier(sentValue.id));
		assertTrue(Identifier.buildIdFromTemplate(v3.getId(), instance).equals(sentValue.id));
	}

}
