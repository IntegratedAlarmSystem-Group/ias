package org.eso.ias.transfer.test

import java.util.Properties

import org.eso.ias.asce.transfer.{IasIO, IasioInfo}
import org.eso.ias.logging.IASLogger
import org.eso.ias.tranfer.BoolToAlarm
import org.eso.ias.types._
import org.scalatest.flatspec.AnyFlatSpec

/** Test the [[org.eso.ias.tranfer.BoolToAlarm]] transfer function  */
class BoolToAlarmTest extends AnyFlatSpec {

  /** The logger */
  private val logger = IASLogger.getLogger(this.getClass)

  /**  The ID of the SUPERVISOR where the components runs */
  val supervId = new Identifier("SupervId",IdentifierType.SUPERVISOR,None)

  /**  The ID of the DASU where the components runs */
  val dasuId = new Identifier("DasuId",IdentifierType.DASU,supervId)

  /** The ID of the component running into the DASU */
  val compID = new Identifier("ASCE-ID-ForTest",IdentifierType.ASCE,Option(dasuId))

  /** The ID of the output generated by the component */
  val outId = new Identifier("OutputId",IdentifierType.IASIO, Some(compID))

  /** the IASIO in output */
  val out: IasIO[Alarm] = new IasIO(InOut.asOutput(outId,IASTypes.ALARM))

  /** Ouput info for the initialize */
  val outputInfo = new IasioInfo(outId.id,IASTypes.ALARM)

  /** THe id of th einput */
  val inputId = "Input-Id"

  /** The Identifier of the ALARM input */
  val inputIdentifier= new Identifier(inputId,IdentifierType.IASIO, Some(compID))

  /** Inputs Info for the initialize */
  val inputInOut =  InOut.asInput(inputIdentifier,IASTypes.BOOLEAN)

  /** Inputs Info for the initialize */
  val inputInfos = Set(new IasioInfo(inputId,IASTypes.BOOLEAN))

  /** The time frame for the validity */
  val validityTimeFrame = 2000

  behavior of "The BoolToAlarmTest"

  it must "get priority and logic from props or use defaults" in {
    val props = new Properties()

    val tf = new BoolToAlarm(compID.id, compID.fullRunningID, validityTimeFrame, props)

    assert(tf.priority==Priority.getDefaultPriority)
    assert(tf.invert==false)

    props.put(BoolToAlarm.InvertLogicPropName,"True")
    props.put(BoolToAlarm.PriorityPropName,Priority.HIGH.toString)
    val tf2 = new BoolToAlarm(compID.id, compID.fullRunningID, validityTimeFrame, props)
    assert(tf2.priority==Priority.HIGH)
    assert(tf2.invert==true)
  }

  it must "produce the expected output with normal logic" in {
    val props = new Properties()

    val tf = new BoolToAlarm(compID.id, compID.fullRunningID, validityTimeFrame, props)

    val i: InOut[?] = inputInOut.updateValue(Some(true))
    val inputMap: Map[String,IasIO[?]] = Map(inputId-> new IasIO(i))
    val ret = tf.eval(inputMap,out)
    assert(ret.value.isDefined)
    assert(ret.value.get.isSet)
    assert(ret.value.get.priority==Priority.getDefaultPriority)

    val i2 = i.updateValue(Some(false))
    val inputMap2: Map[String,IasIO[?]] = Map(inputId-> new IasIO(i2))
    val ret2 = tf.eval(inputMap2,out)
    assert(ret2.value.isDefined)
    assert(!ret2.value.get.isSet)

    val i3 = i2.updateValue(Some(true))
    val inputMap3: Map[String,IasIO[?]] = Map(inputId-> new IasIO(i3))
    val ret3 = tf.eval(inputMap3,out)
    assert(ret3.value.isDefined)
    assert(ret3.value.get.isSet)
    assert(ret3.value.get.priority==Priority.getDefaultPriority)
  }

  it must "produce the expected output with inverted logic" in {
    val props = new Properties()
    props.put(BoolToAlarm.InvertLogicPropName,"True")

    val tf = new BoolToAlarm(compID.id, compID.fullRunningID, validityTimeFrame, props)

    val i: InOut[?] = inputInOut.updateValue(Some(true))
    val inputMap: Map[String,IasIO[?]] = Map(inputId-> new IasIO(i))
    val ret = tf.eval(inputMap,out)
    assert(ret.value.isDefined)
    assert(ret.value.get.isCleared)

    val i2 = i.updateValue(Some(false))
    val inputMap2: Map[String,IasIO[?]] = Map(inputId-> new IasIO(i2))
    val ret2 = tf.eval(inputMap2,out)
    assert(ret2.value.isDefined)
    assert(ret2.value.get.isSet)
    assert(ret2.value.get.priority==Priority.getDefaultPriority)

    val i3 = i2.updateValue(Some(true))
    val inputMap3: Map[String,IasIO[?]] = Map(inputId-> new IasIO(i3))
    val ret3 = tf.eval(inputMap3,out)
    assert(ret3.value.isDefined)
    assert(ret3.value.get.isCleared)
  }

  it must "Forward mode and properties of the input" in {
    val props = new Properties()

    val additionalProps: Map[String, String] = Map("Prop1"-> "Value1", "Prop12"-> "Value2")

    val tf = new BoolToAlarm(compID.id, compID.fullRunningID, validityTimeFrame, props)
    val i: InOut[?] = inputInOut.updateValue(Some(true)).updateMode(OperationalMode.MALFUNCTIONING).updateProps(additionalProps)
    val inputMap: Map[String,IasIO[?]] = Map(inputId-> new IasIO(i))
    val ret = tf.eval(inputMap,out)
    assert(ret.value.isDefined)
    assert(ret.value.get.isSet)
    assert(ret.mode==OperationalMode.MALFUNCTIONING)
    assert(ret.props==additionalProps)
  }

}
