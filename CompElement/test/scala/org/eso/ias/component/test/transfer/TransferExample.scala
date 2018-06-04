package org.eso.ias.component.test.transfer

import java.util.Properties

import org.eso.ias.asce.transfer.ScalaTransferExecutor
import org.eso.ias.types.OperationalMode
import org.eso.ias.types.Alarm
import org.eso.ias.asce.transfer.IasIO

/**
 * A scala TransferExecutor for testing purposes
 * 
 * @see TransferExecutor
 */
class TransferExample(
    cEleId: String, 
		cEleRunningId: String,
		props: Properties) extends ScalaTransferExecutor[Alarm](cEleId,cEleRunningId,props) {
  
  /**
   * Intialization
   * 
   * @see TransferExecutor
   */
  override def initialize() {
    println("Scala TransferExample intializing")
  }
  
  /**
   * Shut dwon
   * 
   * @see TransferExecutor
   */
  override def shutdown() {
    println("Scala TransferExample shutting down")
  }
  
  override def eval(compInputs: Map[String, IasIO[_]], actualOutput: IasIO[Alarm]): IasIO[Alarm] = {
    System.out.println("scala TransferExample: evaluating "+compInputs.size+" inputs");
		System.out.println("scala TransferExample for comp. with ID="+compElementId+" and output "+actualOutput.toString());
    for (hio <- compInputs.values) println(hio.toString())
    
    val newAlarm = Alarm.getSetDefault
    actualOutput.updateMode(OperationalMode.SHUTTEDDOWN).updateValue(newAlarm) 
  }
  
}
