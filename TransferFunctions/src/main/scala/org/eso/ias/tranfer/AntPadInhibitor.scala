package org.eso.ias.tranfer

import java.util.Properties

import com.typesafe.scalalogging.Logger
import org.eso.ias.asce.transfer.{IasIO, IasioInfo, ScalaTransferExecutor}
import org.eso.ias.logging.IASLogger
import org.eso.ias.types.{Alarm, IASTypes, IASValue, OperationalMode}

import scala.util.matching.Regex

/**
  * The AntPadInhibitor transfer functions gets 2 inputs:
  * - the association of antennas to pads
  * - an ALARM
  * It inhibits the alarm if there are no antennas in the pads
  * whose name matches with the given pattern.
  *
  * No check is done on the ID of the input but it must be an ALARM.
  * The TF produces an alarm that is always CLEAR if there
  * are no antennas in the pads or the input is CLEAR.
  * If the input is SET and there are antennas in the PAD, the output
  * is set and with same priority of the input.
  *
  * The association of antennas to pad is a monitor point that contains
  * all the antennas and the names of the relative pads where they seat.
  * The matching is done by a regular expression provided as a java property.
  *
  * This is an example of the value (string) of the Array-AntennasToPads,
  * the monitor points that contains the pads of all the antennas:
  *   DV01:A045,DV02:A025,...
  */
class AntPadInhibitor(asceId: String, asceRunningId: String, validityTimeFrame:Long, props: Properties)
  extends ScalaTransferExecutor[Alarm](asceId,asceRunningId,validityTimeFrame,props) {

  /**
    * Names of pads must match with this regular expression
    */
  val antPadRegExp = {
    val propVal = Option(props.getProperty(AntPadInhibitor.PadNameMatcherName))
      require(propVal.isDefined,AntPadInhibitor.PadNameMatcherName+" property not defined")
    new Regex(propVal.get)

  }
  AntPadInhibitor.logger.info("Pad names pattern: {}",antPadRegExp.toString())

  /**
    * Initialize the TF by making some consistency checks
    *
    * @param inputsInfo The IDs and types of the inputs
    * @param outputInfo The Id and type of thr output
    **/
  override def initialize(inputsInfo: Set[IasioInfo], outputInfo: IasioInfo): Unit = {
    require(inputsInfo.size==2,"Expected inputs are the alarm and the anttenna to pad monitor points")

    val antsPadsIasioInfo = inputsInfo.filter(_.iasioId==AntPadInhibitor.AntennasToPadsID)
    require(!antsPadsIasioInfo.isEmpty,AntPadInhibitor.AntennasToPadsID+" is not in input")
    require (antsPadsIasioInfo.head.iasioType==IASTypes.STRING,AntPadInhibitor.AntennasToPadsID+" is not a STRING")

    val otherIasios =  inputsInfo--antsPadsIasioInfo
    require(otherIasios.head.iasioType==IASTypes.ALARM,"Expected ALARM input missing")
    AntPadInhibitor.logger.info("Input is {}",otherIasios.head.iasioId)

    require(outputInfo.iasioType==IASTypes.ALARM,"Wrong type of output (ALARM expected)")
  }

  /**
    * @see TransferExecutor#shutdown()
    */
  override def shutdown() {}

  /**
    * Check if there is at least one antenna in one of the pads whose name matches with
    * the regular expression passed int he java properties
    *
    * @param antsPadsMP: the natennas to pads string received in input
    * @return true if there is at least one antennas in the pads;
    *         false otherwise
    */
  def isAntennaInPad(antsPadsMP: String): Boolean = {
    // Return false if the input is empty as I imagine that
    // it could happen in operation that the association is
    // not available during short periods of time
    val antsPads = antsPadsMP.split(",")
    antsPads.exists(antPad => {
      assert(antPad.isEmpty || antPad.count(_==':')==1,"Antenna/Pad mismatch: \""+antPad+"\" should be name:pad")
      !antPad.isEmpty && antPadRegExp.pattern.matcher(antPad.split(":")(1)).matches()
    })
  }

  /**
    * Produces the output of the component by evaluating the inputs.
    *
    * @return the computed output of the ASCE
    */
  override def eval(compInputs: Map[String, IasIO[_]], actualOutput: IasIO[Alarm]): IasIO[Alarm] = {
    val antPadMp= getValue(compInputs,AntPadInhibitor.AntennasToPadsID)
    assert(antPadMp.isDefined,AntPadInhibitor.AntennasToPadsID+" inputs not defined!")

    val antPadMPValue = antPadMp.get.value.get.asInstanceOf[String]
    val foundAntennaInPad = isAntennaInPad(antPadMPValue)

    val alarmInput = compInputs.values.filter(_.iasType==IASTypes.ALARM).head


    val alarmOut = if (foundAntennaInPad) {
      actualOutput.updateValue(alarmInput.value.get)
    } else {
      // No antennas in the pads of the WS
      actualOutput.updateValue(Alarm.CLEARED)
    }

    val mode = if (alarmInput.mode==OperationalMode.OPERATIONAL && antPadMp.get.mode==OperationalMode.OPERATIONAL) {
      OperationalMode.OPERATIONAL
    } else {
      OperationalMode.UNKNOWN
    }

    alarmOut.updateMode(mode).updateProps(alarmInput.props)

  }

}

object AntPadInhibitor {

  /** The logger */
  val logger: Logger = IASLogger.getLogger(ThresholdWithBackupsAndDelay.getClass)

  /** The name of the property to pass the regular expression */
  val PadNameMatcherName: String = "org.eso.ias.antpadinhibitor.padnameregexp"

  /** The ID of the monitor point with the position (pad) of the antennas */
  val AntennasToPadsID="Array-AntennasToPad"
}
