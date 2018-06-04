package org.eso.ias.asce.transfer

import org.eso.ias.types.Identifier
import org.eso.ias.types.InOut
import org.eso.ias.asce.ComputingElement
import java.util.Properties
import java.util.{Map => JavaMap, HashMap => JavaHashMap}
import org.eso.ias.types.IASValue
import org.eso.ias.types.IASValue
import scala.util.Try

/**
 * <code>JavaTransfer</code> calls the java
 * transfer function provided by the user.
 * 
 * Note that the Validity of the output is not set by the transfer function
 * but automatically implemented by the ASCE
 */
trait JavaTransfer[T] extends ComputingElement[T] {
  
  /**
   * The programming language of this TF 
   */
  val tfLanguage = TransferFunctionLanguage.java
  
  /**
   * Flush the scala Map into a Java Map
   */
  private[this] def flushInputsOnJavaMap(
      inputs: Map[String, InOut[_]]): JavaMap[String, IASValue[_]] = {
    val map: JavaMap[String, IASValue[_]] = new JavaHashMap[String, IASValue[_]]()
    
    inputs.values.foreach(iasio => {
      map.put(iasio.id.id,iasio.toIASValue)
    })
    map
  }
  
  /**
   * scala data structs need to be converted before invoking
   * the java code.
   * 
   * @see ComputingElementBase#transfer
   */
  def transfer(
      inputs: Map[String, InOut[_]], 
      id: Identifier,
      actualOutput: InOut[T]): Try[InOut[T]] = {
    Try[InOut[T]]{
      val map: JavaMap[String, IASValue[_]] = flushInputsOnJavaMap(inputs)
      val newOutput=tfSetting.transferExecutor.get.asInstanceOf[JavaTransferExecutor].
        eval( map, actualOutput.toIASValue)
      actualOutput.update(newOutput).asInstanceOf[InOut[T]]
    }
  }
  
}
