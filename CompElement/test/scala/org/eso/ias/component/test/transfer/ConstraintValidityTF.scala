package org.eso.ias.component.test.transfer

import java.util.Properties
import org.eso.ias.asce.transfer.ScalaTransferExecutor
import org.eso.ias.types.Alarm
import org.eso.ias.logging.IASLogger
import org.eso.ias.types.InOut
import org.eso.ias.types.OperationalMode
import org.eso.ias.types.IASTypes
import org.eso.ias.asce.transfer.IasIO

/**
 * The TF to check the functioning of the setting of the validity
 * with constraints.
 * 
 * The TF ignores the values of the inputs (it is not the purpose of this TF)
 * and set the validity constraints according to the string contained in the
 * input with ID constraintSetterID
 * 
 * @param asceId: the ID of the ASCE
 * @param asceRunningId: the runningID of the ASCE
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
   * Intialization
   * 
   * @see TransferExecutor
   */
  override def initialize() {
    logger.info("Initializing")
  }
  
  /**
   * Shut dwon
   * 
   * @see TransferExecutor
   */
  override def shutdown() {
    logger.info("Shutting down")
  }
  
  override def eval(compInputs: Map[String, IasIO[_]], actualOutput: IasIO[Alarm]): IasIO[Alarm] = {
    logger.info("Evaluating {} inputs for comp. with ID [] and output []",
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
    
    
    val newAlarm = Alarm.getSetDefault
    actualOutput.updateMode(OperationalMode.DEGRADED).updateValue(newAlarm).setValidityConstraint(validityConstraints)
  }
  
}

/** Companion */
object ConstraintValidityTF {
  /** The Id of the monitor point to set the validity constraints */ 
  val constraintSetterID = "ValConstSetter-ID"
}