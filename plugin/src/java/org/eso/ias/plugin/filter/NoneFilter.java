package org.eso.ias.plugin.filter;

import java.util.Optional;

import org.eso.ias.plugin.Sample;

/**
 * Default implementation of a filter: it does nothing but returning 
 * the value of the last acquired sample
 * 
 * @author acaproni
 *
 */
public class NoneFilter extends FilterBase {
	
	/**
	 * Constructor
	 */
	public NoneFilter() {
		super();
	}


	/**
	 * @see Filter#apply()
	 */
	@Override
	public Optional<FilteredValue> applyFilter() {
		Optional<Sample> sample=peekNewest();
		return sample.map(s -> new FilteredValue(s.value, historySnapshot(),s.timestamp));
	}
	

	/**
	 * 
	 * @see org.eso.ias.plugin.filter.FilterBase#sampleAdded(org.eso.ias.plugin.Sample)
	 */
	@Override
	protected void sampleAdded(Sample newSample) {
		keepNewest(1);
	}
}
