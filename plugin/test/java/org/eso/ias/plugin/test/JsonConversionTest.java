package org.eso.ias.plugin.test;

import java.util.List;
import java.util.Vector;

import org.eso.ias.plugin.AlarmSample;
import org.eso.ias.plugin.Sample;
import org.eso.ias.plugin.ValueToSend;
import org.eso.ias.plugin.filter.FilteredValue;
import org.eso.ias.plugin.publisher.MonitorPointData;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

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

	@Test
	public void testAlarmTypeConversion() throws Exception {
		
		List<Sample> samples = new Vector<>();
		Sample s = new Sample(AlarmSample.SET);
		samples.add(s);
		
		// Check the conversion to/from AlarmSample
		AlarmSample alarmSample = AlarmSample.SET;
		FilteredValue fv = new FilteredValue(alarmSample, samples, System.currentTimeMillis());
		ValueToSend vts = new ValueToSend("IASIO-ALARM-ID", fv);
		
		MonitorPointData mpd = new MonitorPointData(pluginID, vts);
		String jsonRepresentation=mpd.toJsonString();
		System.out.println("["+jsonRepresentation+"]");
		
		MonitorPointData mpdFromJsonStr = MonitorPointData.fromJsonString(jsonRepresentation);
		assertEquals(mpd,mpdFromJsonStr);
		
		System.out.println("Alarm type:");
		System.out.println(mpd);
		System.out.println(mpdFromJsonStr);
	
		// Even if not implemented yet... Check an array of integers
		int[] array = new int[] { 0,1,2,3,4};
		fv = new FilteredValue(array, samples, System.currentTimeMillis());
		vts = new ValueToSend("IASIO-INT_ARRAY-ID", fv);
		mpd = new MonitorPointData(pluginID, vts);
		jsonRepresentation=mpd.toJsonString();
		System.out.println("["+jsonRepresentation+"]");
		mpdFromJsonStr = MonitorPointData.fromJsonString(jsonRepresentation);
		assertEquals(mpd,mpdFromJsonStr);
		
		System.out.println("Int array type:");
		System.out.println(mpd);
		System.out.println(mpdFromJsonStr);
	}

}
