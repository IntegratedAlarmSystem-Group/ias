package org.eso.ias.plugin.filter.test;

import static org.junit.Assert.*;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import org.eso.ias.plugin.Sample;
import org.eso.ias.plugin.filter.FilterBase;
import org.eso.ias.plugin.filter.FilteredValue;
import org.eso.ias.plugin.filter.NoneFilter;
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
		public List<Sample> historySnapshot() {
			return super.historySnapshot();
		}

		/* (non-Javadoc)
		 * @see org.eso.ias.plugin.filter.FilterBase#peekNewest()
		 */
		@Override
		public Optional<Sample> peekNewest() {
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
		public Optional<Sample> peekOldest() {
			return super.peekOldest();
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
		assert(defaultFilter.historySnapshot().size()==0);
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
		defaultFilter.newSample(s);
		
		Optional<FilteredValue> value = defaultFilter.apply();
		assertTrue("Value not assigned to the submitted sample",value.isPresent());
		FilteredValue fValue = value.get();
		assertEquals("Unexpected assignement of the value",s.value,fValue.value);
		assertEquals("Unexpected size of history",1,fValue.samples.size());
		
		// Submit more samples the check again
		List<Sample> samples=TestFilterBase.submitSamples(43,defaultFilter);
		value = defaultFilter.apply();
		assertTrue("Value not assigned to the submitted sample",value.isPresent());
		fValue = value.get();
		assertEquals("Unexpected assignement of the value",samples.get(0).value,fValue.value);
		assertEquals("Unexpected size of history",1,fValue.samples.size());
	}
}
