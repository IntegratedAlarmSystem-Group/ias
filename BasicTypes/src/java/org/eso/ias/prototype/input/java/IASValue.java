package org.eso.ias.prototype.input.java;

import java.util.Objects;

/**
 * The view of a heterogeneous inputs in the java code.
 * 
 * Objects of this class are immutable i.e. updating returns
 * a new immutable object
 * 
 * @param T The type of the value
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
	 * @param tStamp The timestamp
	 * @param mode The new mode of the output
	 * @param fullRunningId: The full running id of this input and its parents
	 * @param valueType: the IAS type of this input
	 */
	protected IASValue(T value,
			long tStamp,
			OperationalMode mode,
			IasValidity iasValidity,
			String fullRunningId,
			IASTypes valueType) {
		super(tStamp,mode,iasValidity,fullRunningId,valueType);
		this.value = value;
	}
	
	/**
	 * Build a new IASValue with the passed value
	 * 
	 * @param newValue The value to set in the new IASValue
	 * @return The new IASValue with the updated value
	 */
	abstract public IASValue<T> updateValue(T newValue);
	
	/**
	 * Build a new IASValue with the passed mode
	 * 
	 * @param newMode The mode to set in the new IASValue
	 * @return The new IASValue with the updated mode
	 */
	abstract public IASValue<T> updateMode(OperationalMode newMode);
	
	@Override
	public String toString() {
		StringBuilder ret = new StringBuilder(super.toString());
		ret.append(", value=");
		ret.append(value);
		return ret.toString();
	}
	
	/**
	 * Factory method to build IASValues of the passed type.
	 * 
	 * @param value The value
	 * @param tStamp The timestamp
	 * @param mode The operartional mode
	 * @param iasValidity The validity
	 * @param fullRunningId Full running ID
	 * @param valueType The type of the value
	 * @return A IasValue initialized with teh passed parameters
	 */
	public static <X> IASValue<?> buildIasValue(
			X value,
			long tStamp,
			OperationalMode mode,
			IasValidity iasValidity,
			String fullRunningId,
			IASTypes valueType) {
		Objects.requireNonNull(valueType);
		switch (valueType) {
			case LONG: return new IasLong((Long)value, tStamp, mode,iasValidity, fullRunningId);
	 		case INT: return new IasInt((Integer)value, tStamp, mode, iasValidity, fullRunningId);
			case SHORT: return new IasShort((Short)value, tStamp, mode, iasValidity, fullRunningId);
			case BYTE: return new IasByte((Byte)value, tStamp, mode, iasValidity, fullRunningId);
			case DOUBLE: return new IasDouble((Double)value, tStamp, mode, iasValidity, fullRunningId);
			case FLOAT: return new IasFloat((Float)value, tStamp, mode, iasValidity, fullRunningId);
			case BOOLEAN: return new IasBool((Boolean)value, tStamp, mode, iasValidity, fullRunningId);
			case CHAR: return new IasChar((Character)value, tStamp, mode, iasValidity, fullRunningId);
			case STRING: return new IasString((String)value, tStamp, mode, iasValidity, fullRunningId);
			case ALARM: return new IasAlarm((AlarmSample )value, tStamp, mode, iasValidity, fullRunningId);
			default: throw new UnsupportedOperationException("Unsupported type "+valueType);
		}
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + ((value == null) ? 0 : value.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (!super.equals(obj))
			return false;
		if (getClass() != obj.getClass())
			return false;
		IASValue other = (IASValue) obj;
		if (value == null) {
			if (other.value != null)
				return false;
		} else if (!value.equals(other.value))
			return false;
		return true;
	}
}
