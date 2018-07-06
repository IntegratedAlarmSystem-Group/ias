package org.eso.ias.converter;

import java.text.ParseException;
import java.util.HashSet;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;

import org.eso.ias.converter.config.IasioConfigurationDAO;
import org.eso.ias.converter.config.MonitorPointConfiguration;
import org.eso.ias.plugin.publisher.MonitorPointData;
import org.eso.ias.plugin.publisher.PublisherException;
import org.eso.ias.types.Identifier;
import org.eso.ias.types.Alarm;
import org.eso.ias.types.IASTypes;
import org.eso.ias.types.IASValue;
import org.eso.ias.types.IasValidity;
import org.eso.ias.types.IasValueSerializerException;
import org.eso.ias.types.IasValueStringSerializer;
import org.eso.ias.types.IdentifierType;
import org.eso.ias.types.OperationalMode;
import org.eso.ias.utils.ISO8601Helper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import scala.Int;

/**
 * The function to convert a string received from a plugin
 * (i.e. a {@link MonitorPointData}) to a value to send
 * to the core of the AIS (i.e. a {@link IASValue}.
 * <P>
 * If something went wrong during the mapping, the exception is masked
 * and a log issued for investigation. 
 * In that case the received monitor point is lost. 
 * 
 * @author acaproni
 *
 */
public class ValueMapper implements Function<String, String> {
	
	/**
	 * The logger
	 */
	public static final Logger logger = LoggerFactory.getLogger(ValueMapper.class);
	
	/**
	 * The DAO to get the configuration of monitor points
	 */
	private IasioConfigurationDAO configDao;
	
	/**
	 * The serializer to transform IASValues into strings 
	 * to send to the core of the IAS
	 */
	private final IasValueStringSerializer iasValueStrSerializer;
	
	/**
	 * The identifier of the converter
	 */
	private final String converterID;
	
	/**
	 * A monitor point ptoduced by a plugin has no dependents
	 */
	private final Set<String> emptySetDependents = new HashSet<>();

	/**
	 * Constructor
	 * 
	 * @param configDao The DAO to get the configuration of monitor points
	 * @param iasValueStrSerializer The serializer to transform a IASValue in the proper string
	 * @param converterID The identifier of the converter
	 */
	public ValueMapper(
			IasioConfigurationDAO configDao, 
			IasValueStringSerializer iasValueStrSerializer,
			String converterID) {
		Objects.requireNonNull(configDao);
		this.configDao=configDao;
		Objects.requireNonNull(iasValueStrSerializer);
		this.iasValueStrSerializer=iasValueStrSerializer;
		Objects.requireNonNull(converterID);
		if (converterID.isEmpty()) {
			throw new IllegalArgumentException("Invalid empty ID");
		}
		this.converterID=converterID;
	}
	
	/**
	 * /**
	 * Build and return the identifier of the passed
	 * monitor point id.
	 * <P>
	 * The generation of the string, is delegated to the {@link Identifier}
	 * to ensure consistency along the system.
	 * 
	 * @param pluginId The ID of the plugin
	 * @param monitoredSystemId The ID of the monitored system
	 * @param iasioId The ID of the monitor point value or alarm
	 * @return the Identifier
	 */
	private Identifier  buildIdentifier(String pluginId, String monitoredSystemId, String iasioId) {
		Objects.requireNonNull(monitoredSystemId);
		Objects.requireNonNull(pluginId);
		Objects.requireNonNull(iasioId);
		if (monitoredSystemId.isEmpty() || pluginId.isEmpty() || iasioId.isEmpty()) {
			throw new IllegalArgumentException("Invalid empty identifier");
		}

		Identifier monSysIdent = new Identifier(monitoredSystemId,IdentifierType.MONITORED_SOFTWARE_SYSTEM);
		Identifier pluginIdent = new Identifier(pluginId,IdentifierType.PLUGIN,monSysIdent);
		Identifier converterIdent = new Identifier(converterID,IdentifierType.CONVERTER,pluginIdent);
		Identifier iasioIdent = new Identifier(iasioId,IdentifierType.IASIO,converterIdent);
		return iasioIdent;
	}
	
	/**
	 * Convert the {@link MonitorPointData} received by a plugin 
	 * to a {@link IASValue} to send to the core of the IAS
	 *
     * @param identifier: the Identifier of the monitor point
	 * @param remoteSystemData the monitor point to translate
	 * @param type the type of the monitor point
	 * @param receptionTStamp the timestamp when the monitor point data has been received
	 * @return the IASValue corresponding on the monitor point
	 */
	public IASValue<?> translate(
	        Identifier identifier,
			MonitorPointData remoteSystemData, 
			IASTypes type,
			long receptionTStamp,
			Optional<Integer> minTemplateIndex,
			Optional<Integer>maxTemplateIndex) {
	    Objects.requireNonNull(identifier);
		Objects.requireNonNull(type);
		Objects.requireNonNull(remoteSystemData);

		logger.debug("Translating {}",remoteSystemData.getId());
		
		// Convert the received string in the proper object type
		Object convertedValue=null;
		switch (type) {
		case LONG: {
			convertedValue=Long.parseLong(remoteSystemData.getValue());
			break;
		}
    	case INT: {
    		convertedValue=Integer.parseInt(remoteSystemData.getValue());
    		break;
    	}
    	case SHORT: {
    		convertedValue=Short.parseShort(remoteSystemData.getValue());
    		break;
    	}
    	case BYTE: {
    		convertedValue=Byte.parseByte(remoteSystemData.getValue());
    		break;
    	}
    	case DOUBLE: {
    		convertedValue=Double.parseDouble(remoteSystemData.getValue());
    		break;
    	}
    	case FLOAT: {
    		convertedValue=Float.parseFloat(remoteSystemData.getValue());
    		break;
    	}
    	case BOOLEAN: {
    		convertedValue=Boolean.parseBoolean(remoteSystemData.getValue());
    		break;
    	}
    	case CHAR: {
    		convertedValue=Character.valueOf(remoteSystemData.getValue().charAt(0));
    		break;
    	}
    	case STRING: {
    		convertedValue=remoteSystemData.getValue();
    		break;
    	}
    	case ALARM: {
    		convertedValue=Alarm.valueOf(remoteSystemData.getValue());
    		break;
    	}
		default: logger.error("Error translating {}",remoteSystemData.getId());
		}
		
		long pluginProductionTime;
		pluginProductionTime=ISO8601Helper.timestampToMillis(remoteSystemData.getSampleTime());
		
		long pluginSentToConvertTime;
		pluginSentToConvertTime=ISO8601Helper.timestampToMillis(remoteSystemData.getPublishTime());
		


		// If templated check the index for consistency
		// and if it is the case discard the value
		if (identifier.templateInstance().isDefined()) {
            Integer index = (Integer)identifier.templateInstance().get();
			if (!minTemplateIndex.isPresent()) {
				logger.error("IASIO {} appears template with instance {} but is not templated in CDB: value lost!",
						remoteSystemData.getId(), index);
			}

			if (index<minTemplateIndex.get() || index>maxTemplateIndex.get()) {
				logger.error("Template instance {} of {} outside of allowed range [{},{}]: value lost",
						remoteSystemData.getId(), index,minTemplateIndex.get(),maxTemplateIndex);
				return null;
			}
		} else if (minTemplateIndex.isPresent()) {
			logger.error("IASIO {} is not templated but should be with index in [{},{}]: value lost!",
					remoteSystemData.getId(),
					minTemplateIndex.get(),
					maxTemplateIndex.get());
		}
		
		// The value is converted to a IASValue and immeaitely sent to the BSDB
		// by the streaming so the 2 timestamps (production and sent to BSDB)
		// are the same point in time with this implementation
		long producedAndSentTStamp = System.currentTimeMillis();

		IASValue<?> ret = IASValue.build(
				convertedValue, 
				OperationalMode.valueOf(remoteSystemData.getOperationalMode()),
				IasValidity.valueOf(remoteSystemData.getValidity()),
				identifier.fullRunningID(),
				type,
				pluginProductionTime, // PLUGIN production
				pluginSentToConvertTime,  // Sent to converter
				receptionTStamp, // received from plugin
				producedAndSentTStamp, // Produced by converter
				producedAndSentTStamp, // Sent to BSDB
				null, // Read from BSDB
				null, // DASU prod time
				null, // dependents
				null); // additional properties
		logger.debug("Translated to {} of type {}",ret.id,ret.valueType);
		return ret;
	}

    /**
     * Get the id taking template into account
     *
     * @param identifier The identifier
     * @return the ID cleaned of the template index
     */
	public String getId(Identifier identifier) {
        // If templated removes the index
        if (identifier.templateInstance().isDefined()) {
            Integer index = (Integer)identifier.templateInstance().get();
            int pos = identifier.id().indexOf(Identifier.templatedIdPrefix());
            return identifier.id().substring(0,pos);
        } else {
            return identifier.id();
        }
    }

	/**
	 * Effectively translate the string received from the plugin in the string
	 * to send to the core of the IAS
	 *  
	 * @param mpString The string received from the plugin
	 * @see java.util.function.Function#apply(java.lang.Object)
	 */
	@Override
	public String apply(String mpString) {
		if (mpString==null || mpString.isEmpty()) {
			logger.warn("Received null or empty string from plugin: nothing to convert");
			return null;
		}
		
		long receptionTimestamp = System.currentTimeMillis();
		
		MonitorPointData mpd;
		try {
			mpd=MonitorPointData.fromJsonString(mpString);
		} catch (PublisherException pe) {
			logger.error("Cannot parse [{}] to a MonitorPointData: vale lost!",mpString,pe);
			return null;
		}

        Identifier mPointIdentifier = buildIdentifier(
                mpd.getPluginID(),
                mpd.getMonitoredSystemID(),
                mpd.getId());
		
		// Get the configuration from the CDB
        // taking templates into account
		String mpId;
        try {
		    mpId=getId(mPointIdentifier);
            logger.debug("Will get the configuration of a monitor point of id {}", mpId);
        } catch (Exception e) {
		    logger.error("Error extracting the id of {}. Soemthing wrong with templates?",mPointIdentifier.id());
		    return null;
        }


		MonitorPointConfiguration mpConfiguration=configDao.getConfiguration(mpId);
		if (mpConfiguration==null) {
			logger.error("No configuration found for {}: raw value lost!",mpd.getId());
			return null;
		}
		IASTypes iasType = mpConfiguration.mpType;

		Optional<IASValue<?>> iasValueOpt;
		try { 
			iasValueOpt= Optional.ofNullable(
			        translate(
                        mPointIdentifier,
                        mpd,iasType,
                        receptionTimestamp,
                        mpConfiguration.minTemplateIndex,
                        mpConfiguration.maxTemplateIndex));
		} catch (Exception cfe) {
			logger.error("Error converting {} to a core value type: value lost!",mpd.getId());
			return null;
		}
		try {
			return (iasValueOpt.isPresent())? iasValueStrSerializer.iasValueToString(iasValueOpt.get()):null;
		} catch (IasValueSerializerException ivse) {
			iasValueOpt.ifPresent(iasValue ->
                    logger.error("Error converting {} to a string to send to the core: value lost!",iasValue.toString()));
			return null;
		}
	}

}
