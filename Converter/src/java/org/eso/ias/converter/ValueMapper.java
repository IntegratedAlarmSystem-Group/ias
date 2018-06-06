package org.eso.ias.converter;

import java.text.ParseException;
import java.util.HashSet;
import java.util.Objects;
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
	 * Build and return the running id of the passed
	 * monitor point id.
	 * <P>
	 * The generation of the string, is delegated to the {@link Identifier}
	 * to ensure consistency along the system.
	 * 
	 * @param converterId The ID of the converter
	 * @param pluginId The ID of the plugin
	 * @param monitoredSystemId The ID of the monitored system
	 * @param iasioId The ID of the monitor point value or alarm
	 * @return
	 */
	private String  buildFullRunningId(String converterId, String pluginId, String monitoredSystemId, String iasioId) {
		Objects.requireNonNull(monitoredSystemId);
		Objects.requireNonNull(pluginId);
		Objects.requireNonNull(converterId);
		Objects.requireNonNull(iasioId);
		
		Identifier monSystemId = new Identifier(monitoredSystemId,IdentifierType.MONITORED_SOFTWARE_SYSTEM);
		Identifier plugId = new Identifier(pluginId,IdentifierType.PLUGIN,monSystemId);
		Identifier converterIdent = new Identifier(converterID,IdentifierType.CONVERTER,plugId);
		Identifier iasioIdent = new Identifier(iasioId,IdentifierType.IASIO,converterIdent);
		return iasioIdent.fullRunningID();
	}
	
	/**
	 * Convert the {@link MonitorPointData} received by a plugin 
	 * to a {@link IASValue} to send to the core of the IAS
	 * 
	 * @param remoteSystemData the monitor point to translate
	 * @param type the type of the monitor point
	 * @param receptionTStamp the timestamp when the monitor point data has been received
	 * @return the IASValue corresponding on the monitor point
	 */
	public IASValue<?> translate(
			MonitorPointData remoteSystemData, 
			IASTypes type,
			long receptionTStamp) {
		Objects.requireNonNull(type);
		Objects.requireNonNull(remoteSystemData);
		
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
		default: throw new UnsupportedOperationException("Unsupported type "+type);
		}
		
		long pluginProductionTime;
		pluginProductionTime=ISO8601Helper.timestampToMillis(remoteSystemData.getSampleTime());
		
		long pluginSentToConvertTime;
		pluginSentToConvertTime=ISO8601Helper.timestampToMillis(remoteSystemData.getPublishTime());
		
		String fullrunId = buildFullRunningId(
				converterID,
				remoteSystemData.getPluginID(),
				remoteSystemData.getMonitoredSystemID(),
				remoteSystemData.getId());
		
		// The value is converted to a IASValue and immeaitely sent to the BSDB
		// by the streaming so the 2 timestamps (production and sent to BSDB)
		// are the same point in time with thisimplementation
		long producedAndSentTStamp = System.currentTimeMillis();
		
		return IASValue.build(
				convertedValue, 
				OperationalMode.valueOf(remoteSystemData.getOperationalMode()),
				IasValidity.valueOf(remoteSystemData.getValidity()),
				fullrunId,
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
		
		// Get the configuration from the CDB
		String mpId = mpd.getId();
		MonitorPointConfiguration mpConfiguration=configDao.getConfiguration(mpId);
		if (mpConfiguration==null) {
			logger.error("No configuration found for {}: raw value lost!",mpId);
			return null;
		}
		IASTypes iasType = mpConfiguration.mpType;
		
		IASValue<?> iasValue;
		try { 
			iasValue= translate(mpd,iasType,receptionTimestamp);
		} catch (Exception cfe) {
			logger.error("Error converting {} to a core value type: value lost!",mpd.getId());
			return null;
		}
		try {
			return iasValueStrSerializer.iasValueToString(iasValue);
		} catch (IasValueSerializerException ivse) {
			logger.error("Error converting {} to a string to send to the core: value lost!",iasValue.toString());
			return null;
		}
		
		
	}

}
