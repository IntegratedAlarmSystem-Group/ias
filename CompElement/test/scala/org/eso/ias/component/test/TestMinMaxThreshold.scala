package org.eso.ias.component.test

import org.scalatest.FlatSpec
import org.eso.ias.prototype.transfer.TransferFunctionSetting
import org.eso.ias.prototype.transfer.TransferFunctionLanguage
import org.eso.ias.prototype.input.Identifier
import org.eso.ias.prototype.input.InOut
import org.eso.ias.prototype.input.java.OperationalMode
import org.eso.ias.prototype.input.Validity
import org.eso.ias.prototype.input.java.IASTypes
import org.eso.ias.prototype.input.AlarmValue
import org.eso.ias.prototype.compele.ComputingElement
import scala.collection.mutable.{Map => MutableMap }
import java.util.concurrent.ScheduledThreadPoolExecutor
import org.eso.ias.prototype.input.AlarmState
import java.util.Properties
import org.eso.ias.prototype.transfer.impls.MinMaxThresholdTF
import org.eso.ias.prototype.transfer.impls.MinMaxThresholdTFJava
import org.eso.ias.prototype.transfer.ScalaTransfer
import org.eso.ias.prototype.transfer.JavaTransfer

class TestMinMaxThreshold extends FlatSpec {
  
  def withScalaTransferSetting(testCode: TransferFunctionSetting => Any) {
    val threadFactory = new TestThreadFactory()
    
    // The TF executor to test
    val scalaMinMaxTF = new TransferFunctionSetting(
        "org.eso.ias.prototype.transfer.impls.MinMaxThresholdTF",
        TransferFunctionLanguage.scala,
        threadFactory)
    try {
      testCode(scalaMinMaxTF)
    } finally {
      assert(threadFactory.numberOfAliveThreads()==0)
      assert(threadFactory.instantiatedThreads==2)
    }
  }
  
  def withJavaTransferSetting(testCode: TransferFunctionSetting => Any) {
    val threadFactory = new TestThreadFactory()
    
    // The TF executor to test
    val javaMinMaxTF = new TransferFunctionSetting(
        "org.eso.ias.prototype.transfer.impls.MinMaxThresholdTFJava",
        TransferFunctionLanguage.java,
        threadFactory)
    try {
      testCode(javaMinMaxTF)
    } finally {
      assert(threadFactory.numberOfAliveThreads()==0)
      assert(threadFactory.instantiatedThreads==2)
    }
  }
  
  def withScalaComp(testCode: (ComputingElement[AlarmValue], MutableMap[String, InOut[_]]) => Any) {
    val commons = new CommonCompBuilder(
        "TestMinMAxThreshold-DASU-ID",
        "TestMinMAxThreshold-ASCE-ID",
        "TestMinMAxThreshold-outputHioS-ID",
        IASTypes.ALARM,
        1,
        IASTypes.LONG)
    
    // Instantiate one ASCE with a scala TF implementation
    val scalaTFSetting =new TransferFunctionSetting(
        "org.eso.ias.prototype.transfer.impls.MinMaxThresholdTF",
        TransferFunctionLanguage.scala,
        commons.threadFactory)
    
    
    val props: Properties= new Properties()
    props.put(MinMaxThresholdTF.highOnPropName, "50")
    props.put(MinMaxThresholdTF.highOffPropName, "25")
    props.put(MinMaxThresholdTF.lowOffPropName, "-10")
    props.put(MinMaxThresholdTF.lowOnPropName, "-20")
    
    val scalaComp: ComputingElement[AlarmValue] = new ComputingElement[AlarmValue](
       commons.compID,
       commons.output.asInstanceOf[InOut[AlarmValue]],
       commons.requiredInputIDs,
       commons.inputsMPs,
       scalaTFSetting,
       Some[Properties](props)) with ScalaTransfer[AlarmValue]
    
    try {
      testCode(scalaComp,commons.inputsMPs)
    } finally {
      scalaComp.shutdown()
    }
  }
  
  def withJavaComp(testCode: (ComputingElement[AlarmValue], MutableMap[String, InOut[_]]) => Any) {
    val commons = new CommonCompBuilder(
        "TestMinMAxThreshold-DASU-ID",
        "TestMinMAxThreshold-ASCE-ID",
        "TestMinMAxThreshold-outputHioJ-ID",
        IASTypes.ALARM,
        1,
        IASTypes.LONG)
    
    // Instantiate one ASCE with a scala TF implementation
    val javaTFSetting =new TransferFunctionSetting(
        "org.eso.ias.prototype.transfer.impls.MinMaxThresholdTFJava",
        TransferFunctionLanguage.java,
        commons.threadFactory)
    
    
    val props: Properties= new Properties()
    props.put(MinMaxThresholdTFJava.highOnPropName, "50")
    props.put(MinMaxThresholdTFJava.highOffPropName, "25")
    props.put(MinMaxThresholdTFJava.lowOffPropName, "-10")
    props.put(MinMaxThresholdTFJava.lowOnPropName, "-20")
    
    val javaComp: ComputingElement[AlarmValue] = new ComputingElement[AlarmValue](
       commons.compID,
       commons.output.asInstanceOf[InOut[AlarmValue]],
       commons.requiredInputIDs,
       commons.inputsMPs,
       javaTFSetting,
       Some[Properties](props)) with JavaTransfer[AlarmValue]
    
    try {
      testCode(javaComp,commons.inputsMPs)
    } finally {
      javaComp.shutdown()
    }
  }
  
  behavior of "The scala MinMaxThreshold executor"
  
  it must "Correctly load, init and shutdown the TF executor" in withScalaTransferSetting { scalaMinMaxTF =>
    assert(!scalaMinMaxTF.initialized)
    assert(!scalaMinMaxTF.isShutDown)
    scalaMinMaxTF.initialize("ASCE-MinMaxTF-ID", "ASCE-running-ID", System.getProperties)
    Thread.sleep(500)
    assert(scalaMinMaxTF.initialized)
    assert(!scalaMinMaxTF.isShutDown)
    scalaMinMaxTF.shutdown()
    Thread.sleep(500)
    assert(scalaMinMaxTF.initialized)
    assert(scalaMinMaxTF.isShutDown)
  }
  
  /**
   * Check the state of the alarm of the passed HIO
   * 
   * @param hio: the HIO to check the alarm state
   * @param alarmState: The state of the alarm
   */
  def checkState(asce: ComputingElement[AlarmValue], alarmState: AlarmState.State): Boolean = {
    assert(asce.isAlarmComponent)
    val hio = asce.output
    assert(hio.iasType==IASTypes.ALARM)
    
    hio.actualValue.value.isDefined && 
    hio.actualValue.value.get.asInstanceOf[AlarmValue].alarmState==alarmState
    
  }
  
  it must "run the scala Min/Max TF executor" in withScalaComp { (scalaComp, inputsMPs) =>
    val stpe: ScheduledThreadPoolExecutor = new ScheduledThreadPoolExecutor(5)
    scalaComp.initialize(stpe)
    Thread.sleep(1000)
    // Change the input to trigger the TF
    val changedMP = inputsMPs(inputsMPs.keys.head).asInstanceOf[InOut[Long]].updateValue(Some(5L))
    scalaComp.inputChanged(Some(changedMP))
    Thread.sleep(2500)
    assert(checkState(scalaComp,AlarmState.Cleared))
    
    // Activate high
    scalaComp.inputChanged(Some(inputsMPs(inputsMPs.keys.head).asInstanceOf[InOut[Long]].updateValue(Some(100L))))
    Thread.sleep(2500)
    assert(checkState(scalaComp,AlarmState.Active))
    
    // Increase does not deactivate the alarm
    scalaComp.inputChanged(Some(inputsMPs(inputsMPs.keys.head).asInstanceOf[InOut[Long]].updateValue(Some(150L))))
    Thread.sleep(2500)
    assert(checkState(scalaComp,AlarmState.Active))
    
    // Decreasing without passing HighOn does not deactivate
    scalaComp.inputChanged(Some(inputsMPs(inputsMPs.keys.head).asInstanceOf[InOut[Long]].updateValue(Some(40L))))
    Thread.sleep(2500)
    assert(checkState(scalaComp,AlarmState.Active))
    
    // Below HighOff deactivate the alarm
    scalaComp.inputChanged(Some(inputsMPs(inputsMPs.keys.head).asInstanceOf[InOut[Long]].updateValue(Some(10L))))
    Thread.sleep(2500)
    assert(checkState(scalaComp,AlarmState.Cleared))
    
    // Below LowOff but not passing LowOn does not activate
    scalaComp.inputChanged(Some(inputsMPs(inputsMPs.keys.head).asInstanceOf[InOut[Long]].updateValue(Some(-15L))))
    Thread.sleep(2500)
    assert(checkState(scalaComp,AlarmState.Cleared))
    
    // Passing LowOn activate
    scalaComp.inputChanged(Some(inputsMPs(inputsMPs.keys.head).asInstanceOf[InOut[Long]].updateValue(Some(-30L))))
    Thread.sleep(2500)
    assert(checkState(scalaComp,AlarmState.Active))
    
    // Decreasing more remain activate
    scalaComp.inputChanged(Some(inputsMPs(inputsMPs.keys.head).asInstanceOf[InOut[Long]].updateValue(Some(-40L))))
    Thread.sleep(2500)
    assert(checkState(scalaComp,AlarmState.Active))
    
    // Increasing but not passing LowOff remains active
    scalaComp.inputChanged(Some(inputsMPs(inputsMPs.keys.head).asInstanceOf[InOut[Long]].updateValue(Some(-15L))))
    Thread.sleep(2500)
    assert(checkState(scalaComp,AlarmState.Active))
    
    // Passing LowOff deactivate
    scalaComp.inputChanged(Some(inputsMPs(inputsMPs.keys.head).asInstanceOf[InOut[Long]].updateValue(Some(0L))))
    Thread.sleep(2500)
    assert(checkState(scalaComp,AlarmState.Cleared))
  }
  
  behavior of "The java MinMaxThreshold executor"
  
  it must "Correctly load, init and shutdown the TF executor" in withJavaTransferSetting { javaMinMaxTF =>
    assert(!javaMinMaxTF.initialized)
    assert(!javaMinMaxTF.isShutDown)
    javaMinMaxTF.initialize("ASCE-MinMaxTF-ID", "ASCE-running-ID", System.getProperties)
    Thread.sleep(500)
    assert(javaMinMaxTF.initialized)
    assert(!javaMinMaxTF.isShutDown)
    javaMinMaxTF.shutdown()
    Thread.sleep(500)
    assert(javaMinMaxTF.initialized)
    assert(javaMinMaxTF.isShutDown)
  }
  
  it must "run the java Min/Max TF executor" in withJavaComp { (javaComp, inputsMPs) =>
    val stpe: ScheduledThreadPoolExecutor = new ScheduledThreadPoolExecutor(5)
    javaComp.initialize(stpe)
    Thread.sleep(1000)
    // Change the input to trigger the TF
    val changedMP = inputsMPs(inputsMPs.keys.head).asInstanceOf[InOut[Long]].updateValue(Some(5L))
    javaComp.inputChanged(Some(changedMP))
    Thread.sleep(2500)
    assert(checkState(javaComp,AlarmState.Cleared))
    
    // Activate high
    javaComp.inputChanged(Some(inputsMPs(inputsMPs.keys.head).asInstanceOf[InOut[Long]].updateValue(Some(100L))))
    Thread.sleep(2500)
    assert(checkState(javaComp,AlarmState.Active))
    
    // Increase does not deactivate the alarm
    javaComp.inputChanged(Some(inputsMPs(inputsMPs.keys.head).asInstanceOf[InOut[Long]].updateValue(Some(150L))))
    Thread.sleep(2500)
    assert(checkState(javaComp,AlarmState.Active))
    
    // Decreasing without passing HighOn does not deactivate
    javaComp.inputChanged(Some(inputsMPs(inputsMPs.keys.head).asInstanceOf[InOut[Long]].updateValue(Some(40L))))
    Thread.sleep(2500)
    assert(checkState(javaComp,AlarmState.Active))
    
    // Below HighOff deactivate the alarm
    javaComp.inputChanged(Some(inputsMPs(inputsMPs.keys.head).asInstanceOf[InOut[Long]].updateValue(Some(10L))))
    Thread.sleep(2500)
    assert(checkState(javaComp,AlarmState.Cleared))
    
    // Below LowOff but not passing LowOn does not activate
    javaComp.inputChanged(Some(inputsMPs(inputsMPs.keys.head).asInstanceOf[InOut[Long]].updateValue(Some(-15L))))
    Thread.sleep(2500)
    assert(checkState(javaComp,AlarmState.Cleared))
    
    // Passing LowOn activate
    javaComp.inputChanged(Some(inputsMPs(inputsMPs.keys.head).asInstanceOf[InOut[Long]].updateValue(Some(-30L))))
    Thread.sleep(2500)
    assert(checkState(javaComp,AlarmState.Active))
    
    // Decreasing more remain activate
    javaComp.inputChanged(Some(inputsMPs(inputsMPs.keys.head).asInstanceOf[InOut[Long]].updateValue(Some(-40L))))
    Thread.sleep(2500)
    assert(checkState(javaComp,AlarmState.Active))
    
    // Increasing but not passing LowOff remains active
    javaComp.inputChanged(Some(inputsMPs(inputsMPs.keys.head).asInstanceOf[InOut[Long]].updateValue(Some(-15L))))
    Thread.sleep(2500)
    assert(checkState(javaComp,AlarmState.Active))
    
    // Passing LowOff deactivate
    javaComp.inputChanged(Some(inputsMPs(inputsMPs.keys.head).asInstanceOf[InOut[Long]].updateValue(Some(0L))))
    Thread.sleep(2500)
    assert(checkState(javaComp,AlarmState.Cleared))
  }
  
}
