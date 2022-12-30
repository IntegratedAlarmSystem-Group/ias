package org.eso.ias.types;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import org.eso.ias.utils.ISO8601Helper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * A java pojo to serialize/deserialize {@link IASValue} objects.
 * <P>
 * This pojo solves the problem of serializing/deserializing abstract
 * classes with jackson2 and offers setters and getters.
 * The reasons to have this class separated by IASValue are:
 * <UL>
 * 	<LI>avoid providing setters that would brake the immutability of the {@link IASValue}
 *  <LI>replace Optional.empty() with <code>null</code> so that a null value
 *      is not serialized in the JSON string (@see {@link Include})
 *  <LI>replace Long timestamps with ISO-8601 strings
 * </UL>
 * 
 * Timestamps are represented as strings (ISO-8601)
 * 
 * @author acaproni
 *
 */
public class IasValueJsonPojo {
	
	/**
	 * The logger
	 */
	private static final Logger logger = LoggerFactory.getLogger(IasValueJsonPojo.class);
	
	/**
	 * The value of the output
	 */
	private String value;

	/**
	 * @see IASValue#readFromMonSysTStamp
	 */
	@JsonInclude(Include.NON_NULL)
	private String readFromMonSysTStamp;
	 
	/**
	 * @see IASValue#productionTStamp
	 */
	@JsonInclude(Include.NON_NULL)
	private String productionTStamp;

	/**
	 * @see IASValue#sentToConverterTStamp
	 */
	@JsonInclude(Include.NON_NULL)
	private String sentToConverterTStamp;
	
	/**
	 * @see IASValue#receivedFromPluginTStamp
	 */
	@JsonInclude(Include.NON_NULL)
	private String receivedFromPluginTStamp;
	
	/**
	 * @see IASValue#convertedProductionTStamp
	 */
	@JsonInclude(Include.NON_NULL)
	private String convertedProductionTStamp;
	
	/**
	 * @see IASValue#sentToBsdbTStamp
	 */
	@JsonInclude(Include.NON_NULL)
	private String sentToBsdbTStamp;
	
	/**
	 * @see IASValue#readFromBsdbTStamp
	 */
	@JsonInclude(Include.NON_NULL)
	private String readFromBsdbTStamp;
	
	/**
	 * @see IASValue#dependentsFullRuningIds
	 */
	@JsonInclude(Include.NON_NULL)
	private Set<String> depsFullRunningIds;
	
	/**
	 * Additional properties
	 */
	@JsonInclude(Include.NON_NULL)
	private Map<String, String> props;
	 
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
		mode=iasValue.mode;
		fullRunningId=iasValue.fullRunningId;
		valueType=iasValue.valueType;
		iasValidity=iasValue.iasValidity;

		// Convert the timestamp in a human readable ISO 8601 string
		if (valueType==IASTypes.TIMESTAMP) {
			String temp = iasValue.value.toString();
			value=ISO8601Helper.getTimestamp(Long.valueOf(temp));
		} else if (valueType==IASTypes.ARRAYOFDOUBLES || valueType==IASTypes.ARRAYOFLONGS) {
			value = ((NumericArray)iasValue.value).codeToString();
		} else {
			value=iasValue.value.toString();
		}

		this.readFromMonSysTStamp=convertTStampToIso8601(iasValue.readFromMonSysTStamp);
		this.productionTStamp=convertTStampToIso8601(iasValue.productionTStamp);
		this.sentToConverterTStamp=convertTStampToIso8601(iasValue.sentToConverterTStamp);
		this.receivedFromPluginTStamp=convertTStampToIso8601(iasValue.receivedFromPluginTStamp);
		this.convertedProductionTStamp=convertTStampToIso8601(iasValue.convertedProductionTStamp);
		this.sentToBsdbTStamp=convertTStampToIso8601(iasValue.sentToBsdbTStamp);
		this.readFromBsdbTStamp=convertTStampToIso8601(iasValue.readFromBsdbTStamp);

		this.depsFullRunningIds=iasValue.dependentsFullRuningIds.orElse(null);
		this.props = iasValue.props.orElse(null);
		
	}
	
	/**
	 * if present, convert the passed timestamp from long
	 * to ISO 8601
	 * 
	 * @param tStamp the timestamp to convert to IAS-8601
	 * @return The ISO-8601 representation of the tStamp
	 *         or <code>null</code> if tStamp is empty
	 */
	private String convertTStampToIso8601(Optional<Long> tStamp) {
		assert(tStamp!=null);
		Optional<String> isoTStamp = tStamp.map( stamp -> ISO8601Helper.getTimestamp(stamp));
		return isoTStamp.orElse(null);
	}
	
	/**
	 * Convert the passed ISO-8601 to string
	 *  
	 * @param iso8601Str the not ISO-8601 string to convert
	 * @return A Long representation of the passed string,
	 *         or empty if iso8601Str was <code>null</code>
	 */
	private Optional<Long> convertIso8601ToTStamp(String iso8601Str) {
		Optional<String> optStr = Optional.ofNullable(iso8601Str);
		if (!optStr.isPresent()) {
			return Optional.empty();
		}
		
		Long tstamp = null;
		tstamp = ISO8601Helper.timestampToMillis(iso8601Str);
		return Optional.ofNullable(tstamp);
		
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

	public String getReadFromMonSysTStamp() { return readFromMonSysTStamp; }

	public String getProductionTStamp() {
		return productionTStamp;
	}

	public String getSentToConverterTStamp() {
		return sentToConverterTStamp;
	}

	public String getReceivedFromPluginTStamp() {
		return receivedFromPluginTStamp;
	}

	public String getConvertedProductionTStamp() {
		return convertedProductionTStamp;
	}

	public String getSentToBsdbTStamp() {
		return sentToBsdbTStamp;
	}

	public String getReadFromBsdbTStamp() {
		return readFromBsdbTStamp;
	}

	public IASValue<?> toIasValue() {
		// Convert the string to the proper type
		Object theValue;
		switch (valueType) {
			case LONG: theValue=Long.valueOf(value); break;
			case TIMESTAMP: theValue=Long.valueOf(ISO8601Helper.timestampToMillis(value)); break;
	 		case INT: theValue=Integer.valueOf(value); break;
			case SHORT: theValue=Short.valueOf(value); break;
			case BYTE: theValue=Byte.valueOf(value); break;
			case DOUBLE: theValue=Double.valueOf(value); break;
			case FLOAT: theValue=Float.valueOf(value); break;
			case BOOLEAN: theValue=Boolean.valueOf(value); break;
			case CHAR: theValue=Character.valueOf(value.charAt(0)); break;
			case STRING: theValue=value; break;
			case ARRAYOFDOUBLES: theValue=NumericArray.valueOf(NumericArray.NumericArrayType.DOUBLE,value); break;
			case ARRAYOFLONGS: theValue=NumericArray.valueOf(NumericArray.NumericArrayType.LONG,value); break;
			case ALARM: {
				System.out.println("Converting object to alarm "+value);
				theValue=Alarm.valueOf(value);
			} break;
			default: throw new UnsupportedOperationException("Unsupported type "+valueType);
		}

		return new IASValue(
				theValue, 
				mode, 
				iasValidity, 
				fullRunningId, 
				valueType,
				convertIso8601ToTStamp(readFromMonSysTStamp),
				convertIso8601ToTStamp(productionTStamp),
				convertIso8601ToTStamp(sentToConverterTStamp), 
				convertIso8601ToTStamp(receivedFromPluginTStamp), 
				convertIso8601ToTStamp(convertedProductionTStamp), 
				convertIso8601ToTStamp(sentToBsdbTStamp), 
				convertIso8601ToTStamp(readFromBsdbTStamp), 
				Optional.ofNullable(depsFullRunningIds),
				Optional.ofNullable(props));
	}

	public Set<String> getDepsFullRunningIds() {
		return depsFullRunningIds;
	}

	public void setDepsFullRunningIds(Set<String> dependentsFullRuningIds) {
		this.depsFullRunningIds = dependentsFullRuningIds;
	}

	public Map<String, String> getProps() {
		return props;
	}

	public void setProps(Map<String, String> props) {
		this.props = props;
	}

}
