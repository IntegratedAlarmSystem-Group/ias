package org.eso.ias.basictypes.test

import org.scalatest.FlatSpec
import org.eso.ias.types.Identifier
import org.eso.ias.types.OperationalMode
import org.eso.ias.types.Validity
import org.eso.ias.types.InOut
import org.eso.ias.types.IASTypes
import org.eso.ias.types.IASValue
import org.eso.ias.types.IdentifierType
import org.eso.ias.types.Alarm
import org.eso.ias.types.IasValidity._
import org.eso.ias.types.IasValidity

// The following import is required by the usage of the fixture
import language.reflectiveCalls

/**
 * Test the conversion between IASIO to IASValue and vice-versa
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
      val alarmValue = Some(Alarm.SET_MEDIUM)
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
      // The IASIOs
      val longHIO = new InOut[Long]( // Input
        longValue, 
        doubleHioId, 
        mode,
        validity,
        None,
        None,
        IASTypes.LONG,
        None,None,None,None,None,None,None,None,None) 
      val intHIO = new InOut[Int]( // Input
        intValue, 
        doubleHioId, 
        mode,
        validity,
        None,
        None,
        IASTypes.INT,
        None,None,None,None,None,None,None,None,None) 
      val shortHIO =  new InOut[Int]( // Input
        shortValue, 
        doubleHioId, 
        mode,
        validity,
        None,
        None,
        IASTypes.SHORT,
        None,None,None,None,None,None,None,None,None) 
      val byteHIO = new InOut[Byte]( // Input
        byteValue, 
        doubleHioId, 
        mode,
        validity,
        None,
        None,
        IASTypes.BYTE,
        None,None,None,None,None,None,None,None,None)
      val charHIO = new InOut[Char]( // Input
        charValue, 
        doubleHioId, 
        mode,
        validity,
        None,
        None,
        IASTypes.CHAR,
        None,None,None,None,None,None,None,None,None) 
      val stringHIO = new InOut[String]( // Input
        stringValue, 
        doubleHioId, 
        mode,
        validity,
        None,
        None,
        IASTypes.STRING,
        None,None,None,None,None,None,None,None,None)
      val boolHIO = new InOut[Boolean]( // Input
        boolValue, 
        doubleHioId, 
        mode,
        validity,
        None,
        None,
        IASTypes.BOOLEAN,
        None,None,None,None,None,None,None,None,None)
      val alarmHIO = new InOut[Alarm]( // Output
        alarmValue, 
        alarmHioId, 
        mode,
        None,
        validity,
        None,
        IASTypes.ALARM,
        None,None,None,None,None,None,None,None,None)
      val doubleHIO = new InOut[Double]( // Input
        doubleValue, 
        doubleHioId, 
        doubleMode,
        validity,
        None,
        None,
        IASTypes.DOUBLE,
        None,None,None,None,None,None,None,None,None)
      val floatHIO = new InOut[Float]( // Input
        floatValue, 
        doubleHioId, 
        mode,
        validity,
        None,
        None,
        IASTypes.FLOAT,
        Some(1L),Some(2L),Some(3L),Some(4L),Some(5L),Some(6L),None,None,None)
      
      // Ensure we are testing all possible types
      val hios = List (longHIO,intHIO,shortHIO,byteHIO,charHIO,stringHIO,boolHIO,alarmHIO,doubleHIO,floatHIO)
      assert(hios.size==IASTypes.values().size)
    }
  }
  
  it must "correctly build the IASValue" in {
    val f = fixture
    val doubleVal = f.doubleHIO.toIASValue()
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
    
    val alarmVal = f.alarmHIO.toIASValue()
    assert(alarmVal.value==f.alarmHIO.value.get)
    assert(alarmVal.iasValidity==IasValidity.RELIABLE)
  }
  
  it must "Update the times in the IASValue" in {
    val f = fixture
    val doubleVal = f.doubleHIO.toIASValue()
    
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
    
    val doubleVal = f.doubleHIO.toIASValue()
    val newdoubleVal = IASValue.build(8.5d, OperationalMode.OPERATIONAL, UNRELIABLE, doubleVal.fullRunningId, IASTypes.DOUBLE)
        
    val iasio = f.doubleHIO.update(newdoubleVal)
    
    assert(newdoubleVal.valueType==iasio.iasType)
    assert(newdoubleVal.mode==iasio.mode)
    assert(newdoubleVal.id==iasio.id.id)
    assert(newdoubleVal.fullRunningId==iasio.id.fullRunningID)
    assert(newdoubleVal.value==iasio.value.get)
    
    val dV: IASValue[Double] = f.doubleHIO.toIASValue()
    val dv2 = dV.updateValue[Double](5.6)
    
    val alarmVal = f.alarmHIO.toIASValue()
    val alarm = alarmVal.value
    val alarmCleared = Alarm.CLEARED
    val newAlarmValue = alarmVal.updateValue(alarmCleared)
    val alarmHio = f.alarmHIO.update(newAlarmValue)
    
    assert(alarmHio.value.get.asInstanceOf[Alarm]==Alarm.CLEARED)
    assert(alarmHio.fromIasValueValidity.isEmpty)
    assert(alarmHio.fromInputsValidity.isDefined)
    assert(alarmHio.fromInputsValidity.get.iasValidity==IasValidity.RELIABLE)
  }
  
  it must "Update a IASIO with the ids of dependents of a IASValue" in { 
    val f = fixture
    
    // The IDs of dependents
    val supervId1 = new Identifier("SupervId1",IdentifierType.SUPERVISOR,None)
    val dasuId1 = new Identifier("dasuVID1",IdentifierType.DASU,supervId1)
    val asceId1 = new Identifier("asceVID1",IdentifierType.ASCE,Option(dasuId1))      
    val depId1 = new Identifier("AlarmID1",IdentifierType.IASIO,Option[Identifier](asceId1))
    
    val supervId2 = new Identifier("SupervId2",IdentifierType.SUPERVISOR,None)
    val dasuId2 = new Identifier("dasuVID2",IdentifierType.DASU,supervId2)
    val asceId2 = new Identifier("asceVID2",IdentifierType.ASCE,Option(dasuId2))      
    val depId2 = new Identifier("AlarmID2",IdentifierType.IASIO,Option[Identifier](asceId2))
    
    val alarmHIO = new InOut[Alarm]( // Output
        f.alarmValue, 
        f.alarmHioId, 
        f.mode,
        None,
        f.validity,
        None,
        IASTypes.ALARM,
        None,None,None,None,None,None,None,Some(Set(depId1,depId2)),None)
    
    val alarmVal = alarmHIO.toIASValue()
    
    val valueDepIds = alarmVal.dependentsFullRuningIds
    assert(valueDepIds.isPresent())
    assert(valueDepIds.get().contains(depId1.fullRunningID))
    assert(valueDepIds.get().contains(depId2.fullRunningID))
    assert(valueDepIds.get().size==2)
    
    // Now check if updating a InOut takes the dependents from the IASValue
    val alarmHIO2 = new InOut[Alarm]( // Output
        f.alarmValue, 
        f.alarmHioId, 
        f.mode,
        None,
        f.validity,
        None,
        IASTypes.ALARM,
        None,None,None,None,None,None,None,None,None).update(alarmVal)
        
    val deps = alarmHIO2.idsOfDependants
    assert(deps.isDefined)
    assert(alarmHIO2.idsOfDependants.get.size==2)
    assert(alarmHIO2.idsOfDependants.get.contains(depId1))
    assert(alarmHIO2.idsOfDependants.get.contains(depId2))
  }
  
  it must "Update a IASIO with the properties of a IASValue" in { 
    val f = fixture
    
    // The IDs of dependents
    val supervId1 = new Identifier("SupervId1",IdentifierType.SUPERVISOR,None)
    val dasuId1 = new Identifier("dasuVID1",IdentifierType.DASU,supervId1)
    val asceId1 = new Identifier("asceVID1",IdentifierType.ASCE,Option(dasuId1))      
    val depId1 = new Identifier("AlarmID1",IdentifierType.IASIO,Option[Identifier](asceId1))
    
    val supervId2 = new Identifier("SupervId2",IdentifierType.SUPERVISOR,None)
    val dasuId2 = new Identifier("dasuVID2",IdentifierType.DASU,supervId2)
    val asceId2 = new Identifier("asceVID2",IdentifierType.ASCE,Option(dasuId2))      
    val depId2 = new Identifier("AlarmID2",IdentifierType.IASIO,Option[Identifier](asceId2))
    
    val properties = Map("key1"->"Value1", "key2"->"Value2")
    
    val alarmHIO = new InOut[Alarm]( // Output
        f.alarmValue, 
        f.alarmHioId, 
        f.mode,
        None,
        f.validity,
        None,
        IASTypes.ALARM,
        None,None,None,None,None,None,None,None,Some(properties))
    
    val alarmVal = alarmHIO.toIASValue()
    
    val valueProps = alarmVal.props
    assert(valueProps.isPresent())
    assert(valueProps.get.size()==properties.size)
    properties.keys.foreach(key => { 
        assert(valueProps.get.keySet().contains(key))
        assert(properties(key)==valueProps.get.get(key))
    })
    
    // Now check if updating a InOut takes the properties from the IASValue
    val alarmHIO2 = new InOut[Alarm]( // Output
        f.alarmValue, 
        f.alarmHioId, 
        f.mode,
        None,
        f.validity,
        None,
        IASTypes.ALARM,
        None,None,None,None,None,None,None,None,None).update(alarmVal)
    
    val valuePropsOpt = alarmHIO2.props
    assert(valuePropsOpt.isDefined)
    assert(valuePropsOpt.get.size==properties.size)
    properties.keys.foreach(key => { 
        assert(valuePropsOpt.get.keys.toList.contains(key))
        assert(properties(key)==valueProps.get.get(key))
    })    
    
    
  }
  
}