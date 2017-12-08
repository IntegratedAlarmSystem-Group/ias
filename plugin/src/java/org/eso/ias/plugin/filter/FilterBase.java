package org.eso.ias.plugin.filter;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.Vector;
import java.util.concurrent.TimeUnit;

/**
 * The basic class for the filters.
 * <P>
 * FilterBase keeps a time ordered {@link #history} of samples:
 * newest items are in the head of the list.
 * <P>
 * Implementation of filters must take care of removing elements from the history when 
 * a new element is added ({@link #sampleAdded(Sample)}) and/or when the filtered value
 * is retrieved {@link Filter#apply()}.
 * <P>
 * Objects of this class stores also:
 * <UL>
 * 	<LI>the point in time when the last sample had been submitted ({@link #lastSampleSubmssionTime}
 * 	<LI>the last filtered value returned applying the filter {@link #lastReturnedValue}
 * </UL>
 * <P>
 * The filtering is not supposed to perform any I/O but to perform
 * the calculation only on the samples in the history.  
 * 
 * @author acaproni
 *
 */
public abstract class FilterBase implements Filter {
	
	/**
	 * The last acquired samples. 
	 * <P>
	 * The list is ordered so that newest items are always
	 * at the head.
	 * <P>
	 * The list is private to ensure it remains ordered: this class
	 * provides methods to manipulate the history.
	 */
	private final LinkedList<ValidatedSample> history = new LinkedList<>();
	
	/**
	 * The point in time then the last sample has been submitted to
	 * the filter.
	 * This is not the timestamp of the last submitted sample.
	 */
	protected Optional<Long>lastSampleSubmssionTime= Optional.empty();
	
	/**
	 * The last returned value of {@link #apply()}.
	 */
	protected Optional<FilteredValue> lastReturnedValue = Optional.empty();
	
	/**
	 * sampleAdded is executed after adding the sample to the history
	 * to let the implementation of a filter does any computation it might need. 
	 * 
	 * @param newSample The sample to add
	 */
	protected abstract void sampleAdded(ValidatedSample newSample);
	
	/**
	 * {@link #apply()} calls this method of the filter to get the 
	 * filtered value and return to the caller.
	 *  
	 * @return the filtered value 
	 */
	protected abstract Optional<FilteredValue>applyFilter();
	
	/**
	 * Apply the filter to the samples in the history 
	 * and return the filtered value or empty if there are no samples in the history.
	 * <P>
	 * The calculation of the filter is delegated to {@link #applyFilter()}.
	 * 
	 *  @return The value after applying the filter
	 */
	@Override
	public final Optional<FilteredValue>apply() {
		lastReturnedValue=applyFilter();
		return lastReturnedValue;
	}
	
	
	
	/**
	 * Acquire a new sample.
	 * <P>
	 * The new sample is added at the head.
	 * 
	 * @param newSample The new sample to add
	 * @return The filtered values after adding the new sample
	 */
	@Override
	public final Optional<FilteredValue> newSample(ValidatedSample newSample) throws FilterException {
		if (newSample==null) {
			throw new IllegalArgumentException("A null sample is not permitted");
		}
		lastSampleSubmssionTime=Optional.of(System.currentTimeMillis());
		synchronized (history) {
			// Check that the timestamp of the new sample
			// is more recent then that of the newest sample
			// in the history
			if (!history.isEmpty()) {
				ValidatedSample s = history.peekFirst();
				if (s.timestamp>=newSample.timestamp) {
					throw new FilterException("The new sample is older then the last submitted sample!");
				}
			}
			history.addFirst(newSample);
			sampleAdded(newSample);
			return apply();
		}
	}
	
	/**
	 * Remove from the history the samples older then the passed time
	 * 
	 * @param time The time to remove samples
	 * @param unit the time unit of the time argument
	 * @return The number of removed samples
	 */
	protected int removeOldSamples(long time, TimeUnit unit) {
		assert(time>0);
		synchronized (history) {
			long threshold = System.currentTimeMillis()-unit.toMillis(time);
			return removeOldSamples(threshold);
		}
	}
	
	/**
	 * Remove from the history the samples whose timestamp
	 * is older then the passed one
	 * 
	 * @param timestamp The timestamp to remove samples
	 * @return The number of removed samples
	 */
	protected int removeOldSamples(long timestamp) {
		assert(timestamp>0);
		synchronized (history) {
			int size = history.size();
			history.removeIf(s -> s.timestamp<=timestamp);
			return size - history.size();
		}
	}
	
	/**
	 * Remove the last nSamples from the history. 
	 * <P>
	 * The removal proceed from the tail that contains the oldest samples.
	 * 
	 * @param nSamples The number of samples to remove
	 * @return The number of removed samples
	 */
	protected int removeLastSamples(int nSamples) {
		assert(nSamples>0);
		synchronized (history) {
			if (history.size()<=nSamples) {
				return clearHistory();
			}
			int removed=0;
			for (int t=0; t<nSamples; t++) {
				history.removeLast();
				removed++;
			}
			return removed;
		}
	}
	
	/**
	 * Remove the last nSamples from the history until
	 * it contains only nSamples samples. 
	 * 
	 * @param nSamples The number of samples to keep in the history
	 * @return The number of removed samples
	 */
	protected int keepNewest(int nSamples) {
		assert(nSamples>0);
		synchronized (history) {
			int numOfSamplesToRemove = history.size()-nSamples;
			if (numOfSamplesToRemove>0) {
				return removeLastSamples(numOfSamplesToRemove);
			} else {
				// Nothing to be done as the history
				// contains less sample that ones to keep
				return 0;
			}
		}
	}
	
	/**
	 * <code>historySnapshot</code> returns a snapshot of the actual history
	 * that is part of the {@link FilteredValue} returned by {@link #apply()}.
	 * <P>
	 * The collection is a read-only view of a copy of the .history
	 * 
	 * @return a read-only snapshot of the actual history
	 */
	protected List<ValidatedSample> historySnapshot() {
		synchronized(history) {
			return Collections.unmodifiableList(new Vector<>(history)); 
		}
	}
	
	/**
	 * Retrieves, but does not remove, the newest sample in the history, 
	 * or returns empty if the history is empty.
	 * 
	 * @return the newest element of the history, or empty if the history is empty 
	 */
	protected Optional<ValidatedSample> peekNewest() {
		// The newest element is at the head
		synchronized(history) {
			return Optional.ofNullable(history.peekFirst());
		}
	}
	
	/**
	 * Clear the history.
	 * 
	 * @return The number of samples removed from the history
	 */
	protected int clearHistory() {
		synchronized (history) {
			int ret = history.size();
			history.clear();
			return ret;
		}
	}
	
	/**
	 * Retrieves, but does not remove, the oldest sample in the history, 
	 * or returns empty if the history is empty.
	 * 
	 * @return the newest element of the history, or empty if the history is empty 
	 */
	protected Optional<ValidatedSample> peekOldest() {
		// The oldest sample is at the end of the list
		synchronized(history) {
			return Optional.ofNullable(history.peekLast());
		}
	}
}
