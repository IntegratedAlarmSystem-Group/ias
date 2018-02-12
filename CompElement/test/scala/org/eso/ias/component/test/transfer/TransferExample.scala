package org.eso.ias.component.test.transfer

import java.util.Properties

import org.eso.ias.prototype.transfer.ScalaTransferExecutor
import org.eso.ias.types.InOut
import org.eso.ias.types.OperationalMode
import org.eso.ias.types.AlarmSample

/**
 * A scala TransferExecutor for testing purposes
 * 
 * @see TransferExecutor
 */
class TransferExample(
    cEleId: String, 
		cEleRunningId: String,
		props: Properties) extends ScalaTransferExecutor[AlarmSample](cEleId,cEleRunningId,props) {
  
  /**
   * Intialization
   * 
   * @see TransferExecutor
   */
  def initialize() {
    println("Scala TransferExample intializing")
  }
  
  /**
   * Shut dwon
   * 
   * @see TransferExecutor
   */
  def shutdown() {
    println("Scala TransferExample shutting down")
  }
  
  def eval(compInputs: Map[String, InOut[_]], actualOutput: InOut[AlarmSample]): InOut[AlarmSample] = {
    System.out.println("scala TransferExample: evaluating "+compInputs.size+" inputs");
		System.out.println("scala TransferExample for comp. with ID="+compElementId+" and output "+actualOutput.toString());
    for (hio <- compInputs.values) println(hio.toString())
    
    val newAlarm = AlarmSample.SET
    actualOutput.updateMode(OperationalMode.SHUTTEDDOWN).updateValue(Some(newAlarm)) 
  }
  
}
