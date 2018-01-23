package org.eso.ias.basictypes.test

import org.scalatest.FlatSpec
import org.eso.ias.prototype.input.Validity
import org.eso.ias.prototype.input.Identifier
import org.eso.ias.prototype.input.InOut
import org.eso.ias.prototype.input.java.IASTypes
import org.eso.ias.prototype.input.java.IdentifierType
import org.eso.ias.prototype.input.java.IASValue
import org.eso.ias.prototype.input.java.IasLong
import org.eso.ias.prototype.input.java.IasValidity._
import org.eso.ias.prototype.input.java.OperationalMode
import org.eso.ias.prototype.input.java.AlarmSample

/**
 * Test the LongMP
 * 
 * @author acaproni
 */
class TestHeteroIO extends FlatSpec {
  val supervId = new Identifier("SupervId",IdentifierType.SUPERVISOR,None)
  val dasuId =  new Identifier("DasuId",IdentifierType.DASU,supervId)
  val asceId =  new Identifier("AsceId",IdentifierType.ASCE,Some(dasuId))
  
  // The ID of the alarms built in this test
  //
  // This test is all about the conversion
  val id = new Identifier("LongMPID", IdentifierType.IASIO,Some(asceId))
  val refreshRate=InOut.MinRefreshRate+10;
  
  behavior of "A heterogeneous IO" 
  
  it must "have an ID" in {
    val mp: InOut[Long] = InOut(id,refreshRate,IASTypes.LONG)
    
    assert (!mp.value.isDefined)
    assert(mp.mode == OperationalMode.UNKNOWN)
    
    println("mpValidity="+mp.validity.iasValidity);
    
    assert(mp.validity == Validity(UNRELIABLE))
  }
  
  it must "Have the same ID after changing other props" in {
    val mp: InOut[Long] = InOut(id,refreshRate,IASTypes.LONG)
    
    // Change the value of the previous MP
    val mp2 = mp.updateValue(Some(3L))
    assert(mp2.id == mp.id)
    assert(mp2.value.isDefined)
    assert(mp2.value.get == 3L)
    // Trivial check of timestamp update
    assert(mp2.timestamp > 0 && mp2.timestamp<=System.currentTimeMillis() )
    assert(mp2.mode == OperationalMode.UNKNOWN)
    assert(mp2.validity == Validity(UNRELIABLE))
    
    // Change validity of the previous MP
    val mp3 = mp2.updateValidity(Validity(RELIABLE))
    assert(mp3.id == mp.id)
    assert(mp3.value.isDefined)
    assert(mp3.value.get  == mp2.value.get)
    assert(mp3.mode == mp2.mode)
    assert(mp3.validity == Validity(RELIABLE))
    
    // Change mode of the previous MP
    val mp4 = mp3.updateMode(OperationalMode.OPERATIONAL)
    assert(mp4.id == mp.id)
    assert(mp4.value.isDefined)
    assert(mp4.value.get  == mp3.value.get)
    assert(mp4.mode == OperationalMode.OPERATIONAL)
    assert(mp4.validity == mp3.validity)
  }
  
  it must "allow to update the value" in {
    val mp: InOut[Long] = InOut(id,refreshRate,IASTypes.LONG)
    val mpUpdatedValue = mp.updateValue(Some(5L))
    assert(mpUpdatedValue.value.get==5L,"The values differ")    
  }
  
  it must "allow to update the validity" in {
    val mp: InOut[Long] = InOut(id,refreshRate,IASTypes.LONG)
    val mpUpdatedValidityRelaible = mp.updateValidity(Validity(RELIABLE))
    assert(mpUpdatedValidityRelaible.validity==Validity(RELIABLE),"The validities differ")
    
    val mpUpdatedValidityUnRelaible = mp.updateValidity(Validity(UNRELIABLE))
    assert(mpUpdatedValidityUnRelaible.validity==Validity(UNRELIABLE),"The validities differ")
  }
  
  it must "allow to update the mode" in {
    val mp: InOut[Long] = InOut(id,refreshRate,IASTypes.LONG)
    val mpUpdatedMode= mp.updateMode(OperationalMode.OPERATIONAL)
    assert(mpUpdatedMode.mode==OperationalMode.OPERATIONAL,"The modes differ")
  }
  
  it must "allow to update the value and validity at once" in {
    val mp: InOut[Long] = InOut(id,refreshRate,IASTypes.LONG)
    val mpUpdated = mp.update(Some(15L),Validity(RELIABLE))
    assert(mpUpdated.value.get==15L,"The values differ")
    assert(mpUpdated.validity==Validity(RELIABLE),"The validities differ")
  }
  
  it must "always update the tiemstamp when updatingvalue, validity or mode" in {
    val mp: InOut[Long] = InOut(id,refreshRate,IASTypes.LONG)
    
    val upVal = mp.updateValue(Some(10L))
    assert(upVal.value.get==10L,"The values differ")
    Thread.sleep(5) // be sure to update with another timestamp
    val upValAgain = upVal.updateValue(Some(10L))
    assert(upValAgain.value.get==10L,"The value differ")
    assert(upVal.timestamp!=upValAgain.timestamp,"Timestamps not updated")
    
    val upValidity = mp.updateValidity(Validity(RELIABLE))
    assert(upValidity.validity==Validity(RELIABLE),"The validity differ")
    Thread.sleep(5) // be sure to update with another timestamp
    val upValidityAgain = upValidity.updateValidity(Validity(RELIABLE))
    assert(upValidityAgain.validity==Validity(RELIABLE),"The validity differ")
    assert(upValidityAgain.timestamp!=upValidity.timestamp,"Timestamps not updated")
    
    val upMode = mp.updateMode(OperationalMode.STARTUP)
    assert(upMode.mode==OperationalMode.STARTUP,"The mode differ")
    Thread.sleep(5) // be sure to update with another timestamp
    val upModeAgain = upMode.updateMode(OperationalMode.STARTUP)
    assert(upModeAgain.mode==OperationalMode.STARTUP,"The mode differ")
    assert(upMode.timestamp!=upModeAgain.timestamp,"Timestamp not updated")
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
    val hioAlarm: InOut[AlarmSample] = InOut(id,refreshRate,IASTypes.ALARM)
    
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
    hioAlarm.updateValue(Some(AlarmSample.SET))
  }
  
  it must "build and update from a passed IASValues" in {
    
    val monitoredSysId= new Identifier("MonSysId",IdentifierType.MONITORED_SOFTWARE_SYSTEM,None)
    val puginId = new Identifier("PluginId",IdentifierType.PLUGIN,Option(monitoredSysId))
    val converterId = new Identifier("ConverterId",IdentifierType.CONVERTER,Option(puginId))
    val iasioId = new Identifier("IasioId",IdentifierType.IASIO,Option(converterId))
    
    // Build the IASIO from the passed IASValue
    val iasValue = new IasLong(821L,System.currentTimeMillis(),OperationalMode.INTIALIZATION,RELIABLE,iasioId.fullRunningID)
    val inOut = InOut(iasValue,3000)
    
    assert(inOut.iasType==iasValue.valueType)
    assert(inOut.value.isDefined)
    assert(inOut.value.get.asInstanceOf[Long]==iasValue.value.asInstanceOf[Long])
    assert(inOut.mode==iasValue.mode)
    
    // Update a IASIO with no value with a passed IASIO
    val iasio: InOut[_] = InOut(iasioId,5500,IASTypes.LONG)
    val newiasIo = iasio.update(iasValue)
    assert(newiasIo.iasType==iasValue.valueType)
    assert(newiasIo.value.isDefined)
    assert(newiasIo.value.get.asInstanceOf[Long]==iasValue.value.asInstanceOf[Long])
    assert(newiasIo.mode==iasValue.mode)
    
    // Update with another value
    val iasValue2 = new IasLong(113142L,System.currentTimeMillis(),OperationalMode.OPERATIONAL,UNRELIABLE,iasio.id.fullRunningID)
    val newiasIo2 = iasio.update(iasValue2)
    assert(newiasIo2.iasType==iasValue2.valueType)
    assert(newiasIo2.value.isDefined)
    assert(newiasIo2.value.get==iasValue2.value)
    assert(newiasIo2.mode==iasValue2.mode)
    assert(newiasIo2.validity.iasValidity==iasValue2.iasValidity)
  }
  

}