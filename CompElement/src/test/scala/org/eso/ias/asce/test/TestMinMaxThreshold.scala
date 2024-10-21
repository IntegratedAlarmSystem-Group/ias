package org.eso.ias.asce.test

import java.util.Properties
import org.eso.ias.asce.ComputingElement
import org.eso.ias.asce.transfer.{JavaTransfer, ScalaTransfer, TransferFunctionLanguage, TransferFunctionSetting}
import org.eso.ias.asce.transfer.impls.{MinMaxThresholdTF, MinMaxThresholdTFJava}
import org.eso.ias.types.{Alarm, IASTypes, InOut, Priority}
import org.scalatest.flatspec.AnyFlatSpec

class TestMinMaxThreshold extends AnyFlatSpec {
  
  // The threshold to assess the validity from the arrival time of the input
  val validityThresholdInSecs = 2
  
  def withScalaTransferSetting(testCode: TransferFunctionSetting => Any): Unit = {
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
    }
  }
  
  def withJavaTransferSetting(testCode: TransferFunctionSetting => Any): Unit = {
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
    }
  }
  
  def withScalaComp(testCode: (ComputingElement[Alarm], Set[InOut[?]]) => Any): Unit = {
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
    props.put(MinMaxThresholdTF.alarmPriorityPropName,Priority.CRITICAL.toString())
    
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
  
  def withJavaComp(testCode: (ComputingElement[Alarm], Set[InOut[?]]) => Any): Unit = {
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
    props.put(MinMaxThresholdTF.alarmPriorityPropName,Priority.CRITICAL.toString)
    
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
  
  it must "Correctly load and shutdown the TF executor" in withScalaTransferSetting { scalaMinMaxTF =>
    assert(!scalaMinMaxTF.initialized)
    assert(!scalaMinMaxTF.isShutDown)
    scalaMinMaxTF.initialize("ASCE-MinMaxTF-ID", "ASCE-running-ID", 1000, new Properties())
    assert(!scalaMinMaxTF.isShutDown)
    scalaMinMaxTF.shutdown()
    assert(scalaMinMaxTF.isShutDown)
  }
  
  /**
   * Check the state of the alarm of the passed IASIO
   * 
   * @param hio: the IASIO to check the alarm state
   * @param alarmState: The expected alarm
   */
  def checkAlarmActivation(asce: ComputingElement[Alarm], isSet: Boolean, priority: Priority): Boolean = {
    assert(asce.isOutputAnAlarm)
    val iasio = asce.output
    assert(iasio.iasType==IASTypes.ALARM)
    
    iasio.value.forall(a => {
      val alarm = a.asInstanceOf[Alarm]

      if (alarm.priority!=priority) System.err.println(s"Priority mismatch found ${alarm.priority} exp. $priority")
      if (alarm.isSet!=isSet) System.err.println(s"Alarm set state mismatch found ${alarm.isSet} exp. $isSet")

      alarm.priority==priority && alarm.isSet==isSet
    })
  }
  
  
  
  it must "run the scala Min/Max TF executor" in withScalaComp { (scalaComp, inputsMPs) =>
    scalaComp.initialize()
    Thread.sleep(1000)
    // Change the input to trigger the TF
    val changedMP = inputsMPs.head.asInstanceOf[InOut[Long]].updateValue(Some(5L)).updateProdTStamp(System.currentTimeMillis())
    scalaComp.update(Set(changedMP.toIASValue()))
    assert(checkAlarmActivation(scalaComp,false, Priority.CRITICAL))
    
    // Activate high
    val highMp = changedMP.updateValue(Some(100L)).updateProdTStamp(System.currentTimeMillis())
    scalaComp.update(Set(highMp.toIASValue()))
    assert(checkAlarmActivation(scalaComp,true, Priority.CRITICAL))
    
    // Is the property set with the value that triggered the alarm?
    assert(scalaComp.output.props.isDefined)
    assert(scalaComp.output.props.get.keys.toList.contains("actualValue"))
    val propValueMap=scalaComp.output.props.get
    assert(propValueMap("actualValue")==100L.toDouble.toString())
    
    
    // Increase does not deactivate the alarm
    val moreHigh=highMp.updateValue(Some(150L)).updateProdTStamp(System.currentTimeMillis())
    scalaComp.update(Set(moreHigh.toIASValue()))
    assert(checkAlarmActivation(scalaComp,true, Priority.CRITICAL))
    
    val propValueMap2=scalaComp.output.props.get
    assert(propValueMap2("actualValue")==150L.toDouble.toString())
    
    // Decreasing without passing HighOn does not deactivate
    val noDeact = moreHigh.updateValue(Some(40L)).updateProdTStamp(System.currentTimeMillis())
    scalaComp.update(Set(noDeact.toIASValue()))
    assert(checkAlarmActivation(scalaComp,true, Priority.CRITICAL))
    
    // Below HighOff deactivate the alarm
    val deact = noDeact.updateValue(Some(10L)).updateProdTStamp(System.currentTimeMillis())
    scalaComp.update(Set(deact.toIASValue()))
    assert(checkAlarmActivation(scalaComp,false, Priority.CRITICAL))
    
    val propValueMap3=scalaComp.output.props.get
    assert(propValueMap3("actualValue")==10L.toDouble.toString())
    
    // Below LowOff but not passing LowOn does not activate
    val aBitLower = deact.updateValue(Some(-15L)).updateProdTStamp(System.currentTimeMillis())
    scalaComp.update(Set(aBitLower.toIASValue()))
    assert(checkAlarmActivation(scalaComp,false, Priority.CRITICAL))
    
    // Passing LowOn activate
    val actLow = aBitLower.updateValue(Some(-30L)).updateProdTStamp(System.currentTimeMillis())
    scalaComp.update(Set(actLow.toIASValue()))
    assert(checkAlarmActivation(scalaComp,true, Priority.CRITICAL))
    
    // Decreasing more remain activate
    val evenLower = actLow.updateValue(Some(-40L)).updateProdTStamp(System.currentTimeMillis())
    scalaComp.update(Set(evenLower.toIASValue()))
    assert(checkAlarmActivation(scalaComp,true, Priority.CRITICAL))
    
    // Increasing but not passing LowOff remains active
    val aBitHigher = evenLower.updateValue(Some(-15L)).updateProdTStamp(System.currentTimeMillis())
    scalaComp.update(Set(aBitHigher.toIASValue()))
    assert(checkAlarmActivation(scalaComp,true, Priority.CRITICAL))
    
    // Passing LowOff deactivate
    val deactFromLow = aBitHigher.updateValue(Some(0L)).updateProdTStamp(System.currentTimeMillis())
    scalaComp.update(Set(deactFromLow.toIASValue()))
    assert(checkAlarmActivation(scalaComp,false, Priority.CRITICAL))
  }
  
  behavior of "The java MinMaxThreshold executor"
  
  it must "Correctly load and shutdown the TF executor" in withJavaTransferSetting { javaMinMaxTF =>
    assert(!javaMinMaxTF.initialized)
    assert(!javaMinMaxTF.isShutDown)
    javaMinMaxTF.initialize("ASCE-MinMaxTF-ID", "ASCE-running-ID", 1000, new Properties())
    assert(!javaMinMaxTF.isShutDown)
    javaMinMaxTF.shutdown()
    Thread.sleep(500)
    assert(javaMinMaxTF.isShutDown)
  }
  
  it must "run the java Min/Max TF executor" in withJavaComp { (javaComp, inputsMPs) =>
    javaComp.initialize()
    Thread.sleep(1000)
    // Change the input to trigger the TF
    val changedMP = inputsMPs.head.asInstanceOf[InOut[Long]].updateValue(Some(5L)).updateProdTStamp(System.currentTimeMillis())
    javaComp.update(Set(changedMP.toIASValue()))

    assert(checkAlarmActivation(javaComp, false, Priority.CRITICAL))
    
    // Activate high
    val highMp = changedMP.updateValue(Some(100L)).updateProdTStamp(System.currentTimeMillis())
    javaComp.update(Set(highMp.toIASValue()))
    println("===> "+javaComp.output.value.get)
    assert(checkAlarmActivation(javaComp,true, Priority.CRITICAL))
    
    // Is the property set with the value that triggered the alarm?
    assert(javaComp.output.props.isDefined)
    assert(javaComp.output.props.get.keys.toList.contains("actualValue"))
    val propValueMap=javaComp.output.props.get
    assert(propValueMap("actualValue")==100L.toDouble.toString())
    
    // Increase does not deactivate the alarm
    val moreHigh=highMp.updateValue(Some(150L)).updateProdTStamp(System.currentTimeMillis())
    javaComp.update(Set(moreHigh.toIASValue()))
    assert(checkAlarmActivation(javaComp, true, Priority.CRITICAL))
    
    val propValueMap2=javaComp.output.props.get
    assert(propValueMap2("actualValue")==150L.toDouble.toString())
    
    // Decreasing without passing HighOn does not deactivate
    val noDeact = moreHigh.updateValue(Some(40L)).updateProdTStamp(System.currentTimeMillis())
    javaComp.update(Set(noDeact.toIASValue()))
    assert(checkAlarmActivation(javaComp, true, Priority.CRITICAL))
    
    // Below HighOff deactivate the alarm
    val deact = noDeact.updateValue(Some(10L)).updateProdTStamp(System.currentTimeMillis())
    javaComp.update(Set(deact.toIASValue()))
    assert(checkAlarmActivation(javaComp, false, Priority.CRITICAL))
    
    val propValueMap3=javaComp.output.props.get
    assert(propValueMap3("actualValue")==10L.toDouble.toString())
    
    // Below LowOff but not passing LowOn does not activate
    val aBitLower = deact.updateValue(Some(-15L)).updateProdTStamp(System.currentTimeMillis())
    javaComp.update(Set(aBitLower.toIASValue()))
    assert(checkAlarmActivation(javaComp, false, Priority.CRITICAL))
    
    // Passing LowOn activate
    val actLow = aBitLower.updateValue(Some(-30L)).updateProdTStamp(System.currentTimeMillis())
    javaComp.update(Set(actLow.toIASValue()))
    assert(checkAlarmActivation(javaComp, true, Priority.CRITICAL))
    
    // Decreasing more remain activate
    val evenLower = actLow.updateValue(Some(-40L)).updateProdTStamp(System.currentTimeMillis())
    javaComp.update(Set(evenLower.toIASValue()))
    assert(checkAlarmActivation(javaComp, true, Priority.CRITICAL))
    
    // Increasing but not passing LowOff remains active
    val aBitHigher = evenLower.updateValue(Some(-15L)).updateProdTStamp(System.currentTimeMillis())
    javaComp.update(Set(aBitHigher.toIASValue()))
    assert(checkAlarmActivation(javaComp, true, Priority.CRITICAL))
    
    // Passing LowOff deactivate
    val deactFromLow = aBitHigher.updateValue(Some(0L)).updateProdTStamp(System.currentTimeMillis())
    javaComp.update(Set(deactFromLow.toIASValue()))
    assert(checkAlarmActivation(javaComp, false, Priority.CRITICAL))
  }
  
}
