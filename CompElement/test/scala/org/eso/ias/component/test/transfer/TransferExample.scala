package org.eso.ias.component.test.transfer

import java.util.Properties

import org.eso.ias.prototype.transfer.ScalaTransferExecutor
import org.eso.ias.prototype.input.InOut
import org.eso.ias.prototype.input.java.OperationalMode
import org.eso.ias.prototype.input.AlarmValue
import org.eso.ias.prototype.input.Set
/**
 * A scala TransferExecutor for testing purposes
 * 
 * @see TransferExecutor
 */
class TransferExample(
    cEleId: String, 
		cEleRunningId: String,
		props: Properties) extends ScalaTransferExecutor[AlarmValue](cEleId,cEleRunningId,props) {
  
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
  
  def eval(compInputs: Map[String, InOut[_]], actualOutput: InOut[AlarmValue]): InOut[AlarmValue] = {
    System.out.println("scala TransferExample: evaluating "+compInputs.size+" inputs");
		System.out.println("scala TransferExample for comp. with ID="+compElementId+" and output "+actualOutput.toString());
    for (hio <- compInputs.values) println(hio.toString())
    
    val av: AlarmValue = actualOutput.actualValue.value.get.asInstanceOf[AlarmValue]
    val newAlarm = AlarmValue.transition(av, new Set())
    newAlarm match {
        case Left(ex) => throw ex
        case Right(alarm) => actualOutput.updateMode(OperationalMode.SHUTDOWN).updateValue(Some(alarm)) 
      }
  }
  
}
