package org.eso.ias.plugin;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.TimeZone;

import org.eso.ias.plugin.filter.Filter.EnrichedSample;
import org.eso.ias.plugin.filter.FilteredValue;
import org.eso.ias.types.IasValidity;
import org.eso.ias.types.OperationalMode;

/**
 * The value to send to the core of the IAS.
 * <P>
 * The {@link FilteredValue} returns the value of the monitor point
 * to send to the IAS after the filtering to avoid sending spurious values.
 * <P>
 * The <code>ValueToSend</code> adds more information not produced by the
 * filtering, like the operational mode, the validity and the identifier. 
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
	 * The value to send can be generated out of many samples
	 * that are all supposed to arrive before the refresh time elapses,
	 * where the refresh rate is the time the monitored system 
	 * or the device produces a new value. If it is not the case
	 * the something went wrong because same sampling has been 
	 * lost.
	 */
	public final boolean degraded;
	
	/**
	 * The validity
	 */
	public final IasValidity iasValidity;

	/**
	 * Constructor 
	 * 
	 * @param id The ID of the value to send.
	 * @param value The value to send to the IAS core
	 * @param samples The history of samples used by the filter to produce the value
	 * @param monitoredSystemTimestamp The timestamp when the value has been provided by the monitored system
	 * @param opMode The operational mode
	 * @param iasValidity The validity
	 */
	public ValueToSend(
			String id, 
			Object value, 
			List<EnrichedSample> samples, 
			long monitoredSystemTimestamp, 
			OperationalMode opMode,
			IasValidity iasValidity) {
		super(value, samples, monitoredSystemTimestamp);
		Objects.requireNonNull(opMode,"Invalid null operational mode");
		Objects.requireNonNull(id,"Invalid null ID");
		if (id.isEmpty()){ 
			throw new IllegalArgumentException("Invalid empty ID");
		}
		Objects.requireNonNull(iasValidity, "Undefined validity");
		this.id=id;
		this.operationalMode=opMode;
		this.iasValidity=iasValidity;
		// The value is degraded if at least one sample
		// was not produced in time (i.e. before the refresh rate elapsed)
		this.degraded = samples.stream().anyMatch( es -> !es.generatedInTime);
	}
	
	/**
	 * Builds a <code>ValueToSend</code> from the passed <code>FilteredValue</code>.
	 * 
	 * @param id The ID of the value 
	 * @param filteredValue The value produced applying the filter
	 * @param opMode The operational mode
	 */
	public ValueToSend(
			String id, 
			FilteredValue filteredValue, 
			OperationalMode opMode,
			IasValidity actualValidity) {
		this(
				id,
				filteredValue.value,
				filteredValue.samples, 
				filteredValue.producedTimestamp,
				opMode,
				actualValidity);
	}

	/**
	 * Builds and return a new <code>ValueToSend</code> with the assigned 
	 * operational mode.
	 * 
	 * @param opMode  The operational mode
	 * @return The new value with the passed operational mode
	 */
	public ValueToSend withMode(OperationalMode opMode) {
		if (opMode==operationalMode) {
			return this;
		} else {
			return new ValueToSend(id, value, samples, producedTimestamp,opMode,iasValidity);
		}
	}
	
	/**
	 * Builds and return a new <code>ValueToSend</code> with the assigned 
	 * ID. 
	 * This method supports replication that changes the ID before sending 
	 * the value to the BSDB
	 * 
	 * @param newId  The new ID of the value to send to the BSDB
	 * @return A new value with the passed ID
	 */
	public ValueToSend withId(String newId) {
		if (newId.equals(id)) {
			return this;
		} else {
			return new ValueToSend(newId, value, samples, producedTimestamp,operationalMode,iasValidity);
		}
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
		df.setTimeZone(TimeZone.getTimeZone("UTC"));
		Date date = new Date(filteredTimestamp);
		ret.append(df.format(date));
		ret.append(", value=");
		ret.append(value.toString());
		ret.append(", operational mode=");
		ret.append(operationalMode.toString());
		ret.append(", validity=");
		ret.append(iasValidity.toString());
		ret.append(')');
		return ret.toString();
	}
}
