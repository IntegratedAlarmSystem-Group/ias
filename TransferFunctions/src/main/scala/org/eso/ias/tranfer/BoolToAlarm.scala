package org.eso.ias.tranfer

import java.util.Properties

import com.typesafe.scalalogging.Logger
import org.eso.ias.asce.exceptions.{TypeMismatchException, UnexpectedNumberOfInputsException}
import org.eso.ias.asce.transfer.{IasIO, IasioInfo, ScalaTransferExecutor}
import org.eso.ias.logging.IASLogger
import org.eso.ias.types.{Alarm, IASTypes}
/**
  * This transfer function generate an alarm depending on th evalue of the boolean in input:
  * if the value of the input is TRUE then set the alarm in output, otherwise the alarm is cleared.
  *
  * To invert the logic (i.e. generate an alarm when the boolean is False), set the BoolToAlarm.InvertLogicPropName
  * property to true
  *
  * @param cEleId: The id of the ASCE
  * @param cEleRunningId: the running ID of the ASCE
  * @param validityTimeFrame: The time frame (msec) to invalidate monitor points
  * @param props: The properties for the executor
  * @author acaproni
  */
class BoolToAlarm(cEleId: String, cEleRunningId: String, validityTimeFrame: Long, props: Properties)
extends ScalaTransferExecutor[Alarm](cEleId,cEleRunningId,validityTimeFrame,props) {

  /**
    * If set the logic is inverted
    *
    * By default it is false because of the definition of java.lang.Boolean.getBoolean
    */
  val invert: Boolean = Option(props.getProperty(BoolToAlarm.InvertLogicPropName)).
    map(java.lang.Boolean.valueOf(_).booleanValue()).
    getOrElse(BoolToAlarm.DefaultLogicPropValue.booleanValue())

  /** The alarm to set in the output */
  val priority: Alarm = Option(props.getProperty(BoolToAlarm.PriorityPropName)).
      map(Alarm.valueOf).
      getOrElse(BoolToAlarm.DefaultPriority)
  require(priority!=Alarm.CLEARED)

  /**
    * Initialize the TF: check that the input is a boolean
    *
    * @param inputsInfo The IDs and types of the inputs
    * @param outputInfo The Id and type of the output
    **/
  override def initialize(inputsInfo: Set[IasioInfo], outputInfo: IasioInfo): Unit = {
    BoolToAlarm.logger.debug("Initializing TF of [{}]", cEleId)

    // This TF expects one and only one input
    if (inputsInfo.size != 1) {
      throw new UnexpectedNumberOfInputsException(1,inputsInfo.size)
    }
    // Is the input a boolean?
    if (inputsInfo.head.iasioType != IASTypes.BOOLEAN) {
      throw new TypeMismatchException("Input type is not boolean: " + inputsInfo.head.iasioType)
    }

    if (invert) BoolToAlarm.logger.info("The logic of this TF is inverted")
    BoolToAlarm.logger.info("Output priority set to {}",priority.toString)
  }

  /**
   * @see TransferExecutor#shutdown()
   */
  override def shutdown() {
    BoolToAlarm.logger.debug("TF of [{}] shut down", cEleId)
  }

  /**
	 * Produces the output of the component by evaluating the inputs.
	 *
	 * @return the computed output of the ASCE
	 */
	override def eval(compInputs: Map[String, IasIO[_]], actualOutput: IasIO[Alarm]): IasIO[Alarm] = {
    val input =   compInputs.values.head

    val value = {
      val inputValue = input.value.get.asInstanceOf[Boolean]
      if (!invert) inputValue
      else !inputValue
    }

    val outputAlarm = if (value) priority else Alarm.CLEARED

    actualOutput.updateValue(outputAlarm).updateProps(input.props).updateMode(input.mode)
  }

}

object BoolToAlarm {
  /**
   * The logger
   */
  val logger: Logger = IASLogger.getLogger(BoolToAlarm.getClass)

  /** The name of the boolean property to invert the logic */
  val InvertLogicPropName: String = "org.eso.ias.tf.booltoalarm.invert"

  /** By default the logic is not inverted */
  val DefaultLogicPropValue: Boolean = false

  /** The nam eof the property to set the prioity of the output */
  val PriorityPropName: String = "org.eso.ias.tf.booltoalarm.priority"

  /** Defaul tpriority level of the output */
  val DefaultPriority = Alarm.SET_MEDIUM
}
