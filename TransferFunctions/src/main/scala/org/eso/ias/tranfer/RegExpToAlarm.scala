package org.eso.ias.tranfer

import java.util.Properties

import com.typesafe.scalalogging.Logger
import org.eso.ias.asce.exceptions.{TypeMismatchException, UnexpectedNumberOfInputsException}
import org.eso.ias.asce.transfer.{IasIO, IasioInfo, ScalaTransferExecutor}
import org.eso.ias.logging.IASLogger
import org.eso.ias.types.{Alarm, IASTypes}

import scala.util.matching.Regex

/**
  * A TF to generate alarms if the value of the input (STRING) matches
  * with the passed regular expression
  *
  * @param cEleId: The id of the ASCE
  * @param cEleRunningId: the running ID of the ASCE
  * @param validityTimeFrame: The time frame (msec) to invalidate monitor points
  * @param props: The properties for the executor
  * @author acaproni
  */
class RegExpToAlarm (cEleId: String, cEleRunningId: String, validityTimeFrame: Long, props: Properties)
  extends ScalaTransferExecutor[Alarm](cEleId,cEleRunningId,validityTimeFrame,props) {

  val regExString: String = props.getProperty(RegExpToAlarm.RegExpPropName)
  require(Option(regExString).isDefined, "Property " + RegExpToAlarm.RegExpPropName + " not defined")

  /** Th eregular expression used for matching */
  val regExp: Regex = regExString.r

  /** The alarm to set in the output */
  val priority: Alarm = Option(props.getProperty(BoolToAlarm.PriorityPropName)).
    map(Alarm.valueOf).
    getOrElse(BoolToAlarm.DefaultPriority)
  require(priority != Alarm.CLEARED)

  /**
    * Initialize the TF: check that the input is a boolean
    *
    * @param inputsInfo The IDs and types of the inputs
    * @param outputInfo The Id and type of the output
    **/
  override def initialize(inputsInfo: Set[IasioInfo], outputInfo: IasioInfo): Unit = {
    // This TF expects one and only one input
    if (inputsInfo.size != 1) {
      throw new UnexpectedNumberOfInputsException(1, inputsInfo.size)
    }
    // Is the input a boolean?
    if (inputsInfo.head.iasioType != IASTypes.STRING) {
      throw new TypeMismatchException("Input type is not STRING: " + inputsInfo.head.iasioType)
    }

    RegExpToAlarm.logger.info("Regular expression to match: {}", regExString)
    RegExpToAlarm.logger.info("Output priority set to {}", priority.toString)
  }

  /**
    * @see TransferExecutor#shutdown()
    */
  override def shutdown() {
    RegExpToAlarm.logger.debug("TF of [{}] shut down", cEleId)
  }

  /**
    * Produces the output of the component by evaluating the inputs.
    *
    * @return the computed output of the ASCE
    */
  override def eval(compInputs: Map[String, IasIO[_]], actualOutput: IasIO[Alarm]): IasIO[Alarm] = {
    val input = compInputs.values.head

    val value = input.value.get.asInstanceOf[String]

    val outputAlarm = if (regExp.findFirstIn(value).isDefined) priority else Alarm.CLEARED

    actualOutput.updateValue(outputAlarm).updateProps(input.props).updateMode(input.mode)
  }
}

object RegExpToAlarm {
  /**
    * The logger
    */
  val logger: Logger = IASLogger.getLogger(RegExpToAlarm.getClass)

    /** The name of the property to pass the regular expression */
  val RegExpPropName = "org.eso.ias.tf.regextoalarm.value"

  /** The nam eof the property to set the prioity of the output */
  val PriorityPropName: String = "org.eso.ias.tf.booltoalarm.priority"

  /** Defaul tpriority level of the output */
  val DefaultPriority = Alarm.getSetDefault
}
