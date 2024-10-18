package org.eso.ias.tranfer

import java.util.Properties

import com.typesafe.scalalogging.Logger
import org.eso.ias.asce.transfer.{IasIO, IasioInfo, ScalaTransferExecutor}
import org.eso.ias.logging.IASLogger
import org.eso.ias.types.{Alarm, IASTypes, OperationalMode}

import scala.util.matching.Regex

/**
  * The AntPadInhibitor transfer functions gets 2 inputs:
  * - the association of antennas to pads (string)
  * - an ALARM
  * It inhibits the alarm if there are no antennas in the pads
  * whose names match with the given pattern.
  * It is also possible to add a filter by antenna type by setting
  * antTypePropName to one of the possible antenna types: the
  * alarm in output is set if the alarm in input is set AND
  * there are antennas in the pads AND
  * at least one antenna of the passed type.
  *
  * No check is done on the ID of the alarm in input.
  * The TF produces an alarm that is always CLEAR if there
  * are no antennas in the pads or the alarm in input is CLEAR.
  * If the input is SET and there are antennas in the PAD, the output
  * is set with the same priority of the alarm in input.
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
  val antPadRegExp: Regex = {
    val propVal = Option(props.getProperty(AntPadInhibitor.PadNameMatcherName))
      require(propVal.isDefined,AntPadInhibitor.PadNameMatcherName+" property not defined")
    new Regex(propVal.get)

  }
  AntPadInhibitor.logger.info("Pad names pattern: {}",antPadRegExp.toString())

  /*
   *The optional antenna type to add the filter by antenna type
   *
   * The type can only be one if the string in AntPadInhibitor.AntennaTypes
   */
  val antType: Option[String] = {
    val propVal = Option(props.getProperty(AntPadInhibitor.AntTypePropName))
    propVal.map(antTypeStr => {
      val antTypeUpperCase = antTypeStr.toUpperCase
      require(AntPadInhibitor.AntennaTypes.contains(antTypeUpperCase),
        "Unrecognized antenna type: "+antTypeUpperCase+" not in "+AntPadInhibitor.AntTypePropName.mkString(","))
      AntPadInhibitor.logger.info("Added a filter by antenna type {}",antTypeUpperCase)
      antTypeUpperCase
    })
  }

  /**
    * Initialize the TF by making some consistency checks
    *
    * @param inputsInfo The IDs and types of the inputs
    * @param outputInfo The Id and type of thr output
    **/
  override def initialize(inputsInfo: Set[IasioInfo], outputInfo: IasioInfo): Unit = {
    require(inputsInfo.size==2,"Expected inputs are the alarm and the anttenna to pad monitor points")

    val antsPadsIasioInfo = inputsInfo.filter(_.iasioId==AntPadInhibitor.AntennasToPadsID)
    require(antsPadsIasioInfo.nonEmpty,AntPadInhibitor.AntennasToPadsID+" is not in input")
    require (antsPadsIasioInfo.head.iasioType==IASTypes.STRING,AntPadInhibitor.AntennasToPadsID+" is not a STRING")

    val otherIasios =  inputsInfo--antsPadsIasioInfo
    require(otherIasios.head.iasioType==IASTypes.ALARM,"Expected ALARM input missing")
    AntPadInhibitor.logger.info("Input is {}",otherIasios.head.iasioId)

    require(outputInfo.iasioType==IASTypes.ALARM,"Wrong type of output (ALARM expected)")
  }

  /**
    * @see TransferExecutor#shutdown()
    */
  override def shutdown(): Unit = {}

  /**
    * Build the comma separated list of names of affected antennas, i.e.
    * the names of the antennas whose names match with
    * the regular expression passed in the java properties
    *
    * @param antsPadsMP: the antennas to pads string received in input
    * @return the comma separated list of names of affected antennas
    */
  def affectedAntennas(antsPadsMP: String): String = {
    // Association of antennas to pad: one entry for each antenna like DV02:A507
    val antsPads = antsPadsMP.split(",")

    // Select only the antennas that are in the proper pads
    val antennasInPads= antsPads.filter(antPad => {
      assert(antPad.isEmpty || antPad.count(_==':')==1,"Antenna/Pad mismatch: \""+antPad+"\" should be name:pad")
      val couple = antPad.split(":")
      antPad.nonEmpty &&
        antPadRegExp.pattern.matcher(couple(1)).matches() &&
        antType.forall(aType => couple(0).toUpperCase().startsWith(aType))
    })

    // Extracts only the names of the antennas
    antennasInPads.map(ap => ap.split(":")(0)).mkString(",")
  }

  /**
    * Produces the output of the component by evaluating the inputs.
    *
    * @return the computed output of the ASCE
    */
  override def eval(compInputs: Map[String, IasIO[?]], actualOutput: IasIO[Alarm]): IasIO[Alarm] = {
    val antPadMp= getValue(compInputs,AntPadInhibitor.AntennasToPadsID)
    assert(antPadMp.isDefined,AntPadInhibitor.AntennasToPadsID+" inputs not defined!")

    val antPadMPValue = antPadMp.get.value.get.asInstanceOf[String]

    val antennasInPads = affectedAntennas(antPadMPValue)
    val foundAntennaInPad = antennasInPads.nonEmpty

    val alarmInput: IasIO[?] = compInputs.values.filter(_.iasType==IASTypes.ALARM).head

    val actualAlarm: Alarm = actualOutput.value.getOrElse(Alarm.getInitialAlarmState)

    val mode = if (alarmInput.mode==OperationalMode.OPERATIONAL && antPadMp.get.mode==OperationalMode.OPERATIONAL) {
      OperationalMode.OPERATIONAL
    } else if (antPadMp.get.mode==OperationalMode.OPERATIONAL) {
      alarmInput.mode
    } else {
      OperationalMode.UNKNOWN
    }

    val alarmOut =
      actualAlarm.setIf(foundAntennaInPad && alarmInput.value.get.asInstanceOf[Alarm].isSet)
        .setPriority(alarmInput.value.get.asInstanceOf[Alarm].priority)

    val outputWithUpdatedMode=actualOutput.updateValue(alarmOut).updateMode(mode)
    if (foundAntennaInPad && alarmOut.isSet)
      outputWithUpdatedMode.updateProps(alarmInput.props++Map(AntPadInhibitor.AffectedAntennaAlarmPropName -> antennasInPads))
    else
      outputWithUpdatedMode.updateProps(alarmInput.props)

  }

}

object AntPadInhibitor {

  /** The logger */
  val logger: Logger = IASLogger.getLogger(AntPadInhibitor.getClass)

  /** The name of the property to pass the regular expression */
  val PadNameMatcherName: String = "org.eso.ias.antpadinhibitor.padnameregexp"

  /** The name of the property to pass the regular expression */
  val AntTypePropName: String = "org.eso.ias.antpadinhibitor.anttype"

  /** The ID of the monitor point with the position (pad) of the antennas */
  val AntennasToPadsID="Array-AntennasToPads"

  /** The possible antenna type to be set in the AntTypePropName property */
  val AntennaTypes = List("DV","DA","CM","PM")

  /** The property set in the alarm in output ith the list of affected antennas */
  val AffectedAntennaAlarmPropName = "affectedAntennas"
}
