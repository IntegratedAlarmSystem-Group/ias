package org.eso.ias.asce.transfer

import org.eso.ias.asce.ComputingElement
import org.eso.ias.types.InOut
import org.eso.ias.types.Identifier
import java.util.Properties
import scala.util.Try

/**
 * <code>ScalaTransfer</code> calls the scala
 * transfer function provided by the user. 
 * user.
 * 
 * Note that the Validity of the output is not set by the transfer function
 * but automatically implemented by the ASCE
 */
trait ScalaTransfer[T] extends ComputingElement[T] {
  
  /**
   * The programming language of this TF 
   */
  val tfLanguage = TransferFunctionLanguage.scala
  
  def transfer(
      inputs: Map[String, InOut[_]], 
      id: Identifier,
      actualOutput: InOut[T]): Try[InOut[T]] = {
    
    val ins: Map[String, IasIO[_]] = inputs.mapValues( inout => new IasIO(inout)) 
    
    val out: IasIO[T] = new IasIO(actualOutput)
    
    Try(tfSetting.transferExecutor.get.asInstanceOf[ScalaTransferExecutor[T]].eval(ins,out).inOut)
  }
  
}
