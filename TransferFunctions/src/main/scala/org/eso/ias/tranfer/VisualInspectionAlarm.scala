package org.eso.ias.tranfer

import java.util.Properties

import org.eso.ias.asce.exceptions.{TypeMismatchException, UnexpectedNumberOfInputsException}
import org.eso.ias.asce.transfer.{IasIO, IasioInfo, ScalaTransferExecutor}
import org.eso.ias.logging.IASLogger
import org.eso.ias.types.{Alarm, IASTypes}
import org.eso.ias.utils.ISO8601Helper

/**
  * The transfer function that generates an alarm is the wind speed is over a threshold
  * and a visual inspection is still pending.
  *
  * The inputs of the  VisualInspectionAlarm TF are:
  * - alarm that the windspeed passed the threshold
  * - date (String) when the last inspection has been done
  *
  * If the last inspection date is older then the activation date of the alarm,
  * the alarm reamions active even if the wind speed drops below the threshold.
  *
  * To keep this TF generic, the IDs of the inputs are not hardcoded but are
  * retrieved at run time from the types of the inputs.
  *
  * @param cEleId: The id of the ASCE
  * @param cEleRunningId: the running ID of the ASCE
  * @param validityTimeFrame: The time frame (msec) to invalidate monitor points
  * @param props: The properties for the executor
  */
class VisualInspectionAlarm(cEleId: String, cEleRunningId: String, validityTimeFrame: Long, props: Properties)
  extends ScalaTransferExecutor[Alarm](cEleId,cEleRunningId,validityTimeFrame,props) {

  /** The ID of the ALARM in input */
  var idOfAlarmInput: String = _

  /** The ID of the timestamp in input */
  var idOfTstampInput: String = _

  /** The point in time of the last timestamp */
  var lastInspectionTimestamp: Long = -1

  /**
    * The point in time when the alarm was cleared
    *
    * This needs to be recorded because the visual inspection must be done after
    * the alarm is cleared
    *
    * It must be greater the the [[lastInspectionTimestamp]] to cope with the initial case
    */
  var alarmDeactivationTimestamp: Long = 0

  /**
    * The initialization checks if there are only 2 inputs one of type ALARM and one of type STRING
    *
    * @param inputsInfo The IDs and types of the inputs
    * @param outputInfo The Id and type of thr output
    **/
  override def initialize(inputsInfo: Set[IasioInfo], outputInfo: IasioInfo): Unit = {
    if (inputsInfo.size!=2) {
      throw new UnexpectedNumberOfInputsException(2,inputsInfo.size)
    }
    if (outputInfo.iasioType!=IASTypes.ALARM) {
      throw new TypeMismatchException("Output should be ALARM, found "+outputInfo.iasioType)
    }

    inputsInfo.foreach(info => {
      (info.iasioType, info.iasioId) match {
        case (IASTypes.ALARM, id) => idOfAlarmInput = id
        case (IASTypes.STRING, id) => idOfTstampInput = id
        case (_, _) => throw new TypeMismatchException("Invalid input type: found " + info.iasioType + " expected STRING and ALARM")
      }
    })
    if (Option(idOfAlarmInput).isEmpty || Option(idOfTstampInput).isEmpty)
      throw new TypeMismatchException("Invalid type of inputs")
  }

  /**
    * @see TransferExecutor#shutdown()
    */
  override def shutdown() {
    VisualInspectionAlarm.logger.debug("TF of [{}] shut down", cEleId)
  }

  /**
    * Produces the output of the component by evaluating the inputs.
    *
    * @return the computed output of the ASCE
    */
  override def eval(compInputs: Map[String, IasIO[_]], actualOutput: IasIO[Alarm]): IasIO[Alarm] = {
    val alarmInput: IasIO[_] = compInputs(idOfAlarmInput)
    val visualInput: IasIO[_] = compInputs(idOfTstampInput)

    // Update the timestamp
    lastInspectionTimestamp=lastInspectionTimestamp.max(ISO8601Helper.timestampToMillis(visualInput.value.get.toString))

    if (alarmInput.value.get.asInstanceOf[Alarm].isSet) {
      // If the alarm is set, the output must always be set
      if (actualOutput.value.isDefined && actualOutput.value.get.isSet) actualOutput
      else actualOutput.updateValue(Alarm.getSetDefault)
    } else {
      // The wind is below the threshold but we must check the timestamp of the visual
      // inspection before clearing the output
      if (lastInspectionTimestamp>alarmDeactivationTimestamp) {
        if (actualOutput.value.isDefined && !actualOutput.value.get.isSet) actualOutput
        else actualOutput.updateValue(Alarm.CLEARED)
      } else {
        if (actualOutput.value.isDefined && actualOutput.value.get.isSet) actualOutput
        else actualOutput.updateValue(Alarm.getSetDefault)
      }
    }
  }
}

/** Companion object */
object VisualInspectionAlarm {
  /** The logger */
  private val logger = IASLogger.getLogger(VisualInspectionAlarm.getClass)
}
