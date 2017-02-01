package org.eso.ias.basictypes.test

import org.scalatest.FlatSpec
import org.eso.ias.prototype.input.Validity
import org.eso.ias.prototype.input.java.OperationalMode
import org.eso.ias.prototype.input.Identifier
import org.eso.ias.prototype.input.InOut
import org.eso.ias.prototype.input.java.IASTypes
import org.eso.ias.prototype.input.AlarmValue
import org.eso.ias.prototype.input.AlarmState
import org.eso.ias.prototype.input.AckState

/**
 * Test the LongMP
 * 
 * @author acaproni
 */
class TestHeteroIO extends FlatSpec {
  // The ID of the alarms built bin this test 
  val id = new Identifier(Some[String]("LongMPID"), None)
  val refreshRate=InOut.MinRefreshRate+10;
  
  behavior of "A heterogeneous IO" 
  
  it must "have an ID" in {
    val mp: InOut[Long] = new InOut(id,refreshRate,IASTypes.LONG)
    
    assert (!mp.actualValue.value.isDefined)
    assert(mp.mode == OperationalMode.UNKNOWN)
    assert(mp.validity == Validity.Unreliable)
  }
  
  it must "Have the same ID after changing other props" in {
    val mp: InOut[Long] = new InOut(id,refreshRate,IASTypes.LONG)
    
    // Change the value of the previous MP
    val mp2 = mp.updateValue(Some(3L))
    assert(mp2.id == mp.id)
    assert(mp2.actualValue.value.isDefined)
    assert(mp2.actualValue.value.get == 3L)
    // Trivial check of timestamp update
    assert(mp2.actualValue.timestamp > 0 && mp2.actualValue.timestamp<=System.currentTimeMillis() )
    assert(mp2.mode == OperationalMode.UNKNOWN)
    assert(mp2.validity == Validity.Unreliable)
    
    // Change validity of the previous MP
    val mp3 = mp2.updateValidity(Validity.Reliable)
    assert(mp3.id == mp.id)
    assert(mp3.actualValue.value.isDefined)
    assert(mp3.actualValue.value.get  == mp2.actualValue.value.get)
    assert(mp3.mode == mp2.mode)
    assert(mp3.validity == Validity.Reliable)
    
    // Change mode of the previous MP
    val mp4 = mp3.updateMode(OperationalMode.OPERATIONAL)
    assert(mp4.id == mp.id)
    assert(mp4.actualValue.value.isDefined)
    assert(mp4.actualValue.value.get  == mp3.actualValue.value.get)
    assert(mp4.mode == OperationalMode.OPERATIONAL)
    assert(mp4.validity == mp3.validity)
  }
  
  it must "allow to update the value" in {
    val mp: InOut[Long] = new InOut(id,refreshRate,IASTypes.LONG)
    val mpUpdatedValue = mp.updateValue(Some(5L))
    assert(mpUpdatedValue.actualValue.value.get==5L,"The values differ")    
  }
  
  it must "allow to update the validity" in {
    val mp: InOut[Long] = new InOut(id,refreshRate,IASTypes.LONG)
    val mpUpdatedValidityRelaible = mp.updateValidity(Validity.Reliable)
    assert(mpUpdatedValidityRelaible.validity==Validity.Reliable,"The validities differ")
    
    val mpUpdatedValidityUnRelaible = mp.updateValidity(Validity.Unreliable)
    assert(mpUpdatedValidityUnRelaible.validity==Validity.Unreliable,"The validities differ")
  }
  
  it must "allow to update the mode" in {
    val mp: InOut[Long] = InOut(id,refreshRate,IASTypes.LONG)
    val mpUpdatedMode= mp.updateMode(OperationalMode.OPERATIONAL)
    assert(mpUpdatedMode.mode==OperationalMode.OPERATIONAL,"The modes differ")
  }
  
  it must "allow to update the value and validity at once" in {
    val mp: InOut[Long] = InOut(id,refreshRate,IASTypes.LONG)
    val mpUpdated = mp.update(Some(15L),Validity.Reliable)
    assert(mpUpdated.actualValue.value.get==15L,"The values differ")
    assert(mpUpdated.validity==Validity.Reliable,"The validities differ")
  }
  
  it must "return the same object if values, validity or mode did not change" in {
    val mp: InOut[Long] = InOut(id,refreshRate,IASTypes.LONG)
    
    val upVal = mp.updateValue(Some(10L))
    assert(upVal.actualValue.value.get==10L,"The values differ")
    val upValAgain = upVal.updateValue(Some(10L))
    assert(upValAgain.actualValue.value.get==10L,"The value differ")
    assert(upVal eq upValAgain,"Unexpected new object after updating the value\n"+upVal.toString()+"\n"+upValAgain.toString())
    
    val upValidity = mp.updateValidity(Validity.Reliable)
    assert(upValidity.validity==Validity.Reliable,"The validity differ")
    val upValidityAgain = upValidity.updateValidity(Validity.Reliable)
    assert(upValidityAgain.validity==Validity.Reliable,"The validity differ")
    assert(upValidityAgain eq upValidity,"Unexpected new object after updating the validity")
    
    val upMode = mp.updateMode(OperationalMode.STARTUP)
    assert(upMode.mode==OperationalMode.STARTUP,"The mode differ")
    val upModeAgain = upMode.updateMode(OperationalMode.STARTUP)
    assert(upModeAgain.mode==OperationalMode.STARTUP,"The mode differ")
    assert(upMode eq upModeAgain,"Unexpected new object after updating the mode")
        
    val mpUpdated = mp.update(Some(15L),Validity.Unreliable)
    val mpUpdated2 = mpUpdated.update(Some(15L),Validity.Unreliable)
    assert(mpUpdated eq mpUpdated2,"Unexpected new object after updating value and validity")
    
  }
  
  it must "support all types" in {
    // Build a HIO of all supported types and update the value checking
    // for possible mismatch
    val hioLong:  InOut[Long] = InOut(id,refreshRate,IASTypes.LONG)
    val hioShort:  InOut[Short] = InOut(id,refreshRate,IASTypes.SHORT)
    val hioInt:  InOut[Int] = InOut(id,refreshRate,IASTypes.INT)
    val hioByte:  InOut[Byte] = InOut(id,refreshRate,IASTypes.BYTE)
    val hioDouble:  InOut[Double] = InOut(id,refreshRate,IASTypes.DOUBLE)
    val hioFloat:  InOut[Float] = InOut(id,refreshRate,IASTypes.FLOAT)
    val hioBool:  InOut[Boolean] = InOut(id,refreshRate,IASTypes.BOOLEAN)
    val hioChar:  InOut[Char] = InOut(id,refreshRate,IASTypes.CHAR)
    val hioString:  InOut[String] = InOut(id,refreshRate,IASTypes.STRING)
    val hioAlarm: InOut[AlarmValue] = InOut(id,refreshRate,IASTypes.ALARM)
    
    // Check if all the types has been instantiated
    val listOfHIOs = List(hioLong,hioShort, hioInt,hioByte,hioDouble,hioFloat, hioBool,hioChar,hioString,hioAlarm)
    assert(listOfHIOs.size==IASTypes.values().length)
    
    hioLong.updateValue(Some(-1L))
    hioShort.updateValue(Some(2.toShort))
    hioInt.updateValue(Some(13))
    hioByte.updateValue(Some(64.toByte))
    hioDouble.updateValue(Some(-1.9D))
    hioFloat.updateValue(Some(-1.3F))
    hioBool.updateValue(Some(false))
    hioChar.updateValue(Some('C'))
    hioString.updateValue(Some("Test"))
    hioAlarm.updateValue(Some(new AlarmValue(AlarmState.Active,true,AckState.New)))
  }
}