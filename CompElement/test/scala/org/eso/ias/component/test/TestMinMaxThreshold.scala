package org.eso.ias.component.test

import org.scalatest.FlatSpec
import org.eso.ias.prototype.transfer.TransferFunctionSetting
import org.eso.ias.prototype.transfer.TransferFunctionLanguage
import org.eso.ias.prototype.input.Identifier
import org.eso.ias.prototype.input.InOut
import org.eso.ias.plugin.OperationalMode
import org.eso.ias.prototype.input.Validity
import org.eso.ias.prototype.input.java.IASTypes
import org.eso.ias.prototype.compele.ComputingElement
import scala.collection.mutable.{Map => MutableMap }
import java.util.concurrent.ScheduledThreadPoolExecutor
import java.util.Properties
import org.eso.ias.prototype.transfer.impls.MinMaxThresholdTF
import org.eso.ias.prototype.transfer.impls.MinMaxThresholdTFJava
import org.eso.ias.prototype.transfer.ScalaTransfer
import org.eso.ias.prototype.transfer.JavaTransfer
import org.eso.ias.plugin.AlarmSample
import org.eso.ias.prototype.input.JavaConverter

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
  
  def withScalaComp(testCode: (ComputingElement[AlarmSample], Set[InOut[_]]) => Any) {
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
    
    val props = new Properties()
    props.put(MinMaxThresholdTF.highOnPropName, "50")
    props.put(MinMaxThresholdTF.highOffPropName, "25")
    props.put(MinMaxThresholdTF.lowOffPropName, "-10")
    props.put(MinMaxThresholdTF.lowOnPropName, "-20")
    
    val scalaComp: ComputingElement[AlarmSample] = new ComputingElement[AlarmSample](
       commons.compID,
       commons.output.asInstanceOf[InOut[AlarmSample]],
       commons.inputsMPs,
       scalaTFSetting,
       props) with ScalaTransfer[AlarmSample]
    
    try {
      testCode(scalaComp,commons.inputsMPs)
    } finally {
      scalaComp.shutdown()
    }
  }
  
  def withJavaComp(testCode: (ComputingElement[AlarmSample], Set[InOut[_]]) => Any) {
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
    
    
    val props = new Properties()
    props.put(MinMaxThresholdTFJava.highOnPropName,"50")
    props.put(MinMaxThresholdTFJava.highOffPropName, "25")
    props.put(MinMaxThresholdTFJava.lowOffPropName, "-10")
    props.put(MinMaxThresholdTFJava.lowOnPropName, "-20")
    
    val javaComp: ComputingElement[AlarmSample] = new ComputingElement[AlarmSample](
       commons.compID,
       commons.output.asInstanceOf[InOut[AlarmSample]],
       commons.inputsMPs,
       javaTFSetting,
       props) with JavaTransfer[AlarmSample]
    
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
    scalaMinMaxTF.initialize("ASCE-MinMaxTF-ID", "ASCE-running-ID", new Properties())
    Thread.sleep(500)
    assert(scalaMinMaxTF.initialized)
    assert(!scalaMinMaxTF.isShutDown)
    scalaMinMaxTF.shutdown()
    Thread.sleep(500)
    assert(scalaMinMaxTF.initialized)
    assert(scalaMinMaxTF.isShutDown)
  }
  
  /**
   * Check the state of the alarm of the passed IASIO
   * 
   * @param hio: the IASIO to check the alarm state
   * @param alarmState: The expected alarm
   */
  def checkAlarmActivation(asce: ComputingElement[AlarmSample], alarmState: AlarmSample): Boolean = {
    assert(asce.isOutputAnAlarm)
    val hio = asce.output
    assert(hio.iasType==IASTypes.ALARM)
    
    val res = hio.actualValue.value.map { av => av==alarmState }.orElse(Some(false))
    
    hio.actualValue.value.isDefined && 
    hio.actualValue.value.get.asInstanceOf[AlarmSample]==alarmState
    
  }
  
  it must "run the scala Min/Max TF executor" in withScalaComp { (scalaComp, inputsMPs) =>
    scalaComp.initialize()
    Thread.sleep(1000)
    // Change the input to trigger the TF
    val changedMP = inputsMPs.head.asInstanceOf[InOut[Long]].updateValue(Some(5L))
    scalaComp.update(Set(changedMP).map(mp => JavaConverter.inOutToIASValue(mp)))
    assert(checkAlarmActivation(scalaComp,AlarmSample.CLEARED))
    
    // Activate high
    scalaComp.update(List(inputsMPs.head.asInstanceOf[InOut[Long]].updateValue(Some(100L))))
    assert(checkAlarmActivation(scalaComp,AlarmSample.SET))
    
    // Increase does not deactivate the alarm
    scalaComp.update(List(inputsMPs.head.asInstanceOf[InOut[Long]].updateValue(Some(150L))))
    assert(checkAlarmActivation(scalaComp,AlarmSample.SET))
    
    // Decreasing without passing HighOn does not deactivate
    scalaComp.update(List(inputsMPs.head.asInstanceOf[InOut[Long]].updateValue(Some(40L))))
    assert(checkAlarmActivation(scalaComp,AlarmSample.SET))
    
    // Below HighOff deactivate the alarm
    scalaComp.update(List(inputsMPs.head.asInstanceOf[InOut[Long]].updateValue(Some(10L))))
    assert(checkAlarmActivation(scalaComp,AlarmSample.CLEARED))
    
    // Below LowOff but not passing LowOn does not activate
    scalaComp.update(List(inputsMPs.head.asInstanceOf[InOut[Long]].updateValue(Some(-15L))))
    assert(checkAlarmActivation(scalaComp,AlarmSample.CLEARED))
    
    // Passing LowOn activate
    scalaComp.update(List(inputsMPs.head.asInstanceOf[InOut[Long]].updateValue(Some(-30L))))
    assert(checkAlarmActivation(scalaComp,AlarmSample.SET))
    
    // Decreasing more remain activate
    scalaComp.update(List(inputsMPs.head.asInstanceOf[InOut[Long]].updateValue(Some(-40L))))
    assert(checkAlarmActivation(scalaComp,AlarmSample.SET))
    
    // Increasing but not passing LowOff remains active
    scalaComp.update(List(inputsMPs.head.asInstanceOf[InOut[Long]].updateValue(Some(-15L))))
    assert(checkAlarmActivation(scalaComp,AlarmSample.SET))
    
    // Passing LowOff deactivate
    scalaComp.update(List(inputsMPs.head.asInstanceOf[InOut[Long]].updateValue(Some(0L))))
    assert(checkAlarmActivation(scalaComp,AlarmSample.CLEARED))
  }
  
  behavior of "The java MinMaxThreshold executor"
  
  it must "Correctly load, init and shutdown the TF executor" in withJavaTransferSetting { javaMinMaxTF =>
    assert(!javaMinMaxTF.initialized)
    assert(!javaMinMaxTF.isShutDown)
    javaMinMaxTF.initialize("ASCE-MinMaxTF-ID", "ASCE-running-ID", new Properties())
    Thread.sleep(500)
    assert(javaMinMaxTF.initialized)
    assert(!javaMinMaxTF.isShutDown)
    javaMinMaxTF.shutdown()
    Thread.sleep(500)
    assert(javaMinMaxTF.initialized)
    assert(javaMinMaxTF.isShutDown)
  }
  
  it must "run the java Min/Max TF executor" in withJavaComp { (javaComp, inputsMPs) =>
    javaComp.initialize()
    Thread.sleep(1000)
    // Change the input to trigger the TF
    val changedMP = inputsMPs.head.asInstanceOf[InOut[Long]].updateValue(Some(5L))
    javaComp.update(List(changedMP))
    assert(checkAlarmActivation(javaComp,AlarmSample.CLEARED))
    
    // Activate high
    javaComp.update(List(inputsMPs.head.asInstanceOf[InOut[Long]].updateValue(Some(100L))))
    assert(checkAlarmActivation(javaComp,AlarmSample.SET))
    
    // Increase does not deactivate the alarm
    javaComp.update(List(inputsMPs.head.asInstanceOf[InOut[Long]].updateValue(Some(150L))))
    assert(checkAlarmActivation(javaComp,AlarmSample.SET))
    
    // Decreasing without passing HighOn does not deactivate
    javaComp.update(List(inputsMPs.head.asInstanceOf[InOut[Long]].updateValue(Some(40L))))
    assert(checkAlarmActivation(javaComp,AlarmSample.SET))
    
    // Below HighOff deactivate the alarm
    javaComp.update(List(inputsMPs.head.asInstanceOf[InOut[Long]].updateValue(Some(10L))))
    assert(checkAlarmActivation(javaComp,AlarmSample.CLEARED))
    
    // Below LowOff but not passing LowOn does not activate
    javaComp.update(List(inputsMPs.head.asInstanceOf[InOut[Long]].updateValue(Some(-15L))))
    assert(checkAlarmActivation(javaComp,AlarmSample.CLEARED))
    
    // Passing LowOn activate
    javaComp.update(List(inputsMPs.head.asInstanceOf[InOut[Long]].updateValue(Some(-30L))))
    assert(checkAlarmActivation(javaComp,AlarmSample.SET))
    
    // Decreasing more remain activate
    javaComp.update(List(inputsMPs.head.asInstanceOf[InOut[Long]].updateValue(Some(-40L))))
    assert(checkAlarmActivation(javaComp,AlarmSample.SET))
    
    // Increasing but not passing LowOff remains active
    javaComp.update(List(inputsMPs.head.asInstanceOf[InOut[Long]].updateValue(Some(-15L))))
    assert(checkAlarmActivation(javaComp,AlarmSample.SET))
    
    // Passing LowOff deactivate
    javaComp.update(List(inputsMPs.head.asInstanceOf[InOut[Long]].updateValue(Some(0L))))
    assert(checkAlarmActivation(javaComp,AlarmSample.CLEARED))
  }
  
}
