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
	 * The full identifier of the input concatenated with
	 * that of its parents.
	 * 
	 * @see Identifier
	 */
	public final String fullRunningId;
	
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
	 * @param runningId: The id of this input and its parents
	 * @param valueType: the IAS type of this input
	 */
	protected IASValueBase(long tStamp,
			OperationalMode mode,
			IasValidity iasValidity,
			String fullRunningId,
			IASTypes valueType) {
		super();
		Objects.requireNonNull(mode,"The mode can't be null");
		Objects.requireNonNull(iasValidity,"The validity can't be null");
		
		this.timestamp=tStamp;
		this.mode = mode;
		this.iasValidity=iasValidity;
		if (!Identifier.checkFullRunningIdFormat(fullRunningId)) {
			throw new IllegalArgumentException("Invalid full running ID ["+fullRunningId+"]");
		}
		this.fullRunningId=fullRunningId;
		Objects.requireNonNull(valueType);
		this.valueType=valueType;
		
		// Get the ID from the passed full running id
		String[] parts = this.fullRunningId.split(Identifier.separator());
		String lastCouple = parts[parts.length-1];
		String[] coupleParts = lastCouple.split(Identifier.coupleSeparator());
		this.id=coupleParts[0].substring(Identifier.coupleGroupPrefix().length());
	}
	
	
	
	@Override
	public String toString() {
		StringBuilder ret = new StringBuilder("IASValueBase: id=");
		ret.append(id);
		ret.append(", runningID=");
		ret.append(fullRunningId);
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
		result = prime * result + ((fullRunningId == null) ? 0 : fullRunningId.hashCode());
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
		if (fullRunningId == null) {
			if (other.fullRunningId != null)
				return false;
		} else if (!fullRunningId.equals(other.fullRunningId))
			return false;
		if (timestamp != other.timestamp)
			return false;
		if (valueType != other.valueType)
			return false;
		return true;
	}
}
