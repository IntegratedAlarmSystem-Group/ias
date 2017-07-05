package org.eso.ias.plugin;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Objects;

import org.eso.ias.plugin.filter.FilteredValue;

/**
 * The value to send to the core of the IAS.
 * <P>
 * The {@link FilteredValue} returns the value of the monitor point
 * to send to the IAS after the filtering to avoid sending spurious values.
 * <P>
 * The <code>ValueToSend</code> adds more information not produced by the
 * filtering, like the operational mode or the identifier. 
 * 
 * <P><code>ValueToSend</code> is immutable.
 * 
 * @author acaproni
 *
 */
public class ValueToSend extends FilteredValue {
	
	/**
	 * The unique ID of the monitor point
	 */
	public final String id;
	
	/**
	 * The operational mode of the monitor point 
	 * (it is overridden by the plugin operational mode, if set)
	 */
	public final OperationalMode operationalMode;

	/**
	 * Constructor 
	 * 
	 * @param id The ID of the value to send.
	 * @param value The value to send to the IAS core
	 * @param samples The history of samples used by the filter to produce the value
	 * @param monitoredSystemTimestamp The timestamp when the value has been provided by the monitored system
	 * @param opMode The operational mode
	 */
	public ValueToSend(String id, Object value, List<Sample> samples, long monitoredSystemTimestamp, OperationalMode opMode) {
		super(value, samples, monitoredSystemTimestamp);
		Objects.requireNonNull(opMode,"Invalid null operational mode");
		Objects.requireNonNull(id,"Invalid null ID");
		if (id.isEmpty()){ 
			throw new IllegalArgumentException("Invalid empty ID");
		}
		this.id=id;
		this.operationalMode=opMode;
	}
	
	/**
	 * Builds a ValueToSend with a unknown operational mode.
	 *  
	 *  @param id The ID of the value
	 * @param value The value to send to the IAS core
	 * @param samples The history of samples used by the filter to produce the value
	 * @param monitoredSystemTimestamp The timestamp when the value has been provided by the monitored system
	 */
	public ValueToSend(String id, Object value, List<Sample> samples, long monitoredSystemTimestamp) {
		this(id,value, samples, monitoredSystemTimestamp,OperationalMode.UNKNOWN);
	}
	
	/**
	 * Builds a <code>ValueToSend</code> from the passed <code>FilteredValue</code>.
	 * 
	 * @param id The ID of the value 
	 * @param filteredValue The value produced applying the filter
	 * @param opMode The operational mode
	 */
	public ValueToSend(String id, FilteredValue filteredValue, OperationalMode opMode) {
		this(id,filteredValue.value,filteredValue.samples, filteredValue.producedTimestamp,opMode);
	}
	
	/**
	 * Builds a <code>ValueToSend</code> from the passed <code>FilteredValue</code>
	 * and a unknown operational mode.
	 * 
	 * @param id The ID of the value 
	 * @param filteredValue The value produced applying the filter
	 */
	public ValueToSend(String id, FilteredValue filteredValue) {
		this(id,filteredValue.value,filteredValue.samples, filteredValue.producedTimestamp,OperationalMode.UNKNOWN);
	}

	/**
	 * Builds and return a new <code>ValueToSend</code> with the assigned 
	 * operational mode.
	 * 
	 * @param opMode  The operational mode
	 * @return The new value with the passed operational mode
	 */
	public ValueToSend withMode(OperationalMode opMode) {
		return new ValueToSend(this.id, this.value, this.samples, this.producedTimestamp,opMode);
	}
	
	/**
	 * 
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		StringBuilder ret = new StringBuilder("ValueToSend(ID=");
		ret.append(id);
		ret.append(", generated at ");
		SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.S");
		Date date = new Date(filteredTimestamp);
		ret.append(df.format(date));
		ret.append(", value=");
		ret.append(value.toString());
		ret.append(", operational mode=");
		ret.append(operationalMode.toString());
		ret.append(')');
		return ret.toString();
	}
}
