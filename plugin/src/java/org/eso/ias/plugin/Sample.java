package org.eso.ias.plugin;

/**
 * A sample of the value read from the remote system.
 * <P>
 * The type of the value of the sample is is Object to allow the developer
 * to send to the core any value without specifying its configuration.
 * The translation to the proper type will be done by the converter that 
 * can always access the configuration database.
 * <P>
 * The alarm is a special case: to send alarms to the core
 * the type of {@link #value} must be {@link AlarmSample}.
 * 
 * A sample is immutable.
 * 
 * @author acaproni
 *
 */
public class Sample {
	
	/**
	 * The monitor point value or alarm red from the remote system.
	 * <P>
	 * Note that for alarms the value must be {@link AlarmSample}
	 */
	public final Object value;
	
	/**
	 * The point in time when the value has been read from the remote system
	 */
	public final long timestamp;

	/**
	 * Constructor.
	 * <P>
	 * Note that for alarms the of the value parameter
	 * must be {@link AlarmSample}
	 * 
	 * @param value The value red from the system
	 * @param timestamp The timestamp
	 */
	public Sample(Object value, long timestamp) {
		if (value==null) {
			throw new IllegalArgumentException("Invalid null value");
		}
		this.value=value;
		this.timestamp=timestamp;
	}
	
	/**
	 * Constructor
	 * <P>
	 * Note that for alarms the of the value parameter
	 * must be {@link AlarmSample}
	 * 
	 * @param value The value red from the system
	 */
	public Sample(Object value) {
		if (value==null) {
			throw new IllegalArgumentException("Invalid null value");
		}
		this.value=value;
		this.timestamp=System.currentTimeMillis();
	}

}
