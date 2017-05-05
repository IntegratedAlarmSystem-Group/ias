package org.eso.ias.plugin;

import java.util.Optional;

import org.eso.ias.plugin.filter.FilteredValue;

/**
 * The interface for the listener of changes in a monitored value.
 * <P> 
 * The method is invoked when the value changes or the refresh
 * time interval elapsed.
 * 
 * @author acaproni
 *
 */
public interface ChangeValueListener {
	
	/**
	 * Notify the listener that the value of the monitor point
	 * has been updated.
	 * <P>
	 * Updated does not mean changed because a value must be resent
	 * to the core of the IAS even if its value did not change whenever
	 * the refresh time interval elapses.
	 * 
	 * @param value the not <code>null</code> new value of the
	 * @return
	 */
	public void monitoredValueUpdated(Optional<FilteredValue> value);
}
