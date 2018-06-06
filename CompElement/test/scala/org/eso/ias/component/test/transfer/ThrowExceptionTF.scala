package org.eso.ias.component.test.transfer

import org.eso.ias.asce.transfer.ScalaTransferExecutor
import java.util.Properties
import org.eso.ias.asce.transfer.IasIO

/**
 * A transfer function that throws an exception: it allows to test if the
 * exception is properly caught and the TF not executed any longer.
 * In this case the state of the ASCE must change to TFBroken.
 * 
 * @param asceId: the ID of the ASCE
 * @param asceRunningId: the runningID of the ASCE
 * @param validityTimeFrame: The time frame (msec) to invalidate monitor points
 * @param props: the user defined properties
 * @author acaproni
 */
class ThrowExceptionTF(
    cEleId: String, 
		cEleRunningId: String,
		validityTimeFrame: Long,
		props: Properties) extends ScalaTransferExecutor(cEleId,cEleRunningId,validityTimeFrame,props) {
  
  /**
   * Intialization
   * 
   * @see TransferExecutor
   */
  override def initialize() { }
  
  /**
   * Shut dwon
   * 
   * @see TransferExecutor
   */
  override def shutdown() {}
  
  /**
   * This method does nothing but throwing an exception
   */
  override def eval(compInputs: Map[String, IasIO[_]], actualOutput: IasIO[Nothing]): IasIO[Nothing] = {
    println("ThrowExceptionTF: Throwing exception!")
    throw new Exception("Exception from a broken TF");
  }
  
}
