package org.eso.ias.transfer

import java.util.Properties
import com.typesafe.scalalogging.Logger
import org.eso.ias.asce.exceptions.{UnexpectedNumberOfInputsException, TypeMismatchException}
import org.eso.ias.asce.transfer.{IasIO, IasioInfo, ScalaTransferExecutor}
import org.eso.ias.logging.IASLogger
import org.eso.ias.types.IASTypes

/**
  * AndBool is a simple transfer function that takes as input n (n>=2) boolean IASIO values 
  * and returns the result of the AND operation between them.
  * 
  * The output produced by this TF is a boolean IASIO. The output can be inverted by setting 
  * the "org.eso.ias.tf.andbool.invert" property to true in the CDB.
  * 
  * 
  * @param cEleId: the ID of the ASCE that executes this TF
  * @param cEleRunningId: the runningID of the ASCE
  * @param validityTimeFrame: The time frame (msec) to invalidate monitor points
  * @param props: the user defined properties
  * @author acaproni
  */
class AndBool(cEleId: String, cEleRunningId: String, validityTimeFrame: Long, props: Properties) 
extends ScalaTransferExecutor[Boolean](cEleId,cEleRunningId,validityTimeFrame,props) {

  /**
   * If set the logic is inverted
   *
   * By default it is false because of the definition of java.lang.Boolean.getBoolean
   */
  val invert: Boolean = Option(props.getProperty(AndBool.InvertLogicPropName)).
    map(java.lang.Boolean.valueOf(_).booleanValue()).
    getOrElse(AndBool.DefaultLogicPropValue.booleanValue())

  /**
   * Check that all the inputs and the output are booleans.
   * There must be at least 2 inputs.
   *
   * @param inputsInfo The IDs and types of the inputs
   * @param outputInfo The Id and type of thr output
   **/
  override def initialize(inputsInfo: Set[IasioInfo], outputInfo: IasioInfo): Unit = {
    AndBool.logger.debug("Initializing")
    if (inputsInfo.size<2) {
      throw new UnexpectedNumberOfInputsException(2, inputsInfo.size)
    }
    inputsInfo.foreach { inputInfo =>
      if (inputInfo.iasioType != IASTypes.BOOLEAN) {
        throw new TypeMismatchException(inputInfo.iasioId, inputInfo.iasioType, IASTypes.BOOLEAN)
      }
    }
    if (outputInfo.iasioType!=IASTypes.BOOLEAN) {
      throw new TypeMismatchException(outputInfo.iasioId,outputInfo.iasioType,IASTypes.BOOLEAN)
    }
    AndBool.logger.debug("Initialized")
  }

  /**
   * @see TransferExecutor#shutdown()
   */
  def shutdown(): Unit = {}

  /**
   * @see ScalaTransferExecutor#eval
   */
  def eval(compInputs: Map[String, IasIO[?]], actualOutput: IasIO[Boolean]): IasIO[Boolean] = {
    val andResult = compInputs.values.map(_.value.get.asInstanceOf[Boolean]).reduce(_ && _)
    return actualOutput.updateValue(if (invert) !andResult else andResult)
  }
}

object AndBool {

  /** The logger */
  private val logger: Logger = IASLogger.getLogger(AndBool.getClass)

  /** The name of the boolean property to invert the logic */
  val InvertLogicPropName: String = "org.eso.ias.tf.andbool.invert"

  /** By default the logic is not inverted */
  val DefaultLogicPropValue: Boolean = false
}