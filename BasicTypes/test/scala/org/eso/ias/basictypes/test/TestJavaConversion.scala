package org.eso.ias.basictypes.test

import org.scalatest.FlatSpec
import org.eso.ias.prototype.input.Identifier
import org.eso.ias.prototype.input.java.OperationalMode
import org.eso.ias.prototype.input.Validity
import org.eso.ias.prototype.input.InOut
import org.eso.ias.prototype.input.java.IASTypes
import org.eso.ias.prototype.input.JavaConverter
import org.eso.ias.prototype.input.java.IASValue
import org.eso.ias.prototype.input.java.IasDouble
import org.eso.ias.prototype.input.java.IasAlarm
import org.eso.ias.prototype.input.java.IdentifierType
import org.eso.ias.prototype.input.java.AlarmSample

// The following import is required by the usage of the fixture
import language.reflectiveCalls
import org.eso.ias.prototype.input.java.IasValidity._

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
      // Refresh rate
      val refRate = InOut.MinRefreshRate+10
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
      val longHIO = InOut[Long](longValue,System.currentTimeMillis(),doubleHioId,refRate,mode,validity,IASTypes.LONG)
      val intHIO = InOut[Int](intValue,System.currentTimeMillis(),doubleHioId,refRate,mode,validity,IASTypes.INT)
      val shortHIO = InOut[Short](shortValue,System.currentTimeMillis(),doubleHioId,refRate,mode,validity,IASTypes.SHORT)
      val byteHIO = InOut[Byte](byteValue,System.currentTimeMillis(),doubleHioId,refRate,mode,validity,IASTypes.BYTE)
      val charHIO = InOut[Char](charValue,System.currentTimeMillis(),doubleHioId,refRate,mode,validity,IASTypes.CHAR)
      val stringHIO = InOut[String](stringValue,System.currentTimeMillis(),doubleHioId,refRate,mode,validity,IASTypes.STRING)
      val boolHIO = InOut[Boolean](boolValue,System.currentTimeMillis(),doubleHioId,refRate,mode,validity,IASTypes.BOOLEAN)
      val alarmHIO = InOut[AlarmSample](alarmValue,System.currentTimeMillis(),alarmHioId,refRate,alarmMode,validity,IASTypes.ALARM)
      val doubleHIO = InOut[Double](doubleValue,System.currentTimeMillis(),doubleHioId,refRate,doubleMode,validity,IASTypes.DOUBLE)
      val floatHIO = InOut[Float](floatValue,System.currentTimeMillis(),doubleHioId,refRate,mode,validity,IASTypes.FLOAT)
      
      // Ensure we are testing all possible types
      val hios = List (longHIO,intHIO,shortHIO,byteHIO,charHIO,stringHIO,boolHIO,alarmHIO,doubleHIO,floatHIO)
      assert(hios.size==IASTypes.values().size)
    }
  }
  
  it must "build the java value with the proper values" in {
    val f = fixture
    val doubleVal = JavaConverter.inOutToIASValue[Double](f.doubleHIO).asInstanceOf[IasDouble]
    assert(doubleVal.valueType==f.doubleHIO.iasType)
    assert(doubleVal.mode==f.doubleHIO.mode)
    assert(doubleVal.timestamp==f.doubleHIO.timestamp)
    assert(doubleVal.id==f.doubleHIO.id.id)
    assert(doubleVal.fullRunningId==f.doubleHIO.id.fullRunningID)
    assert(doubleVal.value==f.doubleHIO.value.get)
    
    val alarmVal = JavaConverter.inOutToIASValue[AlarmSample](f.alarmHIO).asInstanceOf[IasAlarm]
    assert(alarmVal.value==f.alarmHIO.value.get)
  }
  
  it must "Update a HIO with the values from a IASValue" in {
    val f = fixture
    
    val doubleVal = JavaConverter.inOutToIASValue[Double](f.doubleHIO).asInstanceOf[IasDouble]
    val newdoubleVal = new IasDouble(doubleVal.value+8.5,System.currentTimeMillis(),OperationalMode.OPERATIONAL,UNRELIABLE,doubleVal.fullRunningId)
    val hio = JavaConverter.updateHIOWithIasValue(f.doubleHIO,newdoubleVal)
    
    assert(newdoubleVal.valueType==hio.iasType)
    assert(newdoubleVal.mode==hio.mode)
    assert(newdoubleVal.id==hio.id.id)
    assert(newdoubleVal.fullRunningId==hio.id.fullRunningID)
    assert(newdoubleVal.value==hio.value.get)
    
    val alarmVal = JavaConverter.inOutToIASValue[AlarmSample](f.alarmHIO).asInstanceOf[IasAlarm]
    val alarm = alarmVal.value
    val newAlarm = AlarmSample.CLEARED
    val newAlarmValue = alarmVal.updateValue(newAlarm).asInstanceOf[IasAlarm]
    val alarmHio = JavaConverter.updateHIOWithIasValue(f.alarmHIO,newAlarmValue)
    
    assert(alarmHio.value.get.asInstanceOf[AlarmSample]==AlarmSample.CLEARED)
  }
  
}