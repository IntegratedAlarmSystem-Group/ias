package org.eso.ias.asce.transfer

import org.eso.ias.types.Identifier
import org.eso.ias.types.InOut
import org.eso.ias.asce.ComputingElement

import java.util.Optional
import scala.util.Try

/**
 * <code>PythonTransfer</code> calls the python
 * transfer function via the GrpcPythonExecutor.
 *
 * Note that the Validity of the output is not set by the transfer function
 * but automatically implemented by the ASCE
 */
trait PythonTransfer[T] extends ComputingElement[T] {

  /**
    * The programming language of this TF
    */
  val tfLanguage = TransferFunctionLanguage.python

  /**
    * Wraps the scala Map of InOut into a Map of IasIO.
    */
  private def wrapInputsIasIO(
      inputs: Map[String, InOut[?]]): Map[String, IasIO[?]] = {
    inputs.view.map { case (k, inout) =>
      (k, new IasIO(inout))
    }.toMap
  }

  /**
    * @see ComputingElement#transfer
    */
  def transfer(
      inputs: Map[String, InOut[?]],
      id: Identifier,
      actualOutput: InOut[T]): Try[InOut[T]] = {
    Try[InOut[T]]{
      val map: Map[String, IasIO[?]] = wrapInputsIasIO(inputs)
      val out: IasIO[T] = new IasIO(actualOutput)
      tfSetting.transferExecutor.get.asInstanceOf[PythonTransferExecutor[T]].
        eval(map, out).inOut.asInstanceOf[InOut[T]]
    }
  }

  /**
    * Initialize the python transfer function
    *
    * @param inputsInfo The IDs and types of the inputs
    * @param outputInfo The Id and type of thr output
    * @param instance the instance
    * @return
    */
  def initTransferFunction(
                             inputsInfo: Set[IasioInfo],
                             outputInfo: IasioInfo,
                             instance: Option[Int]): Try[Unit] = {
    require(Option(inputsInfo).isDefined && inputsInfo.nonEmpty,"Invalid empty set of IDs of inputs")
    require(Option(outputInfo).isDefined,"Invalid empty set ID of output")
    require(Option(instance).isDefined,"Unknown if it is tempated or not")

    if (instance.isDefined) {
      tfSetting.transferExecutor.get.setTemplateInstance(Optional.of(Int.box(instance.get)));
    } else {
      tfSetting.transferExecutor.get.setTemplateInstance(Optional.empty());
    }
    Try(tfSetting.transferExecutor.get.asInstanceOf[PythonTransferExecutor[T]].
      initialize(inputsInfo, outputInfo))
  }

}
