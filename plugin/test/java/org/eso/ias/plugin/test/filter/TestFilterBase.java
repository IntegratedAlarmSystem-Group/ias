package org.eso.ias.plugin.test.filter;



import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import org.eso.ias.plugin.Sample;
import org.eso.ias.plugin.filter.Filter;
import org.eso.ias.plugin.filter.Filter.EnrichedSample;
import org.eso.ias.plugin.filter.FilterBase;
import org.eso.ias.plugin.filter.FilterException;
import org.eso.ias.plugin.filter.FilteredValue;
import org.eso.ias.types.IasValidity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Test the {@link FilterBase}
 * <P>
 * {@link TestFilter} class extends {@link NoneFilter} to access 
 * its protected methods.
 * 
 * @author acaproni
 *
 */
public class TestFilterBase {
		
		/**
		 * A class that extends {@link NoneFilter} to access the protected methods.
		 * 
		 * @author acaproni
		 *
		 */
		public class TestFilter extends FilterBase {

			/* (non-Javadoc)
			 * @see org.eso.ias.plugin.filter.FilterBase#removeOldSamples(long)
			 */
			@Override
			public int removeOldSamples(long time, TimeUnit unit) {
				return super.removeOldSamples(time,unit);
			}
			
			public int removeOldSamples(long timestamp) {
				return super.removeOldSamples(timestamp);
			}

			/* (non-Javadoc)
			 * @see org.eso.ias.plugin.filter.FilterBase#removeLastSamples(int)
			 */
			@Override
			public int removeLastSamples(int nSamples) {
				return super.removeLastSamples(nSamples);
			}

			/* (non-Javadoc)
			 * @see org.eso.ias.plugin.filter.FilterBase#keepNewest(int)
			 */
			@Override
			public int keepNewest(int nSamples) {
				return super.keepNewest(nSamples);
			}

			/* (non-Javadoc)
			 * @see org.eso.ias.plugin.filter.FilterBase#historySnapshot()
			 */
			@Override
			public List<EnrichedSample> historySnapshot() {
				return super.historySnapshot();
			}

			/* (non-Javadoc)
			 * @see org.eso.ias.plugin.filter.FilterBase#peekNewest()
			 */
			@Override
			public Optional<EnrichedSample> peekNewest() {
				return super.peekNewest();
			}

			/* (non-Javadoc)
			 * @see org.eso.ias.plugin.filter.FilterBase#clearHistory()
			 */
			@Override
			public int clearHistory() {
				return super.clearHistory();
			}

			/* (non-Javadoc)
			 * @see org.eso.ias.plugin.filter.FilterBase#peekOldest()
			 */
			@Override
			public Optional<EnrichedSample> peekOldest() {
				return super.peekOldest();
			}

			/* (non-Javadoc)
			 * @see org.eso.ias.plugin.filter.FilterBase#sampleAdded(org.eso.ias.plugin.Sample)
			 */
			@Override
			protected void sampleAdded(EnrichedSample newSample) {
			}

			/* (non-Javadoc)
			 * @see org.eso.ias.plugin.filter.FilterBase#applyFilter()
			 */
			@Override
			protected Optional<FilteredValue> applyFilter() {
				return Optional.empty();
			}
			
			public Optional<Long> getLastSampleTimeStamp() {
				return lastSampleSubmssionTime;
			}
		}
		
		/**
		 * The filter to test
		 */
		private TestFilter defaultFilter;

		@BeforeEach
		public void setUp() {
			defaultFilter = new TestFilter();
			assertNotNull(defaultFilter);
			defaultFilter.clearHistory();
			assertTrue(defaultFilter.historySnapshot().isEmpty());
		}
		
		/**
		 * Submit n samples to the filter
		 * 
		 * @param n The number of samples
		 * @ param f The filter to submit samples to
		 * @return The timely ordered list of submitted samples
		 */
		public static List<EnrichedSample> submitSamples(int n, Filter f) 
				throws FilterException, InterruptedException {
			assert(n>0);
			LinkedList<EnrichedSample> ret = new LinkedList<>();
			for (int t = 0; t <n; t++) {
				String value ="Test-"+t;
				Sample s = new Sample(value);
				EnrichedSample vs = new EnrichedSample(s, true);
				f.newSample(vs);
				ret.addFirst(vs);
				Thread.sleep(25);
			}
			return ret;
		}
		
		/**
		 * Check the if the passed list is timely ordered
		 * 
		 * @param samples The list to check
		 * @return True if the list is ordered 
		 */
		public static boolean checkOrder(List<EnrichedSample> samples) {
			// Check the order of the timestamps of the samples
			for (int t=1; t<samples.size(); t++) {
				if (samples.get(t-1).timestamp<=samples.get(t).timestamp) {
					return false;
				}
			}
			return true;
		}
		
		/**
		 * Check that the Samples are added to the list preserving the order.
		 * 
		 * @throws Exception
		 */
		@Test
		public void testHistoryOrder() throws Exception {
			// Add n ordered samples
			int n =10;
			submitSamples(n, defaultFilter);
			List<EnrichedSample> samples = defaultFilter.historySnapshot();
			assertEquals(n,samples.size());
			
			// Check the order of the timestamps of the samples
			for (int t=1; t<samples.size(); t++) {
				assertTrue(checkOrder(samples));
			}
		}
		
		/**
		 * Check that adding a sample older then the newer sample
		 * in the history throws an exception
		 */
		@Test
		public void testAddingOldSample() throws Exception {
			EnrichedSample oldest = new EnrichedSample(new Sample("OLD"),true);
			Thread.sleep(125);
			EnrichedSample newest = new EnrichedSample(new Sample("NEW"),true);
			
			// Add in the wrong order
			defaultFilter.newSample(newest);
			assertThrows(FilterException.class, () -> defaultFilter.newSample(oldest));
		}
		
		/**
		 * Check the cleaning of the history
		 * 
		 * @throws Exception
		 */
		public void testRemoveAll() throws Exception {
			int n=10;
			submitSamples(n, defaultFilter);
			assertEquals(n,defaultFilter.clearHistory());
			// No samples removed clearing a empty list
			assertEquals(0,defaultFilter.clearHistory());
		}
		
		/**
		 * Check the removal of samples
		 * 
		 * @throws Exception
		 */
		@Test
		public void removeNSamples() throws Exception {
			int nSamples=25;
			int toRemove=12;
			List<EnrichedSample> samples = submitSamples(nSamples, defaultFilter);
			
			int removed = defaultFilter.removeLastSamples(toRemove);
			assertEquals(toRemove,removed,"Wrong number of removed samples");
			
			List<EnrichedSample> samplesFromFilter = defaultFilter.historySnapshot(); 
			assertTrue(checkOrder(defaultFilter.historySnapshot()));
			assertEquals((long)nSamples-toRemove, defaultFilter.historySnapshot().size());
			// Check that the newer samples have been kept
			for (int t=0; t<nSamples-toRemove; t++) {
				Sample s1 = samples.get(t);
				Sample s2 = samplesFromFilter.get(t);
				assertEquals(s1.timestamp,s2.timestamp);
				assertEquals(s1.value,s2.value);
			}
			
			// Another test removing more samples then the 
			// history contains
			removed = defaultFilter.removeLastSamples(100);
			samplesFromFilter = defaultFilter.historySnapshot();
			assertEquals(0, defaultFilter.historySnapshot().size());
			assertEquals((long)nSamples-toRemove,removed);
		}
		
		/**
		 * Check the removal of all the samples but the 
		 * newest ones
		 * 
		 * @throws Exception
		 */
		@Test
		public void testKeepNewest() throws Exception {
			int nSamples=25;
			int toKeep=7;
			List<EnrichedSample> samples = submitSamples(nSamples, defaultFilter);
			int removed = defaultFilter.keepNewest(toKeep);
			List<EnrichedSample> samplesFromFilter = defaultFilter.historySnapshot();
			assertEquals((long)nSamples-toKeep, removed,"Wrong number of deleted samples returned");
			assertEquals(toKeep, samplesFromFilter.size(),"Wrong number of samples in history");
			
			// Check that the newer samples have been kept
			for (int t=0; t<toKeep; t++) {
				Sample s1 = samples.get(t);
				Sample s2 = samplesFromFilter.get(t);
				assertEquals(s1.timestamp,s2.timestamp);
				assertEquals(s1.value,s2.value);
			}
			
			// Another test keeping more samples then the 
			// history contains
			removed = defaultFilter.keepNewest(100);
			samplesFromFilter = defaultFilter.historySnapshot();
			assertNotNull(samplesFromFilter);
			assertEquals(toKeep, samplesFromFilter.size());
			assertEquals(0,removed);
		}
		
		/** 
		 * Check if peekNewest() returns the newest sample in the history
		 *  
		 * @throws Exception
		 */
		@Test
		public void testPeekNewest() throws Exception {
			int nSamples=10;
			List<EnrichedSample> samples = submitSamples(nSamples, defaultFilter);
			
			Optional<EnrichedSample> newest = defaultFilter.peekNewest();
			assertTrue(newest.isPresent());
			assertEquals(samples.get(0).timestamp, newest.get().timestamp);
			assertEquals(samples.get(0).value, newest.get().value);
			
			// Check that empty is returned if there are
			// no sample in the history
			defaultFilter.clearHistory();
			newest = defaultFilter.peekNewest();
			assertFalse(newest.isPresent());
		}
		
		/** 
		 * Check if peekOldest() returns the oldest sample in the history
		 *  
		 * @throws Exception
		 */
		@Test
		public void testPeekOldest() throws Exception {
			int nSamples=10;
			List<EnrichedSample> samples = submitSamples(nSamples, defaultFilter);
			
			Optional<EnrichedSample> oldest = defaultFilter.peekOldest();
			assertTrue(oldest.isPresent());
			assertEquals(samples.get(samples.size()-1).timestamp, oldest.get().timestamp);
			assertEquals(samples.get(samples.size()-1).value, oldest.get().value);
			
			// Check that empty is returned if there are
			// no sample in the history
			defaultFilter.clearHistory();
			oldest = defaultFilter.peekOldest();
			assertFalse(oldest.isPresent());
		}
		
		
		/**
		 * Test the removal of samples oldest then a given timestamp.
		 *  
		 * @throws Exception
		 */
		@Test
		public void removeOldestByTstamp() throws Exception {
			// First use case: check the removal no samples
			// this is the case when all the samples are newer then 
			// the passed timestamp
			long now = System.currentTimeMillis();
			Thread.sleep(100);
			int nSamples=21;
			submitSamples(nSamples, defaultFilter);
			
			int removed = defaultFilter.removeOldSamples(now);
			assertEquals(0,removed,"Wrong number of removed samples");
			
			List<EnrichedSample> samplesFromFilter = defaultFilter.historySnapshot(); 
			assertEquals(nSamples,samplesFromFilter.size());
			
			defaultFilter.clearHistory();
			
			// Second use case: create an artificial time delay between
			// a group of samples to check that only the oldest are really removed
			nSamples=15;
			submitSamples(nSamples, defaultFilter);
			Thread.sleep(10);
			now = System.currentTimeMillis();
			Thread.sleep(10);
			int nNewerSamples=10;
			submitSamples(nNewerSamples, defaultFilter);
			samplesFromFilter = defaultFilter.historySnapshot();
			assertEquals((long)nNewerSamples+nSamples,samplesFromFilter.size());
			
			
			removed = defaultFilter.removeOldSamples(now);
			samplesFromFilter = defaultFilter.historySnapshot();
			assertEquals(nSamples,removed);
			assertEquals(nNewerSamples,samplesFromFilter.size());
		}
		
		/**
		 * Test the removal of samples oldest then a given time.
		 *  
		 * @throws Exception
		 */
		@Test
		public void removeOldest() throws Exception {
			// First use case: check the removal no samples
			// this is the case when all the samples are newer then 
			// the passed time
			long now = System.currentTimeMillis();
			Thread.sleep(1000);
			int nSamples=10;
			List<EnrichedSample> samples = submitSamples(nSamples, defaultFilter);
			
			int removed = defaultFilter.removeOldSamples(System.currentTimeMillis()-now, TimeUnit.MILLISECONDS);
			assertEquals(0,removed,"Wrong number of removed samples");
			
			List<EnrichedSample> samplesFromFilter = defaultFilter.historySnapshot(); 
			assertEquals(nSamples,samplesFromFilter.size());
			
			defaultFilter.clearHistory();
			
			// Second use case: create an artificial time delay between
			// a group of samples to check that only the oldest are really removed
			nSamples=15;
			samples = submitSamples(nSamples, defaultFilter);
			Thread.sleep(1500);
			now = System.currentTimeMillis();
			Thread.sleep(100);
			int nNewerSamples=10;
			List<EnrichedSample> newerSamples = submitSamples(nNewerSamples, defaultFilter);
			samplesFromFilter = defaultFilter.historySnapshot();
			assertEquals((long)nNewerSamples+nSamples,samplesFromFilter.size());
			
			
			removed = defaultFilter.removeOldSamples(System.currentTimeMillis()-now, TimeUnit.MILLISECONDS);
			samplesFromFilter = defaultFilter.historySnapshot();
			assertEquals(nSamples,removed);
			assertEquals(nNewerSamples,samplesFromFilter.size());
			
		}
		
		/**
		 * Test the setting of the submission timestamp when a 
		 * new sample is submitted
		 * 
		 * @throws Exception
		 */
		@Test
		public void testSubmissionTime() throws Exception {
			assertFalse(defaultFilter.getLastSampleTimeStamp().isPresent());
			long before = System.currentTimeMillis();
			EnrichedSample s = new EnrichedSample(new Sample(Boolean.TRUE),true);
			defaultFilter.newSample(s);
			long after = System.currentTimeMillis();
			
			assertTrue(defaultFilter.getLastSampleTimeStamp().isPresent());
			Optional<Long> submissionTime = defaultFilter.getLastSampleTimeStamp();
			Long time = submissionTime.orElseThrow(() -> new Exception("Time not present"));
			assertTrue(time>=before && time<=after,"Invalid submission time");
			
			// Get it again, did it change?
			Thread.sleep(50);
			assertEquals(defaultFilter.getLastSampleTimeStamp(), submissionTime,"The submissson timestamp should not have changed");
		}
	}

