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
	 * Acquire a new sample and recalculate the value
	 * applying the filtering.
	 * <P> 
	 * The new filter is added to the history then the filter is applied
	 * by calling {@link #apply()} and the value returned to the caller.
	 * <P>
	 * The sample offered must be newer then the newer
	 * sample in the list i.e. the filter expects
	 * that the samples arrive ordered as they are produced i.e. timely ordered.
	 * 
	 * @param newSample The not-null sample to submit to the filter
	 * @return the value after applying the filter the newly received filter
	 * @throws FilterException If the sample is not timely ordered
	 */
	public Optional<FilteredValue> newSample(Sample newSample) throws FilterException;
	
	/**
	 * Apply the filter to the samples already in the history 
	 * and return the filtered value.
	 * <P>
	 * This method is meant to be called when the refresh rate
	 * of the monitored point elapses without adding new samples.
	 * 
	 *  @return The value after applying the filter or empty
	 *  		if no samples has been received
	 */
	public Optional<FilteredValue> apply();
}
