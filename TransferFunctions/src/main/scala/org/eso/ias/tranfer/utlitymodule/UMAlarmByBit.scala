package org.eso.ias.tranfer.utlitymodule

import java.util.Properties

import com.typesafe.scalalogging.Logger
import org.eso.ias.asce.exceptions.{TypeMismatchException, UnexpectedNumberOfInputsException}
import org.eso.ias.asce.transfer.{IasIO, IasioInfo, ScalaTransferExecutor}
import org.eso.ias.logging.IASLogger
import org.eso.ias.types.Alarm
import org.eso.ias.types.IASTypes.{ALARM, STRING}

/**
  * The transfer function for to generate alarms if a bit of the UM is set
  * The number of the bit to check is passed through a java property
  *
  * @param asceId : the ID of the ASCE
  * @param asceRunningId: the runningID of the ASCE
  * @param validityTimeFrame: The time frame (msec) to invalidate monitor points
  * @param props: the user defined properties
  ** @author acaproni
  */
class UMAlarmByBit (asceId: String, asceRunningId: String, validityTimeFrame:Long, props: Properties)
  extends ScalaTransferExecutor[Alarm](asceId,asceRunningId,validityTimeFrame,props){

  /**
    * @see TransferExecutor#shutdown()
    */
  override def shutdown() {
    UMFireTF.logger.info("TF of ASCE [{}] closed", asceId)
  }

  /**
    * Initialize the TF
    *
    * @param inputsInfo The IDs and types of the inputs
    * @param outputInfo The Id and type of thr output
    **/
  override def initialize(inputsInfo: Set[IasioInfo], outputInfo: IasioInfo): Unit = {
    UMFireTF.logger.debug("TF of ASCE [{}] initializing", asceId)

    if (outputInfo.iasioType != ALARM) {
      throw new TypeMismatchException(outputInfo.iasioId, outputInfo.iasioType, ALARM)
    }

    if (inputsInfo.size!=1) {
      throw new UnexpectedNumberOfInputsException(1,inputsInfo.size)
    }

    if (inputsInfo.head.iasioType!=STRING) {
      throw new TypeMismatchException(inputsInfo.head.iasioId, inputsInfo.head.iasioType, STRING)
    }

    UMFireTF.logger.debug("TF of ASCE [{}] initialed", asceId)
  }

  /**
    * Produces the output of the component by evaluating the input
    *
    * @return the computed output of the ASCE
    */
  override def eval(compInputs: Map[String, IasIO[_]], actualOutput: IasIO[Alarm]): IasIO[Alarm] = {
    val input: IasIO[_] = compInputs.values.head
    val umStatusWord = input.value.get.asInstanceOf[String]

    val status = new StatusWord(umStatusWord)

    val fireActivated = status.statusOf(StatusWord.Fire) && status.statusOf(StatusWord.ACPower)

    val newOutput = if (fireActivated) actualOutput.updateValue(Alarm.SET_CRITICAL) else
      actualOutput.updateValue(Alarm.CLEARED)

    newOutput.updateProps(input.props).updateMode(input.mode)
  }

}

object UMAlarmByBit {
  /** The logger */
  val logger: Logger = IASLogger.getLogger(UMAlarmByBit.getClass)

  val BitProeprtyName = ""


}

