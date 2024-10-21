package org.eso.ias.dasu.test.transferfunction

import java.util.Properties

import org.eso.ias.asce.transfer.{IasIO, IasioInfo, ScalaTransferExecutor}
import org.eso.ias.types.InOut
import org.eso.ias.types.IASTypes._
import org.eso.ias.asce.exceptions.TypeMismatchException

/**
 * A transfer function that calculate th e average of its inputs.
 * 
 * This is to test and show the case of a synthetic parameter
 * 
 * @param cEleId: the ID of the ASCE
 * @param cEleRunningId: the runningID of the ASCE
 * @param validityTimeFrame: The time frame (msec) to invalidate monitor points
 * @param props: the user defined properties    
 * @author acaproni
 */
class AverageTempsTF (cEleId: String, cEleRunningId: String, validityTimeFrame: Long,props: Properties) 
extends ScalaTransferExecutor[Double] (cEleId,cEleRunningId,validityTimeFrame,props) {

  /**
    * Initialize the TF
    *
    * @param inputsInfo The IDs and types of the inputs
    * @param outputInfo The Id and type of thr output
    **/
  override def initialize(inputsInfo: Set[IasioInfo], outputInfo: IasioInfo): Unit = { }
  
  /**
   * @see TransferExecutor#shutdown()
   */
  override def shutdown(): Unit = {}
  
  /**
   * eval returns the average of the values of the inputs
   * 
   * @return the average of the values of the inputs
   * @see ScalaTransferExecutor#eval
   */
  override def eval(compInputs: Map[String, IasIO[?]], actualOutput: IasIO[Double]): IasIO[Double] = {
    val inputs = compInputs.values
    val values = inputs.map( input => {
      input.iasType match {
        case LONG => input.value.get.asInstanceOf[Long].toDouble
        case INT => input.value.get.asInstanceOf[Int].toDouble
        case SHORT => input.value.get.asInstanceOf[Short].toDouble
        case BYTE => input.value.get.asInstanceOf[Byte].toDouble
        case DOUBLE => input.value.get.asInstanceOf[Double]
        case FLOAT => input.value.get.asInstanceOf[Float].toDouble
        case _ => throw new TypeMismatchException(input.fullRunningId,input.iasType,List(LONG,INT,SHORT,BYTE,DOUBLE,FLOAT))
      }
    })
    
    // Sum all the values received in input
    val total = values.foldLeft(0.0)( (a,b) => a+b)
    // Average
    val newValue = total/values.size
    
    actualOutput.updateValue(newValue)
  }
}
