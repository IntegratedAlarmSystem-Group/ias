package org.eso.ias.types;

import org.eso.ias.utils.ISO8601Helper;

import java.util.*;

/**
 * The view of a heterogeneous inputs in the java code and in the BSDB.
 * 
 * Objects of this class are immutable i.e. updating returns
 * a new immutable object
 * 
 * @author acaproni
 *
 */
public class IASValue<T> {
	
	/**
	 * The value of the HIO
	 */
	public final T value;

	/**
	 * The point in time when the value has been read from the
	 * monitored system (set by the plugin only)
	 */
	public final Optional<Long> readFromMonSysTStamp;
	
	/**
	 * The point in time when the the value has bene produced by a plugin,
	 * a DASO or a core tool
     *
     * This timestamp is updated when the plugin re-send the last computed
     * value to the converter
	 */
	public final Optional<Long> productionTStamp;
	
	
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
	 * The unmodifiable set of the full running identifiers of the dependent
	 * monitor point, if any 
	 */
	public final Optional<Set<String>> dependentsFullRuningIds;
	
	/**
	 * Unmodifiable map of additional properties, if any
	 */
	public final Optional<Map<String, String>> props;
	
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
	 * @param mode The new mode of the output
	 * @param iasValidity The validity
	 * @param fullRunningId: The full running id of this input and its parents
	 * @param valueType: the IAS type of this input
     * @param readFromMonSysTStamp: the point  in time when the value has been read from the monitored system
	 * @param productionTStamp The point in time when the value has bene produced by a plugin, a DASU
	 *                               or a core tool
	 * @param sentToConverterTStamp The point in time when the plugin sent the value to the converter
	 * @param receivedFromPluginTStamp The point in time when the converter received the value from the plugin
	 * @param convertedProductionTStamp The point in time when the converter generated
	 *                                  the value from the data structure received by the plugin
	 * @param sentToBsdbTStamp The point in time when the value has been sent to the BSDB
	 * @param readFromBsdbTStamp The point in time when the value has been read from the BSDB
	 */
	public IASValue(
			T value,
			OperationalMode mode,
			IasValidity iasValidity,
			String fullRunningId,
			IASTypes valueType,
            Optional<Long> readFromMonSysTStamp,
			Optional<Long> productionTStamp,
			Optional<Long> sentToConverterTStamp,
			Optional<Long> receivedFromPluginTStamp,
			Optional<Long> convertedProductionTStamp,
			Optional<Long> sentToBsdbTStamp,
			Optional<Long> readFromBsdbTStamp,
			Optional<Set<String>> dependentsFullRuningIds,
			Optional<Map<String, String>> properties) {
		Objects.requireNonNull(mode,"The mode can't be null");
		Objects.requireNonNull(iasValidity,"The validity can't be null");
		Objects.requireNonNull(valueType,"The type can't be null");
		Objects.requireNonNull(readFromMonSysTStamp);
		Objects.requireNonNull(productionTStamp);
		Objects.requireNonNull(sentToConverterTStamp);
		Objects.requireNonNull(receivedFromPluginTStamp);
		Objects.requireNonNull(convertedProductionTStamp);
		Objects.requireNonNull(sentToBsdbTStamp);
		Objects.requireNonNull(readFromBsdbTStamp);
		Objects.requireNonNull(dependentsFullRuningIds);
		Objects.requireNonNull(properties);
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

		this.readFromMonSysTStamp = readFromMonSysTStamp;
		this.productionTStamp=productionTStamp;
		this.sentToConverterTStamp=sentToConverterTStamp;
		this.receivedFromPluginTStamp=receivedFromPluginTStamp;
		this.convertedProductionTStamp=convertedProductionTStamp;
		this.sentToBsdbTStamp= sentToBsdbTStamp;
		this.readFromBsdbTStamp= readFromBsdbTStamp;

		Optional<Set<String>> tempDeps = dependentsFullRuningIds.map( ids -> Collections.unmodifiableSet(ids));
		if (tempDeps.isPresent() && tempDeps.get().isEmpty()) {
			tempDeps=Optional.empty();
		}
		this.dependentsFullRuningIds=tempDeps;
		
		Optional<Map<String, String>> tempProps = properties.map( m -> Collections.unmodifiableMap(m));
		if (tempProps.isPresent() && tempProps.get().isEmpty()) {
			tempProps=Optional.empty();
		}
		this.props=tempProps;
	}
	
	/**
	 * Build a new IASValue with the passed value
	 * 
	 * @param newValue The value to set in the new IASValue
	 * @return The new IASValue with the updated value
	 */
	public <X extends T> IASValue<T> updateValue(X newValue) {
		return new IASValue<T>(
				newValue,
				this.mode,
				this.iasValidity,
				this.fullRunningId,
				this.valueType,
				this.readFromMonSysTStamp,
				Optional.of(System.currentTimeMillis()), // Production timestamp
				this.sentToConverterTStamp,
				this.receivedFromPluginTStamp,
				this.convertedProductionTStamp,
				this.sentToBsdbTStamp,
				this.readFromBsdbTStamp,
				this.dependentsFullRuningIds,
				this.props);
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
				this.readFromMonSysTStamp,
				this.productionTStamp,
				this.sentToConverterTStamp,
				this.receivedFromPluginTStamp,
				this.convertedProductionTStamp,
				this.sentToBsdbTStamp,
				this.readFromBsdbTStamp,
				this.dependentsFullRuningIds,
				this.props);
	}
	
	/**
	 * Build a new IASValue with the passed list of 
	 * fullRunningIds of the dependent monitor point
	 * 
	 * @param depfFullIDs The mode to set in the new IASValue
	 * @return The new IASValue with the updated mode
	 */
	public IASValue<T> updateFullIdsOfDependents(Collection<String> depfFullIDs) {
		Objects.requireNonNull(depfFullIDs);
		
		Set<String> newDeps = new HashSet<>(depfFullIDs);
		
		return new IASValue<T>(
				this.value,
				this.mode,
				this.iasValidity,
				this.fullRunningId,
				this.valueType,
                this.readFromMonSysTStamp,
				this.productionTStamp,
				this.sentToConverterTStamp,
				this.receivedFromPluginTStamp,
				this.convertedProductionTStamp,
				this.sentToBsdbTStamp,
				this.readFromBsdbTStamp,
				Optional.of(Collections.unmodifiableSet(newDeps)),
				this.props);
	}
	
	/**
	 * Update the additional properties
	 * 
	 * @param properties The new additional properties (can be null)
	 * @return A new IASValue with updated properties
	 */
	public IASValue<T> updateProperties(Map<String,String> properties) {
		
		Optional<Map<String,String>> propsOpt = Optional.ofNullable(properties);
		Optional<Map<String,String>> newProperties = propsOpt.map( m -> Collections.unmodifiableMap(m));
		
		return new IASValue<T>(
				this.value,
				this.mode,
				this.iasValidity,
				this.fullRunningId,
				this.valueType,
                this.readFromMonSysTStamp,
				this.productionTStamp,
				this.sentToConverterTStamp,
				this.receivedFromPluginTStamp,
				this.convertedProductionTStamp,
				this.sentToBsdbTStamp,
				this.readFromBsdbTStamp,
				this.dependentsFullRuningIds,
				newProperties);
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
                this.readFromMonSysTStamp,
				this.productionTStamp,
				this.sentToConverterTStamp,
				this.receivedFromPluginTStamp,
				this.convertedProductionTStamp,
				this.sentToBsdbTStamp,
				this.readFromBsdbTStamp,
				this.dependentsFullRuningIds,
				this.props);
	}

    /**
     * Build a new IASValue with the passed monitored
     * system production time
     *
     * @param timestamp The value to set in the new IASValue
     * @return The new IASValue with the updated timestamp
     */
    public IASValue<T> updateMonSysProdTime(long timestamp) {
        return new IASValue<T>(
                this.value,
                this.mode,
                this.iasValidity,
                this.fullRunningId,
                this.valueType,
                Optional.ofNullable(timestamp),
                this.productionTStamp,
                this.sentToConverterTStamp,
                this.receivedFromPluginTStamp,
                this.convertedProductionTStamp,
                this.sentToBsdbTStamp,
                this.readFromBsdbTStamp,
                this.dependentsFullRuningIds,
                this.props);
    }

	/**
	 * Build a new IASValue with the passed production time
	 * 
	 * @param timestamp The value to set in the new IASValue
	 * @return The new IASValue with the updated timestamp
	 */
	public IASValue<T> updateProdTime(long timestamp) {
		return new IASValue<T>(
				this.value,
				this.mode,
				this.iasValidity,
				this.fullRunningId,
				this.valueType,
                this.readFromMonSysTStamp,
				Optional.ofNullable(timestamp),
				this.sentToConverterTStamp,
				this.receivedFromPluginTStamp,
				this.convertedProductionTStamp,
				this.sentToBsdbTStamp,
				this.readFromBsdbTStamp,
				this.dependentsFullRuningIds,
				this.props);
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
                this.readFromMonSysTStamp,
				this.productionTStamp,
				Optional.ofNullable(timestamp),
				this.receivedFromPluginTStamp,
				this.convertedProductionTStamp,
				this.sentToBsdbTStamp,
				this.readFromBsdbTStamp,
				this.dependentsFullRuningIds,
				this.props);
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
                this.readFromMonSysTStamp,
				this.productionTStamp,
				this.sentToConverterTStamp,
				Optional.ofNullable(timestamp),
				this.convertedProductionTStamp,
				this.sentToBsdbTStamp,
				this.readFromBsdbTStamp,
				this.dependentsFullRuningIds,
				this.props);
	}
	
	/**
	 * Build a new IASValue with the passed time when produced by Converter
	 * 
	 * @param timestamp the timestamp
	 * @return The new IASValue with the updated timestamp
	 */
	public IASValue<T> updateConverterProdTime(long timestamp) {
		return new IASValue<T>(
				this.value,
				this.mode,
				this.iasValidity,
				this.fullRunningId,
				this.valueType,
                this.readFromMonSysTStamp,
				this.productionTStamp,
				this.sentToConverterTStamp,
				this.receivedFromPluginTStamp,
				Optional.ofNullable(timestamp),
				this.sentToBsdbTStamp,
				this.readFromBsdbTStamp,
				this.dependentsFullRuningIds,
				this.props);
	}
	
	/**
	 * Build a new IASValue with the passed time when sent to BSDB
	 * 
	 * @param timestamp the timestamp
	 * @return The new IASValue with the updated timestamp
	 */
	public IASValue<T> updateSentToBsdbTime(long timestamp) {
		return new IASValue<T>(
				this.value,
				this.mode,
				this.iasValidity,
				this.fullRunningId,
				this.valueType,
                this.readFromMonSysTStamp,
				this.productionTStamp,
				this.sentToConverterTStamp,
				this.receivedFromPluginTStamp,
				this.convertedProductionTStamp,
				Optional.ofNullable(timestamp),
				this.readFromBsdbTStamp,
				this.dependentsFullRuningIds,
				this.props);
	}
	
	/**
	 * Build a new IASValue with the passed time when received from BSDB
	 * 
	 * @param timestamp the timestamp
	 * @return The new IASValue with the updated timestamp
	 */
	public IASValue<T> updateReadFromBsdbTime(long timestamp) {
		return new IASValue<T>(
				this.value,
				this.mode,
				this.iasValidity,
				this.fullRunningId,
				this.valueType,
                this.readFromMonSysTStamp,
				this.productionTStamp,
				this.sentToConverterTStamp,
				this.receivedFromPluginTStamp,
				this.convertedProductionTStamp,
				this.sentToBsdbTStamp,
				Optional.ofNullable(timestamp),
				this.dependentsFullRuningIds,
				this.props);
	}

	@Override
	public String toString() {
		StringBuilder ret = new StringBuilder("IASValue: id=[");
		ret.append(id);
		ret.append("], runningID=");
		ret.append(fullRunningId);
		readFromMonSysTStamp.ifPresent(tStamp -> {
			ret.append(", read from Monitored System at ");
			ret.append(ISO8601Helper.getTimestamp(tStamp));
		});
		productionTStamp.ifPresent(tStamp -> {
			ret.append(", produced at ");
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
		if (valueType==IASTypes.TIMESTAMP) {
			ret.append(" (timestamp=");
			ret.append(ISO8601Helper.getTimestamp(Long.valueOf(value.toString())));
			ret.append(')');
		}
		
		dependentsFullRuningIds.ifPresent( ids -> {
			ret.append(", fullRunningIds of dependents=[");
			for (String s: ids) {
				ret.append(' ');
				ret.append(s);
			}
			ret.append(" ]");
		});
		
		
		props.ifPresent( properties -> {
			ret.append(", props=[");
			for (String key: properties.keySet()) {
				ret.append('(');
				ret.append(key);
				ret.append(',');
				ret.append(properties.get(key));
				ret.append(')');
			}
			ret.append(']');
		});
		
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
		return build(
				value,
				mode,
				iasValidity,
				fullRunningId,
				valueType,
				null,null,null,null,null,null,null,
				null,null);
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
     * @param readFromMonSysTStamp: the pont in time when the value has been read from the monitored system
	 * @param productionTStamp The point in time when the value has been produced by a plugin,
	 *                         a DASU or a core tool
	 * @param sentToConverterTStamp The point in time when the plugin sent the value to the converter
	 * @param receivedFromPluginTStamp The point in time when the converter received the value from the plugin
	 * @param convertedProductionTStamp The point in time when the converter generated
	 *                                  the value from the data structure rceived by the plugin
	 * @param sentToBsdbTStamp The point in time when the value has been sent to the BSDB
	 * @param readFromBsdbTStamp The point in time when the value has been read from the BSDB
	 * @param dependentsFullrunningIds The full running IDs of the dependent monitor points; can be <code>null</code>
	 * @param properties Additional properties (can be <code>null</code>)
	 * @return A IasValue initialized with the passed parameters
	 */
	public static <X> IASValue<?> build(
			X value,
			OperationalMode mode,
			IasValidity iasValidity,
			String fullRunningId,
			IASTypes valueType,
			Long readFromMonSysTStamp,
			Long productionTStamp,
			Long sentToConverterTStamp,
			Long receivedFromPluginTStamp,
			Long convertedProductionTStamp,
			Long sentToBsdbTStamp,
			Long readFromBsdbTStamp,
			Set<String> dependentsFullrunningIds,
			Map<String,String> properties) {
		Objects.requireNonNull(valueType);
		
		Optional<Set<String>> idsOpt = Optional.ofNullable(dependentsFullrunningIds);
		Optional<Set<String>> depIds = idsOpt.map(s -> Collections.unmodifiableSet(s));
		
		Optional<Map<String,String>> propsOpt = Optional.ofNullable(properties);
		Optional<Map<String,String>> theProps = propsOpt.map(p -> Collections.unmodifiableMap(p));
		
		
		switch (valueType) {
			case TIMESTAMP:
			case LONG: return new IASValue<Long>(
					(Long)value, 
					mode,
					iasValidity,
					fullRunningId,
					valueType,
                    Optional.ofNullable(readFromMonSysTStamp),
					Optional.ofNullable(productionTStamp),
					Optional.ofNullable(sentToConverterTStamp),
					Optional.ofNullable(receivedFromPluginTStamp),
					Optional.ofNullable(convertedProductionTStamp),
					Optional.ofNullable(sentToBsdbTStamp),
					Optional.ofNullable(readFromBsdbTStamp),
					depIds,
					theProps);
	 		case INT: return new IASValue<Integer>(
					(Integer)value, 
					mode,
					iasValidity,
					fullRunningId,
					valueType,
                    Optional.ofNullable(readFromMonSysTStamp),
					Optional.ofNullable(productionTStamp),
					Optional.ofNullable(sentToConverterTStamp),
					Optional.ofNullable(receivedFromPluginTStamp),
					Optional.ofNullable(convertedProductionTStamp),
					Optional.ofNullable(sentToBsdbTStamp),
					Optional.ofNullable(readFromBsdbTStamp),
					depIds,
					theProps);
			case SHORT: return new IASValue<Short>(
					(Short)value, 
					mode,
					iasValidity,
					fullRunningId,
					valueType,
                    Optional.ofNullable(readFromMonSysTStamp),
					Optional.ofNullable(productionTStamp),
					Optional.ofNullable(sentToConverterTStamp),
					Optional.ofNullable(receivedFromPluginTStamp),
					Optional.ofNullable(convertedProductionTStamp),
					Optional.ofNullable(sentToBsdbTStamp),
					Optional.ofNullable(readFromBsdbTStamp),
					depIds,
					theProps);
			case BYTE: return new IASValue<Byte>(
					(Byte)value, 
					mode,
					iasValidity,
					fullRunningId,
					valueType,
                    Optional.ofNullable(readFromMonSysTStamp),
					Optional.ofNullable(productionTStamp),
					Optional.ofNullable(sentToConverterTStamp),
					Optional.ofNullable(receivedFromPluginTStamp),
					Optional.ofNullable(convertedProductionTStamp),
					Optional.ofNullable(sentToBsdbTStamp),
					Optional.ofNullable(readFromBsdbTStamp),
					depIds,
					theProps);
			case DOUBLE: return new IASValue<Double>(
					(Double)value, 
					mode,
					iasValidity,
					fullRunningId,
					valueType,
                    Optional.ofNullable(readFromMonSysTStamp),
					Optional.ofNullable(productionTStamp),
					Optional.ofNullable(sentToConverterTStamp),
					Optional.ofNullable(receivedFromPluginTStamp),
					Optional.ofNullable(convertedProductionTStamp),
					Optional.ofNullable(sentToBsdbTStamp),
					Optional.ofNullable(readFromBsdbTStamp),
					depIds,
					theProps);
			case FLOAT: return new IASValue<Float>(
					(Float)value, 
					mode,
					iasValidity,
					fullRunningId,
					valueType,
                    Optional.ofNullable(readFromMonSysTStamp),
					Optional.ofNullable(productionTStamp),
					Optional.ofNullable(sentToConverterTStamp),
					Optional.ofNullable(receivedFromPluginTStamp),
					Optional.ofNullable(convertedProductionTStamp),
					Optional.ofNullable(sentToBsdbTStamp),
					Optional.ofNullable(readFromBsdbTStamp),
					depIds,
					theProps);
			case BOOLEAN: return new IASValue<Boolean>(
					(Boolean)value, 
					mode,
					iasValidity,
					fullRunningId,
					valueType,
                    Optional.ofNullable(readFromMonSysTStamp),
					Optional.ofNullable(productionTStamp),
					Optional.ofNullable(sentToConverterTStamp),
					Optional.ofNullable(receivedFromPluginTStamp),
					Optional.ofNullable(convertedProductionTStamp),
					Optional.ofNullable(sentToBsdbTStamp),
					Optional.ofNullable(readFromBsdbTStamp),
					depIds,
					theProps);
			case CHAR: return new IASValue<Character>(
					(Character)value, 
					mode,
					iasValidity,
					fullRunningId,
					valueType,
                    Optional.ofNullable(readFromMonSysTStamp),
					Optional.ofNullable(productionTStamp),
					Optional.ofNullable(sentToConverterTStamp),
					Optional.ofNullable(receivedFromPluginTStamp),
					Optional.ofNullable(convertedProductionTStamp),
					Optional.ofNullable(sentToBsdbTStamp),
					Optional.ofNullable(readFromBsdbTStamp),
					depIds,
					theProps);
			
			case STRING: return new IASValue<String>(
					(String)value, 
					mode,
					iasValidity,
					fullRunningId,
					valueType,
                    Optional.ofNullable(readFromMonSysTStamp),
					Optional.ofNullable(productionTStamp),
					Optional.ofNullable(sentToConverterTStamp),
					Optional.ofNullable(receivedFromPluginTStamp),
					Optional.ofNullable(convertedProductionTStamp),
					Optional.ofNullable(sentToBsdbTStamp),
					Optional.ofNullable(readFromBsdbTStamp),
					depIds,
					theProps);
			case ALARM: return new IASValue<Alarm>(
					(Alarm)value, 
					mode,
					iasValidity,
					fullRunningId,
					valueType,
                    Optional.ofNullable(readFromMonSysTStamp),
					Optional.ofNullable(productionTStamp),
					Optional.ofNullable(sentToConverterTStamp),
					Optional.ofNullable(receivedFromPluginTStamp),
					Optional.ofNullable(convertedProductionTStamp),
					Optional.ofNullable(sentToBsdbTStamp),
					Optional.ofNullable(readFromBsdbTStamp),
					depIds,
					theProps);
			case ARRAYOFLONGS:
			case ARRAYOFDOUBLES: return new IASValue<NumericArray>(
					(NumericArray)value,
					mode,
					iasValidity,
					fullRunningId,
					valueType,
                    Optional.ofNullable(readFromMonSysTStamp),
					Optional.ofNullable(productionTStamp),
					Optional.ofNullable(sentToConverterTStamp),
					Optional.ofNullable(receivedFromPluginTStamp),
					Optional.ofNullable(convertedProductionTStamp),
					Optional.ofNullable(sentToBsdbTStamp),
					Optional.ofNullable(readFromBsdbTStamp),
					depIds,
					theProps);
			default: throw new UnsupportedOperationException("Unsupported type "+valueType);
		}
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((convertedProductionTStamp == null) ? 0 : convertedProductionTStamp.hashCode());
		result = prime * result + ((productionTStamp == null) ? 0 : productionTStamp.hashCode());
		result = prime * result + ((dependentsFullRuningIds == null) ? 0 : dependentsFullRuningIds.hashCode());
		result = prime * result + ((fullRunningId == null) ? 0 : fullRunningId.hashCode());
		result = prime * result + ((iasValidity == null) ? 0 : iasValidity.hashCode());
		result = prime * result + ((id == null) ? 0 : id.hashCode());
		result = prime * result + ((mode == null) ? 0 : mode.hashCode());
        result = prime * result + ((readFromMonSysTStamp == null) ? 0 : readFromMonSysTStamp.hashCode());
		result = prime * result + ((props == null) ? 0 : props.hashCode());
		result = prime * result + ((readFromBsdbTStamp == null) ? 0 : readFromBsdbTStamp.hashCode());
		result = prime * result + ((receivedFromPluginTStamp == null) ? 0 : receivedFromPluginTStamp.hashCode());
		result = prime * result + ((sentToBsdbTStamp == null) ? 0 : sentToBsdbTStamp.hashCode());
		result = prime * result + ((sentToConverterTStamp == null) ? 0 : sentToConverterTStamp.hashCode());
		result = prime * result + ((value == null) ? 0 : value.hashCode());
		result = prime * result + ((valueType == null) ? 0 : valueType.hashCode());
		return result;
	}
	
	/**
	 * Get the value of the property with the passed key,
	 * if it exists.
	 * 
	 * @param key the key
	 * @return the value of the property or empty if a property with
	 *         the given key does not exist
	 */
	public Optional<String> getProperty(String key) {
		Objects.requireNonNull(key);
		return props.map( ps -> ps.get(key));
	}
	
	/**
	 * Build a new IASValue with a new key,value couple 
	 * in the map of properties
	 * 
	 * @param key The key
	 * @param value The value of the property
	 * @return 
	 */
	public IASValue<T> putProp(String key, String value) {
		Objects.requireNonNull(key);
		Objects.requireNonNull(value);
		if (key.isEmpty() || value.isEmpty()) {
			throw new IllegalArgumentException("Invalid key/value");
		}
		// If the key/value pair already exists return
		// this same object
		if (
				props.isPresent() && 
				props.get().containsKey(key) && 
				props.get().get(key).equals(value)) {
			return this;
		}
		// If the par key/value is not already in the map
		// we need to create a new map because the one in the
		// object is unmodifiable
		Map<String,String> properties = new HashMap<>();
		props.ifPresent( mProps -> {
			for (String k: mProps.keySet()) {
				properties.put(k, mProps.get(k));
			}
		});
		properties.put(key, value);
		return updateProperties(properties);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		IASValue<?> other = (IASValue<?>) obj;
		if (convertedProductionTStamp == null) {
			if (other.convertedProductionTStamp != null)
				return false;
		} else if (!convertedProductionTStamp.equals(other.convertedProductionTStamp))
			return false;
		if (productionTStamp == null) {
			if (other.productionTStamp != null)
				return false;
		} else if (!productionTStamp.equals(other.productionTStamp))
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
		if (readFromMonSysTStamp == null) {
			if (other.readFromMonSysTStamp != null)
				return false;
		} else if (!readFromMonSysTStamp.equals(other.readFromMonSysTStamp))
			return false;
		if (props == null) {
			if (other.props != null)
				return false;
		} else if (!props.equals(other.props))
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
