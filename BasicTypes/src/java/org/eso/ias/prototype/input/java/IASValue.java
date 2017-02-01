package org.eso.ias.prototype.input.java;

import org.eso.ias.prototype.input.java.OperationalMode;
import java.lang.StringBuilder;
/**
 * The view of a heterogeneous inputs in the java code.
 * 
 * Objects of this class are immutable i.e. updating returns
 * a new immutable object
 * 
 * @author acaproni
 *
 */
public abstract class IASValue<T> extends IASValueBase {
	
	/**
	 * The value of the HIO
	 */
	public final T value;
	
	/**
	 * Constructor
	 * 
	 * @param value The value of the output
	 * @param mode The new mode of the output
	 * @param id: The ID of this input
	 * @param runningId: The id of this input and its parents
	 * @param valueType: the IAS type of this input
	 */
	protected IASValue(T value,
			long tStamp,
			OperationalMode mode,
			String id,
			String runningId,
			IASTypes valueType) {
		super(tStamp,mode,id,runningId,valueType);
		this.value = value;
	}
	
	/**
	 * Build a new IASValue with the passed value
	 * 
	 * @param newValue The value to set in the new IASValue
	 * @return The new IASValue with the updated value
	 */
	abstract public IASValue<T> updateValue(T newValue);
	
	@Override
	public String toString() {
		StringBuilder ret = new StringBuilder(super.toString());
		ret.append(", value=");
		ret.append(value);
		return ret.toString();
	}
}
