package org.eso.ias.dasu.test

import org.scalatest.FlatSpec
import org.scalatest.BeforeAndAfter
import org.eso.ias.dasu.DasuImpl
import org.eso.ias.types.IASValue
import org.ias.logging.IASLogger

/**
 * Checks the timestamps of the output produced by a DASU
 * when inputs change and when the auto-refresh is in place.
 * 
 * The test uses the DasuWithOneASCE DASU defined in the CDB
 * by submitting inputs and checking the fields of output
 * published (or not published) by the DASU.
 * 
 * @see  [[https://github.com/IntegratedAlarmSystem-Group/ias/issues/52 Issue #52 on github]]
 */
class CheckDasuOutputTimestamps extends FlatSpec with BeforeAndAfter {
  
  /** The logger */
  val logger = IASLogger.getLogger(this.getClass);
  
  val autoRefreshTime = 3
  val tolerance = 3
  
  val f = new DasuOneAsceCommon(autoRefreshTime,tolerance)
  
  before {
    f.outputValuesReceived.clear()
    f.outputStringsReceived.clear()
    f.dasu = f.buildDasu()
    f.dasu.get.start()
  }
  
  after {
    f.dasu.get.cleanUp()
    f.dasu = None
    f.outputValuesReceived.clear()
    f.outputStringsReceived.clear()
  }
  
  behavior of "The auto-resend of the last output of the DASU"
  
  it must "not refresh the output before getting the input" in {
    f.dasu.get.enableAutoRefreshOfOutput(true)
    // Leave the DASU time to send the last computed output
    Thread.sleep(5*autoRefreshTime)
    assert(f.outputStringsReceived.isEmpty)
    assert(f.outputValuesReceived.isEmpty)
  }
  
  it must "refresh the output once it has been generated" in {
    f.dasu.get.enableAutoRefreshOfOutput(true)
    val inputs: Set[IASValue[_]] = Set(f.buildValue(0))
    f.inputsProvider.sendInputs(inputs)
    
    // Leave the DASU time to send the last computed output
    Thread.sleep(5*autoRefreshTime)
    
    assert(f.outputStringsReceived.size>=5)
    assert(f.outputValuesReceived.size>=5)
  }
  
  it must "enable/disable the auto-refresh" in {
    f.dasu.get.enableAutoRefreshOfOutput(true)
    val inputs: Set[IASValue[_]] = Set(f.buildValue(0))
    f.inputsProvider.sendInputs(inputs)
    
    // Leave the DASU time to send the last computed output
    Thread.sleep(5*autoRefreshTime)
    
    assert(f.outputStringsReceived.size>=5)
    assert(f.outputValuesReceived.size>=5)
    
    f.dasu.get.enableAutoRefreshOfOutput(false)
    f.outputStringsReceived.clear()
    f.outputValuesReceived.clear()
    
    // Leave the DASU time to send the last computed output
    Thread.sleep(3*autoRefreshTime)
    assert(f.outputStringsReceived.isEmpty)
    assert(f.outputValuesReceived.isEmpty)
    
    f.dasu.get.enableAutoRefreshOfOutput(true)
    Thread.sleep(3*autoRefreshTime)
    assert(!f.outputStringsReceived.isEmpty)
    assert(!f.outputValuesReceived.isEmpty)
    
    f.dasu.get.enableAutoRefreshOfOutput(false)
    f.outputStringsReceived.clear()
    f.outputValuesReceived.clear()
    
    // Leave the DASU time to send the last computed output
    Thread.sleep(3*autoRefreshTime)
    assert(f.outputStringsReceived.isEmpty)
    assert(f.outputValuesReceived.isEmpty)
  }
  
  behavior of "The timestamp of the output"
  
  it must "be updated when the value of the output changes" in {
    f.dasu.get.enableAutoRefreshOfOutput(false)
    val inputs: Set[IASValue[_]] = Set(f.buildValue(0)) // CLEARED
    f.inputsProvider.sendInputs(inputs)
    
    Thread.sleep(2*f.dasu.get.throttling)
    val inputs2: Set[IASValue[_]] = Set(f.buildValue(100)) // SET
    f.inputsProvider.sendInputs(inputs2)
    
    Thread.sleep(2*f.dasu.get.throttling)
    val inputs3: Set[IASValue[_]] = Set(f.buildValue(10)) // CLEARED
    f.inputsProvider.sendInputs(inputs3)
    
    Thread.sleep(2*f.dasu.get.throttling)
    assert(f.outputValuesReceived.size==3)
    assert(f.outputStringsReceived.size==3)
    
    val out1= f.outputValuesReceived(0)
    val out2= f.outputValuesReceived(1)
    val out3= f.outputValuesReceived(2)
    
    assert(out1.valueType==out2.valueType && out1.valueType==out3.valueType)
    assert(out1.id==out2.id && out1.id==out3.id)
    assert(out1.mode==out2.mode && out1.mode==out3.mode)
    assert(out1.iasValidity==out2.iasValidity && out1.iasValidity==out3.iasValidity)
    assert(out1.fullRunningId==out2.fullRunningId && out1.fullRunningId==out3.fullRunningId)
    assert(out1.value==out3.value)
    assert(out1.value!=out2.value)
    assert(out1.sentToBsdbTStamp.get<out2.sentToBsdbTStamp.get)
    assert(out2.sentToBsdbTStamp.get<out3.sentToBsdbTStamp.get)
  }
  
  it must "be updated when the value of the output does not change" in {
    
    val inputs: Set[IASValue[_]] = Set(f.buildValue(0)) // CLEARED
    f.inputsProvider.sendInputs(inputs)
    
    f.dasu.get.enableAutoRefreshOfOutput(true)
    
    
    Thread.sleep(2*f.dasu.get.autoSendTimeIntervalMillis+1000)
    val inputs2: Set[IASValue[_]] = Set(f.buildValue(20)) // Again cleared
    f.inputsProvider.sendInputs(inputs2)
    
    Thread.sleep(2*f.dasu.get.throttling)
    assert(f.outputValuesReceived.size==2)
    assert(f.outputStringsReceived.size==2)
    
    val out1= f.outputValuesReceived(0)
    val out2= f.outputValuesReceived(1)
    
    assert(out1.valueType==out2.valueType)
    assert(out1.id==out2.id)
    assert(out1.mode==out2.mode)
    assert(out1.iasValidity==out2.iasValidity)
    assert(out1.fullRunningId==out2.fullRunningId)
    assert(out1.value==out2.value)
    assert(out1.sentToBsdbTStamp.get<out2.sentToBsdbTStamp.get)
  }
  
  it must "re-send the same output" in {
    val inputs: Set[IASValue[_]] = Set(f.buildValue(0))
    f.inputsProvider.sendInputs(inputs)
    
    f.dasu.get.enableAutoRefreshOfOutput(true)
    
    
    // Leave the DASU time to send the last computed output
    Thread.sleep(5000*autoRefreshTime+1000)
    
    assert(f.outputStringsReceived.size>=5)
    assert(f.outputValuesReceived.size>=5)
    
    f.dasu.get.enableAutoRefreshOfOutput(false)
    
    f.outputStringsReceived.foreach( s =>
      logger.info("String received [{}]",s))
    
    val strOutput = f.outputStringsReceived.head
    assert(f.outputStringsReceived.forall(s => s==strOutput))
     
    val firstValue =  f.outputValuesReceived.head
    assert(f.outputValuesReceived.forall(iasVal => iasVal.value==firstValue.value))
    //assert(f.outputValuesReceived.forall(iasVal => iasVal.timestamp==firstValue.timestamp))
    assert(f.outputValuesReceived.forall(iasVal => iasVal.mode==firstValue.mode))
    assert(f.outputValuesReceived.forall(iasVal => iasVal.iasValidity==firstValue.iasValidity))
    assert(f.outputValuesReceived.forall(iasVal => iasVal.id==firstValue.id))
    assert(f.outputValuesReceived.forall(iasVal => iasVal.fullRunningId==firstValue.fullRunningId))
    assert(f.outputValuesReceived.forall(iasVal => iasVal.valueType==firstValue.valueType))
     
  }
  
}
