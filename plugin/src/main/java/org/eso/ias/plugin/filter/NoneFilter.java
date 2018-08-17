package org.eso.ias.plugin.filter;

import java.util.Optional;

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
	 * 
	 * @param pros The properties of this filter (ignored)
	 */
	public NoneFilter(String props) {
		super();
	}
	
	


	/**
	 * @see Filter#apply()
	 */
	@Override
	public Optional<FilteredValue> applyFilter() {
		Optional<EnrichedSample> sample=peekNewest();
		return sample.map(s -> new FilteredValue(s.value, historySnapshot(),s.timestamp));
	}
	

	/**
	 * 
	 * @see org.eso.ias.plugin.filter.FilterBase#sampleAdded(org.eso.ias.plugin.filter.Filter.EnrichedSample)
	 */
	@Override
	protected void sampleAdded(EnrichedSample newSample) {
		keepNewest(1);
	}
}
