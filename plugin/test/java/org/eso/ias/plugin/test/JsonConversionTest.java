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
	
		// Even if not implemented yet... Check an array of integers
		int[] array = new int[] { 0,1,2,3,4};
		fv = new FilteredValue(array, samples, System.currentTimeMillis());
		vts = new ValueToSend("IASIO-INT_ARRAY-ID", fv,OperationalMode.UNKNOWN, IasValidity.RELIABLE);
		mpd = new MonitorPointData(pluginID, monSysID, vts);
		jsonRepresentation=mpd.toJsonString();
		mpdFromJsonStr = MonitorPointData.fromJsonString(jsonRepresentation);
		assertEquals(mpd,mpdFromJsonStr);
		
		System.out.println("Int array type:");
		System.out.println(mpd);
		System.out.println(mpdFromJsonStr);
	}

}
