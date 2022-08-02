package org.eso.ias.plugin.test.stats;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.eso.ias.plugin.DetailedStatsCollector;
import org.eso.ias.plugin.DetailedStatsCollector.StatData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Test {@link DetailedStatsCollector}
 * 
 * @author acaproni
 *
 */
public class DetailedStatsTest {

	/**
	 * The collector to test
	 */
	private DetailedStatsCollector statsCollector;
	
	/**
	 * The max number of monitor points collected
	 */
	private final int maxPointsCollected = DetailedStatsCollector.MONITOR_POINTS_TO_LOG;
	
	/**
	 * the prefix of the IDs of the MPs used for testing
	 */
	private static final String idPrefix ="ID-";
	
	/**
	 * Setup the test
	 */
	@BeforeEach
	public void setup() {
		statsCollector = new DetailedStatsCollector();
		List<StatData> statData = statsCollector.getAndReset();
		assertNotNull(statData);
		assertEquals(0L,statData.size());
	}
	
	/**
	 * Check the number of collected monitor points
	 */
	@Test
	public void testNumOfCollectedMonitorPoints() {
		// Send only one MP
		statsCollector.mPointUpdated(idPrefix);
		List<StatData> data = statsCollector.getAndReset();
		assertEquals(1L, data.size());
		List<StatData> statData = statsCollector.getAndReset();
		assertNotNull(statData);
		assertEquals(0L,statData.size());
		// Now send more MPs 
		for (int t=0; t<2*maxPointsCollected; t++) {
			statsCollector.mPointUpdated(idPrefix+t);
		}
		data = statsCollector.getAndReset();
		assertEquals(2*maxPointsCollected, data.size());
		statData = statsCollector.getAndReset();
		assertNotNull(statData);
		assertEquals(0L,statData.size());
	}
	
	/**
	 * Check that the returned MPs occurrences are properly sorted
	 */
	@Test
	public void testSorting() {
		// Will submit the following updates
		// ie the MP with ID-n will have occurrences.get(n) occurrences
		ArrayList<Integer> occurrences = new ArrayList<>(Arrays.asList(2,150,7,23,25,118,1,99,1023,7,16,21,21,1,8192));
		
		for (int t=0; t<occurrences.size(); t++) {
			String id = idPrefix+t;
			for (int j=0; j<occurrences.get(t); j++) {
				statsCollector.mPointUpdated(id);
			}
		}
		List<StatData> dataList = statsCollector.getAndReset();
		assertEquals(occurrences.size(), dataList.size());
		
		// Check if each ID is correctly associated to the occurrences
		for (StatData data: dataList) {
			int pos = Integer.parseInt(data.id.substring(idPrefix.length()));
			assertEquals(Integer.valueOf(data.getOccurrences()), occurrences.get(pos));
		}
		
		// Check that items are sorted ascending
		boolean ordered=true;
		for (int i = 0; i < dataList.size()-1; i++) {
	        if (dataList.get(i).getOccurrences() > dataList.get(i+1).getOccurrences()) {
	        	ordered=false; 
	        }
	    }
		assertTrue(ordered);
		List<StatData> statData = statsCollector.getAndReset();
		assertNotNull(statData);
		assertEquals(0L,statData.size());
	}
	
	/**
	 * Perform some test on the logged message
	 */
	@Test
	public void testLoggedMsg() {
		// Nothing is logged before adding MPs
		List<StatData> statData = statsCollector.getAndReset();
		assertNotNull(statData);
		assertEquals(0L,statData.size());
		// Few Mps
		ArrayList<Integer> occurrences = new ArrayList<>(Arrays.asList(8,9,1,3,5,10));
		for (int t=0; t<occurrences.size(); t++) {
			String id = idPrefix+t;
			for (int j=0; j<occurrences.get(t); j++) {
				statsCollector.mPointUpdated(id);
			}
		}
		String msg = statsCollector.logAndReset();
		String[] part=msg.split(idPrefix+"[0-9]+");
		assertEquals(occurrences.size()+1,part.length);
		// Will submit the following updates
		// ie the MP with ID-n will have occurrences.get(n) occurrences
		occurrences = new ArrayList<>(Arrays.asList(7,2,67,899,123,1,10267,2,77,99,121,77,34,128,19,20,675));
		
		for (int t=0; t<occurrences.size(); t++) {
			String id = idPrefix+t;
			for (int j=0; j<occurrences.get(t); j++) {
				statsCollector.mPointUpdated(id);
			}
		}
		msg = statsCollector.logAndReset();
		part=msg.split(idPrefix+"[0-9]+");
		assertEquals(maxPointsCollected+1,part.length);
		statData = statsCollector.getAndReset();
		assertNotNull(statData);
		assertEquals(0L,statData.size());
	}
}
