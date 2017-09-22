package org.eso.ias.converter.test;

import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.eso.ias.cdb.CdbReader;
import org.eso.ias.cdb.CdbWriter;
import org.eso.ias.cdb.json.CdbJsonFiles;
import org.eso.ias.cdb.json.JsonReader;
import org.eso.ias.cdb.json.JsonWriter;
import org.eso.ias.cdb.pojos.IasTypeDao;
import org.eso.ias.cdb.pojos.IasioDao;
import org.eso.ias.converter.Converter;
import org.eso.ias.converter.ConverterKafkaStream;
import org.eso.ias.kafkautils.SimpleStringConsumer;
import org.eso.ias.kafkautils.SimpleStringConsumer.KafkaConsumerListener;
import org.eso.ias.kafkautils.SimpleStringProducer;
import org.eso.ias.plugin.publisher.MonitorPointData;
import org.eso.ias.prototype.input.java.IASTypes;
import org.eso.ias.prototype.input.java.IASValue;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * Test the kafka streaming by pushing string as they were
 * published by plugins and getting strings 
 * in the topic use by the core.
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
public class TestKafkaStreaming extends ConverterTestBase implements KafkaConsumerListener {
	
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
	
	/**
	 * The number of monitor point to send to the converter
	 */
	private static final int numOfMPointsToSend = 10;
	
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
	private SimpleStringProducer mPointsProducer;
	
	/**
	 * The consumer to get IASValue published by the converter
	 */
	private SimpleStringConsumer mPointsConsumer;
	
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
		converter = new Converter(converterID, cdbReader);
		converter.setUp();
		
		// Build the consumer that takes out of the kafka topic
		// the output of the converter
		mPointsConsumer = new SimpleStringConsumer(
				defaultKafkaBootstrapServers,
				ConverterKafkaStream.DEFAULTCOREKTOPICNAME,
				this);
		mPointsConsumer.setUp();
		
		// Build the producer that pushes monitor point
		// in the kafka topic
		mPointsProducer = new SimpleStringProducer(
				defaultKafkaBootstrapServers,
				ConverterKafkaStream.DEFAULTPLUGINSINPUTKTOPICNAME,
				"TestKafkaStreamProducer");
		mPointsProducer.setUp();
	}
	
	/**
	 * Cleanup
	 */
	@After
	public void tearDown() throws Exception {
		converter.tearDown();
		mPointsConsumer.tearDown();
		mPointsProducer.tearDown();
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
		// Pushes all the monitor point in the plugin topic
		for (MonitorPointDataHolder mpdh: mpdToSend) {
			MonitorPointData mpd = buildMonitorPointData(mpdh);
			String mpdString = mpd.toJsonString();
			mPointsProducer.push(mpdString, null, mpdh.id);
		}
		mPointsProducer.flush();
		
	}

	/* (non-Javadoc)
	 * @see org.eso.ias.kafkautils.SimpleStringConsumer.KafkaConsumerListener#stringEventReceived(java.lang.String)
	 */
	@Override
	public void stringEventReceived(String event) {
		System.out.println("====>"+event);
		
	}
}
