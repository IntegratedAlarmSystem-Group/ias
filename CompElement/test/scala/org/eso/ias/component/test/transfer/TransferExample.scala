package org.eso.ias.component.test.transfer

import java.util.Properties

import org.eso.ias.asce.transfer.ScalaTransferExecutor
import org.eso.ias.types.OperationalMode
import org.eso.ias.types.Alarm
import org.eso.ias.asce.transfer.IasIO

/**
 * A scala TransferExecutor for testing purposes
 * 
 * @param asceId: the ID of the ASCE
 * @param asceRunningId: the runningID of the ASCE
 * @param validityTimeFrame: The time frame (msec) to invalidate monitor points
 * @param props: the user defined properties
 * @see TransferExecutor
 */
class TransferExample(
    cEleId: String, 
		cEleRunningId: String,
		validityTimeFrame: Long,
		props: Properties) extends ScalaTransferExecutor[Alarm](cEleId,cEleRunningId,validityTimeFrame,props) {
  
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
