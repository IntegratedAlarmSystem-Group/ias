package org.eso.ias.converter.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.kafka.streams.kstream.ValueMapper;
import org.eso.ias.cdb.CdbReader;
import org.eso.ias.cdb.CdbWriter;
import org.eso.ias.cdb.json.CdbJsonFiles;
import org.eso.ias.cdb.json.JsonReader;
import org.eso.ias.cdb.json.JsonWriter;
import org.eso.ias.cdb.pojos.IasTypeDao;
import org.eso.ias.cdb.pojos.IasioDao;
import org.eso.ias.converter.Converter;
import org.eso.ias.converter.ConverterKafkaStream;
import org.eso.ias.kafkautils.KafkaHelper;
import org.eso.ias.kafkautils.SimpleStringConsumer;
import org.eso.ias.kafkautils.SimpleStringConsumer.KafkaConsumerListener;
import org.eso.ias.kafkautils.SimpleStringConsumer.StartPosition;
import org.eso.ias.kafkautils.SimpleStringProducer;
import org.eso.ias.prototype.input.java.AlarmSample;
import org.eso.ias.plugin.publisher.MonitorPointData;
import org.eso.ias.prototype.input.java.IASTypes;
import org.eso.ias.prototype.input.java.IASValue;
import org.eso.ias.prototype.input.java.IasValueJsonSerializer;
import org.eso.ias.prototype.input.java.IasValueSerializerException;
import org.eso.ias.prototype.input.java.IasValueStringSerializer;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Test the kafka streaming by pushing string as they were
 * published by plugins and getting strings 
 * in the topic used by the core.
 * <P>
 * {@link #mPointsProducer} pushes the strings in the kafka topic 
 * (the same way a plugins does). 
 * The converter is expected to 
 * <OL>
 * 	<LI>get those strings from the input topic
 * 	<LI>convert the points in {@link IASValue}s
 * 	<LI>push the IASValue (as JSON string) in the output kafka topic
 * </OL>
 * Finally, {@link #mPointsConsumer} gets the records out of the 
 * kafka topic and perform the checks. 
 * <P>
 * The translation to/from json strings published by
 * plugins and those generated for the core is already
 * tested by {@link MapperTester}: this test checks
 * the reading and publishing of strings in the
 * kafka topics as they are produced by the converter.
 * <P>
 * The test builds the {@link MonitorPointData} to publish
 * the kafka topic. The converter is expected to process each
 * of them and publish the {@link IASValue} (as JSON string).
 * <P>
 * The converter needs a CDBReader so TestKafkaStreaming builds a CDB
 * from the monitor points to convert in {@link #cdbParentPath} folder.
 * 
 * @author acaproni
 *
 */
public class TestKafkaStreaming extends ConverterTestBase {
	
	public static class TestStringConsumer implements KafkaConsumerListener {
		
		/**
		 * The values received i.e. those
		 * translated by the converter
		 */
		private List<IASValue<?>> valuesReceived = Collections.synchronizedList(new ArrayList<>(numOfMPointsToSend));
		
		/**
		 * To translate JSON string into {@link IASValue}
		 */
		private IasValueStringSerializer iasSerializer = new  IasValueJsonSerializer();
		
		/**
		 * The count down latch to signal the number of events received
		 */
		private AtomicReference<CountDownLatch> coundownLatch = new AtomicReference<CountDownLatch>(null);
		
		/**
		 * Preprare to get the passed number of events
		 * 
		 * @param n The number of events to receive
		 * @return the latch to wait for events reception
		 */
		public synchronized CountDownLatch reset(int n) {
			if (n<=0) {
				throw new IllegalArgumentException("Invalid number of events to wait for");
			}
			valuesReceived.clear();
			coundownLatch.set(new CountDownLatch(n));
			return coundownLatch.get();
		}
		
		/**
		 * @see org.eso.ias.kafkautils.SimpleStringConsumer.KafkaConsumerListener#stringEventReceived(java.lang.String)
		 */
		@Override
		public synchronized void stringEventReceived(String event) {
			logger.info("Event received [{}]",event);
			IASValue<?> iasValue;
			try {
				iasValue = iasSerializer.valueOf(event);
				valuesReceived.add(iasValue);
				CountDownLatch latch = coundownLatch.get();
				if (latch!=null) {
					latch.countDown();
				}
			} catch (IasValueSerializerException avse) {
				logger.error("Exception pasing [{]] into a IASValue",event);
			}
			
		}
		
		/**
		 * 
		 * @return The number of events received
		 */
		public int numOfEventsReceived() {
			return valuesReceived.size();
		}
		
	}
	
	/**
	 * The conumer of events
	 */
	private static final TestStringConsumer eventsConsumer = new TestStringConsumer();
	
	/**
	 * The identifier of the converter
	 */
	public final String converterID = "KafkaConverterId";
	
	/**
	 * The prefix of the IDs of the monitor points
	 */
	public final String MP_ID_PREFIX ="MonitorPointData-ID";
	
	/**
	 * The base time stamp: each mp has is created with this timestamp 
	 * plus a delta
	 */
	public final long MP_BASE_TIMESTAMP = 100000;
	
	/**
	 * Default list of servers
	 */
	public static final String defaultKafkaBootstrapServers="localhost:9092";
	
	/**
	 * The logger
	 */
	private final static Logger logger = LoggerFactory.getLogger(TestKafkaStreaming.class);
	
	/**
	 * The number of monitor point to send to the converter
	 */
	private static final int numOfMPointsToSend = 50;
	
	/**
	 * The {@link MonitorPointData} to send to the converter
	 */
	private final MonitorPointDataHolder[] mpdToSend = buildMPDToSend(numOfMPointsToSend);
	
	/**
	 * The parent folder is the actual folder
	 */
	public static final Path cdbParentPath =  FileSystems.getDefault().getPath(".");
	
	/**
	 * The directory structure for the JSON CDB
	 */
	private CdbJsonFiles cdbFiles;
	
	/**
	 * The converter to test
	 */
	private Converter converter;
	
	/**
	 * The producer to push mpoints in input to the converter
	 */
	private static SimpleStringProducer mPointsProducer;
	
	/**
	 * The consumer to get IASValue published by the converter
	 */
	private static SimpleStringConsumer mPointsConsumer;
	
	
	
	/**
	 * Build the {@link MonitorPointData} to send to the converter
	 * for translation.
	 * 
	 * @param n The number of {@link MonitorPointData} to create
	 * @return the array of {@link MonitorPointData}
	 */
	private MonitorPointDataHolder[] buildMPDToSend(int n) {
		if (n<=0) {
			throw new IllegalArgumentException("The parameter must be greater then 0");
		}
		Map<IASTypes, Object> mapTypeToObjectValue = new HashMap<>();
		for (int t=0; t<mpdHolders.length; t++) {
			mapTypeToObjectValue.put(mpdHolders[t].iasType, mpdHolders[t].value);
		}
		MonitorPointDataHolder[] ret = new MonitorPointDataHolder[n];
		for (int t=0; t<n; t++) {
			String id = MP_ID_PREFIX+t;
			IASTypes iasType = IASTypes.values()[t%IASTypes.values().length];
			long tStamp = MP_BASE_TIMESTAMP+t;
			// The value to use is taken by mpHolders
			// that associates the proper value to the given type
			Object value = mapTypeToObjectValue.get(iasType);
			
			ret[t] = new MonitorPointDataHolder(id, value, tStamp, iasType);
		}
		return ret;
	}
	
	@BeforeClass
	public static void classInitializer() throws Exception {
		// Build the consumer that takes out of the kafka topic
		// the output of the converter
		mPointsConsumer = new SimpleStringConsumer(
				defaultKafkaBootstrapServers,
				KafkaHelper.IASIOs_TOPIC_NAME,
				"KafkaConverterTest",
				eventsConsumer);
		Properties props = new Properties();
		props.put("auto.offset.reset", "latest");
		mPointsConsumer.setUp(props);

		// Start getting events
		mPointsConsumer.startGettingEvents(StartPosition.END);
		
		// Build the producer that pushes monitor point
		// in the kafka topic
		mPointsProducer = new SimpleStringProducer(
				defaultKafkaBootstrapServers,
				KafkaHelper.PLUGINS_TOPIC_NAME,
				"TestKafkaStreamProducer");
		mPointsProducer.setUp();
	}
	
	@AfterClass
	public static void classCleanup() throws Exception {
		mPointsConsumer.tearDown();
		mPointsProducer.tearDown();
	}
	
	/**
	 * Initialization
	 * 
	 * @throws Exception
	 */
	@Before
	public void setUp() throws Exception {
		// Builds the JSON CDB
		cdbFiles = new CdbJsonFiles(cdbParentPath);
		CdbWriter cdbWriter = new JsonWriter(cdbFiles);
		Set<IasioDao> iasios = new HashSet<>();
		for (MonitorPointDataHolder mpdh: mpdToSend) {
			IasTypeDao iasTypeDao = IasTypeDao.valueOf(mpdh.iasType.toString());
			String id = mpdh.id;
			iasios.add(new IasioDao(id, "A mock description", 300, iasTypeDao));
		}
		cdbWriter.writeIasios(iasios, false);
		
		// The reader to pass to the converter
		CdbReader cdbReader = new JsonReader(cdbFiles);
		
		// Finally builds the converter
		converter = new Converter(converterID, cdbReader, new ConverterKafkaStream(converterID, new Properties()));
		
		converter.setUp();
		
	}
	
	/**
	 * Cleanup
	 */
	@After
	public void tearDown() throws Exception {
		logger.info("Shutting down the converter");
		converter.tearDown();
	}
	
	/**
	 * Check that all the {@link MonitorPointData} published in the kafka
	 * topic are effectively translated and published in the core topic.
	 * <P>
	 * Correctness of translation is tested somewhere else but this test
	 * repeats at least some of the checking.
	 * 
	 * @throws Exception
	 */
	@Test
	public void testTranslationNumber() throws Exception {
		logger.info("testTranslationNumber....");
		CountDownLatch latch = eventsConsumer.reset(numOfMPointsToSend);
		// Pushes all the monitor point in the plugin topic
		for (MonitorPointDataHolder mpdh: mpdToSend) {
			MonitorPointData mpd = buildMonitorPointData(mpdh);
			String mpdString = mpd.toJsonString();
			mPointsProducer.push(mpdString, null, mpdh.id);
			logger.debug("MPD{} sent",mpd.getId());
		}
		mPointsProducer.flush();
		logger.info("{} strings sent",numOfMPointsToSend);
		logger.info("Waiting for the events from the converter...");
		assertTrue("Not all events received!",latch.await(1, TimeUnit.MINUTES));
		assertEquals(numOfMPointsToSend,eventsConsumer.numOfEventsReceived());
		logger.info("Test done");
		
	}
	
	/**
	 * Check that all the {@link MonitorPointData} published in the kafka
	 * topic are effectively translated and published in the core topic.
	 * <P>
	 * Correctness of translation is tested somewhere else but this test
	 * repeats at least some of the checking.
	 * 
	 * @throws Exception
	 */
	@Test
	public void testUnknowMPoints() throws Exception {
		logger.info("testUnknowMPoints starting....");
		CountDownLatch latch =eventsConsumer.reset(2);
		
		// Pushes 2 unknown monitor points
		MonitorPointDataHolder unknown1 = new MonitorPointDataHolder("UNKNOWN-ID1", Long.MAX_VALUE, System.currentTimeMillis(), IASTypes.LONG);
		MonitorPointData mpd = buildMonitorPointData(unknown1);
		String mpdString = mpd.toJsonString();
		mPointsProducer.push(mpdString, null, unknown1.id);
		
		MonitorPointDataHolder unknown2 = new MonitorPointDataHolder("UNKNOWN-ID2", AlarmSample.SET, System.currentTimeMillis(), IASTypes.ALARM);
		mpd = buildMonitorPointData(unknown2);
		mpdString = mpd.toJsonString();
		mPointsProducer.push(mpdString, null, unknown2.id);
		
		logger.info("Waiting for unkonwn monitor point that should never arrived");
		assertFalse("Should not have received any value!",latch.await(1, TimeUnit.MINUTES));
		assertEquals(0,eventsConsumer.numOfEventsReceived());
		
		// After the error.. Does the translation still work?
		latch=eventsConsumer.reset(numOfMPointsToSend);
		// Pushes all the monitor point in the plugin topic
		logger.info("Pushing some mPoint to check if it works after the error");
		for (MonitorPointDataHolder mpdh: mpdToSend) {
			mpd = buildMonitorPointData(mpdh);
			mpdString = mpd.toJsonString();
			mPointsProducer.push(mpdString, null, mpdh.id);
			logger.debug("MPD{} sent",mpd.getId());
		}
		mPointsProducer.flush();
		logger.info("{} strings sent",numOfMPointsToSend);
		logger.info("Waiting for the events from the converter...");
		assertTrue("Not all events received!",latch.await(1, TimeUnit.MINUTES));
		assertEquals(numOfMPointsToSend,eventsConsumer.numOfEventsReceived());
		logger.info("Test done");
		
	}
}
