package org.eso.ias.basictypes.test

import org.scalatest.FlatSpec
import org.eso.ias.prototype.input.Identifier
import org.eso.ias.prototype.input.java.OperationalMode
import org.eso.ias.prototype.input.Validity
import org.eso.ias.prototype.input.InOut
import org.eso.ias.prototype.input.AlarmValue
import org.eso.ias.prototype.input.AlarmState
import org.eso.ias.prototype.input.AckState
import org.eso.ias.prototype.input.java.IASTypes
import org.eso.ias.prototype.input.JavaConverter
import org.eso.ias.prototype.input.java.IASValue
import org.eso.ias.prototype.input.Clear
import org.eso.ias.prototype.input.java.IasDouble
import org.eso.ias.prototype.input.java.IasAlarm

/**
 * Test the conversion between HIO to IASValue and vice-versa
 */
class TestJavaConversion  extends FlatSpec {
  behavior of "The Scala<->Java converter"
  
  def fixture = {
    new {
      // The IDs
      val parentId = new Identifier(Some[String]("ParentID"),None)
      val doubleHioId = new Identifier(Some[String]("DoubleID"),Option[Identifier](parentId))
      val alarmHioId = new Identifier(Some[String]("AlarmID"),Option[Identifier](parentId))
      // Refresh rate
      val refRate = InOut.MinRefreshRate+10
      // Modes
      val doubleMode = OperationalMode.MAINTENANCE
      val alarmMode = OperationalMode.STARTUP
      val mode = OperationalMode.OPERATIONAL
      // The values
      val alarmValue = Some(new AlarmValue(AlarmState.Active,false,AckState.Acknowledged))
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
      val validity = Validity.Reliable
      // The HIOs
      val longHIO = InOut[Long](longValue,doubleHioId,refRate,mode,validity,IASTypes.LONG)
      val intHIO = InOut[Int](intValue,doubleHioId,refRate,mode,validity,IASTypes.INT)
      val shortHIO = InOut[Short](shortValue,doubleHioId,refRate,mode,validity,IASTypes.SHORT)
      val byteHIO = InOut[Byte](byteValue,doubleHioId,refRate,mode,validity,IASTypes.BYTE)
      val charHIO = InOut[Char](charValue,doubleHioId,refRate,mode,validity,IASTypes.CHAR)
      val stringHIO = InOut[String](stringValue,doubleHioId,refRate,mode,validity,IASTypes.STRING)
      val boolHIO = InOut[Boolean](boolValue,doubleHioId,refRate,mode,validity,IASTypes.BOOLEAN)
      val alarmHIO = InOut[AlarmValue](alarmValue,alarmHioId,refRate,alarmMode,validity,IASTypes.ALARM)
      val doubleHIO = InOut[Double](doubleValue,doubleHioId,refRate,doubleMode,validity,IASTypes.DOUBLE)
      val floatHIO = InOut[Float](floatValue,doubleHioId,refRate,mode,validity,IASTypes.FLOAT)
      
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
    assert(doubleVal.timestamp==f.doubleHIO.actualValue.timestamp)
    assert(doubleVal.id==f.doubleHIO.id.id.get)
    assert(doubleVal.runningId==f.doubleHIO.id.runningID)
    assert(doubleVal.value==f.doubleHIO.actualValue.value.get)
    
    val alarmVal = JavaConverter.inOutToIASValue[AlarmValue](f.alarmHIO).asInstanceOf[IasAlarm]
    assert(alarmVal.value==f.alarmHIO.actualValue.value.get)
  }
  
  it must "Update a HIO with the values from a IASValue" in {
    val f = fixture
    
    val doubleVal = JavaConverter.inOutToIASValue[Double](f.doubleHIO).asInstanceOf[IasDouble]
    val newdoubleVal = new IasDouble(doubleVal.value+8.5,System.currentTimeMillis(),OperationalMode.OPERATIONAL,doubleVal.id,doubleVal.runningId)
    val hio = JavaConverter.updateHIOWithIasValue(f.doubleHIO,newdoubleVal)
    
    assert(newdoubleVal.valueType==hio.iasType)
    assert(newdoubleVal.mode==hio.mode)
    assert(newdoubleVal.id==hio.id.id.get)
    assert(newdoubleVal.runningId==hio.id.runningID)
    assert(newdoubleVal.value==hio.actualValue.value.get)
    
    val alarmVal = JavaConverter.inOutToIASValue[AlarmValue](f.alarmHIO).asInstanceOf[IasAlarm]
    val alarm = alarmVal.value
    val newAlarm = AlarmValue.transition(alarm, new Clear())
    val newAlarmValue = alarmVal.updateValue(newAlarm.right.get).asInstanceOf[IasAlarm]
    val alarmHio = JavaConverter.updateHIOWithIasValue(f.alarmHIO,newAlarmValue)
    
    assert(alarmHio.actualValue.value.get.asInstanceOf[AlarmValue].alarmState==AlarmState.Cleared)
  }
  
  it must "fail updating with wrong IDs, runningIDs, type" in {
    val f = fixture
    val doubleVal = JavaConverter.inOutToIASValue(f.doubleHIO).asInstanceOf[IasDouble]
    // Build a IASValue with another ID
    val newDoubleValueWrongId = new IasDouble(
        doubleVal.value,
        doubleVal.timestamp,
        doubleVal.mode,
        doubleVal.id+"WRONG!",
        doubleVal.runningId)
    
    assertThrows[IllegalStateException] {
      JavaConverter.updateHIOWithIasValue(f.doubleHIO, newDoubleValueWrongId)
    }
    
    val newDoubleValueWrongRunId = new IasDouble(
        doubleVal.value,
        doubleVal.timestamp,
        doubleVal.mode,
        doubleVal.id,
        doubleVal.runningId+"WRONG!")
    assertThrows[IllegalStateException] {
      JavaConverter.updateHIOWithIasValue(f.doubleHIO, newDoubleValueWrongRunId)
    }
  }
  
}