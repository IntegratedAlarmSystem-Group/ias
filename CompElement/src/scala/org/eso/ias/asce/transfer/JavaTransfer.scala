package org.eso.ias.asce.transfer

import org.eso.ias.types.Identifier
import org.eso.ias.types.InOut
import org.eso.ias.asce.ComputingElement
import java.util.Properties
import java.util.{Map => JavaMap, HashMap => JavaHashMap}
import org.eso.ias.types.IASValue
import org.eso.ias.types.JavaConverter
import org.eso.ias.types.IASValueBase

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
    
    inputs.values.foreach(iasio => {
      map.put(iasio.id.id,JavaConverter.inOutToIASValue(iasio,iasio.getValidity(None)))
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
      actualOutput: InOut[T]): Either[Exception,InOut[T]] = {
    
    try { 
      val map: JavaMap[String, IASValueBase] = flushOnJavaMap(inputs)
      val newOutput=tfSetting.transferExecutor.get.asInstanceOf[JavaTransferExecutor].
      eval(
          map,
          JavaConverter.inOutToIASValue(
              actualOutput,
              actualOutput.getValidity(Option(inputs.values.toSet))))
      Right(JavaConverter.updateHIOWithIasValue(actualOutput, newOutput).asInstanceOf[InOut[T]])
    
    } catch { case e:Exception => Left(e) }
  }
  
}
