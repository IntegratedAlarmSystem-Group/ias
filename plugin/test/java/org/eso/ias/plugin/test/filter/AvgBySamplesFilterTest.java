package org.eso.ias.plugin.test.filter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import org.eso.ias.plugin.Sample;
import org.eso.ias.plugin.filter.AverageBySamples;
import org.eso.ias.plugin.filter.Filter.EnrichedSample;
import org.eso.ias.plugin.filter.FilterBase;
import org.eso.ias.plugin.filter.FilteredValue;
import org.eso.ias.plugin.filter.NoneFilter;
import org.eso.ias.types.IasValidity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Test the {@link NoneFilter}.
 * <BR>There is not much to test because, apart of {@link NoneFilter#apply()},
 * all other methods are tested elsewhere.
 * <P>
 * {@link TestFilter} class extends {@link NoneFilter} to access 
 * its protected methods.
 * 
 * 
 * @author acaproni
 *
 */
public class AvgBySamplesFilterTest {
	
	/**
	 * The number of samples used by the filter to average
	 */
	public final int numOfSamples = 5;
	
	/**
	 * A class that extends {@link NoneFilter} to access the protected methods.
	 * 
	 * @author acaproni
	 *
	 */
	public class TestFilter extends AverageBySamples {

		public TestFilter() {
			super(Integer.valueOf(numOfSamples).toString());
		}

		/**
		 * @see org.eso.ias.plugin.filter.FilterBase#removeOldSamples(long)
		 */
		@Override
		public int removeOldSamples(long time, TimeUnit unit) {
			return super.removeOldSamples(time,unit);
		}

		/**
		 * @see org.eso.ias.plugin.filter.FilterBase#removeLastSamples(int)
		 */
		@Override
		public int removeLastSamples(int nSamples) {
			return super.removeLastSamples(nSamples);
		}

		/**
		 * @see org.eso.ias.plugin.filter.FilterBase#keepNewest(int)
		 */
		@Override
		public int keepNewest(int nSamples) {
			return super.keepNewest(nSamples);
		}

		/**
		 * @see org.eso.ias.plugin.filter.FilterBase#historySnapshot()
		 */
		@Override
		public List<EnrichedSample> historySnapshot() {
			return super.historySnapshot();
		}

		/**
		 * @see org.eso.ias.plugin.filter.FilterBase#peekNewest()
		 */
		@Override
		public Optional<EnrichedSample> peekNewest() {
			return super.peekNewest();
		}

		/**
		 * @see org.eso.ias.plugin.filter.FilterBase#clearHistory()
		 */
		@Override
		public int clearHistory() {
			return super.clearHistory();
		}

		/**
		 * @see org.eso.ias.plugin.filter.FilterBase#peekOldest()
		 */
		@Override
		public Optional<EnrichedSample> peekOldest() {
			return super.peekOldest();
		}
		
		/**
		 * 
		 * @return The last returned value
		 */
		public Optional<FilteredValue> getLastReturnedFilteredValue() {
			return lastReturnedValue;
		}

		/**
		 * @ eturn the size of the samples saved in the history
		 * @see {@link FilterBase}
		 */
		@Override
		protected int getHistorySize() {
			return super.getHistorySize();
		}
	}
	
	/**
	 * The filter to test
	 */
	private TestFilter avgFilter;

	@BeforeEach
	public void setUp() {
		avgFilter = new TestFilter();
		assertNotNull(avgFilter);
		avgFilter.clearHistory();
		assertEquals(0, avgFilter.historySnapshot().size());
		assertEquals(numOfSamples, avgFilter.numberOfSamplesToAverage);
	}
	

	
	/**
	 * Test if {@link AverageBySamples#apply()} return empty when there
	 * are no samples
	 * 
	 * @throws Exception
	 */
	@Test
	public void testApplyNoHistory() throws Exception {
		Optional<FilteredValue> value = avgFilter.apply();
		assertFalse(value.isPresent());
	}
	
	/**
	 * Test if {@link AverageBySamples#apply()} return empty when there
	 * are no samples
	 * 
	 * @throws Exception
	 */
	@Test
	public void testReturnEmptyBeforeGetAllSamples() throws Exception {
		for (int t=1; t<numOfSamples; t++) {
			Sample s = new Sample(Integer.valueOf(t));
			EnrichedSample vs = new EnrichedSample(s,true);
			avgFilter.newSample(vs);
			Optional<FilteredValue> value = avgFilter.apply();
			assertFalse(value.isPresent());
		}
	}
	
	/**
	 * Test if {@link AverageBySamples#apply()} return empty when there
	 * are no samples
	 * 
	 * @throws Exception
	 */
	@Test
	public void testReturnValuesAfterGettingAllSamples() throws Exception {
		for (int t=1; t<3*numOfSamples; t++) {
			Sample s = new Sample(Integer.valueOf(t));
			EnrichedSample vs = new EnrichedSample(s,true);
			avgFilter.newSample(vs);
			Optional<FilteredValue> value = avgFilter.apply();
			if (t>=numOfSamples) {
				assertTrue(value.isPresent());
			}
		}
	}
	
	/**
	 * Test if {@link AverageBySamples#apply()} return empty when there
	 * are no samples
	 * 
	 * @throws Exception
	 */
	@Test
	public void testHistorySize() throws Exception {
		for (int t=1; t<3*numOfSamples; t++) {
			Sample s = new Sample(Integer.valueOf(t));
			EnrichedSample vs = new EnrichedSample(s,true);
			avgFilter.newSample(vs);
			int historySz = avgFilter.getHistorySize();
			
			if (t<numOfSamples) {
				assertEquals(t, historySz);
			} else {
				assertEquals(numOfSamples, historySz);
			}
		}
	}
	
	/**
	 * Test if the filter returns the average
	 * 
	 * @throws Exception
	 */
	@Test
	public void testResult() throws Exception {
		for (int t=1; t<=numOfSamples; t++) {
			Sample s = new Sample(1);
			EnrichedSample vs = new EnrichedSample(s,true);
			avgFilter.newSample(vs);
		}
		Optional<FilteredValue> value = avgFilter.apply();
		assertEquals(Double.valueOf(1),(Double)value.get().value);
	}
}
