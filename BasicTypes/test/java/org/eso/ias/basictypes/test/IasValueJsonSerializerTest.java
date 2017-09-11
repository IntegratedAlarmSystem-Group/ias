package org.eso.ias.basictypes.test;

import org.eso.ias.plugin.AlarmSample;
import org.eso.ias.plugin.OperationalMode;
import org.eso.ias.prototype.input.Identifier;
import org.eso.ias.prototype.input.java.IASValue;
import org.eso.ias.prototype.input.java.IasAlarm;
import org.eso.ias.prototype.input.java.IasInt;
import org.eso.ias.prototype.input.java.IasValueJsonSerializer;
import org.eso.ias.prototype.input.java.IdentifierType;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

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
	
	@Test
	public void testConversionValueToJString() throws Exception {
		
		String intId = "IntType-ID";
		IasInt intIasType = new IasInt(
			1200, 
			1000, 
			OperationalMode.CLOSING, 
			intId, 
			new Identifier(intId, IdentifierType.IASIO, convIdentifier).fullRunningID());
		String jsonStr = jsonSerializer.iasValueToString(intIasType);
		
		IasInt intFromSerializer = (IasInt)jsonSerializer.valueOf(jsonStr);
		assertNotNull(intFromSerializer);
		assertEquals(intIasType,intFromSerializer);
		
		String alarmId = "AlarmType-ID";
		IasAlarm alarm = new IasAlarm(
			AlarmSample.SET,
			1100,
			OperationalMode.DEGRADED,
			alarmId,
			new Identifier(alarmId, IdentifierType.IASIO, convIdentifier).fullRunningID());
		
		jsonStr = jsonSerializer.iasValueToString(alarm);
		
		IasAlarm alarmFromSerializer = (IasAlarm)jsonSerializer.valueOf(jsonStr);
		assertNotNull(alarmFromSerializer);
		
	}
	
	@Test
	public void testConversionJStringToValue() {
		
	}
}
