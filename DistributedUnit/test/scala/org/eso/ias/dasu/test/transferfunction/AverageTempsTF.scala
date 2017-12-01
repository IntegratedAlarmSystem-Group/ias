package org.eso.ias.dasu.test.transferfunction

import java.util.Properties
import org.eso.ias.prototype.transfer.ScalaTransferExecutor
import org.eso.ias.prototype.input.InOut
import org.eso.ias.prototype.input.java.IASTypes._
import org.eso.ias.prototype.compele.exceptions.TypeMismatchException

/**
 * A transfer function that calculate th e average of its inputs.
 * 
 * This is to test and show the case of a synthetic parameter
 */
class AverageTempsTF (cEleId: String, cEleRunningId: String, props: Properties) 
extends ScalaTransferExecutor[Double] (cEleId,cEleRunningId,props) {
  /**
   * @see TransferExecutor#shutdown()
   */
  def initialize() {
  }
  
  /**
   * @see TransferExecutor#shutdown()
   */
  def shutdown() {}
  
  /**
   * eval returns the average of the values of the inputs
   * 
   * @return the average of the values of the inputs
   * @see ScalaTransferExecutor#eval
   */
  def eval(compInputs: Map[String, InOut[_]], actualOutput: InOut[Double]): InOut[Double] = {
    val inputs = compInputs.values
    val values = inputs.map( input => {
      input.iasType match {
        case LONG => input.actualValue.value.get.asInstanceOf[Long].toDouble
        case INT => input.actualValue.value.get.asInstanceOf[Int].toDouble
        case SHORT => input.actualValue.value.get.asInstanceOf[Short].toDouble
        case BYTE => input.actualValue.value.get.asInstanceOf[Byte].toDouble
        case DOUBLE => input.actualValue.value.get.asInstanceOf[Double]
        case FLOAT => input.actualValue.value.get.asInstanceOf[Float].toDouble
        case _ => throw new TypeMismatchException(input.id.runningID,input.iasType,List(LONG,INT,SHORT,BYTE,DOUBLE,FLOAT))
      }
    })
    
    // Sum all the values received in input
    val total = values.foldLeft(0.0)( (a,b) => a+b)
    // Average
    val newValue = total/values.size
    
    actualOutput.updateValue(Option(newValue))
  }
}