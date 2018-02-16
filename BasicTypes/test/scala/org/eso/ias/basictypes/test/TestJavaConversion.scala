package org.eso.ias.basictypes.test

import org.scalatest.FlatSpec
import org.eso.ias.types.Identifier
import org.eso.ias.types.OperationalMode
import org.eso.ias.types.Validity
import org.eso.ias.types.InOut
import org.eso.ias.types.IASTypes
import org.eso.ias.types.JavaConverter
import org.eso.ias.types.IASValue
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
  
  it must "correctly build the IASValue" in {
    val f = fixture
    val doubleVal = JavaConverter.inOutToIASValue[Double](f.doubleHIO,Validity(IasValidity.RELIABLE))
    assert(doubleVal.valueType==f.doubleHIO.iasType)
    assert(doubleVal.mode==f.doubleHIO.mode)
    
    assert(!doubleVal.pluginProductionTStamp.isPresent())
	  assert(!doubleVal.sentToConverterTStamp.isPresent())
	  assert(!doubleVal.receivedFromPluginTStamp.isPresent())
	  assert(!doubleVal.convertedProductionTStamp.isPresent())
	  assert(!doubleVal.sentToBsdbTStamp.isPresent())
	  assert(!doubleVal.readFromBsdbTStamp.isPresent())
	  assert(!doubleVal.dasuProductionTStamp.isPresent())
    
    assert(doubleVal.id==f.doubleHIO.id.id)
    assert(doubleVal.fullRunningId==f.doubleHIO.id.fullRunningID)
    assert(doubleVal.value==f.doubleHIO.value.get)
    assert(doubleVal.iasValidity==IasValidity.RELIABLE)
    
    val alarmVal = JavaConverter.inOutToIASValue[AlarmSample](f.alarmHIO,Validity(IasValidity.UNRELIABLE))
    assert(alarmVal.value==f.alarmHIO.value.get)
    assert(alarmVal.iasValidity==IasValidity.UNRELIABLE)
  }
  
  it must "Update the times in the IASValue" in {
    val f = fixture
    val doubleVal = JavaConverter.inOutToIASValue[Double](f.doubleHIO,Validity(IasValidity.RELIABLE))
    
    val updatePluginTime = doubleVal.updatePluginProdTime(1L)
    assert(updatePluginTime.pluginProductionTStamp.get == 1L)
    
    val sentToConvTime = updatePluginTime.updateSentToConverterTime(2L)
    assert(sentToConvTime.sentToConverterTStamp.get == 2L)
   
    val updateCobverterTime = sentToConvTime.updateRecvFromPluginTime(3L)
    assert(updateCobverterTime.receivedFromPluginTStamp.get == 3L)
   
   val convProTime = updateCobverterTime.updateConverterProdTime(4L)
   assert(convProTime.convertedProductionTStamp.get == 4L)
   
   val sentBsdTime = convProTime.updateSentToBsdbTime(5L)
   assert(sentBsdTime.sentToBsdbTStamp.get == 5L)
   
   val readBsdbTime = sentBsdTime.updateReadFromBsdbTime(6L)
   assert(readBsdbTime.readFromBsdbTStamp.get == 6L)
   
   // This last IASValue must contain all the previously set timestamps
   val dasuProdTime = readBsdbTime.updateDasuProdTime(7L)
   assert(dasuProdTime.pluginProductionTStamp.get == 1L)
   assert(dasuProdTime.sentToConverterTStamp.get == 2L)
   assert(dasuProdTime.receivedFromPluginTStamp.get == 3L)
   assert(dasuProdTime.convertedProductionTStamp.get == 4L)
   assert(dasuProdTime.sentToBsdbTStamp.get == 5L)
   assert(dasuProdTime.readFromBsdbTStamp.get == 6L)
   assert(dasuProdTime.dasuProductionTStamp.get == 7L)
  }
  
  it must "Update a IASIO with the values from a IASValue" in {
    val f = fixture
    
    val doubleVal = JavaConverter.inOutToIASValue[Double](f.doubleHIO,Validity(IasValidity.UNRELIABLE))
    val newdoubleVal = IASValue.build(8.5d, OperationalMode.OPERATIONAL, UNRELIABLE, doubleVal.fullRunningId, IASTypes.DOUBLE)
        
    val iasio = JavaConverter.updateHIOWithIasValue(f.doubleHIO,newdoubleVal)
    
    assert(newdoubleVal.valueType==iasio.iasType)
    assert(newdoubleVal.mode==iasio.mode)
    assert(newdoubleVal.id==iasio.id.id)
    assert(newdoubleVal.fullRunningId==iasio.id.fullRunningID)
    assert(newdoubleVal.value==iasio.value.get)
    
    val alarmVal = JavaConverter.inOutToIASValue[AlarmSample](f.alarmHIO,Validity(IasValidity.RELIABLE))
    val alarm = alarmVal.value
    val newAlarm = AlarmSample.CLEARED
    val newAlarmValue = alarmVal.updateValue(newAlarm)
    val alarmHio = JavaConverter.updateHIOWithIasValue(f.alarmHIO,newAlarmValue)
    
    assert(alarmHio.value.get.asInstanceOf[AlarmSample]==AlarmSample.CLEARED)
    assert(alarmHio.fromIasValueValidity.isDefined)
    assert(alarmHio.fromIasValueValidity.get.iasValidity==IasValidity.RELIABLE)
  }
  
}