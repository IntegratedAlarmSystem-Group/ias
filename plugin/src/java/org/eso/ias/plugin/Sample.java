package org.eso.ias.plugin;

/**
 * A sample of the value read from the remote system
 * <P>
 * A sample is immutable
 * 
 * @author acaproni
 *
 */
public class Sample {
	
	/**
	 * The vmonitor point value or alarm red from the remote system.
	 */
	public final Object value;
	
	/**
	 * The point in time when the value has been read from the remote system
	 */
	public final long timestamp;

	/**
	 * Constructor
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
