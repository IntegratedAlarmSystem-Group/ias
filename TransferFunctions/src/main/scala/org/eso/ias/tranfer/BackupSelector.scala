package org.eso.ias.tranfer

import java.util.Properties

import com.typesafe.scalalogging.Logger
import org.eso.ias.asce.transfer.{IasIO, IasioInfo, ScalaTransferExecutor}
import org.eso.ias.logging.IASLogger
import org.eso.ias.types.IasValidity._
import org.eso.ias.types.OperationalMode

/**
  * Sometimes a monitor point has a backup to be used in case of failure
  * getting the value of the main implementation. The backup can
  * be retrieved from a different device (for example a backup pressure sensor)
  * or from a different way to get the value from the hardware (for example
  * one that involves the network and another one by reading a database where the
  * value is also stored)
  *
  * BackupSelector picks up the best option among the inputs i.e.
  * if the main value is operational and reliable then return this value otherwise
  * check if the first backup is operational and valid and return this one otherwise
  * checks the second backup and so on.
  *
  * BackupSelector does not do any computation on the value but returns
  * the first operational and valid input among the main and the backups.
  *
  * If none of the inputs is operational and valid, BackupSelector returns the
  * first option.
  *
  * The order of preference must be passed as a java property of comma
  * separated list of ids of monitor points like
  * "T1Main, T1Backup1, T1Backup2"
  *
  * The list of IDs passed with the SelectionPriorityPropName property
  * must match with the IDs of the inputs processed by the eval method.
  *
  * [[RelocationSelector]] is very similar to this TF but copes with relocation instead
  * of replication.
  *
  * @param asceId: the ID of the ASCE
  * @param asceRunningId: the runningID of the ASCE
  * @param validityTimeFrame: The time frame (msec) to invalidate monitor points
  * @param props: the user defined properties
  *@see [[RelocationSelector]]
  * @author acaproni
  */
class BackupSelector[T](asceId: String, asceRunningId: String, validityTimeFrame:Long, props: Properties)
extends ScalaTransferExecutor[T](asceId,asceRunningId,validityTimeFrame,props) {

  val prioritizedIDs: List[String] = {
    val propValueStr = Option(props.getProperty(BackupSelector.PrioritizedIdsPropName))
    require(propValueStr.isDefined,"PrioritizedIdsPropName property not found")

    val ids = propValueStr.get.split(",")
    ids.foldRight(List.empty[String]){ (str,z) => str.trim() :: z }
  }


  /**
	 * Produces the output of the component by evaluating the inputs.
	 *
	 * @return the computed output of the ASCE
	 */
	override def eval(compInputs: Map[String, IasIO[_]], actualOutput: IasIO[T]): IasIO[T] = {


    def updateOutput[B >: T](
        value: B,
        mode: OperationalMode,
        props: Map[String, String],
        constraints: Option[Set[String]]): IasIO[T] = {

      actualOutput.updateValue(value).updateMode(mode).updateProps(props).setValidityConstraint(constraints)
    }

    assert(compInputs.size==prioritizedIDs.length)

    // Select the ID of the first input that is both operational and valid
    val selectedInputId: Option[String] = prioritizedIDs.find(id => {
      val input = compInputs(id)

      // get the validity from the inputs
      val inputValidityByTime = input.validityOfInputByTime(validityTimeFrame)
      val inputValidityFromInputs = input.validity

      input.mode==OperationalMode.OPERATIONAL && inputValidityByTime==RELIABLE && inputValidityFromInputs==RELIABLE
    })

    selectedInputId match {
      case Some(inputId) =>
        val selectedInput=compInputs(inputId)
        updateOutput(
            selectedInput.value.get,
            selectedInput.mode,
            selectedInput.props,
            Some(Set(inputId)))
      case None =>
        val fallBackInput = compInputs(prioritizedIDs.head)
        updateOutput(
            fallBackInput.value.get,
            fallBackInput.mode,
            fallBackInput.props,
            None)
    }

  }

  /**
    * Initialize the TF
    *
    * @param inputsInfo The IDs and types of the inputs
    * @param outputInfo The Id and type of thr output
    **/
  override def initialize(inputsInfo: Set[IasioInfo], outputInfo: IasioInfo): Unit = {
     BackupSelector.logger.debug("TF of [{}] initializing", asceId)

    if (inputsInfo.size!=prioritizedIDs.size ||
      !inputsInfo.forall(info => prioritizedIDs.contains(info.iasioId))) {
      throw new BackupSelectorException("Input ids ["+inputsInfo.map(_.iasioId).mkString(",")+"] not contained in constraint ["+prioritizedIDs.mkString(",")+"]")
    }
    require(
      inputsInfo.forall(info => prioritizedIDs.contains(info.iasioId)),
      "Input ids ["+inputsInfo.map(_.iasioId).mkString(",")+"] not contained in constraint ["+prioritizedIDs.mkString(",")+"]")

     require(prioritizedIDs.length>1,s"$BackupSelector.PrioritizedIdsPropName must contain at least 2 IDs")
     require(prioritizedIDs.forall(!_.isEmpty),s"$BackupSelector.PrioritizedIdsPropName malformed")
     BackupSelector.logger.info("Priority list of IDs  [{}]", prioritizedIDs.mkString(","))
  }

  /**
   * @see TransferExecutor#shutdown()
   */
  override def shutdown() {
    BackupSelector.logger.debug("TF of [{}] shut down", asceId)
  }
  
}

/**
 * The exception thrown by this TF in case of 
 * malfunctions
 */
class BackupSelectorException(msg: String) extends Exception(msg)

object BackupSelector {
 
  /**
   * The logger
   */
  val logger: Logger = IASLogger.getLogger(BackupSelector.getClass)
      
  /** The property to get the prioritized list of IDs  */
  val PrioritizedIdsPropName = "org.eso.ias.tf.selector.prioritizedids"
}