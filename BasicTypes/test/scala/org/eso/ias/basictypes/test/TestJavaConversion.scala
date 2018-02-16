package org.eso.ias.basictypes.test

import org.scalatest.FlatSpec
import org.eso.ias.types.Identifier
import org.eso.ias.types.OperationalMode
import org.eso.ias.types.Validity
import org.eso.ias.types.InOut
import org.eso.ias.types.IASTypes
import org.eso.ias.types.JavaConverter
import org.eso.ias.types.IASValue
import org.eso.ias.types.IasDouble
import org.eso.ias.types.IasAlarm
import org.eso.ias.types.IdentifierType
import org.eso.ias.types.AlarmSample
import org.eso.ias.types.IasValidity._
import org.eso.ias.types.IasValidity

// The following import is required by the usage of the fixture
import language.reflectiveCalls

/**
 * Test the conversion between HIO to IASValue and vice-versa
 */
class TestJavaConversion  extends FlatSpec {
  behavior of "The Scala<->Java converter"
  
  def fixture = {
    new {
      // The IDs
      val supervId = new Identifier("SupervId",IdentifierType.SUPERVISOR,None)
      val dasuId = new Identifier("dasuVID",IdentifierType.DASU,supervId)
      val asceId = new Identifier("asceVID",IdentifierType.ASCE,Option(dasuId))      
      
      val doubleHioId = new Identifier("DoubleID",IdentifierType.IASIO,Option[Identifier](asceId))
      val alarmHioId = new Identifier("AlarmID",IdentifierType.IASIO,Option[Identifier](asceId))
      // Modes
      val doubleMode = OperationalMode.MAINTENANCE
      val alarmMode = OperationalMode.STARTUP
      val mode = OperationalMode.OPERATIONAL
      // The values
      val alarmValue = Some(AlarmSample.SET)
      val doubleValue = Some(48.6D)
      val floatValue = Some(-11.5F)
      val longValue = Some(11L)
      val intValue = Some(-76)
      val shortValue = Some(15.toShort)
      val byteValue = Some(43.toByte)
      val charValue = Some('X')
      val stringValue = Some("Test")
      val boolValue = Some(false)
      
      // Validity
      val validity = Some(Validity(RELIABLE))
      // The HIOs
      val longHIO = InOut[Long](
        longValue, 
        doubleHioId, 
        mode,
        validity,
        IASTypes.LONG,
        None,None,None,None,None,None,None) 
      val intHIO = InOut[Int](
        intValue, 
        doubleHioId, 
        mode,
        validity,
        IASTypes.INT,
        None,None,None,None,None,None,None) 
      val shortHIO =  InOut[Int](
        shortValue, 
        doubleHioId, 
        mode,
        validity,
        IASTypes.SHORT,
        None,None,None,None,None,None,None) 
      val byteHIO = InOut[Byte](
        byteValue, 
        doubleHioId, 
        mode,
        validity,
        IASTypes.BYTE,
        None,None,None,None,None,None,None)
      val charHIO = InOut[Char](
        charValue, 
        doubleHioId, 
        mode,
        validity,
        IASTypes.CHAR,
        None,None,None,None,None,None,None) 
      val stringHIO = InOut[String](
        stringValue, 
        doubleHioId, 
        mode,
        validity,
        IASTypes.STRING,
        None,None,None,None,None,None,None)
      val boolHIO = InOut[Boolean](
        boolValue, 
        doubleHioId, 
        mode,
        validity,
        IASTypes.BOOLEAN,
        None,None,None,None,None,None,None)
      val alarmHIO = InOut[AlarmSample](
        alarmValue, 
        alarmHioId, 
        mode,
        validity,
        IASTypes.ALARM,
        None,None,None,None,None,None,None)
      val doubleHIO = InOut[Double](
        doubleValue, 
        doubleHioId, 
        doubleMode,
        validity,
        IASTypes.DOUBLE,
        None,None,None,None,None,None,None)
      val floatHIO = InOut[Float](
        floatValue, 
        doubleHioId, 
        mode,
        validity,
        IASTypes.FLOAT,
        Some(1L),Some(2L),Some(3L),Some(4L),Some(5L),Some(6L),Some(7L))
      
      // Ensure we are testing all possible types
      val hios = List (longHIO,intHIO,shortHIO,byteHIO,charHIO,stringHIO,boolHIO,alarmHIO,doubleHIO,floatHIO)
      assert(hios.size==IASTypes.values().size)
    }
  }
  
  it must "build the java value with the proper values" in {
    val f = fixture
    val doubleVal = JavaConverter.inOutToIASValue[Double](f.doubleHIO,Validity(IasValidity.RELIABLE))
    assert(doubleVal.valueType==f.doubleHIO.iasType)
    assert(doubleVal.mode==f.doubleHIO.mode)
    
    assert(doubleVal.pluginProductionTStamp.get == f.doubleHIO.pluginProductionTStamp.get)
	  assert(doubleVal.sentToConverterTStamp.get == f.doubleHIO.sentToConverterTStamp.get)
	  assert(doubleVal.receivedFromPluginTStamp.get == f.doubleHIO.receivedFromPluginTStamp.get)
	  assert(doubleVal.convertedProductionTStamp.get == f.doubleHIO.convertedProductionTStamp.get)
	  assert(doubleVal.sentToBsdbTStamp.get == f.doubleHIO.sentToBsdbTStamp.get)
	  assert(doubleVal.readFromBsdbTStamp.get == f.doubleHIO.readFromBsdbTStamp.get)
	  assert(doubleVal.dasuProductionTStamp.get == f.doubleHIO.dasuProductionTStamp.get)
    
    assert(doubleVal.id==f.doubleHIO.id.id)
    assert(doubleVal.fullRunningId==f.doubleHIO.id.fullRunningID)
    assert(doubleVal.value==f.doubleHIO.value.get)
    assert(doubleVal.iasValidity==IasValidity.RELIABLE)
    
    val alarmVal = JavaConverter.inOutToIASValue[AlarmSample](f.alarmHIO,Validity(IasValidity.UNRELIABLE)).asInstanceOf[IasAlarm]
    assert(alarmVal.value==f.alarmHIO.value.get)
    assert(alarmVal.iasValidity==IasValidity.UNRELIABLE)
  }
  
  it must "Update a HIO with the values from a IASValue" in {
    val f = fixture
    
    val doubleVal = JavaConverter.inOutToIASValue[Double](f.doubleHIO,Validity(IasValidity.UNRELIABLE)).asInstanceOf[IasDouble]
    val newdoubleVal = new IasDouble(doubleVal.value+8.5,System.currentTimeMillis(),OperationalMode.OPERATIONAL,UNRELIABLE,doubleVal.fullRunningId)
    val hio = JavaConverter.updateHIOWithIasValue(f.doubleHIO,newdoubleVal)
    
    assert(newdoubleVal.valueType==hio.iasType)
    assert(newdoubleVal.mode==hio.mode)
    assert(newdoubleVal.id==hio.id.id)
    assert(newdoubleVal.fullRunningId==hio.id.fullRunningID)
    assert(newdoubleVal.value==hio.value.get)
    
    val alarmVal = JavaConverter.inOutToIASValue[AlarmSample](f.alarmHIO,Validity(IasValidity.RELIABLE)).asInstanceOf[IasAlarm]
    val alarm = alarmVal.value
    val newAlarm = AlarmSample.CLEARED
    val newAlarmValue = alarmVal.updateValue(newAlarm).asInstanceOf[IasAlarm]
    val alarmHio = JavaConverter.updateHIOWithIasValue(f.alarmHIO,newAlarmValue)
    
    assert(alarmHio.value.get.asInstanceOf[AlarmSample]==AlarmSample.CLEARED)
    assert(alarmHio.fromIasValueValidity.isDefined)
    assert(alarmHio.fromIasValueValidity.get.iasValidity==IasValidity.RELIABLE)
  }
  
}