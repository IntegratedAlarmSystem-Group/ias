package org.eso.ias.transfer.test

import java.util.Properties

import org.eso.ias.asce.transfer.{IasIO, IasioInfo}
import org.eso.ias.logging.IASLogger
import org.eso.ias.tranfer.AntPadInhibitor
import org.eso.ias.types._
import org.scalatest.{BeforeAndAfterEach, FlatSpec}

/**
  * Test the AntPadInhibitor
  *
  * The TF takes 2 inputs and produces one ouput.
  * One of the input is always the antenna/pad association while the other one is an alarm.
  */
class AntPadInhibitorTest extends FlatSpec with BeforeAndAfterEach {

  /** The logger */
  val logger = IASLogger.getLogger(this.getClass)

  /** The name of the pads matches with the following string */
  val padPattern = "A.*"

  /** Default set of properties for testing */
  val defaultProps: Properties = new Properties

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

   /** Default AntPadInhibitor TF to test */
  var defaultTF: AntPadInhibitor = _

  /** The time frame for the validity */
  val validityTimeFrame = 2000

  /** The ID of the antenna/pad inputof type String  */
  val antPadInputId = AntPadInhibitor.AntennasToPadsID

  /** The Identifier of the ALARM input */
  val antPadIdentifier= new Identifier(antPadInputId,IdentifierType.IASIO, Some(compID))

  /** The id of the alarm in input */
  val inputId = "InputId"

  /** The Identifier of the ALARM input */
  val inputIdentifier= new Identifier(inputId,IdentifierType.IASIO, Some(compID))

  /** Inputs Info for the initialize */
  val inputInOut =  InOut.asInput(inputIdentifier,IASTypes.ALARM)

  /** Inputs Info for the initialize */
  val antPadInOut =  InOut.asInput(antPadIdentifier,IASTypes.STRING)


  /** Inputs Info for the initialize */
  val inputInfos = Set(
    new IasioInfo(inputId,IASTypes.ALARM),
    new IasioInfo(antPadInputId,IASTypes.STRING)
  )


  override def beforeEach() : scala.Unit = {
    logger.info("Initializing before running a test")

    defaultProps.put(AntPadInhibitor.PadNameMatcherName, padPattern)

    defaultTF = new AntPadInhibitor(compID.id, compID.fullRunningID, validityTimeFrame, defaultProps)
  }

  behavior of "The AntPadInhibitor TF"

  it must "get the ant/pad regular expression from the properties" in {
    assert(defaultTF.antPadRegExp.toString()==padPattern)
  }

  it must "return CLEAR when ant/pad input is empty" in {
    val i: InOut[_] = inputInOut.updateValue(Some(Alarm.SET_HIGH))
    val a: InOut[_] = antPadInOut.updateValue(Some(""))

    val inputMap: Map[String,IasIO[_]] = Map(inputId-> new IasIO(i), antPadInputId-> new IasIO(a))

    val ret = defaultTF.eval(inputMap,out)

    assert(ret.value.isDefined)
    assert(ret.value.get.asInstanceOf[Alarm]==Alarm.CLEARED)

  }

  it must "return CLEAR when there are no antennas in the pads" in {
    val i: InOut[_] = inputInOut.updateValue(Some(Alarm.SET_HIGH))
    val a: InOut[_] = antPadInOut.updateValue(Some("DA41:W001,DV01:S003"))

    val inputMap: Map[String,IasIO[_]] = Map(inputId-> new IasIO(i), antPadInputId-> new IasIO(a))

    val ret = defaultTF.eval(inputMap,out)

    assert(ret.value.isDefined)
    assert(ret.value.get.asInstanceOf[Alarm]==Alarm.CLEARED)

  }

  it must "return SET when there are antennas in the pads" in {
    val i: InOut[_] = inputInOut.updateValue(Some(Alarm.SET_HIGH))
    val a: InOut[_] = antPadInOut.updateValue(Some("DA41:W001,DV01:S003,DA54:A025"))

    val inputMap: Map[String,IasIO[_]] = Map(inputId-> new IasIO(i), antPadInputId-> new IasIO(a))

    val ret = defaultTF.eval(inputMap,out)

    assert(ret.value.isDefined)
    assert(ret.value.get.asInstanceOf[Alarm]==Alarm.SET_HIGH)

  }

  it must "return CLEAR when there are antennas in the pads but input is CLEAR" in {
    val i: InOut[_] = inputInOut.updateValue(Some(Alarm.CLEARED))
    val a: InOut[_] = antPadInOut.updateValue(Some("DA41:W001,DV01:S003,DA54:A025"))

    val inputMap: Map[String,IasIO[_]] = Map(inputId-> new IasIO(i), antPadInputId-> new IasIO(a))

    val ret = defaultTF.eval(inputMap,out)

    assert(ret.value.isDefined)
    assert(ret.value.get.asInstanceOf[Alarm]==Alarm.CLEARED)

  }


}
