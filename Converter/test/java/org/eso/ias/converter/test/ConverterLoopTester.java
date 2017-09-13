package org.eso.ias.converter.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import org.eso.ias.cdb.CdbReader;
import org.eso.ias.cdb.CdbWriter;
import org.eso.ias.cdb.json.CdbFiles;
import org.eso.ias.cdb.json.CdbFolders;
import org.eso.ias.cdb.json.CdbJsonFiles;
import org.eso.ias.cdb.json.JsonReader;
import org.eso.ias.cdb.json.JsonWriter;
import org.eso.ias.cdb.pojos.IasTypeDao;
import org.eso.ias.cdb.pojos.IasioDao;
import org.eso.ias.converter.Converter;
import org.eso.ias.converter.corepublisher.CoreFeeder;
import org.eso.ias.converter.corepublisher.ListenerPublisher;
import org.eso.ias.converter.corepublisher.ListenerPublisher.CoreFeederListener;
import org.eso.ias.converter.pluginconsumer.RawDataReader;
import org.eso.ias.converter.test.CommonHelpers.MonitorPointsBuilderHelper;
import org.eso.ias.converter.test.pluginconsumer.PluginSenderSimulator;
import org.eso.ias.plugin.Sample;
import org.eso.ias.plugin.ValueToSend;
import org.eso.ias.plugin.filter.FilteredValue;
import org.eso.ias.plugin.publisher.MonitorPointData;
import org.eso.ias.prototype.input.java.IASTypes;
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
 * <P>
 * The correctness of the conversion is tested by {@link ConverterEngineTest}
 * so there is no need to repeat the test here: we check only the type.
 * 
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
	private final CdbFiles cdbFiles;
	
	/**
	 * The reader to get data published by plugins (simulated)
	 */
	private RawDataReader rawDataReader = new PluginSenderSimulator();
	
	/**
	 * The reader of the CDB
	 */
	private final CdbReader cdbReader;
	
	/**
	 * The number of events processed by the converter
	 */
	private AtomicInteger processedEvents = new AtomicInteger(0);
	
	/**
	 * The number of RAW events processed by the converter
	 */
	private AtomicInteger processedRawEvents = new AtomicInteger(0);
	
	/**
	 * Constructor 
	 * @throws Exception
	 */
	public ConverterLoopTester() throws Exception {
		cdbFiles = new CdbJsonFiles(CommonHelpers.cdbParentPath);
		cdbReader = new JsonReader(cdbFiles);
	}
	
	@Before
	public void setUp() throws Exception {
		// Remove any CDB folder if present
		CdbFolders.ROOT.delete(CommonHelpers.cdbParentPath);
		assertFalse(CdbFolders.ROOT.exists(CommonHelpers.cdbParentPath));
		
		ListenerPublisher listenerPublisher = new ListenerPublisher(this, new IasValueJsonSerializer());
		converter = new Converter("ConverterID", rawDataReader, cdbReader, listenerPublisher);
	}
	
	@After
	public void tearDown() throws Exception {
		CdbFolders.ROOT.delete(CommonHelpers.cdbParentPath);
		assertFalse(CdbFolders.ROOT.exists(CommonHelpers.cdbParentPath));
		converter.tearDown();
	}

	/**
	 * Check if the converter forward all the events to the core.
	 * 
	 * @throws Exception
	 */
	@Test
	public void testNumOfEvents() throws Exception {
		int numOfEvents = 5000;
		Set<IasioDao> iasios = CommonHelpers.biuldIasios(numOfEvents);
		Set<MonitorPointsBuilderHelper> mpData = new HashSet<>();
		
		for (IasioDao iasio: iasios) {
			Object value=CommonHelpers.buildIasioValue(iasio.getIasType());
			mpData.add(new MonitorPointsBuilderHelper(iasio, value));
		}
		CdbWriter jsonCdbWriter = new JsonWriter(cdbFiles);
		jsonCdbWriter.writeIasios(iasios, false);
		
		// The monitor points sent by the plugin
		Set<MonitorPointData> monitorPointsFromPlugin = CommonHelpers.buildMonitorPointDatas(mpData, "PluginID", "MonitoredSystemID");
		
		// Inject the monitor point values into the simulated plugin
		for (MonitorPointData mpd: monitorPointsFromPlugin) {
			((PluginSenderSimulator)rawDataReader).addMonitorPointToReceive(mpd);
		}
		
		converter.setUp();
		
		// Give the converter time to convert all the data
		long startTime = System.currentTimeMillis();
		while (((PluginSenderSimulator)rawDataReader).size()>0 && System.currentTimeMillis()<startTime+60000) {
			int remamining = ((PluginSenderSimulator)rawDataReader).size();
			logger.info("Give the converter time to consume all the events ({} remaining)",remamining);
			Thread.sleep(1000);
		}
		assertEquals(numOfEvents,processedEvents.get());
		assertEquals(numOfEvents,processedRawEvents.get());
	}
	
	/**
	 * Test what happens if the configuration of a monitor point
	 * does not exists: we expect that the event is not forwarded
	 * to the core but it does not have effects on the other
	 * values.
	 * 
	 * @throws Exception
	 */
	@Test
	public void testMpointNotFound() throws Exception {
		int numOfEvents = 10;
		Set<IasioDao> iasios = CommonHelpers.biuldIasios(numOfEvents);
		Set<MonitorPointsBuilderHelper> mpData = new HashSet<>();
		
		for (IasioDao iasio: iasios) {
			Object value=CommonHelpers.buildIasioValue(iasio.getIasType());
			mpData.add(new MonitorPointsBuilderHelper(iasio, value));
		}
		CdbWriter jsonCdbWriter = new JsonWriter(cdbFiles);
		jsonCdbWriter.writeIasios(iasios, false);
		
		// The monitor points sent by the plugin
		Set<MonitorPointData> monitorPointsFromPlugin = CommonHelpers.buildMonitorPointDatas(mpData, "PluginID", "MonitoredSystemID");
		
		// Inject the monitor point values into the simulated plugin
		for (MonitorPointData mpd: monitorPointsFromPlugin) {
			((PluginSenderSimulator)rawDataReader).addMonitorPointToReceive(mpd);
		}
		
		converter.setUp();
		
		// Give the converter time to convert all the data
		long startTime = System.currentTimeMillis();
		while (((PluginSenderSimulator)rawDataReader).size()>0 && System.currentTimeMillis()<startTime+60000) {
			int remamining = ((PluginSenderSimulator)rawDataReader).size();
			logger.info("Give the converter time to consume all the events ({} remaining)",remamining);
			Thread.sleep(1000);
		}
		assertEquals(numOfEvents,processedEvents.get());
		
		// Send a unconfigured monitor point
		List<Sample> samples = new ArrayList<>();
		Sample s = new Sample(Long.valueOf(222));
		samples.add(s);
		FilteredValue fv = new FilteredValue(Long.valueOf(111), samples, System.currentTimeMillis());
		ValueToSend unconfVts= new ValueToSend("NotConfigured-ID", fv);
		MonitorPointData unconfiguredMPD = new MonitorPointData("AnotherPlufingID", "AnotherSysID", unconfVts);
		((PluginSenderSimulator)rawDataReader).addMonitorPointToReceive(unconfiguredMPD);
		
		for (MonitorPointData mpd: monitorPointsFromPlugin) {
			((PluginSenderSimulator)rawDataReader).addMonitorPointToReceive(mpd);
		}
		
		// Give the converter time to convert all the data
		startTime = System.currentTimeMillis();
		while (((PluginSenderSimulator)rawDataReader).size()>0 && System.currentTimeMillis()<startTime+60000) {
			int remamining = ((PluginSenderSimulator)rawDataReader).size();
			logger.info("Give the converter time to consume all the events ({} remaining)",remamining);
			Thread.sleep(1000);
		}
		assertEquals(2*numOfEvents,processedEvents.get());
		assertEquals(2*numOfEvents,processedRawEvents.get());
	}

	@Override
	public void rawDataPublished(String strRepresentation) {
		Objects.requireNonNull(strRepresentation);
		processedRawEvents.incrementAndGet();
	}

	@Override
	public void dataPublished(IASValue<?> iasValue) {
		Objects.requireNonNull(iasValue);
		processedEvents.incrementAndGet();
		
		IasTypeDao iasTypeDao = CommonHelpers.typeOfId(iasValue.id);
		IASTypes iasTypeValue = iasValue.valueType;
		assertEquals(iasTypeDao.toString(), iasTypeValue.toString());
		logger.info("IASValue received: {}",iasValue.id);
		
	}
}
