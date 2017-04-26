package org.eso.ias.plugin.filter;

import java.util.Optional;

import org.eso.ias.plugin.Sample;

/**
 * The interface defining filter to apply to a monitored value 
 * before sending to the IAS core.
 * 
 * @author acaproni
 *
 */
public interface Filter {

	/**
	 * Acquire a new sample.
	 * <P>
	 * The sample offered must be newer then the newer
	 * sample in the list i.e. the filter expects
	 * that the sample arrive as they are produced i.e. timely ordered.
	 * 
	 * @param newSample The not-null sample to submit to the filter
	 * @throws FilterException If the sample is not timely ordered
	 */
	public void newSample(Sample newSample) throws FilterException;
	
	/**
	 * Apply the filter to the samples in the history 
	 * and return the filtered value.
	 * 
	 *  @return The value after applying the filter or empty
	 *  		if no samples has been received
	 */
	public Optional<FilteredValue> apply();
}
