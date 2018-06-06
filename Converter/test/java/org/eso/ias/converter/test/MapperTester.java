package org.eso.ias.converter.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.eso.ias.converter.ValueMapper;
import org.eso.ias.converter.config.IasioConfigurationDAO;
import org.eso.ias.types.Alarm;
import org.eso.ias.plugin.publisher.MonitorPointData;
import org.eso.ias.types.IASTypes;
import org.eso.ias.types.IASValue;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Test the mapper function i.e. the function that gets
 * strings received from plugins (i.e. MonitorPointData)
 * and produces string to send to the core of the IAS (i.e. IASValue)
 * 
 * @author acaproni
 *
 */
public class MapperTester extends ConverterTestBase {
	
	/**
	 * The object to test
	 */
	private ValueMapper mapper;
	
	@BeforeEach
	public void setUp() {
		// Ensures we are going to test all implemented types
		assertTrue(IASTypes.values().length==mpdHolders.length);
		
		IasioConfigurationDAO testerDao = new TesterConfigDao(mpdHolders);
		
		mapper = new ValueMapper(testerDao, iasValueSerializer, converterID);
	}
	
	@AfterEach
	public void tearDown() {
		
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
		MonitorPointDataHolder unconfiguredMpdh = 
				new MonitorPointDataHolder(
						"Unrecognized",
						Alarm.CLEARED, 
						System.currentTimeMillis()-100,
						System.currentTimeMillis(),
						IASTypes.ALARM);
		
		MonitorPointData mpd = buildMonitorPointData(unconfiguredMpdh);
		
		String ret = mapper.apply(mpd.toJsonString());
		assertNull(ret);
		
		// Translate another mp to be sure the previous error
		// did not brake the processor
		assertNotNull(mapper.apply(buildMonitorPointData(mpdHolders[0]).toJsonString()));
	}
	
	/**
	 * Test the translation of each type in the correct {@link IASValue}
	 * 
	 * @throws Exception
	 */
	@Test
	public void testMapping() throws Exception {
		for (MonitorPointDataHolder mpdh: mpdHolders) {
			MonitorPointData mpd = buildMonitorPointData(mpdh);
			String iasValueStr = mapper.apply(mpd.toJsonString());
			assertNotNull(iasValueStr);
			
			IASValue<?> iasValue =  iasValueSerializer.valueOf(iasValueStr);
			
			assertEquals(mpdh.iasType, iasValue.valueType);
			assertTrue(iasValue.pluginProductionTStamp.isPresent());
			assertEquals(Long.valueOf(mpdh.pluginProductionTSamp), iasValue.pluginProductionTStamp.get());
			
			// The timestamp when the value is sent to the converter is set by the mapper
			// so we expect sentToConverterTStamp to represent a timestamp close to the actual date
			assertTrue(iasValue.sentToConverterTStamp.isPresent());
			long now = System.currentTimeMillis();
			assertTrue(iasValue.sentToConverterTStamp.get()<=now && iasValue.sentToConverterTStamp.get()>now-1000);
			
			assertEquals(mpdh.id, iasValue.id);
			assertEquals(mpdh.value,iasValue.value);
		}
	}

}
