package org.eso.ias.converter.test;

import org.eso.ias.converter.ValueMapper;
import org.eso.ias.converter.config.IasioConfigurationDAO;
import org.eso.ias.plugin.publisher.MonitorPointData;
import org.eso.ias.types.Alarm;
import org.eso.ias.types.IASTypes;
import org.eso.ias.types.IASValue;
import org.eso.ias.types.NumericArray;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test the mapper function i.e. the function that gets
 * strings received from plugins (i.e. MonitorPointData)
 * and produces string to send to the core of the IAS (i.e. IASValue)
 * 
 * @author acaproni
 *
 */
public class MapperTest extends ConverterTestBase {
	
	/**
	 * The object to test
	 */
	private ValueMapper mapper;

	/**
	 * The logger
	 */
	public static final Logger logger = LoggerFactory.getLogger(MapperTest.class);
	
	@BeforeEach
	public void setUp() {
		logger.info("Setting up");
		// Ensures we are going to test all implemented types
		assertTrue(IASTypes.values().length==mpdHolders.length);
		
		IasioConfigurationDAO testerDao = new TesterConfigDao(mpdHolders);
		
		mapper = new ValueMapper(testerDao, iasValueSerializer, converterID);
	}
	
	/**
	 * Test the behavior of the mapper when tries to translate a unconfigured
	 * monitor point.
	 * <P>
	 * If the monitor point is unconfigured, the translation is
	 * not possible and the mapper return <code>null</code>.
	 * @throws Exception
	 */
	@Test
	public void testUnconfiguredMPD() throws Exception {
		logger.info("Testing testUnconfiguredMPD");
		MonitorPointDataHolder unconfiguredMpdh = 
				new MonitorPointDataHolder(
						"Unrecognized",
						Alarm.CLEARED, 
						System.currentTimeMillis()-100,
						System.currentTimeMillis(),
						IASTypes.ALARM);
		
		MonitorPointData mpd = buildMonitorPointData(unconfiguredMpdh);
		mpd.setPublishTime(org.eso.ias.utils.ISO8601Helper.getTimestamp(System.currentTimeMillis()));
		
		String ret = mapper.apply(mpd.toJsonString());
		assertNull(ret);
		
		// Translate another mp to be sure the previous error
		// did not brake the processor
		MonitorPointData mpd2 = buildMonitorPointData(mpdHolders[0]);
		mpd2.setPublishTime(org.eso.ias.utils.ISO8601Helper.getTimestamp(System.currentTimeMillis()));
		assertNotNull(mapper.apply(mpd2.toJsonString()));
		logger.info("testUnconfiguredMPD done");
	}
	
	/**
	 * Test the translation of each type in the correct {@link IASValue}
	 * 
	 * @throws Exception
	 */
	@Test
	public void testMapping() throws Exception {
		logger.info("Testing testMapping");
		for (MonitorPointDataHolder mpdh: mpdHolders) {
			MonitorPointData mpd = buildMonitorPointData(mpdh);
			mpd.setPublishTime(org.eso.ias.utils.ISO8601Helper.getTimestamp(System.currentTimeMillis()));
			logger.debug("MonitorPointData built {}",mpd);
			String iasValueStr = mapper.apply(mpd.toJsonString());
			assertNotNull(iasValueStr);
			
			IASValue<?> iasValue =  iasValueSerializer.valueOf(iasValueStr);
			logger.debug("Mapped IASValue {} ",iasValue);

			assertEquals(mpdh.iasType, iasValue.valueType);
			assertTrue(iasValue.productionTStamp.isPresent());
			if (iasValue.valueType!=IASTypes.ARRAYOFDOUBLES && iasValue.valueType!=IASTypes.ARRAYOFLONGS) {
				assertEquals(iasValue.value.toString(),mpd.getValue());
			} else {
				assertEquals(((NumericArray)iasValue.value).codeToString(),mpd.getValue().replace(" ",""));
			}
			assertEquals(
					mpd.getProducedByPluginTime(),
					org.eso.ias.utils.ISO8601Helper.getTimestamp(iasValue.productionTStamp.get()));
			
			// The timestamp when the value is sent to the converter is set by the mapper
			// so we expect sentToConverterTStamp to represent a timestamp close to the actual date
			assertTrue(iasValue.sentToConverterTStamp.isPresent());
			long now = System.currentTimeMillis();
			assertTrue(iasValue.sentToConverterTStamp.get()<=now && iasValue.sentToConverterTStamp.get()>now-1000);
			
			assertEquals(mpdh.id, iasValue.id,"Identifier differs");

			if (iasValue.valueType!=IASTypes.ARRAYOFDOUBLES && iasValue.valueType!=IASTypes.ARRAYOFLONGS) {
				assertEquals(mpdh.value,iasValue.value,"Values differ");
			} else {
				assertEquals(((NumericArray)iasValue.value).codeToString(),mpd.getValue().replace(" ",""));
			}
		}
	}

}
