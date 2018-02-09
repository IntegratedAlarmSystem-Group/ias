package org.eso.ias.basictypes.test;

import org.eso.ias.prototype.input.java.AlarmSample;
import org.eso.ias.prototype.input.java.OperationalMode;
import org.eso.ias.prototype.input.Identifier;
import org.eso.ias.prototype.input.java.IASValue;
import org.eso.ias.prototype.input.java.IasAlarm;
import org.eso.ias.prototype.input.java.IasBool;
import org.eso.ias.prototype.input.java.IasByte;
import org.eso.ias.prototype.input.java.IasChar;
import org.eso.ias.prototype.input.java.IasDouble;
import org.eso.ias.prototype.input.java.IasFloat;
import org.eso.ias.prototype.input.java.IasInt;
import org.eso.ias.prototype.input.java.IasLong;
import org.eso.ias.prototype.input.java.IasShort;
import org.eso.ias.prototype.input.java.IasString;
import org.eso.ias.prototype.input.java.IasValidity;
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
	private final Identifier mSysIdentifier = new Identifier(monSysID, IdentifierType.MONITORED_SOFTWARE_SYSTEM);
	
	/**
	 * The ID of the plugin
	 */
	private final String pluginID = "plugin-ID";
	
	/**
	 * The identifier of the plugin.
	 */
	private final Identifier plIdentifier = new Identifier(pluginID, IdentifierType.PLUGIN, mSysIdentifier);
	
	/**
	 * The id of the converter.
	 */
	private final String converterID="Converter-ID";
	
	/**
	 * The identifier of the converter.
	 */
	private final Identifier convIdentifier = new Identifier(converterID, IdentifierType.CONVERTER, plIdentifier);
	
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
			IasValidity.RELIABLE,
			new Identifier(intId, IdentifierType.IASIO, convIdentifier).fullRunningID());
		String jsonStr = jsonSerializer.iasValueToString(intIasType);
		
		IasInt intFromSerializer = (IasInt)jsonSerializer.valueOf(jsonStr);
		assertNotNull(intFromSerializer);
		assertEquals(intIasType,intFromSerializer);
		
		String shortId = "ShortType-ID";
		IasShort shortIasType = new IasShort(
			(short)120, 
			1100, 
			OperationalMode.INITIALIZATION, 
			IasValidity.RELIABLE,
			new Identifier(shortId, IdentifierType.IASIO, convIdentifier).fullRunningID());
		jsonStr = jsonSerializer.iasValueToString(shortIasType);
		
		IasShort shortFromSerializer = (IasShort)jsonSerializer.valueOf(jsonStr);
		assertNotNull(shortFromSerializer);
		assertEquals(shortIasType,shortFromSerializer);
		
		String byteId = "ByteType-ID";
		IasByte byteIasType = new IasByte(
			(byte)90, 
			1200, 
			OperationalMode.INITIALIZATION, 
			IasValidity.UNRELIABLE,
			new Identifier(byteId, IdentifierType.IASIO, convIdentifier).fullRunningID());
		jsonStr = jsonSerializer.iasValueToString(byteIasType);
		
		IasByte byteFromSerializer = (IasByte)jsonSerializer.valueOf(jsonStr);
		assertNotNull(byteFromSerializer);
		assertEquals(byteIasType,byteFromSerializer);
		
		String doubleId = "ByteType-ID";
		IasDouble doubleIasType = new IasDouble(
			Double.valueOf(123456789.4321), 
			1300, 
			OperationalMode.INITIALIZATION, 
			IasValidity.UNRELIABLE,
			new Identifier(doubleId, IdentifierType.IASIO, convIdentifier).fullRunningID());
		jsonStr = jsonSerializer.iasValueToString(doubleIasType);
		
		IasDouble doubleFromSerializer = (IasDouble)jsonSerializer.valueOf(jsonStr);
		assertNotNull(doubleFromSerializer);
		assertEquals(doubleIasType,doubleFromSerializer);
		
		String floatId = "ByteType-ID";
		IasFloat floatIasType = new IasFloat(
			670811.81167f, 
			1400, 
			OperationalMode.SHUTTEDDOWN, 
			IasValidity.RELIABLE,
			new Identifier(floatId, IdentifierType.IASIO, convIdentifier).fullRunningID());
		jsonStr = jsonSerializer.iasValueToString(floatIasType);
		
		IasFloat floatFromSerializer = (IasFloat)jsonSerializer.valueOf(jsonStr);
		assertNotNull(floatFromSerializer);
		assertEquals(floatIasType,floatFromSerializer);
		
		String alarmId = "AlarmType-ID";
		IasAlarm alarm = new IasAlarm(
			AlarmSample.SET,
			1500,
			OperationalMode.DEGRADED,
			IasValidity.RELIABLE,
			new Identifier(alarmId, IdentifierType.IASIO, convIdentifier).fullRunningID());
		
		jsonStr = jsonSerializer.iasValueToString(alarm);
		
		IasAlarm alarmFromSerializer = (IasAlarm)jsonSerializer.valueOf(jsonStr);
		assertNotNull(alarmFromSerializer);
		assertEquals(alarm,alarmFromSerializer);
		
		String boolId = "BooleanType-ID";
		IasBool bool = new IasBool(
			false,
			1600,
			OperationalMode.OPERATIONAL,
			IasValidity.RELIABLE,
			new Identifier(boolId, IdentifierType.IASIO, convIdentifier).fullRunningID());
		
		jsonStr = jsonSerializer.iasValueToString(bool);
		
		IasBool boolFromSerializer = (IasBool)jsonSerializer.valueOf(jsonStr);
		assertNotNull(boolFromSerializer);
		assertEquals(bool,boolFromSerializer);
		
		String charId = "CharType-ID";
		IasChar character = new IasChar(
			'a',
			1700,
			OperationalMode.MAINTENANCE,
			IasValidity.UNRELIABLE,
			new Identifier(charId, IdentifierType.IASIO, convIdentifier).fullRunningID());
		
		jsonStr = jsonSerializer.iasValueToString(character);
		
		IasChar charFromSerializer = (IasChar)jsonSerializer.valueOf(jsonStr);
		assertNotNull(charFromSerializer);
		assertEquals(character,charFromSerializer);
		
		String strId = "StringType-ID";
		IasString  str = new IasString(
			"Test-str",
			1800,
			OperationalMode.UNKNOWN,
			IasValidity.UNRELIABLE,
			new Identifier(strId, IdentifierType.IASIO, convIdentifier).fullRunningID());
		
		jsonStr = jsonSerializer.iasValueToString(str);
		
		IasString strFromSerializer = (IasString)jsonSerializer.valueOf(jsonStr);
		assertNotNull(strFromSerializer);
		assertEquals(str,strFromSerializer);
		
		String longId = "IntType-ID";
		IasLong longIasType = new IasLong(
			1200L, 
			1900, 
			OperationalMode.CLOSING, 
			IasValidity.RELIABLE,
			new Identifier(longId, IdentifierType.IASIO, convIdentifier).fullRunningID());
		jsonStr = jsonSerializer.iasValueToString(longIasType);
		
		IasLong longFromSerializer = (IasLong)jsonSerializer.valueOf(jsonStr);
		assertNotNull(longFromSerializer);
		assertEquals(longIasType,longFromSerializer);
	}
	
	@Test
	public void testConversionJStringToValue() {
		
	}
}
