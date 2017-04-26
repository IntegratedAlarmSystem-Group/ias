package org.eso.ias.plugin;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;

import org.eso.ias.plugin.filter.Filter;
import org.eso.ias.plugin.filter.FilterBase;
import org.eso.ias.plugin.filter.FilteredValue;
import org.eso.ias.plugin.filter.NoneFilter;

/**
 * A {@link MonitoredValue} is a monitor point value or alarm read from the 
 * controlled system and sent to the IAS core after applying the filter.
 * <P>
 * The history of samples needed to apply the filters is part
 * of the filter itself because its management depends on the filter.
 * <BR>For example a filter that returns the last value can save a history
 * with only one sample but a filters that averages the values acquired 
 * in the last minute needs a longer history even if its refresh rate is
 * much shorter then the averaging period.   
 * 
 * @author acaproni
 *
 */
public class MonitoredValue {
	
	/**
	 * The ID of the monitored value
	 */
	public final String id;
	
	/**
	 * The refresh rate (msec) of this monitored value:
	 * the value must be sent to the IAS core on change or before the
	 * refresh rate expires
	 */
	public final long refreshRate;
	
	/**
	 * The filter to apply to the acquired samples 
	 * before sending the value to the IAS core 
	 */
	public final Filter filter;
	
	/**
	 * Build a {@link MonitoredValue} with the passed filter
	 * @param id The identifier of the value
	 * @param refreshRate The refresh time interval
	 * @param filter The filter to apply to the samples
	 */
	public MonitoredValue(String id, long refreshRate, Filter filter) {
		if (id==null || id.isEmpty()) {
			throw new IllegalArgumentException("Invalid monitored value ID");
		}
		if (refreshRate<=0) {
			throw new IllegalArgumentException("Invalid refresh rate vale: "+refreshRate);
		}
		if (filter==null) {
			throw new IllegalArgumentException("The filter can't be null");
		}
		this.id=id;
		this.refreshRate=refreshRate;
		this.filter = filter;
	}

	/**
	 * Build a {@link MonitoredValue} with the default filter, {@link NoneFilter}
	 * 
	 * @param id
	 * @param refreshRate
	 */
	public MonitoredValue(String id, long refreshRate) {
		this(id,refreshRate, new NoneFilter());
	}
	
	/**
	 * Return the value to send i.e. the value
	 * returned after applying the {@link #filter} to the {@link #history} of samples
	 * 
	 * @return The value to send
	 */
	public Optional<FilteredValue> getValueTosend() {
		return filter.apply();
	}
	
	
	
	
}
