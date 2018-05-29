package org.eso.ias.component.test.transfer

import java.util.Properties
import org.eso.ias.asce.transfer.ScalaTransferExecutor
import org.eso.ias.types.Alarm
import org.eso.ias.logging.IASLogger
import org.eso.ias.types.InOut
import org.eso.ias.types.OperationalMode
import org.eso.ias.types.IASTypes

/**
 * The TF to check the functioning of the setting of the validity
 * with constraints.
 * 
 * The TF ignores the values of the inputs (it is not the purpose of this TF)
 * and set the validity constraints according to the string contained in the
 * input with ID constraintSetterID
 */
class ConstraintValidityTF (
    cEleId: String, 
		cEleRunningId: String,
		props: Properties) extends ScalaTransferExecutor[Alarm](cEleId,cEleRunningId,props) {
  
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
  
  override def eval(compInputs: Map[String, InOut[_]], actualOutput: InOut[Alarm]): InOut[Alarm] = {
    logger.info("Evaluating {} inputs for comp. with ID [] and output []",
        compInputs.size.toString(),
        compElementId,
        actualOutput.id.id)
    for (hio <- compInputs.values) logger.info("Input {} with ias validity {} and value {}",
        hio.id.id,
        hio.fromIasValueValidity.toString,
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
    actualOutput.updateMode(OperationalMode.DEGRADED).updateValue(Some(newAlarm)).setValidityConstraint(validityConstraints)
  }
  
}

/** Companion */
object ConstraintValidityTF {
  /** The Id of the monitor point to set the validity constraints */ 
  val constraintSetterID = "ValConstSetter-ID"
}