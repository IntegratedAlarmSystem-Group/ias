package org.eso.ias.prototype.transfer

import org.eso.ias.prototype.input.Identifier
import org.eso.ias.prototype.input.InOut
import org.eso.ias.prototype.compele.ComputingElement
import java.util.Properties
import java.util.{Map => JavaMap, HashMap => JavaHashMap}
import org.eso.ias.prototype.input.java.IASValue
import org.eso.ias.prototype.input.JavaConverter
import org.eso.ias.prototype.input.java.IASValueBase

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
  private[this] def flushOnJavaMap(
      inputs: Map[String, InOut[_]]): JavaMap[String, IASValueBase] = {
    val map: JavaMap[String, IASValueBase] = new JavaHashMap[String, IASValueBase]()
    for (key <-inputs.keySet) {
      val hio = inputs(key)
      val iasVal = JavaConverter.inOutToIASValue(hio)
      map.put(key,iasVal)
    }
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
      actualOutput: InOut[T]): Either[Exception,InOut[T]] = {
    
    val map: JavaMap[String, IASValueBase] = flushOnJavaMap(inputs)
    val newOutput=tfSetting.transferExecutor.get.asInstanceOf[JavaTransferExecutor].eval(map,JavaConverter.inOutToIASValue(actualOutput))
    val x=JavaConverter.updateHIOWithIasValue(actualOutput, newOutput).asInstanceOf[InOut[T]]
    try { 
      val map: JavaMap[String, IASValueBase] = flushOnJavaMap(inputs)
      val newOutput=tfSetting.transferExecutor.get.asInstanceOf[JavaTransferExecutor].eval(map,JavaConverter.inOutToIASValue(actualOutput))
      Right(JavaConverter.updateHIOWithIasValue(actualOutput, newOutput).asInstanceOf[InOut[T]])
    
    } catch { case e:Exception => Left(e) }
  }
  
}
