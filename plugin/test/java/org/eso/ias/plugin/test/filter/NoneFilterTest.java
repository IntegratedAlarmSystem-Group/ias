package org.eso.ias.plugin.test.filter;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import org.eso.ias.plugin.Sample;
import org.eso.ias.plugin.filter.Filter.ValidatedSample;
import org.eso.ias.plugin.filter.FilteredValue;
import org.eso.ias.plugin.filter.NoneFilter;
import org.eso.ias.prototype.input.java.IasValidity;
import org.junit.Before;
import org.junit.Test;

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
public class NoneFilterTest {
	
	/**
	 * A class that extends {@link NoneFilter} to access the protected methods.
	 * 
	 * @author acaproni
	 *
	 */
	public class TestFilter extends NoneFilter {

		public TestFilter() {
			super();
			// TODO Auto-generated constructor stub
		}

		/* (non-Javadoc)
		 * @see org.eso.ias.plugin.filter.FilterBase#removeOldSamples(long)
		 */
		@Override
		public int removeOldSamples(long time, TimeUnit unit) {
			return super.removeOldSamples(time,unit);
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
		public List<ValidatedSample> historySnapshot() {
			return super.historySnapshot();
		}

		/* (non-Javadoc)
		 * @see org.eso.ias.plugin.filter.FilterBase#peekNewest()
		 */
		@Override
		public Optional<ValidatedSample> peekNewest() {
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
		public Optional<ValidatedSample> peekOldest() {
			return super.peekOldest();
		}
		
		public Optional<FilteredValue> getLastReturnedFilteredValue() {
			return lastReturnedValue;
		}
	}
	
	/**
	 * The filter to test
	 */
	private TestFilter defaultFilter;

	@Before
	public void setUp() {
		defaultFilter = new TestFilter();
		assertNotNull(defaultFilter);
		defaultFilter.clearHistory();
		assertEquals(0, defaultFilter.historySnapshot().size());
	}
	

	
	/**
	 * Test if {@link NoneFilter#apply()} return empty when there
	 * are no samples
	 * 
	 * @throws Exception
	 */
	@Test
	public void testApplyNoHistory() throws Exception {
		Optional<FilteredValue> value = defaultFilter.apply();
		assertFalse(value.isPresent());
	}
	
	/**
	 * Test if {@link NoneFilter#apply()} always return the last sample
	 * 
	 * @throws Exception
	 */
	@Test
	public void testApply() throws Exception {
		Sample s = new Sample(Integer.valueOf(12));
		ValidatedSample vs = new ValidatedSample(s,IasValidity.RELIABLE);
		defaultFilter.newSample(vs);
		
		Optional<FilteredValue> value = defaultFilter.apply();
		assertTrue("Value not assigned to the submitted sample",value.isPresent());
		FilteredValue fValue = value.orElseThrow(() -> new Exception("Not present value"));
		assertEquals("Unexpected assignement of the value",s.value,fValue.value);
		assertEquals("Unexpected size of history",1,fValue.samples.size());
		
		// Submit more samples the check again
		List<ValidatedSample> samples=TestFilterBase.submitSamples(43,defaultFilter);
		value = defaultFilter.apply();
		fValue = value.orElseThrow(() -> new Exception("Value not assigned to the submitted sample"));
		assertEquals("Unexpected assignement of the value",samples.get(0).value,fValue.value);
		assertEquals("Unexpected size of history",1,fValue.samples.size());
	}
	
	/**
	 * Test that the last returned value is correctly saved
	 * by (@link FilterBase}
	 * @throws Exception
	 */
	@Test
	public void testLastReturnedValue() throws Exception {
		assertFalse(defaultFilter.getLastReturnedFilteredValue().isPresent());
		
		Sample s = new Sample(Long.valueOf(3));
		ValidatedSample vs = new ValidatedSample(s,IasValidity.RELIABLE);
		defaultFilter.newSample(vs);
		Optional<FilteredValue> filteredValue = defaultFilter.apply();
		Optional<FilteredValue> lastReturnedilteredValue = defaultFilter.getLastReturnedFilteredValue();
		
		
		FilteredValue v = filteredValue.orElseThrow(() -> new Exception("Value not present"));
		FilteredValue lastValue = lastReturnedilteredValue.orElseThrow(() -> new Exception("Last returned filtered value not present"));
		assertEquals(lastValue,v);
		
		Thread.sleep(25);
		Sample s2 = new Sample(Long.valueOf(9));
		vs = new ValidatedSample(s2,IasValidity.RELIABLE);
		defaultFilter.newSample(vs);
		
		Optional<FilteredValue> anotherFilteredValue = defaultFilter.apply();
		FilteredValue v2 = anotherFilteredValue.orElseThrow(() -> new Exception("Value not present"));
		lastReturnedilteredValue = defaultFilter.getLastReturnedFilteredValue();
		FilteredValue anotherlastValue = lastReturnedilteredValue.orElseThrow(() -> new Exception("Last returned filtered value not present"));
		assertEquals(anotherlastValue,v2);
		
	}
}
