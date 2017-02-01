package org.eso.ias.component.test.transfer

import org.eso.ias.prototype.transfer.ScalaTransferExecutor
import java.util.Properties
import org.eso.ias.prototype.input.InOut

/**
 * A transfer function that throws an exception: it allows to test if the
 * exception is properly caught and the TF not executed any longer.
 * In this case the state of the ASCE must change to TFBroken.
 * 
 * @author acaproni
 */
class ThrowExceptionTF(
    cEleId: String, 
		cEleRunningId: String,
		props: Properties) extends ScalaTransferExecutor(cEleId,cEleRunningId,props) {
  
  /**
   * Intialization
   * 
   * @see TransferExecutor
   */
  def initialize() { }
  
  /**
   * Shut dwon
   * 
   * @see TransferExecutor
   */
  def shutdown() {}
  
  /**
   * This method does nothing but throwing an exception
   */
  def eval(compInputs: Map[String, InOut[_]], actualOutput: InOut[Nothing]): InOut[Nothing] = {
    println("ThrowExceptionTF: Throwing exception!")
    throw new Exception("Exception from a broken TF");
  }
  
}
