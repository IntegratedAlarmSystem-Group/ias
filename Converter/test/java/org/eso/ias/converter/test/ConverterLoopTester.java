package org.eso.ias.converter.test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertEquals;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import org.eso.ias.cdb.CdbReader;
import org.eso.ias.cdb.json.CdbFiles;
import org.eso.ias.cdb.json.CdbFolders;
import org.eso.ias.cdb.json.CdbJsonFiles;
import org.eso.ias.cdb.json.JsonReader;
import org.eso.ias.cdb.pojos.IasioDao;
import org.eso.ias.converter.Converter;
import org.eso.ias.converter.config.IasioConfigurationDaoImpl;
import org.eso.ias.converter.corepublisher.CoreFeeder;
import org.eso.ias.converter.corepublisher.ListenerPublisher;
import org.eso.ias.converter.corepublisher.ListenerPublisher.CoreFeederListener;
import org.eso.ias.converter.pluginconsumer.RawDataReader;
import org.eso.ias.converter.test.CommonHelpers.MonitorPointsBuilderHelper;
import org.eso.ias.converter.test.pluginconsumer.PluginSenderSimulator;
import org.eso.ias.plugin.AlarmSample;
import org.eso.ias.plugin.publisher.MonitorPointData;
import org.eso.ias.prototype.input.java.IASValue;
import org.eso.ias.prototype.input.java.IasValueJsonSerializer;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Test the {@link Converter} loop i.e. from
 * the receiving of events produced by plugins (simulated),
 * to the receiving of data by the publisher (simulated).
 * <P>
 * The test is done by creating a setting of monitor points data 
 * as if they were created by plugins.
 * The monitor points are submitted to the {@link PluginSenderSimulator}
 * that is the {@link RawDataReader} of the converter.
 * <BR>
 * The {@link IASValue}s produced after conversion by the {@link Converter}
 * are received by the {@link ListenerPublisher} previously set in the
 * {@link Converter} as {@link CoreFeeder}.
 * <P>
 * This closes the loop from the sending to the publishing of the
 * converted monitor point values and alarms.
 * <P>
 * Specific data producers and consumers (in particular those
 * for kafka) will be tested elsewhere. 
 * 
 * @author acaproni
 *
 */
public class ConverterLoopTester implements CoreFeederListener {
	
	/**
	 * The logger
	 */
	private final static Logger logger = LoggerFactory.getLogger(ConverterLoopTester.class);
	
	/**
	 * The object to test
	 */
	private Converter converter;
	
	/**
	 * The folder struct of the CDB
	 */
	private CdbFiles cdbFiles;
	
	/**
	 * The configuraton DAO for the converter
	 */
	private IasioConfigurationDaoImpl configDao;
	
	/**
	 * The reader to get data published by plugins (simulated)
	 */
	private RawDataReader rawDataReader = new PluginSenderSimulator();
	
	/**
	 * The number of events processed by the converter
	 */
	private AtomicInteger processedEvents = new AtomicInteger(0);
	
	@Before
	public void setUp() throws Exception {
		// Remove any CDB folder if present
		CdbFolders.ROOT.delete(CommonHelpers.cdbParentPath);
		assertFalse(CdbFolders.ROOT.exists(CommonHelpers.cdbParentPath));
		cdbFiles = new CdbJsonFiles(CommonHelpers.cdbParentPath);
		CdbReader cdbReader = new JsonReader(cdbFiles);
		configDao = new IasioConfigurationDaoImpl(cdbReader);
		
		ListenerPublisher listenerPublisher = new ListenerPublisher(this, new IasValueJsonSerializer());
		converter = new Converter("ConverterID", rawDataReader, cdbReader, listenerPublisher);
	}
	
	@After
	public void tearDown() throws Exception {
		configDao.tearDown();
		CdbFolders.ROOT.delete(CommonHelpers.cdbParentPath);
		assertFalse(CdbFolders.ROOT.exists(CommonHelpers.cdbParentPath));
		converter.tearDown();
	}

	/**
	 * Check if the converter forward all the events to the core 
	 * 
	 * @throws Exception
	 */
	@Test
	public void testNumOfEvents() throws Exception {
		int numOfEvents = 5000;
		Set<IasioDao> iasios = CommonHelpers.biuldIasios(numOfEvents);
		Set<MonitorPointsBuilderHelper> mpData = new HashSet<>();
		
		for (IasioDao iasio: iasios) {
			Object value=null;
			switch (iasio.getIasType()) {
			case LONG: value = Long.valueOf(1234455667); break;
			case INT: value =Integer.valueOf(321456); break;
			case SHORT: value =Short.valueOf("121"); break;
			case BYTE: value =Byte.valueOf("10"); break;
			case DOUBLE: value =Double.valueOf(2234.6589); break;
			case FLOAT: value =Float.valueOf(554466.8702f); break;
			case BOOLEAN: value=Boolean.FALSE; break; 
			case CHAR: value = Character.valueOf('X'); break;
			case STRING: value="The string"; break;
			case ALARM: value = AlarmSample.SET; break;
			default: throw new UnsupportedOperationException("Unrecognized type "+iasio.getIasType());
			}
			mpData.add(new MonitorPointsBuilderHelper(iasio, value));
		}
		
		// The monitor points sent by the plugin
		Set<MonitorPointData> monitorPointsFromPlugin = CommonHelpers.buildMonitorPointDatas(mpData, "PluginID", "MonitoredSystemID");
		
		// Inject the monitor point values into the simulated plugin
		for (MonitorPointData mpd: monitorPointsFromPlugin) {
			((PluginSenderSimulator)rawDataReader).addMonitorPointToReceive(mpd);
		}
		
		configDao.setUp();
		converter.setUp();
		
		// Give the converter time to convert all the data
		long startTime = System.currentTimeMillis();
		while (((PluginSenderSimulator)rawDataReader).size()>0 && System.currentTimeMillis()<startTime+60000) {
			int remamining = ((PluginSenderSimulator)rawDataReader).size();
			logger.info("Give the converter time to consume all the events ({} remaining)",remamining);
			Thread.sleep(1000);
		}
		assertEquals(numOfEvents,processedEvents.get());
	}
	
	/**
	 * Test what happens if the configuration of a monitor point
	 * does not exists: we expect that the event is not forwarded
	 * to the core but it does not have effects on the other
	 * values
	 * 
	 * @throws Exception
	 */
//	@Test
//	public void testMpointNotFound() throws Exception {
//		
//	}

	@Override
	public void rawDataPublished(String strRepresentation) {}

	@Override
	public void dataPublished(IASValue<?> iasValue) {
		processedEvents.incrementAndGet();
		logger.info("IASValue received: {}",iasValue.id);
		
	}
}
