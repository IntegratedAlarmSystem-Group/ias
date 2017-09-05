package org.eso.ias.prototype.input.java;

import java.util.Objects;

import org.eso.ias.plugin.OperationalMode;
import org.eso.ias.prototype.input.Identifier;
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
	 * @param tStamp The timestamp
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
		Objects.requireNonNull(mode,"The mode can't be null");
		
		this.timestamp=tStamp;
		this.mode = mode;
		Objects.requireNonNull(id);
		if (id.isEmpty()) {
			throw new IllegalArgumentException("The ID can't be empty");
		}
		this.id=id;
		this.runningId=runningId;
		Objects.requireNonNull(valueType);
		this.valueType=valueType;
	}
	
	
	
	@Override
	public String toString() {
		StringBuilder ret = new StringBuilder("IASValueBase: id=");
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
