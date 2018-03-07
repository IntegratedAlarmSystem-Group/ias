package org.eso.ias.types;

import java.util.HashSet;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import org.eso.ias.utils.ISO8601Helper;

/**
 * The view of a heterogeneous inputs in the java code and in the BSDB.
 * 
 * Objects of this class are immutable i.e. updating returns
 * a new immutable object
 * 
 * @param T The type of the value
 * @author acaproni
 *
 */
public class IASValue<T> {
	
	/**
	 * The value of the HIO
	 */
	public final T value;
	
	/**
	 * The point in time when the plugin produced this value
	 */
	public final Optional<Long> pluginProductionTStamp;
	
	
	/**
	 * The point in time when the plugin sent the 
	 * value to the converter
	 */
	public final Optional<Long> sentToConverterTStamp;
	
	/**
	 * The point in time when the converter received the  
	 * value from the plugin
	 */
	public final Optional<Long> receivedFromPluginTStamp;
	
	/**
	 * The point in time when the converter generated
	 * the value from the data structure received by the plugin
	 */
	public final Optional<Long> convertedProductionTStamp;
	
	/**
	 * The list of the full running identifiers of the dependent
	 * monitor point 
	 */
	public final Set<String> dependentsFullRuningIds;
	
	/**
	 * The point in time when the value has been sent to the BSDB
	 * 
	 * Note that this timestamp is set
	 * <UL>
	 * 	<LI>by the converter when it sends the converted value to the BSDB
	 *  <LI>by the DASU when it publishes the output to the BSDB
	 * </UL>
	 */
	public final Optional<Long> sentToBsdbTStamp;
	
	/**
	 * The point in time when the value has been read from the BSDB
	 */
	public final Optional<Long> readFromBsdbTStamp;
	
	/**
	 * The point in time when the value has been
	 * generated by the DASU
	 */
	public final Optional<Long> dasuProductionTStamp;
	
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
	 * @param value The value of the output
	 * @param tStamp The timestamp
	 * @param mode The new mode of the output
	 * @param fullRunningId: The full running id of this input and its parents
	 * @param valueType: the IAS type of this input
	 * @param pluginProductionTStamp The point in time when the plugin produced this value
	 * @param sentToConverterTStamp The point in time when the plugin sent the value to the converter
	 * @param receivedFromPluginTStamp The point in time when the converter received the value from the plugin
	 * @param convertedProductionTStamp The point in time when the converter generated
	 *                                  the value from the data structure received by the plugin
	 * @param sentToBsdbTStamp The point in time when the value has been sent to the BSDB
	 * @param readFromBsdbTStamp The point in time when the value has been read from the BSDB
	 * @param dasuProductionTStamp The point in time when the value has been generated by the DASU
	 */
	public IASValue(
			T value,
			OperationalMode mode,
			IasValidity iasValidity,
			String fullRunningId,
			IASTypes valueType,
			Optional<Long> pluginProductionTStamp,
			Optional<Long> sentToConverterTStamp,
			Optional<Long> receivedFromPluginTStamp,
			Optional<Long> convertedProductionTStamp,
			Optional<Long> sentToBsdbTStamp,
			Optional<Long> readFromBsdbTStamp,
			Optional<Long> dasuProductionTStamp,
			Set<String> dependentsFullRuningIds) {
		Objects.requireNonNull(mode,"The mode can't be null");
		Objects.requireNonNull(iasValidity,"The validity can't be null");
		Objects.requireNonNull(valueType,"The type can't be null");
		Objects.requireNonNull(pluginProductionTStamp);
		Objects.requireNonNull(sentToConverterTStamp);
		Objects.requireNonNull(receivedFromPluginTStamp);
		Objects.requireNonNull(convertedProductionTStamp);
		Objects.requireNonNull(sentToBsdbTStamp);
		Objects.requireNonNull(readFromBsdbTStamp);
		Objects.requireNonNull(dasuProductionTStamp);
		Objects.requireNonNull(dependentsFullRuningIds);
		this.value = value;
		this.mode = mode;
		this.iasValidity=iasValidity;
		if (!Identifier.checkFullRunningIdFormat(fullRunningId)) {
			throw new IllegalArgumentException("Invalid full running ID ["+fullRunningId+"]");
		}
		this.fullRunningId=fullRunningId;
		this.valueType=valueType;
		
		// Get the ID from the passed full running id
		String[] parts = this.fullRunningId.split(Identifier.separator());
		String lastCouple = parts[parts.length-1];
		String[] coupleParts = lastCouple.split(Identifier.coupleSeparator());
		this.id=coupleParts[0].substring(Identifier.coupleGroupPrefix().length());
		
		this.pluginProductionTStamp=pluginProductionTStamp;
		this.sentToConverterTStamp=sentToConverterTStamp;
		this.receivedFromPluginTStamp=receivedFromPluginTStamp;
		this.convertedProductionTStamp=convertedProductionTStamp;
		this.sentToBsdbTStamp= sentToBsdbTStamp;
		this.readFromBsdbTStamp= readFromBsdbTStamp;
		this.dasuProductionTStamp= dasuProductionTStamp;
		this.dependentsFullRuningIds=dependentsFullRuningIds;
	}
	
	/**
	 * Build a new IASValue with the passed value
	 * 
	 * @param newValue The value to set in the new IASValue
	 * @return The new IASValue with the updated value
	 */
	public <X> IASValue<?> updateValue(X newValue) {
		return new IASValue<X>(
				newValue,
				this.mode,
				this.iasValidity,
				this.fullRunningId,
				this.valueType,
				this.pluginProductionTStamp,
				this.sentToConverterTStamp,
				this.receivedFromPluginTStamp,
				this.convertedProductionTStamp,
				this.sentToBsdbTStamp,
				this.readFromBsdbTStamp,
				this.dasuProductionTStamp,
				this.dependentsFullRuningIds);
	}
	
	/**
	 * Build a new IASValue with the passed mode
	 * 
	 * @param newMode The mode to set in the new IASValue
	 * @return The new IASValue with the updated mode
	 */
	public IASValue<T> updateMode(OperationalMode newMode) {
		return new IASValue<T>(
				this.value,
				newMode,
				this.iasValidity,
				this.fullRunningId,
				this.valueType,
				this.pluginProductionTStamp,
				this.sentToConverterTStamp,
				this.receivedFromPluginTStamp,
				this.convertedProductionTStamp,
				this.sentToBsdbTStamp,
				this.readFromBsdbTStamp,
				this.dasuProductionTStamp,
				this.dependentsFullRuningIds);
	}
	
	/**
	 * Build a new IASValue with the passed validity
	 * 
	 * @param validity the validity
	 * @return The new IASValue with the updated validity
	 */
	public IASValue<T> updateValidity(IasValidity validity) {
		return new IASValue<T>(
				this.value,
				this.mode,
				validity,
				this.fullRunningId,
				this.valueType,
				this.pluginProductionTStamp,
				this.sentToConverterTStamp,
				this.receivedFromPluginTStamp,
				this.convertedProductionTStamp,
				this.sentToBsdbTStamp,
				this.readFromBsdbTStamp,
				this.dasuProductionTStamp,
				this.dependentsFullRuningIds);
	}
	
	/**
	 * Build a new IASValue with the passed plugin production time
	 * 
	 * @param timestamp The value to set in the new IASValue
	 * @return The new IASValue with the updated timestamp
	 */
	public IASValue<T> updatePluginProdTime(long timestamp) {
		return new IASValue<T>(
				this.value,
				this.mode,
				this.iasValidity,
				this.fullRunningId,
				this.valueType,
				Optional.ofNullable(timestamp),
				this.sentToConverterTStamp,
				this.receivedFromPluginTStamp,
				this.convertedProductionTStamp,
				this.sentToBsdbTStamp,
				this.readFromBsdbTStamp,
				this.dasuProductionTStamp,
				this.dependentsFullRuningIds);
	}
	
	/**
	 * Build a new IASValue with the passed time when sent to the converter
	 * 
	 * @param timestamp The value to set in the new IASValue
	 * @return The new IASValue with the updated timestamp
	 */
	public IASValue<T> updateSentToConverterTime(long timestamp) {
		return new IASValue<T>(
				this.value,
				this.mode,
				this.iasValidity,
				this.fullRunningId,
				this.valueType,
				this.pluginProductionTStamp,
				Optional.ofNullable(timestamp),
				this.receivedFromPluginTStamp,
				this.convertedProductionTStamp,
				this.sentToBsdbTStamp,
				this.readFromBsdbTStamp,
				this.dasuProductionTStamp,
				this.dependentsFullRuningIds);
	}
	
	/**
	 * Build a new IASValue with the passed time when received from plugin
	 * 
	 * @param timestamp The value to set in the new IASValue
	 * @return The new IASValue with the updated timestamp
	 */
	public IASValue<T> updateRecvFromPluginTime(long timestamp) {
		return new IASValue<T>(
				this.value,
				this.mode,
				this.iasValidity,
				this.fullRunningId,
				this.valueType,
				this.pluginProductionTStamp,
				this.sentToConverterTStamp,
				Optional.ofNullable(timestamp),
				this.convertedProductionTStamp,
				this.sentToBsdbTStamp,
				this.readFromBsdbTStamp,
				this.dasuProductionTStamp,
				this.dependentsFullRuningIds);
	}
	
	/**
	 * Build a new IASValue with the passed time when produced by Converter
	 * 
	 * @param validity the validity
	 * @return The new IASValue with the updated timestamp
	 */
	public IASValue<T> updateConverterProdTime(long timestamp) {
		return new IASValue<T>(
				this.value,
				this.mode,
				this.iasValidity,
				this.fullRunningId,
				this.valueType,
				this.pluginProductionTStamp,
				this.sentToConverterTStamp,
				this.receivedFromPluginTStamp,
				Optional.ofNullable(timestamp),
				this.sentToBsdbTStamp,
				this.readFromBsdbTStamp,
				this.dasuProductionTStamp,
				this.dependentsFullRuningIds);
	}
	
	/**
	 * Build a new IASValue with the passed time when sent to BSDB
	 * 
	 * @param validity the validity
	 * @return The new IASValue with the updated timestamp
	 */
	public IASValue<T> updateSentToBsdbTime(long timestamp) {
		return new IASValue<T>(
				this.value,
				this.mode,
				this.iasValidity,
				this.fullRunningId,
				this.valueType,
				this.pluginProductionTStamp,
				this.sentToConverterTStamp,
				this.receivedFromPluginTStamp,
				this.convertedProductionTStamp,
				Optional.ofNullable(timestamp),
				this.readFromBsdbTStamp,
				this.dasuProductionTStamp,
				this.dependentsFullRuningIds);
	}
	
	/**
	 * Build a new IASValue with the passed time when received from BSDB
	 * 
	 * @param validity the validity
	 * @return The new IASValue with the updated timestamp
	 */
	public IASValue<T> updateReadFromBsdbTime(long timestamp) {
		return new IASValue<T>(
				this.value,
				this.mode,
				this.iasValidity,
				this.fullRunningId,
				this.valueType,
				this.pluginProductionTStamp,
				this.sentToConverterTStamp,
				this.receivedFromPluginTStamp,
				this.convertedProductionTStamp,
				this.sentToBsdbTStamp,
				Optional.ofNullable(timestamp),
				this.dasuProductionTStamp,
				this.dependentsFullRuningIds);
	}
	
	/**
	 * Build a new IASValue with the passed time when produced by DASU
	 * 
	 * @param validity the validity
	 * @return The new IASValue with the updated timestamp
	 */
	public IASValue<T> updateDasuProdTime(long timestamp) {
		return new IASValue<T>(
				this.value,
				this.mode,
				this.iasValidity,
				this.fullRunningId,
				this.valueType,
				this.pluginProductionTStamp,
				this.sentToConverterTStamp,
				this.receivedFromPluginTStamp,
				this.convertedProductionTStamp,
				this.sentToBsdbTStamp,
				this.readFromBsdbTStamp,
				Optional.ofNullable(timestamp),
				this.dependentsFullRuningIds);
	}
	
	@Override
	public String toString() {
		StringBuilder ret = new StringBuilder("IASValue: id=[");
		ret.append(id);
		ret.append("], runningID=");
		ret.append(fullRunningId);
		pluginProductionTStamp.ifPresent(tStamp -> {
			ret.append(", produced by plugin at ");
			ret.append(ISO8601Helper.getTimestamp(tStamp));
		});
		
		sentToConverterTStamp.ifPresent(tStamp -> {
			ret.append(", sent by plugin to converter at ");
			ret.append(ISO8601Helper.getTimestamp(tStamp));
		});
		
		receivedFromPluginTStamp.ifPresent(tStamp -> {
			ret.append(", received by converter at ");
			ret.append(ISO8601Helper.getTimestamp(tStamp));
		});
		convertedProductionTStamp.ifPresent(tStamp -> {
			ret.append(", processed by converter at ");
			ret.append(ISO8601Helper.getTimestamp(tStamp));
		});
		sentToBsdbTStamp.ifPresent(tStamp -> {
			ret.append(", sent to BSDB at ");
			ret.append(ISO8601Helper.getTimestamp(tStamp));
		});
		dasuProductionTStamp.ifPresent(tStamp -> {
			ret.append(", generated by DASU at ");
			ret.append(ISO8601Helper.getTimestamp(tStamp));
		});
		readFromBsdbTStamp.ifPresent(tStamp -> {
			ret.append(", read from BSDB at ");
			ret.append(ISO8601Helper.getTimestamp(tStamp));
		});
		ret.append(", mode=");
		ret.append(mode);
		ret.append(", type=");
		ret.append(valueType);
		ret.append(", value=");
		ret.append(value);
		
		ret.append(", fullRunningIds of dependents=[");
		for (String s: dependentsFullRuningIds) {
			ret.append(' ');
			ret.append(s);
		}
		ret.append(" ]");
		
		return ret.toString();
	}
	
	/**
	 * Factory method to build IASValues of the passed type with no times.
	 * 
	 * @param value The value
	 * @param mode The operational mode
	 * @param iasValidity The validity
	 * @param fullRunningId Full running ID
	 * @param valueType The type of the value
	 * @return A IasValue initialized with the passed parameters and no timestamps
	 */
	public static <X> IASValue<?> build(
			X value,
			OperationalMode mode,
			IasValidity iasValidity,
			String fullRunningId,
			IASTypes valueType) {
		return build(value,mode,iasValidity,fullRunningId,valueType,null,null,null,null,null,null,null,new HashSet<String>());
	}
	
	/**
	 * Factory method to build IASValues of the passed type.
	 * 
	 * @param value The value
	 * @param mode The operational mode
	 * @param iasValidity The validity
	 * @param fullRunningId Full running ID
	 * 
	 * @param valueType The type of the value
	 * @param pluginProductionTStamp The point in time when the plugin produced this value
	 * @param sentToConverterTStamp The point in time when the plugin sent the value to the converter
	 * @param receivedFromPluginTStamp The point in time when the converter received the value from the plugin
	 * @param convertedProductionTStamp The point in time when the converter generated
	 *                                  the value from the data structure rceived by the plugin
	 * @param sentToBsdbTStamp The point in time when the value has been sent to the BSDB
	 * @param readFromBsdbTStamp The point in time when the value has been read from the BSDB
	 * @param dasuProductionTStamp The point in time when the value has been generated by the DASU
	 * @return A IasValue initialized with the passed parameters
	 */
	public static <X> IASValue<?> build(
			X value,
			OperationalMode mode,
			IasValidity iasValidity,
			String fullRunningId,
			IASTypes valueType,
			Long pluginProductionTStamp,
			Long sentToConverterTStamp,
			Long receivedFromPluginTStamp,
			Long convertedProductionTStamp,
			Long sentToBsdbTStamp,
			Long readFromBsdbTStamp,
			Long dasuProductionTStamp,
			Set<String> dependentsFullrunningIds) {
		Objects.requireNonNull(valueType);
		Objects.requireNonNull(dependentsFullrunningIds);
		switch (valueType) {
			case LONG: return new IASValue<Long>(
					(Long)value, 
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
					Optional.ofNullable(dasuProductionTStamp),
					dependentsFullrunningIds);
	 		case INT: return new IASValue<Integer>(
					(Integer)value, 
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
					Optional.ofNullable(dasuProductionTStamp),
					dependentsFullrunningIds);
			case SHORT: return new IASValue<Short>(
					(Short)value, 
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
					Optional.ofNullable(dasuProductionTStamp),
					dependentsFullrunningIds);
			case BYTE: return new IASValue<Byte>(
					(Byte)value, 
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
					Optional.ofNullable(dasuProductionTStamp),
					dependentsFullrunningIds);
			case DOUBLE: return new IASValue<Double>(
					(Double)value, 
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
					Optional.ofNullable(dasuProductionTStamp),
					dependentsFullrunningIds);
			case FLOAT: return new IASValue<Float>(
					(Float)value, 
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
					Optional.ofNullable(dasuProductionTStamp),
					dependentsFullrunningIds);
			case BOOLEAN: return new IASValue<Boolean>(
					(Boolean)value, 
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
					Optional.ofNullable(dasuProductionTStamp),
					dependentsFullrunningIds);
			case CHAR: return new IASValue<Character>(
					(Character)value, 
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
					Optional.ofNullable(dasuProductionTStamp),
					dependentsFullrunningIds);
			
			case STRING: return new IASValue<String>(
					(String)value, 
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
					Optional.ofNullable(dasuProductionTStamp),
					dependentsFullrunningIds);
			case ALARM: return new IASValue<AlarmSample>(
					(AlarmSample)value, 
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
					Optional.ofNullable(dasuProductionTStamp),
					dependentsFullrunningIds);
			default: throw new UnsupportedOperationException("Unsupported type "+valueType);
		}
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((convertedProductionTStamp == null) ? 0 : convertedProductionTStamp.hashCode());
		result = prime * result + ((dasuProductionTStamp == null) ? 0 : dasuProductionTStamp.hashCode());
		result = prime * result + ((dependentsFullRuningIds == null) ? 0 : dependentsFullRuningIds.hashCode());
		result = prime * result + ((fullRunningId == null) ? 0 : fullRunningId.hashCode());
		result = prime * result + ((iasValidity == null) ? 0 : iasValidity.hashCode());
		result = prime * result + ((id == null) ? 0 : id.hashCode());
		result = prime * result + ((mode == null) ? 0 : mode.hashCode());
		result = prime * result + ((pluginProductionTStamp == null) ? 0 : pluginProductionTStamp.hashCode());
		result = prime * result + ((readFromBsdbTStamp == null) ? 0 : readFromBsdbTStamp.hashCode());
		result = prime * result + ((receivedFromPluginTStamp == null) ? 0 : receivedFromPluginTStamp.hashCode());
		result = prime * result + ((sentToBsdbTStamp == null) ? 0 : sentToBsdbTStamp.hashCode());
		result = prime * result + ((sentToConverterTStamp == null) ? 0 : sentToConverterTStamp.hashCode());
		result = prime * result + ((value == null) ? 0 : value.hashCode());
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
		IASValue other = (IASValue) obj;
		if (convertedProductionTStamp == null) {
			if (other.convertedProductionTStamp != null)
				return false;
		} else if (!convertedProductionTStamp.equals(other.convertedProductionTStamp))
			return false;
		if (dasuProductionTStamp == null) {
			if (other.dasuProductionTStamp != null)
				return false;
		} else if (!dasuProductionTStamp.equals(other.dasuProductionTStamp))
			return false;
		if (dependentsFullRuningIds == null) {
			if (other.dependentsFullRuningIds != null)
				return false;
		} else if (!dependentsFullRuningIds.equals(other.dependentsFullRuningIds))
			return false;
		if (fullRunningId == null) {
			if (other.fullRunningId != null)
				return false;
		} else if (!fullRunningId.equals(other.fullRunningId))
			return false;
		if (iasValidity != other.iasValidity)
			return false;
		if (id == null) {
			if (other.id != null)
				return false;
		} else if (!id.equals(other.id))
			return false;
		if (mode != other.mode)
			return false;
		if (pluginProductionTStamp == null) {
			if (other.pluginProductionTStamp != null)
				return false;
		} else if (!pluginProductionTStamp.equals(other.pluginProductionTStamp))
			return false;
		if (readFromBsdbTStamp == null) {
			if (other.readFromBsdbTStamp != null)
				return false;
		} else if (!readFromBsdbTStamp.equals(other.readFromBsdbTStamp))
			return false;
		if (receivedFromPluginTStamp == null) {
			if (other.receivedFromPluginTStamp != null)
				return false;
		} else if (!receivedFromPluginTStamp.equals(other.receivedFromPluginTStamp))
			return false;
		if (sentToBsdbTStamp == null) {
			if (other.sentToBsdbTStamp != null)
				return false;
		} else if (!sentToBsdbTStamp.equals(other.sentToBsdbTStamp))
			return false;
		if (sentToConverterTStamp == null) {
			if (other.sentToConverterTStamp != null)
				return false;
		} else if (!sentToConverterTStamp.equals(other.sentToConverterTStamp))
			return false;
		if (value == null) {
			if (other.value != null)
				return false;
		} else if (!value.equals(other.value))
			return false;
		if (valueType != other.valueType)
			return false;
		return true;
	}

}
