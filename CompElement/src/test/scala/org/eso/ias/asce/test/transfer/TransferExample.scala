package org.eso.ias.asce.test.transfer

import org.eso.ias.asce.transfer.{IasIO, IasioInfo, ScalaTransferExecutor}
import org.eso.ias.types.{Alarm, OperationalMode}

import java.util.Properties

/**
 * A scala TransferExecutor for testing purposes
 * 
 * @param cEleId: the ID of the ASCE
 * @param cEleRunningId: the runningID of the ASCE
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
    * Initialize the TF
    *
    * @param inputsInfo The IDs and types of the inputs
    * @param outputInfo The Id and type of thr output
    **/
  override def initialize(inputsInfo: Set[IasioInfo], outputInfo: IasioInfo): Unit = {
    println("Scala TransferExample intializing")
  }
  
  /**
   * Shut dwon
   * 
   * @see TransferExecutor
   */
  override def shutdown(): Unit = {
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
