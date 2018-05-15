package org.eso.ias.plugin.test.publisher;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.List;

import org.eso.ias.plugin.Sample;
import org.eso.ias.plugin.ValueToSend;
import org.eso.ias.plugin.filter.Filter.EnrichedSample;
import org.eso.ias.plugin.publisher.MonitorPointDataToBuffer;
import org.eso.ias.types.IasValidity;
import org.eso.ias.types.OperationalMode;
import org.eso.ias.plugin.publisher.BufferedMonitoredSystemData;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Test the {@link BufferedMonitoredSystemData}, in particular the JSON part.
 * 
 * @author acaproni
 *
 */
public class BufferedMonitoredSystemDataTest extends PublisherTestCommon {
	
	/**
	 * The logger
	 */
	private final static Logger logger = LoggerFactory.getLogger(BufferedMonitoredSystemDataTest.class);
	
	/**
	 * ISO 8601 date formatter
	 */
	private final SimpleDateFormat iso8601dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.S");
	
	/**
	 * The plugin ID for testing
	 */
	private final String pluginId = "PluginIdentifier";
	
	/**
	 * Check the correctness of the JSON string generated by a {@link BufferedMonitoredSystemData}
	 * without values
	 */
	@Test
	public void testJsonNoValues() throws Exception {
		BufferedMonitoredSystemData msData = new BufferedMonitoredSystemData();
		msData.setSystemID(pluginId);
		msData.setMonitoredSystemID("TheIdOfTheMonitoredSystem");
		long now = System.currentTimeMillis();
		String isoDate = iso8601dateFormat.format(new Date(now));
		msData.setPublishTime(isoDate);
		
		String jsonString = msData.toJsonString();
		
		BufferedMonitoredSystemData receivedData = BufferedMonitoredSystemData.fromJsonString(jsonString);
		assertEquals(msData.getSystemID(), receivedData.getSystemID());
		assertEquals(msData.getMonitoredSystemID(), receivedData.getMonitoredSystemID());
		assertEquals(msData.getPublishTime(), receivedData.getPublishTime());
		assertNull(receivedData.getMonitorPoints());
		
		logger.info(msData.toJsonString());
	}
	
	/**
	 * Check the correctness of the JSON string generated by a {@link BufferedMonitoredSystemData}
	 * with only value
	 */
	@Test
	public void testJsonOneValue() throws Exception {
		BufferedMonitoredSystemData msData = new BufferedMonitoredSystemData();
		msData.setSystemID(pluginId);
		msData.setMonitoredSystemID("TheIdOfTheMonitoredSystem");
		long now = System.currentTimeMillis();
		String isoDate = iso8601dateFormat.format(new Date(now));
		msData.setPublishTime(isoDate);
		
		String valueId="ValueIdentifier";
		Object objValue = Boolean.TRUE;
		long filteredTimeStamp =now+200;
		long valueTimeStamp =now+150;
		String valueTimeStampStr = iso8601dateFormat.format(new Date(valueTimeStamp));
		String filteredTimeStampStr = iso8601dateFormat.format(new Date(filteredTimeStamp));
		MonitorPointDataToBuffer value = new MonitorPointDataToBuffer();
		value.setId(valueId);
		value.setValue(objValue.toString());
		value.setSampleTime(valueTimeStampStr);
		value.setFilteredTime(filteredTimeStampStr);
		
		Collection<MonitorPointDataToBuffer> values = new ArrayList<>();
		values.add(value);
		msData.setMonitorPoints(values);
		
		String jsonString = msData.toJsonString();
		
		BufferedMonitoredSystemData receivedData = BufferedMonitoredSystemData.fromJsonString(jsonString);
		assertEquals(msData.getSystemID(), receivedData.getSystemID());
		assertEquals(msData.getMonitoredSystemID(), receivedData.getMonitoredSystemID());
		assertEquals(msData.getPublishTime(), receivedData.getPublishTime());
		assertEquals(1L,receivedData.getMonitorPoints().size());

		// Comparison is based on MonitorPointData#equals()
		//
		// for is easy to use ;-) It works because the collection contains only one item!
		for (MonitorPointDataToBuffer rcvMPData: receivedData.getMonitorPoints()) {
			assertEquals(rcvMPData,value);
		}
		
		logger.info(msData.toJsonString());
	}
	
	/**
	 * Check the correctness of the JSON string generated by a {@link BufferedMonitoredSystemData}
	 * with only value
	 */
	@Test
	public void testJsonManyValues() throws Exception {
		BufferedMonitoredSystemData msData = new BufferedMonitoredSystemData();
		msData.setSystemID(pluginId);
		msData.setMonitoredSystemID("TheIdOfTheMonitoredSystem");
		long now = System.currentTimeMillis();
		String isoDate = iso8601dateFormat.format(new Date(now));
		msData.setPublishTime(isoDate);
		
		List<EnrichedSample> samples = Arrays.asList(new EnrichedSample(new Sample(Integer.valueOf(67)),true));
		List<MonitorPointDataToBuffer> values = Arrays.asList(
				new MonitorPointDataToBuffer(new ValueToSend("FV-ID1", Integer.valueOf(67), samples, System.currentTimeMillis()+10,OperationalMode.OPERATIONAL,IasValidity.RELIABLE)),
				new MonitorPointDataToBuffer(new ValueToSend("FV-ID2", Long.valueOf(123), samples, System.currentTimeMillis()+20,OperationalMode.OPERATIONAL,IasValidity.RELIABLE)),
				new MonitorPointDataToBuffer(new ValueToSend("FV-ID3", "A string", samples, System.currentTimeMillis()+30,OperationalMode.OPERATIONAL,IasValidity.RELIABLE)),
				new MonitorPointDataToBuffer(new ValueToSend("FV-ID4", Boolean.valueOf(true), samples, System.currentTimeMillis()+40,OperationalMode.OPERATIONAL,IasValidity.RELIABLE)),
				new MonitorPointDataToBuffer(new ValueToSend("FV-ID5", Integer.valueOf(11), samples, System.currentTimeMillis()+50,OperationalMode.OPERATIONAL,IasValidity.RELIABLE)));
		
		msData.setMonitorPoints(values);
		

		String jsonString = msData.toJsonString();
		
		BufferedMonitoredSystemData receivedData = BufferedMonitoredSystemData.fromJsonString(jsonString);
		
		assertEquals(msData.getSystemID(), receivedData.getSystemID());
		assertEquals(msData.getMonitoredSystemID(), receivedData.getMonitoredSystemID());
		assertEquals(msData.getPublishTime(), receivedData.getPublishTime());
		assertEquals(values.size(),receivedData.getMonitorPoints().size());

		values.forEach(mpd -> { 
			assertTrue(receivedData.getMonitorPoints().contains(mpd));
		});
		
		logger.info(msData.toJsonString());
	}

}
