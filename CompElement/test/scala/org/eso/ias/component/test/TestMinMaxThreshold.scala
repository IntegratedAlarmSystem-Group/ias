package org.eso.ias.component.test

import org.scalatest.FlatSpec
import org.eso.ias.asce.transfer.TransferFunctionSetting
import org.eso.ias.asce.transfer.TransferFunctionLanguage
import org.eso.ias.types.Identifier
import org.eso.ias.types.InOut
import org.eso.ias.types.OperationalMode
import org.eso.ias.types.Validity
import org.eso.ias.types.IASTypes
import org.eso.ias.asce.ComputingElement
import scala.collection.mutable.{Map => MutableMap }
import java.util.concurrent.ScheduledThreadPoolExecutor
import java.util.Properties
import org.eso.ias.asce.transfer.impls.MinMaxThresholdTF
import org.eso.ias.asce.transfer.impls.MinMaxThresholdTFJava
import org.eso.ias.asce.transfer.ScalaTransfer
import org.eso.ias.asce.transfer.JavaTransfer
import org.eso.ias.types.Alarm
import org.eso.ias.types.IASValue

class TestMinMaxThreshold extends FlatSpec {
  
  // The threshold to assess the validity from the arrival time of the input
  val validityThresholdInSecs = 2
  
  def withScalaTransferSetting(testCode: TransferFunctionSetting => Any) {
    val threadFactory = new TestThreadFactory()
    
    // The TF executor to test
    val scalaMinMaxTF = new TransferFunctionSetting(
        "org.eso.ias.asce.transfer.impls.MinMaxThresholdTF",
        TransferFunctionLanguage.scala,
        None,
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
        "org.eso.ias.asce.transfer.impls.MinMaxThresholdTFJava",
        TransferFunctionLanguage.java,
        None,
        threadFactory)
    try {
      testCode(javaMinMaxTF)
    } finally {
      assert(threadFactory.numberOfAliveThreads()==0)
      assert(threadFactory.instantiatedThreads==2)
    }
  }
  
  def withScalaComp(testCode: (ComputingElement[Alarm], Set[InOut[_]]) => Any) {
    val commons = new CommonCompBuilder(
        "TestMinMAxThreshold-DASU-ID",
        "TestMinMAxThreshold-ASCE-ID",
        "TestMinMAxThreshold-outputHioS-ID",
        IASTypes.ALARM,
        1,
        IASTypes.LONG)
    
    // Instantiate one ASCE with a scala TF implementation
    val scalaTFSetting =new TransferFunctionSetting(
        "org.eso.ias.asce.transfer.impls.MinMaxThresholdTF",
        TransferFunctionLanguage.scala,
        None,
        commons.threadFactory)
    
    val props = new Properties()
    props.put(MinMaxThresholdTF.highOnPropName, "50")
    props.put(MinMaxThresholdTF.highOffPropName, "25")
    props.put(MinMaxThresholdTF.lowOffPropName, "-10")
    props.put(MinMaxThresholdTF.lowOnPropName, "-20")
    props.put(MinMaxThresholdTF.alarmPriorityPropName,Alarm.SET_CRITICAL.toString())
    
    val scalaComp: ComputingElement[Alarm] = new ComputingElement[Alarm](
       commons.compID,
       commons.output.asInstanceOf[InOut[Alarm]],
       commons.inputsMPs,
       scalaTFSetting,
       validityThresholdInSecs,
       props) with ScalaTransfer[Alarm]
    
    try {
      testCode(scalaComp,commons.inputsMPs)
    } finally {
      scalaComp.shutdown()
    }
  }
  
  def withJavaComp(testCode: (ComputingElement[Alarm], Set[InOut[_]]) => Any) {
    val commons = new CommonCompBuilder(
        "TestMinMAxThreshold-DASU-ID",
        "TestMinMAxThreshold-ASCE-ID",
        "TestMinMAxThreshold-outputHioJ-ID",
        IASTypes.ALARM,
        1,
        IASTypes.LONG)
    
    // Instantiate one ASCE with a scala TF implementation
    val javaTFSetting =new TransferFunctionSetting(
        "org.eso.ias.asce.transfer.impls.MinMaxThresholdTFJava",
        TransferFunctionLanguage.java,
        None,
        commons.threadFactory)
    
    
    val props = new Properties()
    props.put(MinMaxThresholdTFJava.highOnPropName,"50")
    props.put(MinMaxThresholdTFJava.highOffPropName, "25")
    props.put(MinMaxThresholdTFJava.lowOffPropName, "-10")
    props.put(MinMaxThresholdTFJava.lowOnPropName, "-20")
    props.put(MinMaxThresholdTF.alarmPriorityPropName,Alarm.SET_HIGH.toString())
    
    val javaComp: ComputingElement[Alarm] = new ComputingElement[Alarm](
       commons.compID,
       commons.output.asInstanceOf[InOut[Alarm]],
       commons.inputsMPs,
       javaTFSetting,
       validityThresholdInSecs,
       props) with JavaTransfer[Alarm]
    
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
    scalaMinMaxTF.initialize("ASCE-MinMaxTF-ID", "ASCE-running-ID", 1000, new Properties())
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
  def checkAlarmActivation(asce: ComputingElement[Alarm], alarmState: Alarm): Boolean = {
    assert(asce.isOutputAnAlarm)
    val iasio = asce.output
    assert(iasio.iasType==IASTypes.ALARM)
    
    iasio.value.forall(a => a==alarmState)
  }
  
  
  
  it must "run the scala Min/Max TF executor" in withScalaComp { (scalaComp, inputsMPs) =>
    scalaComp.initialize()
    Thread.sleep(1000)
    // Change the input to trigger the TF
    val changedMP = inputsMPs.head.asInstanceOf[InOut[Long]].updateValue(Some(5L)).updateDasuProdTStamp(System.currentTimeMillis())
    scalaComp.update(Set(changedMP.toIASValue()))
    assert(checkAlarmActivation(scalaComp,Alarm.CLEARED))
    
    // Activate high
    val highMp = changedMP.updateValue(Some(100L)).updateDasuProdTStamp(System.currentTimeMillis())
    scalaComp.update(Set(highMp.toIASValue()))
    assert(checkAlarmActivation(scalaComp,Alarm.SET_CRITICAL))
    
    // Is the property set with the value that triggered the alarm?
    assert(scalaComp.output.props.isDefined)
    assert(scalaComp.output.props.get.keys.toList.contains("actualValue"))
    val propValueMap=scalaComp.output.props.get
    assert(propValueMap("actualValue")==100L.toDouble.toString())
    
    
    // Increase does not deactivate the alarm
    val moreHigh=highMp.updateValue(Some(150L)).updateDasuProdTStamp(System.currentTimeMillis())
    scalaComp.update(Set(moreHigh.toIASValue()))
    assert(checkAlarmActivation(scalaComp,Alarm.SET_CRITICAL))
    
    val propValueMap2=scalaComp.output.props.get
    assert(propValueMap2("actualValue")==150L.toDouble.toString())
    
    // Decreasing without passing HighOn does not deactivate
    val noDeact = moreHigh.updateValue(Some(40L)).updateDasuProdTStamp(System.currentTimeMillis())
    scalaComp.update(Set(noDeact.toIASValue()))
    assert(checkAlarmActivation(scalaComp,Alarm.SET_CRITICAL))
    
    // Below HighOff deactivate the alarm
    val deact = noDeact.updateValue(Some(10L)).updateDasuProdTStamp(System.currentTimeMillis())
    scalaComp.update(Set(deact.toIASValue()))
    assert(checkAlarmActivation(scalaComp,Alarm.CLEARED))
    
    val propValueMap3=scalaComp.output.props.get
    assert(propValueMap3("actualValue")==10L.toDouble.toString())
    
    // Below LowOff but not passing LowOn does not activate
    val aBitLower = deact.updateValue(Some(-15L)).updateDasuProdTStamp(System.currentTimeMillis())
    scalaComp.update(Set(aBitLower.toIASValue()))
    assert(checkAlarmActivation(scalaComp,Alarm.CLEARED))
    
    // Passing LowOn activate
    val actLow = aBitLower.updateValue(Some(-30L)).updateDasuProdTStamp(System.currentTimeMillis())
    scalaComp.update(Set(actLow.toIASValue()))
    assert(checkAlarmActivation(scalaComp,Alarm.SET_CRITICAL))
    
    // Decreasing more remain activate
    val evenLower = actLow.updateValue(Some(-40L)).updateDasuProdTStamp(System.currentTimeMillis())
    scalaComp.update(Set(evenLower.toIASValue()))
    assert(checkAlarmActivation(scalaComp,Alarm.SET_CRITICAL))
    
    // Increasing but not passing LowOff remains active
    val aBitHigher = evenLower.updateValue(Some(-15L)).updateDasuProdTStamp(System.currentTimeMillis())
    scalaComp.update(Set(aBitHigher.toIASValue()))
    assert(checkAlarmActivation(scalaComp,Alarm.SET_CRITICAL))
    
    // Passing LowOff deactivate
    val deactFromLow = aBitHigher.updateValue(Some(0L)).updateDasuProdTStamp(System.currentTimeMillis())
    scalaComp.update(Set(deactFromLow.toIASValue()))
    assert(checkAlarmActivation(scalaComp,Alarm.CLEARED))
  }
  
  behavior of "The java MinMaxThreshold executor"
  
  it must "Correctly load, init and shutdown the TF executor" in withJavaTransferSetting { javaMinMaxTF =>
    assert(!javaMinMaxTF.initialized)
    assert(!javaMinMaxTF.isShutDown)
    javaMinMaxTF.initialize("ASCE-MinMaxTF-ID", "ASCE-running-ID", 1000, new Properties())
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
    val changedMP = inputsMPs.head.asInstanceOf[InOut[Long]].updateValue(Some(5L)).updateDasuProdTStamp(System.currentTimeMillis())
    javaComp.update(Set(changedMP.toIASValue()))

    assert(checkAlarmActivation(javaComp,Alarm.CLEARED))
    
    // Activate high
    val highMp = changedMP.updateValue(Some(100L)).updateDasuProdTStamp(System.currentTimeMillis())
    javaComp.update(Set(highMp.toIASValue()))
    println("===> "+javaComp.output.value.get)
    assert(checkAlarmActivation(javaComp,Alarm.SET_HIGH))
    
    // Is the property set with the value that triggered the alarm?
    assert(javaComp.output.props.isDefined)
    assert(javaComp.output.props.get.keys.toList.contains("actualValue"))
    val propValueMap=javaComp.output.props.get
    assert(propValueMap("actualValue")==100L.toDouble.toString())
    
    // Increase does not deactivate the alarm
    val moreHigh=highMp.updateValue(Some(150L)).updateDasuProdTStamp(System.currentTimeMillis())
    javaComp.update(Set(moreHigh.toIASValue()))
    assert(checkAlarmActivation(javaComp,Alarm.SET_HIGH))
    
    val propValueMap2=javaComp.output.props.get
    assert(propValueMap2("actualValue")==150L.toDouble.toString())
    
    // Decreasing without passing HighOn does not deactivate
    val noDeact = moreHigh.updateValue(Some(40L)).updateDasuProdTStamp(System.currentTimeMillis())
    javaComp.update(Set(noDeact.toIASValue()))
    assert(checkAlarmActivation(javaComp,Alarm.SET_HIGH))
    
    // Below HighOff deactivate the alarm
    val deact = noDeact.updateValue(Some(10L)).updateDasuProdTStamp(System.currentTimeMillis())
    javaComp.update(Set(deact.toIASValue()))
    assert(checkAlarmActivation(javaComp,Alarm.CLEARED))
    
    val propValueMap3=javaComp.output.props.get
    assert(propValueMap3("actualValue")==10L.toDouble.toString())
    
    // Below LowOff but not passing LowOn does not activate
    val aBitLower = deact.updateValue(Some(-15L)).updateDasuProdTStamp(System.currentTimeMillis())
    javaComp.update(Set(aBitLower.toIASValue()))
    assert(checkAlarmActivation(javaComp,Alarm.CLEARED))
    
    // Passing LowOn activate
    val actLow = aBitLower.updateValue(Some(-30L)).updateDasuProdTStamp(System.currentTimeMillis())
    javaComp.update(Set(actLow.toIASValue()))
    assert(checkAlarmActivation(javaComp,Alarm.SET_HIGH))
    
    // Decreasing more remain activate
    val evenLower = actLow.updateValue(Some(-40L)).updateDasuProdTStamp(System.currentTimeMillis())
    javaComp.update(Set(evenLower.toIASValue()))
    assert(checkAlarmActivation(javaComp,Alarm.SET_HIGH))
    
    // Increasing but not passing LowOff remains active
    val aBitHigher = evenLower.updateValue(Some(-15L)).updateDasuProdTStamp(System.currentTimeMillis())
    javaComp.update(Set(aBitHigher.toIASValue()))
    assert(checkAlarmActivation(javaComp,Alarm.SET_HIGH))
    
    // Passing LowOff deactivate
    val deactFromLow = aBitHigher.updateValue(Some(0L)).updateDasuProdTStamp(System.currentTimeMillis())
    javaComp.update(Set(deactFromLow.toIASValue()))
    assert(checkAlarmActivation(javaComp,Alarm.CLEARED))
  }
  
}
