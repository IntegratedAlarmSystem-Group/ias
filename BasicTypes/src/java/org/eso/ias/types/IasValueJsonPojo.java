package org.eso.ias.types;

import java.util.Objects;
import java.util.Optional;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

/**
 * A java pojo to serialize/deserialize {@link IASValue} objects.
 * <P>
 * This pojo is meant to solve the problem of
 * serializing/deserializing abstract classes with jackson2
 * and offers setters and getters. The reason to have this class
 * separated by IASValue is to 
 * <UL>
 * 	<LI>avoid providing setters that would brake the immutability of the {@link IASValue}
 *  <LI>replace optiona.empty() with <code>null</code> so that a null value 
 *      is not serialized in the JSON string (@see {@link Include})
 * </UL>
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
	 * @see IASValue#pluginProductionTStamp
	 */
	@JsonInclude(Include.NON_NULL)
	private Long pluginProductionTStamp;
	
	
	/**
	 * @see IASValue#sentToConverterTStamp
	 */
	@JsonInclude(Include.NON_NULL)
	private Long sentToConverterTStamp;
	
	/**
	 * @see IASValue#receivedFromPluginTStamp
	 */
	@JsonInclude(Include.NON_NULL)
	private Long receivedFromPluginTStamp;
	
	/**
	 * @see IASValue#convertedProductionTStamp
	 */
	@JsonInclude(Include.NON_NULL)
	private Long convertedProductionTStamp;
	
	/**
	 * @see IASValue#sentToBsdbTStamp
	 */
	@JsonInclude(Include.NON_NULL)
	private Long sentToBsdbTStamp;
	
	/**
	 * @see IASValue#readFromBsdbTStamp
	 */
	@JsonInclude(Include.NON_NULL)
	private Long readFromBsdbTStamp;
	
	/**
	 * @see IASValue#dasuProductionTStamp
	 */
	@JsonInclude(Include.NON_NULL)
	private Long dasuProductionTStamp;
	 
	/**
	 * The operational mode
	 */
	private OperationalMode mode;
	
	/**
	 * The validity
	 */
	private IasValidity iasValidity;
	 
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
		mode=iasValue.mode;
		fullRunningId=iasValue.fullRunningId;
		valueType=iasValue.valueType;
		iasValidity=iasValue.iasValidity;
		
		this.pluginProductionTStamp=iasValue.pluginProductionTStamp.orElse(null);
		this.sentToConverterTStamp=iasValue.sentToConverterTStamp.orElse(null);
		this.receivedFromPluginTStamp=iasValue.receivedFromPluginTStamp.orElse(null);
		this.convertedProductionTStamp=iasValue.convertedProductionTStamp.orElse(null);
		this.sentToBsdbTStamp=iasValue.sentToBsdbTStamp.orElse(null);
		this.readFromBsdbTStamp=iasValue.readFromBsdbTStamp.orElse(null);
		this.dasuProductionTStamp=iasValue.dasuProductionTStamp.orElse(null);
		
	}

	public String getValue() {
		return value;
	}

	public void setValue(String value) {
		this.value = value;
	}

	public OperationalMode getMode() {
		return mode;
	}

	public void setMode(OperationalMode mode) {
		this.mode = mode;
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

	public IasValidity getIasValidity() {
		return iasValidity;
	}

	public void setIasValidity(IasValidity iasValidity) {
		this.iasValidity = iasValidity;
	}

	public Long getPluginProductionTStamp() {
		return pluginProductionTStamp;
	}

	public Long getSentToConverterTStamp() {
		return sentToConverterTStamp;
	}

	public Long getReceivedFromPluginTStamp() {
		return receivedFromPluginTStamp;
	}

	public Long getConvertedProductionTStamp() {
		return convertedProductionTStamp;
	}

	public Long getSentToBsdbTStamp() {
		return sentToBsdbTStamp;
	}

	public Long getReadFromBsdbTStamp() {
		return readFromBsdbTStamp;
	}

	public Long getDasuProductionTStamp() {
		return dasuProductionTStamp;
	}
	
	public IASValue<?> toIasValue() {
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
		return new IASValue(
				theValue, 
				mode, 
				iasValidity, 
				fullRunningId, 
				valueType, 
				Optional.ofNullable(pluginProductionTStamp), 
				Optional.ofNullable(sentToConverterTStamp), 
				Optional.ofNullable(receivedFromPluginTStamp), 
				Optional.ofNullable(convertedProductionTStamp), 
				Optional.ofNullable(sentToBsdbTStamp), 
				Optional.ofNullable(readFromBsdbTStamp), 
				Optional.ofNullable(dasuProductionTStamp));
	}

}
