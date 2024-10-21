package org.eso.ias.tranfer

import java.util.Properties

import com.typesafe.scalalogging.Logger
import org.eso.ias.asce.exceptions.{TypeMismatchException, UnexpectedNumberOfInputsException}
import org.eso.ias.asce.transfer.{IasIO, IasioInfo, ScalaTransferExecutor}
import org.eso.ias.logging.IASLogger
import org.eso.ias.types.IasValidity

/**
  * A TF very similar to [[BackupSelector]] but that does not take into account
  * the operational state of the inputs as it copes with relocation instead of
  * duplication for which only the validity is to be considered.
  *
  * One and only one input is expected to be valid at a given point in time.
  *
  * The TF expects at least 2 inputs, each of which represent one of the possible
  * place of relocation. None of the inputs is more important than the others.
  * The TF does not make any assumption of the type of the inputs neither on thier values
  * because they are not relevant.
  * For consistency as all the inputs refer to the same monitor point, it is reasonable
  * to expect that all of them have the same type regardless of what it is.
  *
  * [[BackupSelector]] aims to select one input that is produced by replicated plugins
  * that monitors the same device with different meanings so that if one fails, one of the other
  * provides the information.
  * [[RelocationSelector()]] instead aims to select one input that is produce by only one of the inputs for example
  * as consequence of relocation. The difference with [[BackupSelector]] is that one and only one
  * input is expected to be valid at a given point in time.
  *
  * @param cEleId: The id of the ASCE
  * @param cEleRunningId: the running ID of the ASCE
  * @param validityTimeFrame: The time frame (msec) to invalidate monitor points
  * @param props: The properties for the executor
  * @author acaproni
  */
class RelocationSelector[T](cEleId: String, cEleRunningId: String, validityTimeFrame: Long, props: Properties)
extends ScalaTransferExecutor[T](cEleId,cEleRunningId,validityTimeFrame,props) {

  /**
    * Initialize the TF.
    *
    * Checks that there are at least 2 inputs defined and that thier types are the same.
    *
    * @param inputsInfo The IDs and types of the inputs
    * @param outputInfo The Id and type of thr output
    **/
  override def initialize(inputsInfo: Set[IasioInfo], outputInfo: IasioInfo): Unit = {
    if (inputsInfo.size < 2) {
      throw new UnexpectedNumberOfInputsException(2, inputsInfo.size)
    }
    if (inputsInfo.exists(_.iasioType != inputsInfo.head.iasioType)) {
      throw new TypeMismatchException("Inputs have different types " + inputsInfo.map(_.iasioType).mkString(","))
    }
    if (outputInfo.iasioType!=inputsInfo.head.iasioType) {
      throw new TypeMismatchException("Output must be of the same type of inputs the")
    }
    RelocationSelector.logger.debug("Intialized")
  }

  /**
    * @see TransferExecutor#shutdown()
    */
  override def shutdown(): Unit = {
    RelocationSelector.logger.debug("Closed")
  }

  /**
    * The output is the input that is valid. If this TF is correctly used,
    * only ine input is valid at a given point in time as effect of relocation.
    *
    * If no inputs are valid, the TF return the actual output
    *
    * @return the computed output of the ASCE
    */
  override def eval(compInputs: Map[String, IasIO[?]], actualOutput: IasIO[T]): IasIO[T] = {

    // Gets the first input that is reliable by time and from inputs.
    val selectedInput: Option[IasIO[T]] = compInputs.values.find(input => {
      // get the validity from the inputs
      val inputValidityByTime = input.validityOfInputByTime(validityTimeFrame)
      val inputValidityFromInputs = input.validity
      inputValidityByTime == IasValidity.RELIABLE && inputValidityFromInputs == IasValidity.RELIABLE
    }).asInstanceOf[Option[IasIO[T]]]

    ( selectedInput, actualOutput.value)  match {
      case (Some(out), _) =>
        actualOutput.
          updateValue(out.value.get).
          updateMode(out.mode).
          updateProps(out.props++Map(RelocationSelector.SeletedInputPropName->out.id)).
          setValidityConstraint(Some(Set(out.id)))
      case ( None, None ) =>
        val firstInput = compInputs.values.head
        actualOutput.
          updateValue(firstInput.value.get).
          updateProps(firstInput.props++Map(RelocationSelector.SeletedInputPropName->firstInput.id))
      case ( _, _ ) => actualOutput
    }

  }
}

object RelocationSelector {
  /**
    * The logger
    */
  val logger: Logger = IASLogger.getLogger(RelocationSelector.getClass)

  /**
    * The key of the property to be added to the properties of the output and reporting which
    * of the inputs has been selected to generate the output
    */
  val SeletedInputPropName: String = "selectedByRelocationInput"
}
