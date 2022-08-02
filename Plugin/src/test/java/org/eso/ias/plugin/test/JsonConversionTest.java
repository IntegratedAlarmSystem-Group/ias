package org.eso.ias.plugin.test;

import java.util.List;
import java.util.Vector;

import org.eso.ias.types.Alarm;
import org.eso.ias.types.IasValidity;
import org.eso.ias.types.OperationalMode;
import org.eso.ias.plugin.Sample;
import org.eso.ias.plugin.ValueToSend;
import org.eso.ias.plugin.filter.Filter.EnrichedSample;
import org.eso.ias.plugin.filter.FilteredValue;
import org.eso.ias.plugin.publisher.MonitorPointData;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Test the conversion to/from java and json
 * of {@link MonitorPointData}.
 * 
 * @author acaproni
 *
 */
public class JsonConversionTest {
	
	/**
	 * The ID of the plugin
	 */
	private final String pluginID = "TheIdOfThePlugin";
	
	/**
	 * The id of the system monitored by the plugin.
	 */
	private final String monSysID="Monitored-System-ID";

	@Test
	public void testAlarmTypeConversion() throws Exception {
		
		List<EnrichedSample> samples = new Vector<>();
		Sample s = new Sample(Alarm.SET_MEDIUM);
		EnrichedSample vs = new EnrichedSample(s, true);
		samples.add(vs);
		
		// Check the conversion to/from Alarm
		Alarm alarmSample = Alarm.SET_MEDIUM;
		FilteredValue fv = new FilteredValue(alarmSample, samples, System.currentTimeMillis());
		ValueToSend vts = new ValueToSend("IASIO-ALARM-ID", fv,OperationalMode.UNKNOWN, IasValidity.RELIABLE);
		
		MonitorPointData mpd = new MonitorPointData(pluginID, monSysID, vts);
		String jsonRepresentation=mpd.toJsonString();
		
		MonitorPointData mpdFromJsonStr = MonitorPointData.fromJsonString(jsonRepresentation);
		assertEquals(mpd,mpdFromJsonStr);
	}

	@Test
	public void testTimestampTypeConversion() throws Exception {

		List<EnrichedSample> samples = new Vector<>();
		Long now = System.currentTimeMillis();
		Sample s = new Sample(now);
		EnrichedSample vs = new EnrichedSample(s, true);
		samples.add(vs);

		// Check the conversion to/from Timestamp
		Long tStampSample = 1024L;
		FilteredValue fv = new FilteredValue(tStampSample, samples, System.currentTimeMillis());
		ValueToSend vts = new ValueToSend("IASIO-TSTAMP-ID", fv,OperationalMode.UNKNOWN, IasValidity.RELIABLE);

		MonitorPointData mpd = new MonitorPointData(pluginID, monSysID, vts);
		String jsonRepresentation=mpd.toJsonString();

		MonitorPointData mpdFromJsonStr = MonitorPointData.fromJsonString(jsonRepresentation);
		assertEquals(mpd,mpdFromJsonStr);
	}

	@Test
	public void testArrayOfLongTypeConversion() throws Exception {

		List<EnrichedSample> samples = new Vector<>();
		Long[] array = new Long[] { 0L,1L,2L,3L,4L};
		Sample s = new Sample(array);
		EnrichedSample vs = new EnrichedSample(s, true);
		samples.add(vs);
		FilteredValue fv = new FilteredValue(array, samples, System.currentTimeMillis());
		ValueToSend vts = new ValueToSend("IASIO-LONGARRAY-ID", fv,OperationalMode.UNKNOWN, IasValidity.RELIABLE);
		MonitorPointData mpd = new MonitorPointData(pluginID, monSysID, vts);
		String jsonRepresentation=mpd.toJsonString();
		MonitorPointData mpdFromJsonStr = MonitorPointData.fromJsonString(jsonRepresentation);
		assertEquals(mpd,mpdFromJsonStr);

	}

	@Test
	public void testArrayOfDoublesTypeConversion() throws Exception {

		List<EnrichedSample> samples = new Vector<>();
		Double[] array = new Double[] { 0D,1.345D,-0.546D,-3D,4.000976D};
		Sample s = new Sample(array);
		EnrichedSample vs = new EnrichedSample(s, true);
		samples.add(vs);
		FilteredValue fv = new FilteredValue(array, samples, System.currentTimeMillis());
		ValueToSend vts = new ValueToSend("IASIO-DOUBLEARRAY-ID", fv,OperationalMode.UNKNOWN, IasValidity.RELIABLE);
		MonitorPointData mpd = new MonitorPointData(pluginID, monSysID, vts);
		String jsonRepresentation=mpd.toJsonString();
		MonitorPointData mpdFromJsonStr = MonitorPointData.fromJsonString(jsonRepresentation);
		assertEquals(mpd,mpdFromJsonStr);

	}

}
