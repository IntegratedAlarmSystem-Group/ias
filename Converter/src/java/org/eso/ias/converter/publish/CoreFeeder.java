package org.eso.ias.converter.publish;

import org.eso.ias.prototype.input.java.IASValue;

/**
 * The interface defining the method to call to send a value
 * to the core of the IAS.
 * 
 * @author acaproni
 *
 */
public interface CoreFeeder {

	/**
	 * Send the passed value to the core of the IAS for processing
	 * 
	 * @param iasValue The not <code>null</code> to send to the core of the IAS
	 * @throws CoreFeederException in case of error pushing the value
	 */
	public void push(IASValue<?> iasValue) throws CoreFeederException ;
}
