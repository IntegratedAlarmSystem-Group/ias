package org.eso.ias.converter.test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.eso.ias.converter.ValueMapper;
import org.eso.ias.converter.config.ConfigurationException;
import org.eso.ias.converter.config.IasioConfigurationDAO;
import org.eso.ias.converter.config.MonitorPointConfiguration;
import org.eso.ias.plugin.AlarmSample;
import org.eso.ias.plugin.Sample;
import org.eso.ias.plugin.ValueToSend;
import org.eso.ias.plugin.filter.FilteredValue;
import org.eso.ias.plugin.publisher.MonitorPointData;
import org.eso.ias.prototype.input.java.IASTypes;
import org.eso.ias.prototype.input.java.IASValue;
import org.eso.ias.prototype.input.java.IasValueJsonSerializer;
import org.eso.ias.prototype.input.java.IasValueStringSerializer;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

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
	
	@Before
	public void setUp() {
		// Ensures we are going to test all implemented types
		assertTrue(IASTypes.values().length==mpdHolders.length);
		
		IasioConfigurationDAO testerDao = new TesterConfigDao(mpdHolders);
		
		mapper = new ValueMapper(testerDao, iasValueSerializer, converterID);
	}
	
	@After
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
				new MonitorPointDataHolder("Unrecognized", AlarmSample.CLEARED, System.currentTimeMillis(), IASTypes.ALARM);
		
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
			assertEquals(mpdh.timestamp, iasValue.timestamp);
			assertEquals(mpdh.id, iasValue.id);
			assertEquals(mpdh.value,iasValue.value);
		}
	}

}
