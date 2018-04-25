package org.eso.ias.plugin.filter;

import java.util.List;
import java.util.Optional;

import org.eso.ias.plugin.filter.Filter.EnrichedSample;

/**
 * AverageBySamples produces a new Double value by applying the
 * arithmetic average of the samples.
 * <P>
 * The filter does not produce any value until it receives
 * {@link #numberOfSamplesToAverage}; 
 * after that moment the value is produced by applying the arithmetic 
 * average of the values of the sample.
 * 
 * @author acaproni
 *
 */
public class AverageBySamples extends FilterBase {
	
	/**
	 * The number of samples to average
	 */
	public final int numberOfSamplesToAverage;
	
	/**
	 * Constructor
	 * 
	 * @param props The properties must contain the  number of sample to average
	 */
	public AverageBySamples(String props) {
		super();
		if (props==null || props.isEmpty()) {
			throw new IllegalArgumentException("Missing number of sample to average in proerties");
		}
		try {
			numberOfSamplesToAverage = Integer.parseInt(props);
		} catch (NumberFormatException nfe) {
			throw new IllegalArgumentException("Error parsing the number of samples to average: "+props);
		}
		if (numberOfSamplesToAverage<=1) {
			throw new IllegalArgumentException("Invalid number of samples to average "+numberOfSamplesToAverage);
		}
	}

	/**
	 * Adds a new sample
	 * 
	 * @see Filter#newSample(org.eso.ias.plugin.filter.Filter.EnrichedSample)
	 */
	@Override
	protected void sampleAdded(EnrichedSample newSample) {
		keepNewest(numberOfSamplesToAverage);
	}

	/**
	 * Apply the filter and produces the output
	 * 
	 * @see Filter#apply()
	 */
	@Override
	protected Optional<FilteredValue> applyFilter() {
		if (getHistorySize()<numberOfSamplesToAverage) {
			return Optional.empty();
		}
		List<EnrichedSample> samples = historySnapshot();
		double value=0.0;
		for (EnrichedSample sample: samples) {
			value+= ((Number)sample.value).doubleValue();
		}
		double valueToreturn = value/samples.size();
		Optional<EnrichedSample> newestSample = peekNewest();
		assert(newestSample.isPresent());
		FilteredValue fv = new FilteredValue(
				Double.valueOf(valueToreturn), 
				samples, 
				newestSample.get().timestamp);
		return Optional.of(fv);
	}
}
