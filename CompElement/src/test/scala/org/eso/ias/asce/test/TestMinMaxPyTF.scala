package org.eso.ias.asce.test

import java.util.Properties

import org.eso.ias.asce.transfer.{JavaTransfer, TransferFunctionLanguage, TransferFunctionSetting}
import org.eso.ias.asce.{AsceStates, CompEleThreadFactory, ComputingElement}
import org.eso.ias.logging.IASLogger
import org.eso.ias.types._
import org.scalatest.flatspec.AnyFlatSpec

import scala.collection.mutable.{Map => MutableMap}
import scala.util.Try

/**
 * Test the python transfer class: build a PythonExecutorTF that, in turn,
 * delegates method execution to python code
 */
class TestMinMaxPyTF extends AnyFlatSpec {
  /** The logger */
  private val logger = IASLogger.getLogger(this.getClass)

  /** The ID of the DASU where the components runs */
  val supervId = new Identifier("SupervId",IdentifierType.SUPERVISOR,None)
  val dasId = new Identifier("DAS-ID",IdentifierType.DASU,supervId)

  /** The ID of the component running into the DASU */
  val compID = new Identifier("COMP-ID",IdentifierType.ASCE,Option(dasId))

  // The ID of the output generated by the component
  val outId = new Identifier("OutputId",IdentifierType.IASIO,Option(compID))

  // Build the MP in output
  // The inherited validity is undefined
  val output: InOut[Alarm] = InOut.asOutput(outId, IASTypes.ALARM)

  val threadFactory: CompEleThreadFactory = new CompEleThreadFactory("Test-runningId")

  val numOfInputs = 1

  // The IDs of the monitor point in input
  // to pass when building a Component
  val inputID = "MPointID"
  val mpId = new Identifier(inputID,IdentifierType.IASIO,Option(compID))
  val mp = InOut.asInput(mpId,IASTypes.LONG).updateValue(Some(1L)).updateProdTStamp(System.currentTimeMillis())
  val inputsMPs: MutableMap[String, InOut[_]] = MutableMap[String, InOut[_]]()
  inputsMPs+=(mp.id.id -> mp)

  // The threshold to assess the validity from the arrival time of the input
  val validityThresholdInSecs = 2

  // Instantiate on ASCE with a java TF implementation
  val pythonTFSetting =new TransferFunctionSetting(
      "IasTransferFunction.Impls.MinMaxThreshold",
      TransferFunctionLanguage.python,
      None,
      threadFactory)

  val props = new Properties();
  props.setProperty("HighOn","50")
  props.setProperty("HighOff","40")
  props.setProperty("LowOn","-20")
  props.setProperty("LowOff","-10")
  props.setProperty("Priority", "SET_CRITICAL")

  val javaComp: ComputingElement[Alarm] = new ComputingElement[Alarm](
    compID,
    output,
    inputsMPs.values.toSet,
    pythonTFSetting,
    validityThresholdInSecs,
    props) with JavaTransfer[Alarm]

  behavior of "The python MinMaxThreshold TF"

  it must "load and initialize the python object" in {
    logger.info("Testing initialize")
    val ret = javaComp.initialize()
    assert(ret!=AsceStates.TFBroken, "Error initializing the TF")
    logger.info("Initialize tested")
  }

  it must "run the python TF" in {
    logger.info("Testing transfer function execution")
    val inputs = Map(inputID -> mp)
    val newOut: Try[InOut[Alarm]] = javaComp.transfer(inputs,compID,output)
    logger.info("TF execution tested")
    assert(newOut.isSuccess,"Exception got from the TF")

    val out: InOut[Alarm] = newOut.get
    assert (out.iasType==IASTypes.ALARM,"The TF produced a value of the worng type "+out.iasType )
    assert(out.value.isDefined)
    assert(out.value.get==Alarm.CLEARED)

    assert (out.id.id==outId.id,"Unexpected output ID")
    assert(out.id.fullRunningID==outId.fullRunningID,"Unexpected output full running ID")

    assert(out.props.isDefined)
    val props = out.props.get
    assert(props.size==1,"The TF should have set one and only one property")
    val propValue = props.get("actualValue")
    assert(propValue.isDefined,"Property actualValue NOT set")
    assert(propValue.get=="1")

    assert(out.mode==OperationalMode.UNKNOWN)
  }

  it must "set the mode of the input in the output" in {
    logger.info("Testing the setting of the Operational mode")
    val inputs = Map(inputID -> mp.updateMode(OperationalMode.OPERATIONAL))
    val newOut: Try[InOut[Alarm]] = javaComp.transfer(inputs,compID,output)
    assert(newOut.isSuccess,"Exception got from the TF")
    val out = newOut.get
    assert(out.value.isDefined)
    assert(out.mode==OperationalMode.OPERATIONAL)
  }

  it must "activate the output depending on the value of the input" in {
    logger.info("Testing the functioning of the pyton TF")
    val inputs = Map(inputID -> mp.updateValue(Some(3L)))
    val newOut: Try[InOut[Alarm]] = javaComp.transfer(inputs,compID,output)
    assert(newOut.isSuccess,"Exception got from the TF")
    val out: InOut[Alarm] = newOut.get
    assert(out.value.isDefined)
    assert(out.value.get==Alarm.CLEARED)

    // Do not activate between HOff and HOn
    // Remain active between hOFF and hON
    val inputs2 = Map(inputID -> mp.updateValue(Some(45L)))
    val newOut2: Try[InOut[Alarm]] = javaComp.transfer(inputs2,compID,out)
    assert(newOut2.isSuccess,"Exception got from the TF")
    val out2 = newOut2.get
    assert(out2.value.isDefined)
    assert(out2.value.get==Alarm.CLEARED)

    // Activation by too high
    val inputs3 = Map(inputID -> mp.updateValue(Some(55L)))
    val newOut3: Try[InOut[Alarm]] = javaComp.transfer(inputs3,compID,out2)
    assert(newOut3.isSuccess,"Exception got from the TF")
    val out3: InOut[Alarm] = newOut3.get
    assert(out3.value.isDefined)
    assert(out3.value.get==Alarm.SET_CRITICAL)

    // Remain active between hOFF and hON
    val inputs4 = Map(inputID -> mp.updateValue(Some(45L)))
    val newOut4: Try[InOut[Alarm]] = javaComp.transfer(inputs4,compID,out3)
    assert(newOut4.isSuccess,"Exception got from the TF")
    val out4 = newOut4.get
    assert(out4.value.isDefined)
    assert(out4.value.get==Alarm.SET_CRITICAL)

    // Deactivate below hOn
    val inputs5 = Map(inputID -> mp.updateValue(Some(35L)))
    val newOut5: Try[InOut[Alarm]] = javaComp.transfer(inputs5,compID,out4)
    assert(newOut5.isSuccess,"Exception got from the TF")
    val out5 = newOut5.get
    assert(out5.value.isDefined)
    assert(out5.value.get==Alarm.CLEARED)

    // Do not activate between lOn and lOff
    val inputs6 = Map(inputID -> mp.updateValue(Some(-15L)))
    val newOut6: Try[InOut[Alarm]] = javaComp.transfer(inputs6,compID,out5)
    assert(newOut6.isSuccess,"Exception got from the TF")
    val out6 = newOut6.get
    assert(out6.value.isDefined)
    assert(out6.value.get==Alarm.CLEARED)

    // Activate between lOn and lOff
    val inputs7 = Map(inputID -> mp.updateValue(Some(-25L)))
    val newOut7: Try[InOut[Alarm]] = javaComp.transfer(inputs7,compID,out6)
    assert(newOut7.isSuccess,"Exception got from the TF")
    val out7 = newOut7.get
    assert(out7.value.isDefined)
    assert(out7.value.get==Alarm.SET_CRITICAL)

    // Do not activate between lOn and lOff
    val inputs8 = Map(inputID -> mp.updateValue(Some(-15L)))
    val newOut8: Try[InOut[Alarm]] = javaComp.transfer(inputs8,compID,out7)
    assert(newOut8.isSuccess,"Exception got from the TF")
    val out8 = newOut8.get
    assert(out8.value.isDefined)
    assert(out8.value.get==Alarm.SET_CRITICAL)

    // Deactivate when greater than lOff
    val inputs9 = Map(inputID -> mp.updateValue(Some(0L)))
    val newOut9: Try[InOut[Alarm]] = javaComp.transfer(inputs9,compID,out8)
    assert(newOut9.isSuccess,"Exception got from the TF")
    val out9 = newOut9.get
    assert(out9.value.isDefined)
    assert(out9.value.get==Alarm.CLEARED)
  }

}
