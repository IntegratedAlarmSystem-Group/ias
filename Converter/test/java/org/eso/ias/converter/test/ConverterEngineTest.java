package org.eso.ias.converter.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Vector;

import org.eso.ias.converter.ConverterEngineImpl;
import org.eso.ias.plugin.AlarmSample;
import org.eso.ias.plugin.Sample;
import org.eso.ias.plugin.ValueToSend;
import org.eso.ias.plugin.filter.FilteredValue;
import org.eso.ias.plugin.publisher.MonitorPointData;
import org.eso.ias.prototype.input.java.IASTypes;
import org.eso.ias.prototype.input.java.IASValue;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class ConverterEngineTest {
	
	/**
	 * The ID of the converter
	 */
	private final String converterIDForTest="ConverterID";
	
	/**
	 * The ID of the plugin that generated the monitor point
	 */
	private final String pluginId= "SimulatedPluginID";
	
	private final ConverterEngineImpl engine = new ConverterEngineImpl(converterIDForTest);
	
	/**
	 * The map of monitor point values and alarms to translate for testing.
	 * <P>
	 * There is one MP per each type to convert
	 */
	private final Map<IASTypes, MonitorPointData> mpPointsToTranslate = new HashMap<>();
	
	/**
	 * Fill the map with the MP to translate: one for each
	 * possible IAS type
	 * 
	 * @param mPoints The map of monitor points
	 */
	private void popluateMap(Map<IASTypes, MonitorPointData> mPoints) {
		Objects.requireNonNull(mPoints);
		
		// Empty list of samples
		List<Sample> samples = new Vector<>();
		Sample s = new Sample("empty");
		samples.add(s);
		
		FilteredValue fv = new FilteredValue(Integer.valueOf(1011), samples, 1000);
		ValueToSend vts = new ValueToSend("Int-ValueToSend", fv);
		MonitorPointData mpd = new MonitorPointData(pluginId, vts);
		mpPointsToTranslate.put(IASTypes.INT, mpd);
		
		fv = new FilteredValue(Long.valueOf(110550), samples, 1100);
		vts = new ValueToSend("Long-ValueToSend", fv);
		mpd = new MonitorPointData(pluginId, vts);
		mpPointsToTranslate.put(IASTypes.LONG, mpd);
		
		fv = new FilteredValue(Short.valueOf((short)125), samples, 1200);
		vts = new ValueToSend("Short-ValueToSend", fv);
		mpd = new MonitorPointData(pluginId, vts);
		mpPointsToTranslate.put(IASTypes.SHORT, mpd);
		
		fv = new FilteredValue(Byte.valueOf((byte)5), samples, 1300);
		vts = new ValueToSend("Byte-ValueToSend", fv);
		mpd = new MonitorPointData(pluginId, vts);
		mpPointsToTranslate.put(IASTypes.BYTE, mpd);
		
		fv = new FilteredValue(Float.valueOf((float)133.6), samples, 1400);
		vts = new ValueToSend("Float-ValueToSend", fv);
		mpd = new MonitorPointData(pluginId, vts);
		mpPointsToTranslate.put(IASTypes.FLOAT, mpd);
		
		fv = new FilteredValue(Double.valueOf((double)1334433.676), samples, 1500);
		vts = new ValueToSend("Double-ValueToSend", fv);
		mpd = new MonitorPointData(pluginId, vts);
		mpPointsToTranslate.put(IASTypes.DOUBLE, mpd);
		
		fv = new FilteredValue(Boolean.FALSE, samples, 1600);
		vts = new ValueToSend("Boolean-ValueToSend", fv);
		mpd = new MonitorPointData(pluginId, vts);
		mpPointsToTranslate.put(IASTypes.BOOLEAN, mpd);
		
		fv = new FilteredValue(Character.valueOf('X'), samples, 1700);
		vts = new ValueToSend("Char-ValueToSend", fv);
		mpd = new MonitorPointData(pluginId, vts);
		mpPointsToTranslate.put(IASTypes.CHAR, mpd);
		
		fv = new FilteredValue("A string", samples, 1800);
		vts = new ValueToSend("String-ValueToSend", fv);
		mpd = new MonitorPointData(pluginId, vts);
		mpPointsToTranslate.put(IASTypes.STRING, mpd);
		
		fv = new FilteredValue(AlarmSample.SET, samples, 1900);
		vts = new ValueToSend("Alarm-ValueToSend", fv);
		mpd = new MonitorPointData(pluginId, vts);
		mpPointsToTranslate.put(IASTypes.ALARM, mpd);
		
	}

	@Before
	public void setUp() {
		popluateMap(mpPointsToTranslate);
	}
	
	@After
	public void tearDown() {
		
	}
	
	/**
	 * Test If the ID of the plugin is preserved.
	 * 
	 * @throws Exception
	 */
	@Test
	public void testConverterID() throws Exception {
		assertEquals("Wrong ID",converterIDForTest,engine.converterID);
	}

	/**
	 * Test the translation of monitor point values and alarms (primitive types)
	 * 
	 * @throws Exception
	 */
	@Test
	public void testPrimitiveTypesConversion() throws Exception {
		for (IASTypes iasType: mpPointsToTranslate.keySet()) {
			MonitorPointData mpd = mpPointsToTranslate.get(iasType);
			assertNotNull(mpd);
			
			IASValue<?> translatedValue=engine.translate(mpd, iasType);
			assertEquals(mpd.getId(), translatedValue.id);
			assertEquals(iasType, translatedValue.valueType);
		}
	}
}
