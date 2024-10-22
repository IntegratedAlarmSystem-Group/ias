package org.eso.ias.transfer.test

import java.util.concurrent.TimeUnit
import java.util.{Optional, Properties}
import scala.compiletime.uninitialized

import org.eso.ias.asce.exceptions.PropsMisconfiguredException
import org.eso.ias.asce.transfer.{IasIO, IasioInfo}
import org.eso.ias.logging.IASLogger
import org.eso.ias.tranfer.ThresholdWithBackupsAndDelay
import org.eso.ias.types._
import org.scalatest.{BeforeAndAfterEach}
import org.scalatest.flatspec.AnyFlatSpec

/**
  * Test the ThresholdWithBackupsAndDelay transfer function
  */
class ThresholdBackupAndDelayTest extends AnyFlatSpec with BeforeAndAfterEach {

  /** The logger */
  val logger = IASLogger.getLogger(this.getClass)

  /**  The ID of the SUPERVISOR where the components runs */
  val supervId = new Identifier("SupervId",IdentifierType.SUPERVISOR,None)

  /**  The ID of the DASU where the components runs */
  val dasuId = new Identifier("DasuId",IdentifierType.DASU,supervId)

  /** The ID of the component running into the DASU */
  val compID = new Identifier("ASCE-ID-ForTest",IdentifierType.ASCE,Option(dasuId))

  /** The ID of the output generated by the component */
  val outId = new Identifier("OutputId",IdentifierType.IASIO, Some(compID))

  /** the IASIO in output */
  val out: IasIO[Double] = new IasIO(InOut.asOutput(outId,IASTypes.ALARM))

  /** Ouptut info for the initialize */
  val outputInfo = new IasioInfo(outId.id,IASTypes.ALARM)

  val inputIds: List[String] = (for (i <- 1 to 5) yield "InputID"+i).toList

  /** Inputs Info for the initialize */
  val inputinfos = inputIds.map(id => new IasioInfo(id,IASTypes.DOUBLE)).toSet

  /** The Identifiers of the inputs */
  val inputIDentifiers= inputIds.map(id => new Identifier(id,IdentifierType.IASIO, Some(compID)))

  /** the ID of the main input */
  val mainInputId = "InputID1"

  /** The Identifier of the main input */
  val mainInputIdentifer = inputIDentifiers.find(id => id.id==mainInputId).get

  /** The ID of the main IASIO */
  val aId = new Identifier("A",IdentifierType.IASIO, Some(compID))

  /** The time frame for the validity */
  val validityTimeFrame = 2000

  /** Default set of properties for testing */
  val defaultProps: Properties = new Properties

  /** Default ThresholdWithBackupsAndDelay TF to test */
  var defaultTF: ThresholdWithBackupsAndDelay = uninitialized

  /** Initial set of inputs */
  val inputs: List[IasIO[Double]] = inputIDentifiers.map(id => buildInput(id,0))

  /** Delay before setting the alarm */
  val delayTimeToSet = 3

  /** Delay before setting the alarm */
  val delayTimeToClear = 5


  /** The initial map of input for the TF */
  val initialMapOfInputs: Map[String, IasIO[Double]] =
    inputs.foldLeft(Map[String, IasIO[Double]]())((z, iasio) => z ++ Map(iasio.id -> iasio))
  initialMapOfInputs.foreach( x => ThresholdWithBackupsAndDelay.logger.info("id={}, type={} value={}",x._2.id, x._2.iasType,x._2.value.toString))

  /** The initial the IASIO in output */
  val initialOutput: IasIO[Alarm] = new IasIO(InOut.asOutput(outId,IASTypes.ALARM))

  /** The priority of the alarm set by the TF */
  val alarmSetByTf = Priority.CRITICAL

  override def beforeEach() : scala.Unit = {
    logger.info("Initializing before running a test")
    defaultProps.put(ThresholdWithBackupsAndDelay.HighOnPropName,"10")
    defaultProps.put(ThresholdWithBackupsAndDelay.HighOffPropName,"8")
    defaultProps.put(ThresholdWithBackupsAndDelay.LowOnPropName,"-12")
    defaultProps.put(ThresholdWithBackupsAndDelay.LowOffPropName,"-5")
    defaultProps.put(ThresholdWithBackupsAndDelay.AlarmPriorityPropName,alarmSetByTf.name())
    defaultProps.put(ThresholdWithBackupsAndDelay.DelayToSetTimePropName,delayTimeToSet.toString)
    defaultProps.put(ThresholdWithBackupsAndDelay.DelayToClearTimePropName,delayTimeToClear.toString)
    defaultProps.put(ThresholdWithBackupsAndDelay.MaindIdPropName,mainInputId)

    defaultTF = new ThresholdWithBackupsAndDelay(compID.id,compID.fullRunningID,validityTimeFrame,defaultProps)
  }

  /**
    *  Build the IASIO in input with the passed id, type and timestamp
    *
    *  @param id The identifier of the input
    *  @param value initial value
    *  @param tStamp the daus production timestamp
    *  @param mode the operational mode
    */
  def buildInput(
                  id: Identifier,
                  value: Double,
                  tStamp: Long = System.currentTimeMillis(),
                  mode: OperationalMode = OperationalMode.OPERATIONAL,
                  validity: IasValidity = IasValidity.RELIABLE): IasIO[Double] = {
    require(Option(id).isDefined)
    require(Option(value).isDefined)

    val inout: InOut[Double] = InOut.asInput(id,IASTypes.DOUBLE)
      .updateProdTStamp(tStamp)
      .updateValueValidity(Some(value),Some(Validity(validity)))
    (new IasIO(inout)).updateMode(mode)
  }

  behavior of "The ThresholdWithBackupsAndDelay"

  it must "get the list from the property" in {
    assert(defaultTF.highOn==10)
    assert(defaultTF.highOff==8)
    assert(defaultTF.lowOn==(-12))
    assert(defaultTF.lowOff==(-5))
    assert(defaultTF.alarmPriority==Priority.CRITICAL)
    assert(defaultTF.delayToSet==delayTimeToSet*1000)
    assert(defaultTF.delayToClear==delayTimeToClear*1000)
    assert(defaultTF.idOfMainInput==Some(mainInputId))
  }

  it must "correctly intialize" in {
    defaultTF.initialize(inputinfos,outputInfo)
  }

  it must "catch errors in parameters" in {
    val props = new Properties
    props.put(ThresholdWithBackupsAndDelay.HighOnPropName,"10")
    props.put(ThresholdWithBackupsAndDelay.HighOffPropName,"12") // Error!
    props.put(ThresholdWithBackupsAndDelay.LowOnPropName,"-12")
    props.put(ThresholdWithBackupsAndDelay.LowOffPropName,"-5")
    props.put(ThresholdWithBackupsAndDelay.AlarmPriorityPropName,Priority.CRITICAL.name())
    props.put(ThresholdWithBackupsAndDelay.DelayToSetTimePropName,"2")
    props.put(ThresholdWithBackupsAndDelay.DelayToClearTimePropName,"3")
    props.put(ThresholdWithBackupsAndDelay.MaindIdPropName,mainInputId)

    val tf = new ThresholdWithBackupsAndDelay(compID.id,compID.fullRunningID,validityTimeFrame,props)
    assertThrows[PropsMisconfiguredException] { tf.initialize(inputinfos,outputInfo) }

    props.put(ThresholdWithBackupsAndDelay.HighOffPropName,"8") // Removes previous error
    props.put(ThresholdWithBackupsAndDelay.LowOnPropName,"0") // New Error
    val tf2 = new ThresholdWithBackupsAndDelay(compID.id,compID.fullRunningID,validityTimeFrame,props)
    assertThrows[PropsMisconfiguredException] { tf2.initialize(inputinfos,outputInfo) }

    props.put(ThresholdWithBackupsAndDelay.AlarmPriorityPropName,Priority.MEDIUM.name()) // Removes previous error
    props.put(ThresholdWithBackupsAndDelay.DelayToSetTimePropName,"-1")
    val tf4 = new ThresholdWithBackupsAndDelay(compID.id,compID.fullRunningID,validityTimeFrame,props)
    assertThrows[PropsMisconfiguredException] { tf4.initialize(inputinfos,outputInfo) }

    props.put(ThresholdWithBackupsAndDelay.DelayToSetTimePropName,"0") // Removes previous error
    props.put(ThresholdWithBackupsAndDelay.DelayToClearTimePropName,"-1")
    val tf5 = new ThresholdWithBackupsAndDelay(compID.id,compID.fullRunningID,validityTimeFrame,props)
    assertThrows[PropsMisconfiguredException] { tf5.initialize(inputinfos,outputInfo) }

    props.put(ThresholdWithBackupsAndDelay.DelayToClearTimePropName,"5") // Removes previous error
    props.put(ThresholdWithBackupsAndDelay.MaindIdPropName,"")
    val tf6 = new ThresholdWithBackupsAndDelay(compID.id,compID.fullRunningID,validityTimeFrame,props)
    assertThrows[PropsMisconfiguredException] { tf6.initialize(inputinfos,outputInfo) }

  }

  it must "produce a CLEAR alarm at beginning" in {
    defaultTF.initialize(inputinfos,outputInfo)
    defaultTF.setTemplateInstance(Optional.empty()) // Normally done by the ASCE
    val newOutput: IasIO[Alarm] = defaultTF.eval(initialMapOfInputs, initialOutput)

    assert(newOutput.value.isDefined)
    assert(newOutput.value.get.isCleared)
  }

  it must "immediately SET and CLEAR if there are no delays" in {
    defaultProps.put(ThresholdWithBackupsAndDelay.DelayToClearTimePropName, "0")
    defaultProps.put(ThresholdWithBackupsAndDelay.DelayToSetTimePropName, "0")

    val tf = new ThresholdWithBackupsAndDelay(compID.id, compID.fullRunningID, validityTimeFrame, defaultProps)
    tf.initialize(inputinfos,outputInfo)
    tf.setTemplateInstance(Optional.empty()) // Normally done by the ASCE

    val io = initialMapOfInputs(mainInputId)
    val newValue = io.updateValue(100.0)

    val mapWithSet = initialMapOfInputs + (mainInputId -> newValue)

    val newOutput: IasIO[Alarm] = tf.eval(mapWithSet, initialOutput)

    assert(newOutput.value.isDefined)
    assert(newOutput.value.get.isSet)

    val mapWithClear = initialMapOfInputs + (mainInputId -> initialMapOfInputs(mainInputId).updateValue(0.0))
    val newOutput2: IasIO[Alarm] = tf.eval(mapWithClear, initialOutput)

    assert(newOutput2.value.isDefined)
    assert(!newOutput2.value.get.isSet)
  }

  it must "immediately SET and CLEAR respecting thresholds only" in {


    defaultProps.put(ThresholdWithBackupsAndDelay.DelayToClearTimePropName, "0")
    defaultProps.put(ThresholdWithBackupsAndDelay.DelayToSetTimePropName, "0")

    val tf = new ThresholdWithBackupsAndDelay(compID.id, compID.fullRunningID, validityTimeFrame, defaultProps)
    tf.initialize(inputinfos,outputInfo)
    tf.setTemplateInstance(Optional.empty()) // Normally done by the ASCE

    var output = initialOutput
    for (i <- 0 to 15) {
      val map = initialMapOfInputs + (mainInputId -> initialMapOfInputs(mainInputId).updateValue(i.toDouble))
      output = tf.eval(map, output)
      assert(output.value.isDefined)
      if (i<tf.highOn) assert(output.value.get.isCleared)
      else assert(output.value.get.isSet)
    }

    for (i <- 20 to -20 by -1) {
      val map = initialMapOfInputs + (mainInputId -> initialMapOfInputs(mainInputId).updateValue(i.toDouble))
      output = tf.eval(map, output)
      assert(output.value.isDefined)
      if (i>=tf.highOff || i<=tf.lowOn) assert(output.value.get.isSet)
      else assert(output.value.get.isCleared)
    }

    for (i <- -20 to 0) {
      val map = initialMapOfInputs + (mainInputId -> initialMapOfInputs(mainInputId).updateValue(i.toDouble))
      output = tf.eval(map, output)
      assert(output.value.isDefined)
      if (i<=tf.lowOff) assert(output.value.get.isSet)
      else assert(output.value.get.isCleared)
    }
  }

  it must "discard the main input if not Reliable and/or not valid" in {
    defaultProps.put(ThresholdWithBackupsAndDelay.DelayToClearTimePropName, "0")
    defaultProps.put(ThresholdWithBackupsAndDelay.DelayToSetTimePropName, "0")

    val tf = new ThresholdWithBackupsAndDelay(compID.id, compID.fullRunningID, validityTimeFrame, defaultProps)
    tf.initialize(inputinfos,outputInfo)
    tf.setTemplateInstance(Optional.empty()) // Normally done by the ASCE

    val inOut: InOut[Double] = InOut.asInput(mainInputIdentifer,IASTypes.DOUBLE).updateMode(OperationalMode.MAINTENANCE).updateValue(Some(3D))
    val value =initialMapOfInputs("InputID3").updateValue(25.toDouble)
    val map = initialMapOfInputs + (mainInputId -> new IasIO[Double](inOut))+("InputID3" ->value)

    var output = initialOutput
    output = tf.eval(map, output)
    assert(output.value.isDefined)
    assert(output.value.get.isSet)

    val map2 = map+
      (mainInputId -> map(mainInputId).updateValue(30D))+
      ("InputID3" -> value.updateValue(0D))
    output = tf.eval(map2, output)
    assert(output.value.isDefined)
    assert(!output.value.get.isSet)
  }

  it must "set the properties" in {
    defaultProps.put(ThresholdWithBackupsAndDelay.DelayToClearTimePropName, "0")
    defaultProps.put(ThresholdWithBackupsAndDelay.DelayToSetTimePropName, "0")

    val tf = new ThresholdWithBackupsAndDelay(compID.id, compID.fullRunningID, validityTimeFrame, defaultProps)
    tf.initialize(inputinfos,outputInfo)
    tf.setTemplateInstance(Optional.empty()) // Normally done by the ASCE

    val inOut: InOut[Double] = InOut.asInput(mainInputIdentifer,IASTypes.DOUBLE).updateMode(OperationalMode.MAINTENANCE).updateValue(Some(3D))
    val value =initialMapOfInputs("InputID3").updateValue(25.toDouble)
    val map = initialMapOfInputs + (mainInputId -> new IasIO[Double](inOut))+("InputID3" ->value)

    var output = tf.eval(map, initialOutput)

    output.props.keys.foreach(k => logger.info("Property of output with backup {}->{}",k,output.props(k)))
    val pMain = output.props.get("value")
    assert(pMain.isEmpty)
    val pBackups = output.props.get("backups")
    assert(pBackups.isDefined)
    assert(pBackups.get.contains("25.0"))

    val map2 = map+(mainInputId -> buildInput(mainInputIdentifer,5D))
    output = tf.eval(map2, output)
    output.props.keys.foreach(k => logger.info("Property of output with main {}->{}",k,output.props(k)))
    val pMain2 = output.props.get("value")
    assert(pMain2.isDefined)
    val pBackups2 = output.props.get("backups")
    assert(pBackups2.isEmpty)

  }

  it must "wait before activating" in {
    val tf = new ThresholdWithBackupsAndDelay(compID.id, compID.fullRunningID, validityTimeFrame, defaultProps)
    tf.initialize(inputinfos,outputInfo)
    tf.setTemplateInstance(Optional.empty()) // Normally done by the ASCE

    val mapSet = initialMapOfInputs +( mainInputId -> initialMapOfInputs(mainInputId).updateValue(30D))
    val mapUnset =  initialMapOfInputs +( mainInputId -> initialMapOfInputs(mainInputId).updateValue(0D))

    // Force activation
    var output = initialOutput
    output = tf.eval(mapSet, output)
    assert(output.value.isDefined)
    assert(!output.value.get.isSet)

    val expectedTimeChange = System.currentTimeMillis()+TimeUnit.MILLISECONDS.convert(delayTimeToSet, TimeUnit.SECONDS)

    // Continously try to set and check if activation
    // happens only after delayTimeToSet time elapsed
    while (System.currentTimeMillis() < expectedTimeChange-500) {
      output = tf.eval(mapSet, output)
      assert(output.value.isDefined)
      assert(!output.value.get.isSet)
      Thread.sleep(500)
    }
    // Now set again and the TF must finally activate
    Thread.sleep(1000)
    output = tf.eval(mapSet, output)
    assert(output.value.isDefined)
    assert(output.value.get.isSet)
  }

  it must "wait before de-activating" in {
    defaultProps.put(ThresholdWithBackupsAndDelay.DelayToSetTimePropName,"0")
    val tf = new ThresholdWithBackupsAndDelay(compID.id, compID.fullRunningID, validityTimeFrame, defaultProps)
    tf.initialize(inputinfos,outputInfo)
    tf.setTemplateInstance(Optional.empty()) // Normally done by the ASCE

    val mapSet = initialMapOfInputs +( mainInputId -> initialMapOfInputs(mainInputId).updateValue(-200D))
    val mapUnset =  initialMapOfInputs +( mainInputId -> initialMapOfInputs(mainInputId).updateValue(0D))

    // Force activation
    var output = initialOutput
    output = tf.eval(mapSet, output)
    assert(output.value.isDefined)
    assert(output.value.get.isSet)

    val expectedTimeChange = System.currentTimeMillis()+TimeUnit.MILLISECONDS.convert(delayTimeToClear, TimeUnit.SECONDS)

    // Continously try to set and check if activation
    // happens only after delayTimeToSet time elapsed
    while (System.currentTimeMillis() < expectedTimeChange-500) {
      output = tf.eval(mapUnset, output)
      assert(output.value.isDefined)
      assert(output.value.get.isSet)
      Thread.sleep(500)
    }
    // Now set again and the TF must finally activate
    Thread.sleep(1000)
    output = tf.eval(mapUnset, output)
    assert(output.value.isDefined)
    assert(!output.value.get.isSet)

  }

  it must "be steady if the input oscillates" in {
    val tf = new ThresholdWithBackupsAndDelay(compID.id, compID.fullRunningID, validityTimeFrame, defaultProps)
    tf.initialize(inputinfos,outputInfo)
    tf.setTemplateInstance(Optional.empty()) // Normally done by the ASCE

    val mapSet = initialMapOfInputs +( mainInputId -> initialMapOfInputs(mainInputId).updateValue(30D))
    val mapUnset =  initialMapOfInputs +( mainInputId -> initialMapOfInputs(mainInputId).updateValue(0D))

    // Force activation
    var output = initialOutput
    output = tf.eval(mapSet, output)
    assert(!output.value.get.isSet)

    Thread.sleep(TimeUnit.MILLISECONDS.convert(delayTimeToSet+1, TimeUnit.SECONDS))
    output = tf.eval(mapSet, output)
    assert(output.value.isDefined)
    assert(output.value.get.isSet)

    val initialTime = System.currentTimeMillis()
    val expectedTimeChange = initialTime+TimeUnit.MILLISECONDS.convert(delayTimeToClear+delayTimeToSet, TimeUnit.SECONDS)
    var lastSentMap = mapSet
    while (System.currentTimeMillis() < expectedTimeChange) {
      logger.info("Sending input {}",lastSentMap.values.head.value.get)
      output = tf.eval(lastSentMap, output)
      lastSentMap = if (lastSentMap.eq(mapSet)) mapUnset else mapSet
      assert(output.value.isDefined)
      assert(output.value.get.isSet)
      logger.info("TF produced {}",output.value.get)
      Thread.sleep(500)
    }
  }

}
