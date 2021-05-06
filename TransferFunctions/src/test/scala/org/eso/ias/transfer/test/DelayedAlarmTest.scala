package org.eso.ias.transfer.test

import org.scalatest.flatspec.AnyFlatSpec
import org.eso.ias.tranfer.DelayedAlarm
import org.eso.ias.types.Identifier
import org.eso.ias.types.IdentifierType
import java.util.Properties

import org.eso.ias.types.InOut
import org.eso.ias.types.IASTypes
import org.eso.ias.tranfer.DelayedAlarmException
import org.eso.ias.types.Alarm
import java.util.concurrent.TimeUnit

import org.eso.ias.logging.IASLogger
import org.eso.ias.asce.transfer.{IasIO, IasioInfo}

/**
 * test the DelayedAlarm TF
 */
class DelayedAlarmTest extends AnyFlatSpec {
  
  /**
   * The logger
   */
  val logger = IASLogger.getLogger(this.getClass)
  
  // The ID of the SUPERVISOR and the DASU where the components runs
  val supervId = new Identifier("SupervId",IdentifierType.SUPERVISOR,None)
  val dasuId = new Identifier("DasuId",IdentifierType.DASU,supervId)

  // The ID of the component running into the DASU
  val compID = new Identifier("ASCE-ID-ForTest",IdentifierType.ASCE,Option(dasuId))
  
  // The ID of the output generated by the component
  val outId = new Identifier("OutputId",IdentifierType.IASIO, Some(compID))
  // Build the IASIO in output
  val initialOutput: IasIO[Alarm] = new IasIO(InOut.asOutput(outId,IASTypes.ALARM))
  
  // Build the IASIO in input
  val initialInput: IasIO[Alarm]= new IasIO(InOut.asInput(
       new Identifier(("II#"), IdentifierType.IASIO,compID),
       IASTypes.ALARM))
  
  behavior of "The DelayedAlarm transfer function"
  
  it must "get the times from the properties" in {
    val timeToSet = 9L
    val timeToClear = 33L
    val props = new Properties
    props.put(DelayedAlarm.delayToClearTimePropName, timeToClear.toString())
    props.put(DelayedAlarm.delayToSetTimePropName, timeToSet.toString())
    val tf = new DelayedAlarm(compID.id,compID.fullRunningID,1000,props)
    assert(tf.waitTimeToClear.get==TimeUnit.MILLISECONDS.convert(timeToClear,TimeUnit.SECONDS))
    assert(tf.waitTimeToSet.get==TimeUnit.MILLISECONDS.convert(timeToSet,TimeUnit.SECONDS))
  }
  
  it must "throw an exception when inited with missing or wrong times" in {
    
    // No times
    val tf = new DelayedAlarm(compID.id,compID.fullRunningID,1000,new Properties())
    
    assertThrows[DelayedAlarmException] {
      tf.initialize(Set(new IasioInfo(initialInput.id,IASTypes.ALARM)),new IasioInfo(initialOutput.id,initialOutput.iasType))
    }
    
    // Invalid times
    val props = new Properties
    props.put(DelayedAlarm.delayToClearTimePropName, "wrong!")
    props.put(DelayedAlarm.delayToSetTimePropName, "1")
    
    val tf2 = new DelayedAlarm(compID.id,compID.fullRunningID,1000,props)
    assertThrows[DelayedAlarmException] {
      tf.initialize(Set(new IasioInfo(initialInput.id,IASTypes.ALARM)),new IasioInfo(initialOutput.id,initialOutput.iasType))
    }
    
    // Invalid times
    val props2 = new Properties
    props2.put(DelayedAlarm.delayToClearTimePropName, "15")
    props2.put(DelayedAlarm.delayToSetTimePropName, "-5")
    
    val tf3 = new DelayedAlarm(compID.id,compID.fullRunningID,1000,props2)
    assertThrows[DelayedAlarmException] {
      tf.initialize(Set(new IasioInfo(initialInput.id,IASTypes.ALARM)),new IasioInfo(initialOutput.id,initialOutput.iasType))
    }
    
    // Valid times: initialize succeeds
    val props3 = new Properties
    props3.put(DelayedAlarm.delayToClearTimePropName, "5")
    props3.put(DelayedAlarm.delayToSetTimePropName, "10")
    
    val tf4 = new DelayedAlarm(compID.id,compID.fullRunningID,1000,props3)
    tf4.initialize(Set(new IasioInfo(initialInput.id,IASTypes.ALARM)),new IasioInfo(initialOutput.id,initialOutput.iasType))
  }
  
  it must "produce a CLEARED alarm at the beginning" in {
    val props = new Properties
    props.put(DelayedAlarm.delayToClearTimePropName, "5")
    props.put(DelayedAlarm.delayToSetTimePropName, "10")
    val tf = new DelayedAlarm(compID.id,compID.fullRunningID,1000,props)
    tf.initialize(Set(new IasioInfo(initialInput.id,IASTypes.ALARM)),new IasioInfo(initialOutput.id,initialOutput.iasType))
    val map = Map[String, IasIO[_]]( initialOutput.id -> initialOutput.updateValue(Alarm.getSetDefault))
    
    val newOutput = tf.eval(map, initialOutput)
    
    assert(newOutput.value.isDefined)
    assert(newOutput.value.get==Alarm.CLEARED)
  }
  
  it must "set the alarm if the input does not change in the time interval" in {
    val timeToSet = 11L
    val props = new Properties
    props.put(DelayedAlarm.delayToClearTimePropName, "15")
    props.put(DelayedAlarm.delayToSetTimePropName, timeToSet.toString())
    val tf = new DelayedAlarm(compID.id,compID.fullRunningID,1000,props)
    tf.initialize(Set(new IasioInfo(initialInput.id,IASTypes.ALARM)),new IasioInfo(initialOutput.id,initialOutput.iasType))
    
    // Send the initial value
    val map = Map[String, IasIO[_]]( initialOutput.id -> initialOutput.updateValue(Alarm.getSetDefault))
    
    val initialTime = System.currentTimeMillis()
    val expectedTimeChange = initialTime+TimeUnit.MILLISECONDS.convert(timeToSet, TimeUnit.SECONDS)
    
    // Send the same input many times and ensure that the output
    // changes only after the time to set elapsed
    var output = initialOutput
    while (System.currentTimeMillis() < expectedTimeChange) {
      logger.info("Sending input {}",map.values.head.value.get)
      output = tf.eval(map, output)
      assert(output.value.isDefined)
      assert(output.value.get==Alarm.CLEARED)
      logger.info("TF produced {}",output.value.get)
      Thread.sleep(3000)
    }
    // The output shall change
    logger.info("Sending again input {} now output should change",map.values.head.value.get)
    val newOutput = tf.eval(map, output)
    assert(newOutput.value.isDefined)
    logger.info("TF produced {}",newOutput.value.get)
    assert(newOutput.value.get==Alarm.getSetDefault)
  }
  
  it must "clear the alarm if the input remains clear in the time interval" in {
    val timeToSet = 4L 
    val timeToClear = 9L
    val props = new Properties
    props.put(DelayedAlarm.delayToClearTimePropName, timeToClear.toString())
    props.put(DelayedAlarm.delayToSetTimePropName, timeToSet.toString())
    val tf = new DelayedAlarm(compID.id,compID.fullRunningID,1000,props)
    tf.initialize(Set(new IasioInfo(initialInput.id,IASTypes.ALARM)),new IasioInfo(initialOutput.id,initialOutput.iasType))
    
    // Send the initial value
    val map = Map[String, IasIO[_]]( initialOutput.id -> initialOutput.updateValue(Alarm.getSetDefault))
    
    // Force activation
    var output = initialOutput
    output = tf.eval(map, output)
    Thread.sleep(TimeUnit.MILLISECONDS.convert(timeToSet+1, TimeUnit.SECONDS))
    output = tf.eval(map, output)
    assert(output.value.isDefined)
    logger.info("TF produced {}",output.value.get)
    assert(output.value.get==Alarm.getSetDefault)
    
    val initialTime = System.currentTimeMillis()
    val expectedTimeChange = initialTime+TimeUnit.MILLISECONDS.convert(timeToClear, TimeUnit.SECONDS)
    
    val mapToUnset = Map[String, IasIO[_]]( initialOutput.id -> initialOutput.updateValue(Alarm.CLEARED))
    while (System.currentTimeMillis() < expectedTimeChange) {
      logger.info("Sending input {}",map.values.head.value.get)
      output = tf.eval(mapToUnset, output)
      assert(output.value.isDefined)
      assert(output.value.get==Alarm.getSetDefault)
      logger.info("TF produced {}",output.value.get)
      Thread.sleep(2500)
    }
    // The output shall change
    logger.info("Sending again input {} now output should change",map.values.head.value.get)
    val newOutput = tf.eval(mapToUnset, output)
    assert(newOutput.value.isDefined)
    logger.info("TF produced {}",newOutput.value.get)
    assert(newOutput.value.get==Alarm.CLEARED)
  }
  
  it must "be steady if the input oscillates" in {
    val timeToSet = 3L 
    val timeToClear = 7L
    val props = new Properties
    props.put(DelayedAlarm.delayToClearTimePropName, timeToClear.toString())
    props.put(DelayedAlarm.delayToSetTimePropName, timeToSet.toString())
    val tf = new DelayedAlarm(compID.id,compID.fullRunningID,1000,props)
    tf.initialize(Set(new IasioInfo(initialInput.id,IASTypes.ALARM)),new IasioInfo(initialOutput.id,initialOutput.iasType))
    
    val mapSet = Map[String, IasIO[_]]( initialOutput.id -> initialOutput.updateValue(Alarm.getSetDefault))
    val mapUnset = Map[String, IasIO[_]]( initialOutput.id -> initialOutput.updateValue(Alarm.CLEARED))
    
    // Force activation
    var output = initialOutput
    output = tf.eval(mapSet, output)
    Thread.sleep(TimeUnit.MILLISECONDS.convert(timeToSet+1, TimeUnit.SECONDS))
    output = tf.eval(mapSet, output)
    assert(output.value.isDefined)
    logger.info("TF produced {}",output.value.get)
    assert(output.value.get==Alarm.getSetDefault)
    
    val initialTime = System.currentTimeMillis()
    val expectedTimeChange = initialTime+TimeUnit.MILLISECONDS.convert(timeToClear+timeToSet, TimeUnit.SECONDS)
    var lastSentMap = mapSet
    while (System.currentTimeMillis() < expectedTimeChange) {
      logger.info("Sending input {}",lastSentMap.values.head.value.get)
      output = tf.eval(lastSentMap, output)
      lastSentMap = if (lastSentMap==mapSet) mapUnset else mapSet
      assert(output.value.isDefined)
      assert(output.value.get==Alarm.getSetDefault)
      logger.info("TF produced {}",output.value.get)
      Thread.sleep(500)
    }
  }
  
}
