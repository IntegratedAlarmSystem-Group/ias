package org.eso.ias.tranfer.utlitymodule

import java.util.Properties

import com.typesafe.scalalogging.Logger
import org.eso.ias.asce.exceptions.{TypeMismatchException, UnexpectedNumberOfInputsException}
import org.eso.ias.asce.transfer.{IasIO, IasioInfo, ScalaTransferExecutor}
import org.eso.ias.logging.IASLogger
import org.eso.ias.types.Alarm
import org.eso.ias.types.IASTypes._

/**
  * The transfer function for the Fire alarm of the utility module
  *
  * This TF takes only one input string, the status word read from the utility module.
  *
  *
  * @param asceId : the ID of the ASCE
  * @param asceRunningId: the runningID of the ASCE
  * @param validityTimeFrame: The time frame (msec) to invalidate monitor points
  * @param props: the user defined properties
  ** @author acaproni
  */
class UMFireTF(asceId: String, asceRunningId: String, validityTimeFrame:Long, props: Properties)
  extends ScalaTransferExecutor[Alarm](asceId,asceRunningId,validityTimeFrame,props) {

  /**
    * @see TransferExecutor#shutdown()
    */
  override def shutdown(): Unit = {
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

    val fireActivated: Boolean = status.statusOf(StatusWord.Fire) && status.statusOf(StatusWord.ACPower)

    val newOutput = actualOutput.updateValue(actualOutput.value.getOrElse(Alarm.getInitialAlarmState).setIf(fireActivated))

    newOutput.updateProps(input.props).updateMode(input.mode)
  }
}

  object UMFireTF {

    /** The logger */
    val logger: Logger = IASLogger.getLogger(UMFireTF.getClass)

  }
