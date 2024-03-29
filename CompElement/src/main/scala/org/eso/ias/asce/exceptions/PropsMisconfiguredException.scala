package org.eso.ias.asce.exceptions

import java.util.Properties
import scala.jdk.javaapi.CollectionConverters

/**
 * Exception thrown when the properties expected by the 
 * TF executor are misconfigured
 * 
 * @param props: the misconfigured properties
 * @param cause: the cause
 * @see Exception
 */
class PropsMisconfiguredException(props: Map[String, String], cause: Throwable) 
extends Exception("Misconfigured properties: "+props.mkString(", "),cause) {
  assert(!props.isEmpty)
  
  /**
   * Overloaded constructor without cause
   * 
   * @param props: the misconfigured properties
   */
  def this(props: Map[String, String]) = this(props,null)
  
  /**
   * Overloaded constructor without cause
   * 
   * @param p: the misconfigured properties
   */
  
  /**
   * Overloaded constructor without cause
   * 
   * @param props: the misconfigured properties
   */
  def this(props: Properties) = this(CollectionConverters.asScala(props).asInstanceOf[Map[String,String]])
  
  /**
   * Overloaded constructor
   * 
   * @param props: the misconfigured java properties
   * @param cause: the cause
   */
  def this(props: Properties, cause: Throwable) = this(CollectionConverters.asScala(props).asInstanceOf[Map[String,String]],cause)
}