package org.eso.ias.plugin.filter;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import org.eso.ias.plugin.filter.Filter.EnrichedSample;

/**
 * AverageByTime produces a new Double value by applying the
 * arithmetic average of the samples received in the specified time 
 * frame.
 * 
 * @author acaproni
 *
 */
public class AverageByTime extends FilterBase {
	
	/**
	 * The time frame (msecs) of samples to average
	 */
	public final long timeFrame;
	
	/**
	 * Constructor
	 * 
	 * @param props The properties must contain the  number of sample to average
	 */
	public AverageByTime(String props) {
		super();
		if (props==null || props.isEmpty()) {
			throw new IllegalArgumentException("Missing number of sample to average in proerties");
		}
		try {
			timeFrame = Long.parseLong(props);
		} catch (NumberFormatException nfe) {
			throw new IllegalArgumentException("Error parsing the time frame of samples to average: "+props);
		}
		if (timeFrame<=0) {
			throw new IllegalArgumentException("Invalid time frame to average "+timeFrame);
		}
	}

	/**
	 * Adds a new sample
	 * 
	 * @see Filter#newSample(org.eso.ias.plugin.filter.Filter.EnrichedSample)
	 */
	@Override
	protected void sampleAdded(EnrichedSample newSample) {
		removeOldSamples(timeFrame,TimeUnit.MILLISECONDS);
	}

	/**
	 * Apply the filter and produces the output
	 * 
	 * @see Filter#apply()
	 */
	@Override
	protected Optional<FilteredValue> applyFilter() {
		removeOldSamples(timeFrame,TimeUnit.MILLISECONDS);
		if (getHistorySize()==0) {
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
