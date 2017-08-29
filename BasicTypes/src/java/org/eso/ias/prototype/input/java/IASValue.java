package org.eso.ias.prototype.input.java;

import java.util.Objects;

import org.eso.ias.plugin.OperationalMode;
import org.eso.ias.prototype.input.AlarmValue;

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
	
	public static IASValue buildIasValue(
			Object value,
			long tStamp,
			OperationalMode mode,
			String id,
			String runningId,
			IASTypes valueType) {
		Objects.requireNonNull(valueType);
		switch (valueType) {
		case LONG: return new IasLong((Long)value, tStamp, mode, id, runningId);
 		case INT: return new IasInt((Integer)value, tStamp, mode, id, runningId);
		case SHORT: return new IasShort((Short)value, tStamp, mode, id, runningId);
		case BYTE: return new IasByte((Byte)value, tStamp, mode, id, runningId);
		case DOUBLE: return new IasDouble((Double)value, tStamp, mode, id, runningId);
		case FLOAT: return new IasFloat((Float)value, tStamp, mode, id, runningId);
		case BOOLEAN: return new IasBool((Boolean)value, tStamp, mode, id, runningId);
		case CHAR: return new IasChar((Character)value, tStamp, mode, id, runningId);
		case STRING: return new IasString((String)value, tStamp, mode, id, runningId);
		case ALARM: return new IasAlarm((AlarmValue)value, tStamp, mode, id, runningId);
		default: throw new UnsupportedOperationException("Unsupported type "+valueType);
		}
	}
}
