package org.eso.ias.prototype.input.java;

public enum IasValidity {
	
	/** 
	 * The value has been provided in time
	 * and the operator can trust what the IAS shows 
	 */
	RELIABLE(1),
	
	/**
	 * The values has not been produced in time
	 * either by the monitored system or due to 
	 * network problems or any other reason: 
	 * what the IAS hows can be misaligned with 
	 * the actual information
	 */
	UNRELIABLE(0);
	
	/**
	 * The reliability factor gives a measure of how reliable is
	 * a validity.
	 * 
	 * The greater the reliablity factor, the better.
	 * 
	 * Implementation note: having an integer here is not very useful
	 * with only 2 possible values for the IASValidity but it could be
	 * important in future if we wish to distinguish between more cases
	 * and it offers implicitly an order. 
	 */
	public final int reliabilityFactor;
	
	/**
	 * Constructor 
	 * @param reliabilityFactor the reliability factor
	 */
	private IasValidity(int reliabilityFactor) {
		this.reliabilityFactor=reliabilityFactor;
	}
}
