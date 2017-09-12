/**
 * 
 */
package org.eso.ias.converter.translation;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Objects;

import org.eso.ias.plugin.AlarmSample;
import org.eso.ias.plugin.OperationalMode;
import org.eso.ias.plugin.publisher.MonitorPointData;
import org.eso.ias.prototype.input.Identifier;
import org.eso.ias.prototype.input.java.IASTypes;
import org.eso.ias.prototype.input.java.IASValue;
import org.eso.ias.prototype.input.java.IdentifierType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The engine to convert monitor point values and alarms received from
 * remote monitored systems into IASIO data types for the core of the IAS.
 * 
 * @author acaproni
 *
 */
public class ConverterEngineImpl implements ConverterEngine {
	
	/**
	 * The logger
	 */
	private final static Logger logger = LoggerFactory.getLogger(ConverterEngineImpl.class);
	
	/**
	 * ISO 8601 date formatter
	 */
	protected final SimpleDateFormat iso8601dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.S");
	
	/**
	 * The ID of this converter. 
	 */
	public final String converterID;

	/**
	 * Constructor 
	 */
	public ConverterEngineImpl(String converterID) {
		Objects.requireNonNull(converterID);
		if (converterID.isEmpty()) {
			throw new IllegalArgumentException("The ID of the converter can't be empty");
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
		Identifier converterIdent = new Identifier(converterId,IdentifierType.CONVERTER,plugId);
		Identifier iasioIdent = new Identifier(iasioId,IdentifierType.IASIO,converterIdent);
		return iasioIdent.fullRunningID();
	}

	/**
	 * @see org.eso.ias.converter.translation.ConverterEngine#translate(org.eso.ias.plugin.publisher.MonitorPointData, org.eso.ias.prototype.input.java.IASTypes)
	 */
	@Override
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

}
