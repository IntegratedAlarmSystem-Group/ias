package org.eso.ias.plugin.filter;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.eso.ias.plugin.Sample;

/**
 * The value returned after applying the filter.
 * 
 * @author osboxes
 *
 */
public class FilteredValue {
	
	/**
	 * The samples used by the filter to generate the filtered value.
	 * <P>
	 * The number of samples in the collection varies
	 * depending on the filter. For example a filter that
	 * does nothing only has one sample but the value generated averaging 
	 * many sample sample contains that many sample.
	 */
	public final List<Sample> samples;
	
	/**
	 * The value obtained applying the filter to the samples
	 */
	public final Object value;
	
	/**
	 * The point in time when the value has been generated
	 */
	public final long timestamp;

	
	public FilteredValue(Object value, List<Sample> samples) {
		if (value==null) {
			throw new IllegalArgumentException("The filtered value can't be null");
		}
		if (samples==null) {
			throw new IllegalArgumentException("The collection of samples can't be null");
		}
		if (samples.isEmpty()) {
			throw new IllegalArgumentException("The collection of samples can't empty");
		}
		this.value=value;
		this.timestamp=System.currentTimeMillis();
		this.samples=Collections.unmodifiableList(samples);
	}

}
