package org.eso.ias.asce.transfer

import org.eso.ias.types.Identifier
import org.eso.ias.types.InOut
import org.eso.ias.asce.ComputingElement
import java.util.Properties
import java.util.{Map => JavaMap, HashMap => JavaHashMap}
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
      inputs: Map[String, InOut[_]]): JavaMap[String, IasIOJ[_]] = {
    val map: JavaMap[String, IasIOJ[_]] = new JavaHashMap[String, IasIOJ[_]]()
    
    inputs.values.foreach(iasio => {
      map.put(iasio.id.id,new IasIOJ(iasio))
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
      val map: JavaMap[String, IasIOJ[_]] = flushInputsOnJavaMap(inputs)
      val out: IasIOJ[T] = new IasIOJ(actualOutput)
      tfSetting.transferExecutor.get.asInstanceOf[JavaTransferExecutor[T]].
        eval( map, out).inOut.asInstanceOf[InOut[T]]
    }
  }
  
}
