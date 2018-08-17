package org.eso.ias.plugin;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Collects detailed statistics about updated of monitor point values.
 * <P>
 * This objects is notified about every monitor point update received by the plugin from the
 * monitored system..
 * <BR>When requested, the plugin submits a log with the statistics collected so far
 * and get ready for the next time interval where a time interval is whatever time is
 * between two consecutive calls of {@link #getAndReset()}.
 * <P>
 * If there has been no update in the last time interval, no message is logged.
 * 
 * @author acaproni
 *
 */
public class DetailedStatsCollector {
	
	/**
	 * The data to collect for each monitor point
	 * <P>
	 * The list contains only one entry for each updated monitor point, 
	 * represented by its ID only.
	 * <P>
	 * {@link TreeMap} allows to sort by the key but we need
	 * to sort for the value of the object i.e. the number of its occurrences.
	 *  
	 * @author acaproni
	 *
	 */
	public class StatData implements Comparable<StatData> {
		/**
		 * The ID of the monitor point
		 */
		public final String id; 
		
		/**
		 * The number of times this monitor point has been update
		 * during the current time interval.
		 */
		private int occurrences=0;
		
		/**
		 * The constructor for a given monitor point
		 * 
		 * @param id The not <code>null</code> nor empty ID of a monitor point
		 */
		public StatData(String id) {
			if (id==null || id.isEmpty()) {
				throw new IllegalArgumentException("Invalid ID");
			}
			this.id=id;
		}
		
		/**
		 * The monitor point has been updated.
		 */
		public void updated() {
			occurrences++;
		}

		/**
		 * @see java.lang.Object#hashCode()
		 */
		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + getOuterType().hashCode();
			result = prime * result + ((id == null) ? 0 : id.hashCode());
			result = prime * result + occurrences;
			return result;
		}

		/**
		 * @see java.lang.Object#equals(java.lang.Object)
		 */
		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			StatData other = (StatData) obj;
			if (!getOuterType().equals(other.getOuterType()))
				return false;
			if (id == null) {
				if (other.id != null)
					return false;
			} else if (!id.equals(other.id))
				return false;
			if (occurrences != other.occurrences)
				return false;
			return true;
		}

		private DetailedStatsCollector getOuterType() {
			return DetailedStatsCollector.this;
		}

		/**
		 * @see java.lang.Comparable#compareTo(java.lang.Object)
		 */
		@Override
		public int compareTo(StatData o) {
			if (o==null) {
				throw new NullPointerException("Cannot compare with null");
			}
			return Integer.valueOf(occurrences).compareTo(Integer.valueOf(o.occurrences));
		}

		/**
		 * @return the occurrences
		 */
		public int getOccurrences() {
			return occurrences;
		}
	}
	
	
	
	/**
	 * The map of monitor point IDs and their number of updates in the
	 * time interval.
	 */
	private final Map<String, StatData> monitorPointsFreqs = new HashMap<>();
	
	/**
	 * The number of monitor points to log
	 */
	public static final int MONITOR_POINTS_TO_LOG=10;
	
	/**
	 * The logger
	 */
	public static final Logger logger = LoggerFactory.getLogger(DetailedStatsCollector.class);
	
	/**
	 * A monitor point with the given ID has been 
	 * update
	 * 
	 * @param id The not <code>null</code> nor empty ID of
	 *           the updated monitor point 
	 */
	public synchronized void mPointUpdated(String id) {
		StatData data = monitorPointsFreqs.getOrDefault(id, new StatData(id));
		data.updated();
		monitorPointsFreqs.put(id, data);
	}
	
	/**
	 * Get the {@value #MONITOR_POINTS_TO_LOG} most frequently updated monitor points
	 * 
	 * @return The most frequently update monitor points (sorted ascending);
	 *         empty if no monitor point has been updated in the time interval
	 * 		
	 */
	public synchronized List<StatData> getAndReset() {
		if (monitorPointsFreqs.isEmpty()) {
			// No updates: nothing to return
			return new ArrayList<>();
		}
		List<StatData> vals = new ArrayList<>(monitorPointsFreqs.values());
		Collections.sort(vals);
		monitorPointsFreqs.clear();
		return vals;
		
	}
	
	/**
	 * Logs the message and get ready for the next iteration period.
	 * 
	 * @return The logged message or <code>null</code> if the list of
	 *         occurrences is empty.
	 */
	public synchronized String logAndReset() {
		List<StatData> vals = getAndReset();
		if (vals==null || vals.isEmpty() || !logger.isInfoEnabled()) {
			// No updates: nothing to log
			return null;
		} else {
		
			// Build the log message
			StringBuilder logMsg = new StringBuilder("Stats: topmost updated monitor points:");
			int numOfItemsToLog=MONITOR_POINTS_TO_LOG;
			for (int n=vals.size()-1; n>=0 && numOfItemsToLog>0; n--) {
				StatData sd = vals.get(n);
				logMsg.append(' ');
				logMsg.append(sd.id);
				logMsg.append('[');
				logMsg.append(sd.occurrences);
				logMsg.append(']');
				numOfItemsToLog--;
			}
			logger.info(logMsg.toString());
			vals.clear();
			return logMsg.toString();
		}
	}
}
