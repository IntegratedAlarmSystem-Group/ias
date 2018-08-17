package org.eso.ias.basictypes.test;

import org.eso.ias.types.Alarm;
import org.eso.ias.types.IASTypes;
import org.eso.ias.types.IASValue;
import org.eso.ias.types.IasValidity;
import org.eso.ias.types.IasValueJsonSerializer;
import org.eso.ias.types.Identifier;
import org.eso.ias.types.IdentifierType;
import org.eso.ias.types.OperationalMode;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Test the JSON serialization of {@link IASValue}
 * 
 * @author acaproni
 */
public class IasValueJsonSerializerTest {
	
	/**
	 * The id of the system monitored by the plugin.
	 */
	private final String supervID ="Supervisor-ID";
	
	/**
	 * The identifier of the system monitored by the plugin.
	 */
	private final Identifier supervIdentifier = new Identifier(supervID, IdentifierType.SUPERVISOR);
	
	/**
	 * The ID of the DASU
	 */
	private final String dasuID = "dasu-ID";
	
	/**
	 * The identifier of the DASU
	 */
	private final Identifier dasuIdentifier = new Identifier(dasuID, IdentifierType.DASU, supervIdentifier);
	
	/**
	 * The id of the ASCE
	 */
	private final String asceID ="asce-ID";
	
	/**
	 * The identifier of the converter.
	 */
	private final Identifier asceIdentifier = new Identifier(asceID, IdentifierType.ASCE, dasuIdentifier);
	
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
				new Identifier(intId, IdentifierType.IASIO, asceIdentifier).fullRunningID(),
				IASTypes.INT);
				
		String jsonStr = jsonSerializer.iasValueToString(intIasType);
		
		IASValue<?> intFromSerializer = jsonSerializer.valueOf(jsonStr);
		assertNotNull(intFromSerializer,"Got a null de-serializing ["+jsonStr+"]");
		assertEquals(intIasType,intFromSerializer);
		
		String shortId = "ShortType-ID";
		IASValue<?> shortIasType = IASValue.build(
			(short)120, 
			OperationalMode.INITIALIZATION, 
			IasValidity.RELIABLE,
			new Identifier(shortId, IdentifierType.IASIO, asceIdentifier).fullRunningID(),
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
			new Identifier(byteId, IdentifierType.IASIO, asceIdentifier).fullRunningID(),
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
			new Identifier(doubleId, IdentifierType.IASIO, asceIdentifier).fullRunningID(),
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
			new Identifier(floatId, IdentifierType.IASIO, asceIdentifier).fullRunningID(),
			IASTypes.FLOAT);
		jsonStr = jsonSerializer.iasValueToString(floatIasType);
		
		IASValue<?> floatFromSerializer = jsonSerializer.valueOf(jsonStr);
		assertNotNull(floatFromSerializer);
		assertEquals(floatIasType,floatFromSerializer);
		
		String alarmId = "AlarmType-ID";
		IASValue<?> alarm = IASValue.build(
			Alarm.SET_MEDIUM,
			OperationalMode.DEGRADED,
			IasValidity.RELIABLE,
			new Identifier(alarmId, IdentifierType.IASIO, asceIdentifier).fullRunningID(),
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
			new Identifier(boolId, IdentifierType.IASIO, asceIdentifier).fullRunningID(),
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
			new Identifier(charId, IdentifierType.IASIO, asceIdentifier).fullRunningID(),
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
			new Identifier(strId, IdentifierType.IASIO, asceIdentifier).fullRunningID(),
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
			new Identifier(longId, IdentifierType.IASIO, asceIdentifier).fullRunningID(),
			IASTypes.LONG);
		jsonStr = jsonSerializer.iasValueToString(longIasType);
		
		IASValue<?> longFromSerializer = jsonSerializer.valueOf(jsonStr);
		assertNotNull(longFromSerializer);
		assertEquals(longIasType,longFromSerializer);
	}
	
	/**
	 * Ensure that timestamps are properly serialized/deserialized
	 */
	@Test
	public void testConversionWithTimestamps() throws Exception {
		
		// One test setting all the timestamps
		String alarmId = "AlarmType-ID";
		IASValue<?> alarm = IASValue.build(
			Alarm.SET_LOW,
			OperationalMode.DEGRADED,
			IasValidity.RELIABLE,
			new Identifier(alarmId, IdentifierType.IASIO, asceIdentifier).fullRunningID(),
			IASTypes.ALARM,
			1L,
			2L,
			3L,
			4L,
			5L,
			6L,
			7L,
			new HashSet<String>(),
			null);
		
		String jsonStr = jsonSerializer.iasValueToString(alarm);
		
		IASValue<?> alarmFromSerializer = jsonSerializer.valueOf(jsonStr);
		assertNotNull(alarmFromSerializer);
		assertEquals(alarm,alarmFromSerializer);
		
		// Explicit check the values of the tstamps even if the assertEquals
		// already ensure that
		assertTrue(alarmFromSerializer.pluginProductionTStamp.isPresent());
		assertTrue(alarmFromSerializer.pluginProductionTStamp.get()==1L);
		assertTrue(alarmFromSerializer.sentToConverterTStamp.isPresent());
		assertTrue(alarmFromSerializer.sentToConverterTStamp.get()==2L);
		assertTrue(alarmFromSerializer.receivedFromPluginTStamp.isPresent());
		assertTrue(alarmFromSerializer.receivedFromPluginTStamp.get()==3L);
		assertTrue(alarmFromSerializer.convertedProductionTStamp.isPresent());
		assertTrue(alarmFromSerializer.convertedProductionTStamp.get()==4L);
		assertTrue(alarmFromSerializer.sentToBsdbTStamp.isPresent());
		assertTrue(alarmFromSerializer.sentToBsdbTStamp.get()==5L);
		assertTrue(alarmFromSerializer.readFromBsdbTStamp.isPresent());
		assertTrue(alarmFromSerializer.readFromBsdbTStamp.get()==6L);
		assertTrue(alarmFromSerializer.dasuProductionTStamp.isPresent());
		assertTrue(alarmFromSerializer.dasuProductionTStamp.get()==7L);
		
		// Another test when some tstamps are unset to be
		// sure the property is empty
		String alarmId2 = "AlarmType-ID2";
		IASValue<?> alarm2 = IASValue.build(
			Alarm.SET_CRITICAL,
			OperationalMode.DEGRADED,
			IasValidity.RELIABLE,
			new Identifier(alarmId, IdentifierType.IASIO, asceIdentifier).fullRunningID(),
			IASTypes.ALARM,
			1L,
			null,
			3L,
			null,
			5L,
			null,
			7L,
			new HashSet<String>(),
			null);
		
		jsonStr = jsonSerializer.iasValueToString(alarm2);
		
		alarmFromSerializer = jsonSerializer.valueOf(jsonStr);
		assertNotNull(alarmFromSerializer);
		assertEquals(alarm2,alarmFromSerializer);
		
		// Explicit check the values of the tstamps even if the assertEquals
		// already ensure that
		assertTrue(alarmFromSerializer.pluginProductionTStamp.isPresent());
		assertTrue(alarmFromSerializer.pluginProductionTStamp.get()==1L);
		assertFalse(alarmFromSerializer.sentToConverterTStamp.isPresent());
		assertTrue(alarmFromSerializer.receivedFromPluginTStamp.isPresent());
		assertTrue(alarmFromSerializer.receivedFromPluginTStamp.get()==3L);
		assertFalse(alarmFromSerializer.convertedProductionTStamp.isPresent());
		assertTrue(alarmFromSerializer.sentToBsdbTStamp.isPresent());
		assertTrue(alarmFromSerializer.sentToBsdbTStamp.get()==5L);
		assertFalse(alarmFromSerializer.readFromBsdbTStamp.isPresent());
		assertTrue(alarmFromSerializer.dasuProductionTStamp.isPresent());
		assertTrue(alarmFromSerializer.dasuProductionTStamp.get()==7L);
		
		assertTrue(!alarmFromSerializer.dependentsFullRuningIds.isPresent());
	}
	
	/**
	 * Ensure that the Ids of the dependents monitor points
	 * are properly serialized/deserialized
	 */
	@Test
	public void testIdsOfDeps() throws Exception {
		
		String fullruningId1="(SupervId1:SUPERVISOR)@(dasuVID1:DASU)@(asceVID1:ASCE)@(AlarmID1:IASIO)";
		String fullruningId2="(SupervId2:SUPERVISOR)@(dasuVID2:DASU)@(asceVID2:ASCE)@(AlarmID2:IASIO)";
		
		Set<String> deps = new HashSet<>(Arrays.asList(fullruningId1,fullruningId2));
		
		// One test setting all the timestamps
		String alarmId = "AlarmType-ID";
		IASValue<?> alarm = IASValue.build(
			Alarm.SET_MEDIUM,
			OperationalMode.DEGRADED,
			IasValidity.RELIABLE,
			new Identifier(alarmId, IdentifierType.IASIO, asceIdentifier).fullRunningID(),
			IASTypes.ALARM,
			1L,
			2L,
			3L,
			4L,
			5L,
			6L,
			7L,
			deps,
			null);
		String jsonStr = jsonSerializer.iasValueToString(alarm);
		System.out.println("jsonStr ="+jsonStr);
		
		IASValue<?> fromJson = jsonSerializer.valueOf(jsonStr);
		assertTrue(alarm.dependentsFullRuningIds.isPresent());
		assertEquals(alarm.dependentsFullRuningIds.get().size(),fromJson.dependentsFullRuningIds.get().size());
		for (String frId: alarm.dependentsFullRuningIds.get()) {
			assertTrue(fromJson.dependentsFullRuningIds.get().contains(frId));
		}
	}
	
	/**
	 * test the serialization/desetrialization of user properties
	 * 
	 * @throws Exception
	 */
	@Test
	public void testAdditionalProperties() throws Exception {
		
		Map<String,String> props = new HashMap<>();
		props.put("key1", "value1");
		props.put("key2", "value2");
		
		// One test setting additional properties
		String alarmId = "AlarmType-ID";
		IASValue<?> alarm = IASValue.build(
			Alarm.getSetDefault(),
			OperationalMode.DEGRADED,
			IasValidity.RELIABLE,
			new Identifier(alarmId, IdentifierType.IASIO, asceIdentifier).fullRunningID(),
			IASTypes.ALARM,
			1L,
			2L,
			3L,
			4L,
			5L,
			6L,
			7L,
			new HashSet<String>(),
			props);
		String jsonStr = jsonSerializer.iasValueToString(alarm);
		System.out.println("jsonStr ="+jsonStr);
		for (String key: props.keySet()) {
			assertTrue(jsonStr.contains(key));
			assertTrue(jsonStr.contains(props.get(key)));
		}
		
		// Now generate the IASValue from the string
		IASValue<?> fromJson = jsonSerializer.valueOf(jsonStr);
		assertTrue(fromJson.props.isPresent());
		for (String key: props.keySet()) {
			assertTrue(fromJson.props.get().keySet().contains(key));
			assertEquals(props.get(key),fromJson.props.get().get(key));
		}
	}
}
