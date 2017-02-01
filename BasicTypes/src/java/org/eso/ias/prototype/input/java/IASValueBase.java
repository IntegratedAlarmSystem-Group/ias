package org.eso.ias.prototype.input.java;

import org.eso.ias.prototype.input.Identifier;
import org.eso.ias.prototype.input.java.OperationalMode;
import java.lang.StringBuilder;
/**
 * The view of the untyped heterogeneous inputs in the java code.
 * 
 * Objects of this class are immutable i.e. updating returns
 * a new immutable object
 * 
 * @author acaproni
 *
 */
public abstract class IASValueBase {
	
	/**
	 * The time when the value has been assigned to the HIO
	 */
	public final long timestamp;
	
	/**
	 * The mode of the input
	 * 
	 * @see OperationalMode
	 */
	public final OperationalMode mode;
	
	/**
	 * The identifier of the input
	 * 
	 * @see Identifier
	 */
	public final String id;
	
	/**
	 * The identifier of the input concatenated with
	 * that of its parents
	 * 
	 * @see Identifier
	 */
	public final String runningId;
	
	/**
	 * The IAS representation of the type of this input.
	 * 
	 * @see IASTypes
	 */
	public final IASTypes valueType;
	
	/**
	 * Constructor
	 * 
	 * @param mode The new mode of the output
	 * @param id: The ID of this input
	 * @param runningId: The id of this input and its parents
	 * @param valueType: the IAS type of this input
	 */
	protected IASValueBase(long tStamp,
			OperationalMode mode,
			String id,
			String runningId,
			IASTypes valueType) {
		super();
		if (mode==null) {
			throw new NullPointerException("The mode can't be null");
		}
		this.timestamp=tStamp;
		this.mode = mode;
		this.id=id;
		this.runningId=runningId;
		this.valueType=valueType;
	}
	
	/**
	 * Build a new IASValue with the passed mode
	 * 
	 * @param newMode The mode to set in the new IASValue
	 * @return The new IASValue with the updated mode
	 */
	abstract public IASValueBase updateMode(OperationalMode newMode);
	
	@Override
	public String toString() {
		StringBuilder ret = new StringBuilder("IASValue: id=");
		ret.append(id);
		ret.append(", runningID=");
		ret.append(runningId);
		ret.append(", timestamp=");
		ret.append(timestamp);
		ret.append(", mode=");
		ret.append(mode);
		ret.append(", type=");
		ret.append(valueType);
		return ret.toString();
	}
}
