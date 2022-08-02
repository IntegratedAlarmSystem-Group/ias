package org.eso.ias.asce.transfer

import org.eso.ias.types.Identifier
import org.eso.ias.types.InOut
import org.eso.ias.asce.ComputingElement

import java.util.{Optional, Properties, HashMap => JavaHashMap, Map => JavaMap}
import scala.jdk.javaapi.CollectionConverters
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

  /**
    * Initialize the scala transfer function
    *
    * @param inputsInfo The IDs and types of the inputs
    * @param outputInfo The Id and type of thr output
    * @param instance the instance
    * @return
    */
  def initTransferFunction(
                            inputsInfo: Set[IasioInfo],
                            outputInfo: IasioInfo,
                            instance: Option[Int]): Try[Unit] = {
    require(Option(inputsInfo).isDefined && inputsInfo.nonEmpty,"Invalid empty set of IDs of inputs")
    require(Option(outputInfo).isDefined,"Invalid empty set ID of output")
    require(Option(instance).isDefined,"Unknown if it is tempated or not")

    if (instance.isDefined) {
      tfSetting.transferExecutor.get.setTemplateInstance(Optional.of(Int.box(instance.get)));
    } else {
      tfSetting.transferExecutor.get.setTemplateInstance(Optional.empty());
    }
    val javaInputIds = CollectionConverters.asJava(inputsInfo)
    Try(tfSetting.transferExecutor.get.asInstanceOf[JavaTransferExecutor[T]].
      initialize(javaInputIds, outputInfo))
  }
  
}
