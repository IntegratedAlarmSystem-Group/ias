package org.eso.ias.prototype.input.java;

import java.util.Objects;

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
	 * The validity
	 */
	public final IasValidity iasValidity;
	
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
			IasValidity iasValidity,
			String id,
			String runningId,
			IASTypes valueType) {
		super();
		Objects.requireNonNull(mode,"The mode can't be null");
		Objects.requireNonNull(iasValidity,"The validity can't be null");
		
		this.timestamp=tStamp;
		this.mode = mode;
		this.iasValidity=iasValidity;
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



	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((id == null) ? 0 : id.hashCode());
		result = prime * result + ((mode == null) ? 0 : mode.hashCode());
		result = prime * result + ((iasValidity == null) ? 0 : iasValidity.hashCode());
		result = prime * result + ((runningId == null) ? 0 : runningId.hashCode());
		result = prime * result + (int) (timestamp ^ (timestamp >>> 32));
		result = prime * result + ((valueType == null) ? 0 : valueType.hashCode());
		return result;
	}



	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		IASValueBase other = (IASValueBase) obj;
		if (id == null) {
			if (other.id != null)
				return false;
		} else if (!id.equals(other.id))
			return false;
		if (mode != other.mode)
			return false;
		if (iasValidity!=other.iasValidity)
			return false;
		if (runningId == null) {
			if (other.runningId != null)
				return false;
		} else if (!runningId.equals(other.runningId))
			return false;
		if (timestamp != other.timestamp)
			return false;
		if (valueType != other.valueType)
			return false;
		return true;
	}
}
