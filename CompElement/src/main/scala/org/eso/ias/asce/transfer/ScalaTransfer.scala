package org.eso.ias.asce.transfer

import java.util.Optional

import org.eso.ias.asce.ComputingElement
import org.eso.ias.logging.IASLogger
import org.eso.ias.types.{IASTypes, Identifier, InOut}

import scala.util.Try

/**
 * <code>ScalaTransfer</code> calls the scala
 * transfer function provided by the user. 
 * user.
 * 
 * Note that the Validity of the output is not set by the transfer function
 * but automatically implemented by the ASCE
 */
trait ScalaTransfer[T] extends ComputingElement[T] {
  
  /**
   * The programming language of this TF 
   */
  val tfLanguage = TransferFunctionLanguage.scala

  /** The logger */
  private val logger = IASLogger.getLogger(this.getClass)

  /**
    * Runs the scala transfer function
    *
    * @param inputs The actual inputs
    * @param id The identifier
    * @param actualOutput the actual output
    * @return
    */
  def transfer(
      inputs: Map[String, InOut[_]], 
      id: Identifier,
      actualOutput: InOut[T]): Try[InOut[T]] = {
    
    val ins: Map[String, IasIO[_]] = inputs.mapValues( inout => new IasIO(inout)).toMap
    
    val out: IasIO[T] = new IasIO(actualOutput)
    
    Try(tfSetting.transferExecutor.get.asInstanceOf[ScalaTransferExecutor[T]].eval(ins,out).inOut)
  }


  /**
    * Initialize the scala transfer function
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

    logger.debug("Initializing the transfer function wint inputs {} and output {}",
      inputsInfo.map(_.iasioId).mkString(","),outputInfo.iasioId)
    instance.foreach(i =>  logger.debug("This TF is templated with index {}",i))
    if(instance.isDefined) {
      tfSetting.transferExecutor.get.setTemplateInstance(Optional.of(Int.box(instance.get)))
    } else {
      tfSetting.transferExecutor.get.setTemplateInstance(Optional.empty());
    }
    Try(tfSetting.transferExecutor.get.asInstanceOf[ScalaTransferExecutor[T]].
      initialize(inputsInfo, outputInfo))
  }

  
}
