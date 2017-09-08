package org.eso.ias.basictypes.test;

import org.eso.ias.plugin.AlarmSample;
import org.eso.ias.plugin.OperationalMode;
import org.eso.ias.prototype.input.Identifier;
import org.eso.ias.prototype.input.java.IASValue;
import org.eso.ias.prototype.input.java.IasAlarm;
import org.eso.ias.prototype.input.java.IasValueJsonSerializer;
import org.eso.ias.prototype.input.java.IdentifierType;
import org.junit.Test;

/**
 * Test the JSON serialization of {@link IASValue}
 * 
 * @author acaproni
 */
public class IasValueJsonSerializerTest {
	
	/**
	 * The id of the system monitored by the plugin.
	 */
	private final String monSysID="Monitored-System-ID";
	
	/**
	 * The identifier of the system monitored by the plugin.
	 */
	Identifier mSysIdentifier = new Identifier(monSysID, IdentifierType.MONITORED_SOFTWARE_SYSTEM, null);
	
	/**
	 * The ID of the plugin
	 */
	private final String pluginID = "plugin-ID";
	
	/**
	 * The identifier of the plugin.
	 */
	Identifier plIdentifier = new Identifier(pluginID, IdentifierType.PLUGIN, mSysIdentifier);
	
	/**
	 * The id of the converter.
	 */
	private final String converterID="Converter-ID";
	
	/**
	 * The identifier of the converter.
	 */
	Identifier convIdentifier = new Identifier(converterID, IdentifierType.CONVERTER, plIdentifier);
	
	
	
	/**
	 * The object to test
	 */
	private final IasValueJsonSerializer jsonSerializer = new IasValueJsonSerializer();
	
	

	public IasValueJsonSerializerTest() {
		// TODO Auto-generated constructor stub
	}

	@Test
	public void testConversionValueToJString() throws Exception {
		String alarmId = "AlarmType-ID";
		IasAlarm alarm = new IasAlarm(
			AlarmSample.SET,
			1000,
			OperationalMode.DEGRADED,
			alarmId,
			new Identifier(alarmId, IdentifierType.IASIO, convIdentifier).fullRunningID());
		
		String jsonStr = jsonSerializer.iasValueToString(alarm);
		System.out.println(jsonStr);
		
		IasAlarm alarmFromSerializer = (IasAlarm)jsonSerializer.valueOf(jsonStr);
		
		
	}
	
	@Test
	public void testConversionJStringToValue() {
		
	}
}
