package org.eso.ias.basictypes.test;

import org.eso.ias.types.*;
import org.junit.jupiter.api.Test;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

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

		String tStampId = "TimestampType-ID";
		IASValue<?> tStampIasType = IASValue.build(
			Long.valueOf(System.currentTimeMillis()),
			OperationalMode.OPERATIONAL,
			IasValidity.RELIABLE,
			new Identifier(tStampId, IdentifierType.IASIO, asceIdentifier).fullRunningID(),
			IASTypes.TIMESTAMP);
		jsonStr = jsonSerializer.iasValueToString(tStampIasType);

		IASValue<?> tStampFromSerializer = jsonSerializer.valueOf(jsonStr);
		assertNotNull(tStampFromSerializer);
		assertEquals(tStampIasType,tStampFromSerializer);

		String alarmId = "AlarmType-ID";
		IASValue<?> alarm = IASValue.build(
			Alarm.getInitialAlarmState(),
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
	 * Test the conversion of arrays of doubles and longs
	 *
	 * @throws Exception
	 */
	@Test
	public void testConversionsOfArraysToJString() throws Exception {

		String longArrayId = "LongArray-ID";

		List<Long> longs = Arrays.asList(new Long[]{9L,8L,7L,6L,5L,4L,3L,2L,-1L,0L});
		NumericArray arrayOfLongs = new NumericArray(NumericArray.NumericArrayType.LONG,longs);

		IASValue<?> arrayLongIasType = IASValue.build(
				arrayOfLongs,
				OperationalMode.CLOSING,
				IasValidity.RELIABLE,
				new Identifier(longArrayId, IdentifierType.IASIO, asceIdentifier).fullRunningID(),
				IASTypes.ARRAYOFLONGS);

		String jsonStr = jsonSerializer.iasValueToString(arrayLongIasType);

		IASValue<?> fromSerializer = jsonSerializer.valueOf(jsonStr);
		assertNotNull(fromSerializer, "Got a null de-serializing [" + jsonStr + "]");
		assertEquals(arrayLongIasType, fromSerializer);

		String doubleArrayId = "DoubleArray-ID";

		List<Double> doubles = Arrays.asList(new Double[]{9.2,8.3,7D,6.4,-5.6,-4D,3.657,2.1324,-1D,0D});
		NumericArray arrayOfDoubles = new NumericArray(NumericArray.NumericArrayType.DOUBLE,doubles);

		IASValue<?> arrayDoubelIasType = IASValue.build(
				arrayOfDoubles,
				OperationalMode.CLOSING,
				IasValidity.RELIABLE,
				new Identifier(longArrayId, IdentifierType.IASIO, asceIdentifier).fullRunningID(),
				IASTypes.ARRAYOFDOUBLES);

		jsonStr = jsonSerializer.iasValueToString(arrayDoubelIasType);

		fromSerializer = jsonSerializer.valueOf(jsonStr);
		assertNotNull(fromSerializer, "Got a null de-serializing [" + jsonStr + "]");
		assertEquals(arrayDoubelIasType, fromSerializer);
	}
	
	/**
	 * Ensure that timestamps are properly serialized/deserialized
	 */
	@Test
	public void testConversionWithTimestamps() throws Exception {
		
		// One test setting all the timestamps
		String alarmId = "AlarmType-ID";
		IASValue<?> alarm = IASValue.build(
			Alarm.getInitialAlarmState(Priority.LOW),
			OperationalMode.DEGRADED,
			IasValidity.RELIABLE,
			new Identifier(alarmId, IdentifierType.IASIO, asceIdentifier).fullRunningID(),
			IASTypes.ALARM,
			8L,
			1L,
			2L,
			3L,
			4L,
			5L,
			6L,
			new HashSet<String>(),
			null);
		
		String jsonStr = jsonSerializer.iasValueToString(alarm);
		
		IASValue<?> alarmFromSerializer = jsonSerializer.valueOf(jsonStr);
		assertNotNull(alarmFromSerializer);
		assertEquals(alarm,alarmFromSerializer);
		
		// Explicit check the values of the tstamps even if the assertEquals
		// already ensure that
		assertTrue(alarmFromSerializer.readFromMonSysTStamp.isPresent());
		assertTrue(alarmFromSerializer.readFromMonSysTStamp.get()==8L);
		assertTrue(alarmFromSerializer.productionTStamp.isPresent());
		assertTrue(alarmFromSerializer.productionTStamp.get()==1L);
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

		// Another test when some tstamps are unset to be
		// sure the property is empty
		String alarmId2 = "AlarmType-ID2";
		IASValue<?> alarm2 = IASValue.build(
			Alarm.getInitialAlarmState(Priority.CRITICAL).set(),
			OperationalMode.DEGRADED,
			IasValidity.RELIABLE,
			new Identifier(alarmId, IdentifierType.IASIO, asceIdentifier).fullRunningID(),
			IASTypes.ALARM,
			null,
			1L,
			null,
			3L,
			null,
			5L,
			null,
			new HashSet<String>(),
			null);
		
		jsonStr = jsonSerializer.iasValueToString(alarm2);
		
		alarmFromSerializer = jsonSerializer.valueOf(jsonStr);
		assertNotNull(alarmFromSerializer);
		assertEquals(alarm2,alarmFromSerializer);
		
		// Explicit check the values of the tstamps even if the assertEquals
		// already ensure that
		assertFalse(alarmFromSerializer.readFromMonSysTStamp.isPresent());
		assertTrue(alarmFromSerializer.productionTStamp.isPresent());
		assertTrue(alarmFromSerializer.productionTStamp.get()==1L);
		assertFalse(alarmFromSerializer.sentToConverterTStamp.isPresent());
		assertTrue(alarmFromSerializer.receivedFromPluginTStamp.isPresent());
		assertTrue(alarmFromSerializer.receivedFromPluginTStamp.get()==3L);
		assertFalse(alarmFromSerializer.convertedProductionTStamp.isPresent());
		assertTrue(alarmFromSerializer.sentToBsdbTStamp.isPresent());
		assertTrue(alarmFromSerializer.sentToBsdbTStamp.get()==5L);
		assertFalse(alarmFromSerializer.readFromBsdbTStamp.isPresent());

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
			Alarm.getInitialAlarmState(),
			OperationalMode.DEGRADED,
			IasValidity.RELIABLE,
			new Identifier(alarmId, IdentifierType.IASIO, asceIdentifier).fullRunningID(),
			IASTypes.ALARM,
			8L,
			1L,
			2L,
			3L,
			4L,
			5L,
			6L,
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
			Alarm.getInitialAlarmState(Priority.HIGH),
			OperationalMode.DEGRADED,
			IasValidity.RELIABLE,
			new Identifier(alarmId, IdentifierType.IASIO, asceIdentifier).fullRunningID(),
			IASTypes.ALARM,
			8L,
			1L,
			2L,
			3L,
			4L,
			5L,
			6L,
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
