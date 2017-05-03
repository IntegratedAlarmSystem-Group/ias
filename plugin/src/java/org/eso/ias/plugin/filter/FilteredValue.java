package org.eso.ias.plugin.filter;

import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import org.eso.ias.plugin.Sample;

/**
 * The value returned after applying the filter.
 * 
 * <P><code>FilteredValue</code> is immutable.
 * 
 * @author acaproni
 */
public class FilteredValue {
	
	/**
	 * The unique ID of a filtered value is the ID of 
	 * the monitor point
	 */
	public final String id;
	
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
	public final long filteredTimestamp;
	
	/**
	 * The point in time when the value has been provided by the remote system
	 */
	public final long producedTimestamp;

	/**
	 * Constructor 
	 * 
	 * @param id The ID of the value produced applying the filter
	 * @param value The value to send to the IAS core
	 * @param samples The history of samples used by the filter to produce the value
	 * @param monitoredSystemTimestamp The timestamp when the value has been provided by the monitored system
	 */
	public FilteredValue(String id,Object value, List<Sample> samples, long monitoredSystemTimestamp) {
		if (value==null) {
			throw new IllegalArgumentException("The filtered value can't be null");
		}
		if (samples==null) {
			throw new IllegalArgumentException("The collection of samples can't be null");
		}
		if (samples.isEmpty()) {
			throw new IllegalArgumentException("The collection of samples can't empty");
		}
		if (id==null || id.isEmpty()){ 
			throw new IllegalArgumentException("Invalid null or empty ID");
		}
		this.id=id;
		this.value=value;
		this.filteredTimestamp=System.currentTimeMillis();
		this.samples=Collections.unmodifiableList(samples);
		this.producedTimestamp=monitoredSystemTimestamp;
	}

	/**
	 * 
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		StringBuilder ret = new StringBuilder("[");
		ret.append("ID=");
		ret.append(id);
		ret.append(", generated at ");
		SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.S");
		Date date = new Date(filteredTimestamp);
		ret.append(df.format(date));
		ret.append(", value=");
		ret.append(value.toString());
		ret.append(']');
		return ret.toString();
	}
	
	

}
