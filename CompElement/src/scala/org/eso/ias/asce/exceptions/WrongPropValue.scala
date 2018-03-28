package org.eso.ias.asce.exceptions

/**
 * Exception thrown by the TF when the passed property 
 * has a wrong value
 * 
 * @param value: the wrong value of the property
 * @param name: the name of the property
 * @param cause: the cause
 */
class WrongPropValue(name: String, value: String, cause: Throwable) 
extends Exception("Wrong value "+value+" for property "+name,cause) {
  assert(Option[String](name).isDefined && !name.isEmpty())
  assert(Option[String](value).isDefined)
  
  /**
   * Overloaded constructor with no value
   * 
   * @param name: the name of the property
   * @param cause: the cause
   */
  def this(name: String, t: Throwable) = this(name,"",t)
  
  /**
   * Overloaded constructor with no cause and no value
   * 
   * @param name: the name of the property with a wrong value
   */
  def this(name: String) = this(name,"",null)
  
  /**
   * Overloaded constructor with no cause and no value
   * 
   * @param name: the name of the property with a wrong value
   * @param value: the wrong value of the property
   */
  def this(name: String, value: String) = this(name,value,null)
  
}