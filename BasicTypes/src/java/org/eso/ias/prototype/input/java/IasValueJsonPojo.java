package org.eso.ias.prototype.input.java;

import java.util.Objects;

/**
 * A java pojo to serialize/deserialize {@link IASValue} objects.
 * <P>
 * This pojo is meant to solve the problem of
 * serializing/deserializing abstract classes with jackson2.
 * 
 * @author acaproni
 *
 */
public class IasValueJsonPojo {
	
	/**
	 * The value of the output
	 */
	private String value;
	 
	/**
	 * The timestamp
	 */
	private long tStamp;
	 
	/**
	 * mode The new mode of the output
	 */

	/**
	 * The operational mode
	 */
	private OperationalMode mode;
	
	/**
	 * The validity
	 */
	private IasValidity iasValidity;
	 
	/**
	 * The ID of this input
	 */
	private String id;
	
	/**
	 * The full running id of this input and its parents
	 */
	private String fullRunningId;
	
	/**
	 *  The type of this input
	 */
	private IASTypes valueType;
	
	/**
	 * Empty constructor
	 */
	public IasValueJsonPojo() {}

	/**
	 * Constructor
	 * 
	 * @param iasValue The {@link IASValue}
	 */
	public IasValueJsonPojo(IASValue<?> iasValue) {
		Objects.requireNonNull(iasValue);
		
		value=iasValue.value.toString();
		tStamp=iasValue.timestamp;
		mode=iasValue.mode;
		id=iasValue.id;
		fullRunningId=iasValue.runningId;
		valueType=iasValue.valueType;
		iasValidity=iasValue.iasValidity;
	}

	public String getValue() {
		return value;
	}

	public void setValue(String value) {
		this.value = value;
	}

	public long gettStamp() {
		return tStamp;
	}

	public void settStamp(long tStamp) {
		this.tStamp = tStamp;
	}

	public OperationalMode getMode() {
		return mode;
	}

	public void setMode(OperationalMode mode) {
		this.mode = mode;
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getFullRunningId() {
		return fullRunningId;
	}

	public void setFullRunningId(String fullRunningId) {
		this.fullRunningId = fullRunningId;
	}

	public IASTypes getValueType() {
		return valueType;
	}

	public void setValueType(IASTypes valueType) {
		this.valueType = valueType;
	}
	
	public IASValue<?> asIasValue() {

		
		// Convert the string to the proper type
		Object theValue;
		switch (valueType) {
			case LONG: theValue=Long.valueOf(value); break;
	 		case INT: theValue=Integer.valueOf(value); break;
			case SHORT: theValue=Short.valueOf(value); break;
			case BYTE: theValue=Byte.valueOf(value); break;
			case DOUBLE: theValue=Double.valueOf(value); break;
			case FLOAT: theValue=Float.valueOf(value); break;
			case BOOLEAN: theValue=Boolean.valueOf(value); break;
			case CHAR: theValue=Character.valueOf(value.charAt(0)); break;
			case STRING: theValue=value; break;
			case ALARM: theValue=AlarmSample.valueOf(value); break;
			default: throw new UnsupportedOperationException("Unsupported type "+valueType);
		}
		return IASValue.buildIasValue(theValue, tStamp, mode, iasValidity,id, fullRunningId, valueType);
	}

	public IasValidity getIasValidity() {
		return iasValidity;
	}

	public void setIasValidity(IasValidity iasValidity) {
		this.iasValidity = iasValidity;
	}

}
