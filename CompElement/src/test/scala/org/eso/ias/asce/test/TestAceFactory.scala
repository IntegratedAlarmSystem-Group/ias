package org.eso.ias.asce.test

import org.eso.ias.asce.{AsceStates, ComputingElement}
import org.eso.ias.asce.transfer.impls.{MinMaxThresholdTF, MinMaxThresholdTFJava}
import org.eso.ias.cdb.pojos.{AsceDao, IasTypeDao, IasioDao, TFLanguageDao, TransferFunctionDao}
import org.eso.ias.logging.IASLogger
import org.eso.ias.types.{Alarm, IASTypes, Identifier, IdentifierType, Priority}
import org.scalatest.flatspec.AnyFlatSpec

import java.util.Properties

class TestAceFactory extends AnyFlatSpec {

  /** The logger */
  private val logger = IASLogger.getLogger(this.getClass)

  val validityThresholdInSecs = 5

  val props = new Properties()
  props.put(MinMaxThresholdTF.highOnPropName, "50")
  props.put(MinMaxThresholdTF.highOffPropName, "25")
  props.put(MinMaxThresholdTF.lowOffPropName, "-10")
  props.put(MinMaxThresholdTF.lowOnPropName, "-20")
  props.put(MinMaxThresholdTF.alarmPriorityPropName, Priority.HIGH.toString)

  // Shared DASU identifier (required by factory)
  val supervId = new Identifier("SupervId", IdentifierType.SUPERVISOR, None)
  val dasuId = new Identifier("DASU-ID", IdentifierType.DASU, supervId)

  /**
   * Build a fully populated AsceDao for the factory method.
   */
  def buildAsceDao(
    asceId: String,
    tfClass: String,
    tfLang: TFLanguageDao,
    outputAutoAck: Boolean): AsceDao = {

    val outputDao = new IasioDao(s"${asceId}-Out", "Output", IasTypeDao.ALARM, null)
    outputDao.setAutoAck(outputAutoAck)

    val inputDao = new IasioDao(s"${asceId}-In", "Input", IasTypeDao.LONG, null)

    val tfDao = new TransferFunctionDao(tfClass, tfLang)

    val asceDao = new AsceDao()
    asceDao.setId(asceId)
    asceDao.setOutput(outputDao)
    asceDao.addInput(inputDao, false)
    asceDao.setTransferFunction(tfDao)

    asceDao
  }

  behavior of "ComputingElement.apply"

  it must "auto-ACK an alarm when AsceDao output has autoAck=true (Scala TF)" in {
    logger.info("Testing factory auto-ACK with Scala TF")

    val asceDao = buildAsceDao(
      "FactoryAutoAckScala",
      "org.eso.ias.asce.transfer.impls.MinMaxThresholdTF",
      TFLanguageDao.SCALA,
      outputAutoAck = true)

    val comp = ComputingElement[Alarm](asceDao, dasuId, validityThresholdInSecs, props)

    comp.initialize()
    assert(comp.getState() == AsceStates.InputsUndefined)

    // The factory should have set autoAck=true from the DAO
    assert(comp.autoAck, "Factory should produce a component with autoAck=true")

    // Trigger alarm SET — should be auto-ACKed immediately
    val input = comp.inputs.values.head.asInstanceOf[org.eso.ias.types.InOut[Long]]
    val in1 = input.updateValue(Some(100L)).updateProdTStamp(System.currentTimeMillis())
    val ret1 = comp.update(Set(in1.toIASValue()))

    val outAlarm1 = ret1._1.get.value.get.asInstanceOf[Alarm]
    logger.info("Output produced: {}", outAlarm1.toString)
    assert(outAlarm1.isSet, "Alarm should be SET")
    assert(comp.output.value.get.asInstanceOf[Alarm].isAcked, "Alarm should be auto-ACKed after update()")

    // Clear alarm — should remain ACKed
    val in2 = input.updateValue(Some(0L)).updateProdTStamp(System.currentTimeMillis())
    val ret2 = comp.update(Set(in2.toIASValue()))
    val outAlarm2 = ret2._1.get.value.get.asInstanceOf[Alarm]
    logger.info("Output produced: {}", outAlarm2.toString)
    assert(outAlarm2.isCleared, "Alarm should be CLEARED")
    assert(outAlarm2.isAcked, "Cleared alarm should remain ACKed")

    // Re-trigger SET — should be auto-ACKed again
    val in3 = input.updateValue(Some(100L)).updateProdTStamp(System.currentTimeMillis())
    val ret3 = comp.update(Set(in3.toIASValue()))
    val outAlarm3 = ret3._1.get.value.get.asInstanceOf[Alarm]
    logger.info("Output produced: {}", outAlarm3.toString)
    assert(outAlarm3.isSet, "Alarm should be SET again")
    assert(comp.output.value.get.asInstanceOf[Alarm].isAcked, "Re-set alarm should be auto-ACKed again")

    comp.shutdown()
  }

  it must "auto-ACK an alarm when AsceDao output has autoAck=true (Java TF)" in {
    logger.info("Testing factory auto-ACK with Java TF")

    val asceDao = buildAsceDao(
      "FactoryAutoAckJava",
      "org.eso.ias.asce.transfer.impls.MinMaxThresholdTFJava",
      TFLanguageDao.JAVA,
      outputAutoAck = true)

    val javaProps = new Properties()
    javaProps.put(MinMaxThresholdTFJava.highOnPropName, "50")
    javaProps.put(MinMaxThresholdTFJava.highOffPropName, "25")
    javaProps.put(MinMaxThresholdTFJava.lowOffPropName, "-10")
    javaProps.put(MinMaxThresholdTFJava.lowOnPropName, "-20")
    javaProps.put(MinMaxThresholdTFJava.alarmPriorityPropName, Priority.HIGH.toString)

    val comp = ComputingElement[Alarm](asceDao, dasuId, validityThresholdInSecs, javaProps)

    assert(comp.autoAck, "Factory should produce a component with autoAck=true")

    comp.initialize()

    val input = comp.inputs.values.head.asInstanceOf[org.eso.ias.types.InOut[Long]]
    // Trigger alarm SET
    val in1 = input.updateValue(Some(100L)).updateProdTStamp(System.currentTimeMillis())
    comp.update(Set(in1.toIASValue()))
    assert(comp.output.value.get.asInstanceOf[Alarm].isSet)
    assert(comp.output.value.get.asInstanceOf[Alarm].isAcked, "Java TF: Alarm should be auto-ACKed")

    // Clear and re-set verifies repeated auto-ACK
    val in2 = input.updateValue(Some(0L)).updateProdTStamp(System.currentTimeMillis())
    comp.update(Set(in2.toIASValue()))

    val in3 = input.updateValue(Some(100L)).updateProdTStamp(System.currentTimeMillis())
    comp.update(Set(in3.toIASValue()))
    assert(comp.output.value.get.asInstanceOf[Alarm].isSet)
    assert(comp.output.value.get.asInstanceOf[Alarm].isAcked, "Java TF: Re-set alarm should be auto-ACKed again")

    comp.shutdown()
  }

  it must "NOT auto-ACK when AsceDao output has autoAck=false" in {
    logger.info("Testing factory does not auto-ACK when autoAck is false")

    val asceDao = buildAsceDao(
      "FactoryNoAutoAck",
      "org.eso.ias.asce.transfer.impls.MinMaxThresholdTF",
      TFLanguageDao.SCALA,
      outputAutoAck = false)

    val comp = ComputingElement[Alarm](asceDao, dasuId, validityThresholdInSecs, props)

    assert(!comp.autoAck, "Factory should produce a component with autoAck=false")

    comp.initialize()

    val input = comp.inputs.values.head.asInstanceOf[org.eso.ias.types.InOut[Long]]
    val in1 = input.updateValue(Some(100L)).updateProdTStamp(System.currentTimeMillis())
    val ret1 = comp.update(Set(in1.toIASValue()))
    val outAlarm1 = ret1._1.get.value.get.asInstanceOf[Alarm]
    logger.info("Output produced: {}", outAlarm1.toString)
    assert(outAlarm1.isSet, "Alarm should be SET")
    assert(!comp.output.value.get.asInstanceOf[Alarm].isAcked, "Alarm should NOT be auto-ACKed when autoAck is false")

    comp.shutdown()
  }

  it must "NOT auto-ACK a non-alarm output even when IasioDao has autoAck=true" in {
    logger.info("Testing that autoAck is only effective for alarm outputs")

    // Build an AsceDao with autoAck=true but output type is LONG (not ALARM)
    val outputDao = new IasioDao("SynthParamOut", "Synthetic param", IasTypeDao.LONG, null)
    outputDao.setAutoAck(true)

    val inputDao = new IasioDao("SynthParamIn", "Input", IasTypeDao.LONG, null)

    val tfDao = new TransferFunctionDao(
      "org.eso.ias.asce.transfer.impls.MinMaxThresholdTFJava",
      TFLanguageDao.JAVA)

    val asceDao = new AsceDao()
    asceDao.setId("SynthParamASCE")
    asceDao.setOutput(outputDao)
    asceDao.addInput(inputDao, false)
    asceDao.setTransferFunction(tfDao)

    // This should not throw — factory handles non-alarm with autoAck gracefully
    val comp = ComputingElement[Long](asceDao, dasuId, validityThresholdInSecs, props)

    // autoAck should be false because the output is not an alarm
    assert(!comp.autoAck, "autoAck should be false for non-alarm output even when IasioDao autoAck=true")

    comp.initialize()
    comp.shutdown()
  }

}
