package org.eso.ias.converter;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Objects;
import java.util.function.Function;

import org.eso.ias.converter.config.IasioConfigurationDAO;
import org.eso.ias.converter.config.MonitorPointConfiguration;
import org.eso.ias.plugin.AlarmSample;
import org.eso.ias.plugin.OperationalMode;
import org.eso.ias.plugin.publisher.MonitorPointData;
import org.eso.ias.plugin.publisher.PublisherException;
import org.eso.ias.prototype.input.Identifier;
import org.eso.ias.prototype.input.java.IASTypes;
import org.eso.ias.prototype.input.java.IASValue;
import org.eso.ias.prototype.input.java.IasValueSerializerException;
import org.eso.ias.prototype.input.java.IasValueStringSerializer;
import org.eso.ias.prototype.input.java.IdentifierType;
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
	 * ISO 8601 date formatter
	 */
	private final SimpleDateFormat iso8601dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.S");

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
		
		Identifier monSystemId = new Identifier(monitoredSystemId,IdentifierType.MONITORED_SOFTWARE_SYSTEM,null);
		Identifier plugId = new Identifier(pluginId,IdentifierType.PLUGIN,monSystemId);
		Identifier converterIdent = new Identifier(converterID,IdentifierType.CONVERTER,plugId);
		Identifier iasioIdent = new Identifier(iasioId,IdentifierType.IASIO,converterIdent);
		return iasioIdent.fullRunningID();
	}
	
	/**
	 * Convert the {@link MonitorPointData} received by a plugin 
	 * to a {@link IASValue} to send to the core of the IAS
	 * 
	 */
	public IASValue<?> translate(MonitorPointData remoteSystemData, IASTypes type) {
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
    		convertedValue=AlarmSample.valueOf(remoteSystemData.getValue());
    		break;
    	}
		default: throw new UnsupportedOperationException("Unsupported type "+type);
		}
		
		long tStamp;
		try { 
			tStamp=iso8601dateFormat.parse(remoteSystemData.getSampleTime()).getTime();
		} catch (ParseException pe) {
			logger.error("Cannot parse the IASO 8601 timestamp {}: using actual time instad",remoteSystemData.getSampleTime());
			tStamp=System.currentTimeMillis();
		}
		
		String fullrunId = buildFullRunningId(
				converterID,
				remoteSystemData.getPluginID(),
				remoteSystemData.getMonitoredSystemID(),
				remoteSystemData.getId());
		
		return IASValue.buildIasValue(
				convertedValue, 
				tStamp, 
				OperationalMode.valueOf(remoteSystemData.getOperationalMode()), 
				remoteSystemData.getId(), 
				fullrunId,
				type);
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
			return null;
		}
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
			iasValue= translate(mpd,iasType);
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
