package org.eso.ias.asce.test.transfer

import org.eso.ias.asce.transfer.{IasIO, IasioInfo, ScalaTransferExecutor}
import org.eso.ias.logging.IASLogger
import org.eso.ias.types.{Alarm, IASTypes, OperationalMode}

import java.util.Properties

/**
 * The TF to check the functioning of the setting of the validity
 * with constraints.
 * 
 * The TF ignores the values of the inputs (it is not the purpose of this TF)
 * and set the validity constraints according to the string contained in the
 * input with ID constraintSetterID
 * 
 * @param cEleId: the ID of the ASCE
 * @param cEleRunningId: the runningID of the ASCE
 * @param validityTimeFrame: The time frame (msec) to invalidate monitor points
 * @param props: the user defined properties
 */
class ConstraintValidityTF (
    cEleId: String, 
    cEleRunningId: String,
    validityTimeFrame: Long,
    props: Properties) extends ScalaTransferExecutor[Alarm](cEleId,cEleRunningId,validityTimeFrame,props) {
  
  /** The logger */
  private val logger = IASLogger.getLogger(this.getClass)

  /**
    * @param inputsInfo The IDs and types of the inputs
    * @param outputInfo The Id and type of thr output
    **/
  override def initialize(inputsInfo: Set[IasioInfo], outputInfo: IasioInfo): Unit = {
    logger.info("Initializing")
  }
  
  /**
   * Shut dwon
   * 
   * @see TransferExecutor
   */
  override def shutdown(): Unit = {
    logger.info("Shutting down")
  }
  
  override def eval(compInputs: Map[String, IasIO[?]], actualOutput: IasIO[Alarm]): IasIO[Alarm] = {
    logger.info("Evaluating {} inputs for comp. with ID {} and output {}",
        compInputs.size.toString(),
        compElementId,
        actualOutput.id)
    for (hio <- compInputs.values) logger.info("Input {} with value {}",
        hio.id,
        hio.value.toString)
    
    require(compInputs.keySet.contains(ConstraintValidityTF.constraintSetterID))
    val inputWithConstraints = compInputs(ConstraintValidityTF.constraintSetterID)
    require(inputWithConstraints.iasType==IASTypes.STRING)
    val constraintStr = inputWithConstraints.value.get.asInstanceOf[String]
    val validityConstraints = 
      Option(constraintStr.split(",").map(_.trim()).toSet).
      flatMap(set => if (set.size==1 && set.head=="") None else Some(set))
      
    validityConstraints.foreach(set => logger.info("Setting validity constraint to {}",set.mkString(",")))
    if (!validityConstraints.isDefined) {
      logger.info("Setting validity constraint to None")  
    }
    
    
    val newAlarm = Alarm.getInitialAlarmState.set()
    actualOutput.updateMode(OperationalMode.DEGRADED).updateValue(newAlarm).setValidityConstraint(validityConstraints)
  }
  
}

/** Companion */
object ConstraintValidityTF {
  /** The Id of the monitor point to set the validity constraints */ 
  val constraintSetterID = "ValConstSetter-ID"
}