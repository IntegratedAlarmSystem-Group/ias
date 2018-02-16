package org.eso.ias.basictypes.test;

import org.eso.ias.types.AlarmSample;
import org.eso.ias.types.IASTypes;
import org.eso.ias.types.IASValue;
import org.eso.ias.types.IasValidity;
import org.eso.ias.types.IasValueJsonSerializer;
import org.eso.ias.types.Identifier;
import org.eso.ias.types.IdentifierType;
import org.eso.ias.types.OperationalMode;
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
		IASValue<?> intIasType = IASValue.build(
				1200,
				OperationalMode.CLOSING,
				IasValidity.RELIABLE,
				new Identifier(intId, IdentifierType.IASIO, convIdentifier).fullRunningID(),
				IASTypes.INT);
				
		String jsonStr = jsonSerializer.iasValueToString(intIasType);
		
		IASValue<?> intFromSerializer = jsonSerializer.valueOf(jsonStr);
		assertNotNull(intFromSerializer);
		assertEquals(intIasType,intFromSerializer);
		
		String shortId = "ShortType-ID";
		IASValue<?> shortIasType = IASValue.build(
			(short)120, 
			OperationalMode.INITIALIZATION, 
			IasValidity.RELIABLE,
			new Identifier(shortId, IdentifierType.IASIO, convIdentifier).fullRunningID(),
			IASTypes.SHORT);
		jsonStr = jsonSerializer.iasValueToString(shortIasType);
		
		IASValue<?> shortFromSerializer = jsonSerializer.valueOf(jsonStr);
		assertNotNull(shortFromSerializer);
		assertEquals(shortIasType,shortFromSerializer);
		
		String byteId = "ByteType-ID";
		IASValue<?> byteIasType = IASValue.build(
			(byte)90, 
			OperationalMode.INITIALIZATION, 
			IasValidity.UNRELIABLE,
			new Identifier(byteId, IdentifierType.IASIO, convIdentifier).fullRunningID(),
			IASTypes.BYTE);
		jsonStr = jsonSerializer.iasValueToString(byteIasType);
		
		IASValue<?> byteFromSerializer = jsonSerializer.valueOf(jsonStr);
		assertNotNull(byteFromSerializer);
		assertEquals(byteIasType,byteFromSerializer);
		
		String doubleId = "ByteType-ID";
		IASValue<?> doubleIasType = IASValue.build(
			Double.valueOf(123456789.4321), 
			OperationalMode.INITIALIZATION, 
			IasValidity.UNRELIABLE,
			new Identifier(doubleId, IdentifierType.IASIO, convIdentifier).fullRunningID(),
			IASTypes.DOUBLE);
		jsonStr = jsonSerializer.iasValueToString(doubleIasType);
		
		IASValue<?> doubleFromSerializer = jsonSerializer.valueOf(jsonStr);
		assertNotNull(doubleFromSerializer);
		assertEquals(doubleIasType,doubleFromSerializer);
		
		String floatId = "ByteType-ID";
		IASValue<?> floatIasType = IASValue.build(
			670811.81167f, 
			OperationalMode.SHUTTEDDOWN, 
			IasValidity.RELIABLE,
			new Identifier(floatId, IdentifierType.IASIO, convIdentifier).fullRunningID(),
			IASTypes.STRING);
		jsonStr = jsonSerializer.iasValueToString(floatIasType);
		
		IASValue<?> floatFromSerializer = jsonSerializer.valueOf(jsonStr);
		assertNotNull(floatFromSerializer);
		assertEquals(floatIasType,floatFromSerializer);
		
		String alarmId = "AlarmType-ID";
		IASValue<?> alarm = IASValue.build(
			AlarmSample.SET,
			OperationalMode.DEGRADED,
			IasValidity.RELIABLE,
			new Identifier(alarmId, IdentifierType.IASIO, convIdentifier).fullRunningID(),
			IASTypes.ALARM);
		
		jsonStr = jsonSerializer.iasValueToString(alarm);
		
		IASValue<?> alarmFromSerializer = jsonSerializer.valueOf(jsonStr);
		assertNotNull(alarmFromSerializer);
		assertEquals(alarm,alarmFromSerializer);
		
		String boolId = "BooleanType-ID";
		IASValue<?> bool = IASValue.build(
			false,
			OperationalMode.OPERATIONAL,
			IasValidity.RELIABLE,
			new Identifier(boolId, IdentifierType.IASIO, convIdentifier).fullRunningID(),
			IASTypes.BOOLEAN);
		
		jsonStr = jsonSerializer.iasValueToString(bool);
		
		IASValue<?> boolFromSerializer = jsonSerializer.valueOf(jsonStr);
		assertNotNull(boolFromSerializer);
		assertEquals(bool,boolFromSerializer);
		
		String charId = "CharType-ID";
		IASValue<?> character = IASValue.build(
			'a',
			OperationalMode.MAINTENANCE,
			IasValidity.UNRELIABLE,
			new Identifier(charId, IdentifierType.IASIO, convIdentifier).fullRunningID(),
			IASTypes.CHAR);
		
		jsonStr = jsonSerializer.iasValueToString(character);
		
		IASValue<?> charFromSerializer = jsonSerializer.valueOf(jsonStr);
		assertNotNull(charFromSerializer);
		assertEquals(character,charFromSerializer);
		
		String strId = "StringType-ID";
		IASValue<?>  str = IASValue.build(
			"Test-str",
			OperationalMode.UNKNOWN,
			IasValidity.UNRELIABLE,
			new Identifier(strId, IdentifierType.IASIO, convIdentifier).fullRunningID(),
			IASTypes.STRING);
		
		jsonStr = jsonSerializer.iasValueToString(str);
		
		IASValue<?> strFromSerializer = jsonSerializer.valueOf(jsonStr);
		assertNotNull(strFromSerializer);
		assertEquals(str,strFromSerializer);
		
		String longId = "IntType-ID";
		IASValue<?> longIasType = IASValue.build(
			1200L, 
			OperationalMode.CLOSING, 
			IasValidity.RELIABLE,
			new Identifier(longId, IdentifierType.IASIO, convIdentifier).fullRunningID(),
			IASTypes.LONG);
		jsonStr = jsonSerializer.iasValueToString(longIasType);
		
		IASValue<?> longFromSerializer = jsonSerializer.valueOf(jsonStr);
		assertNotNull(longFromSerializer);
		assertEquals(longIasType,longFromSerializer);
	}
	
	@Test
	public void testConversionJStringToValue() {
		
	}
}
