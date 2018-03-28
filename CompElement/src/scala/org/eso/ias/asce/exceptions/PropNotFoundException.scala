package org.eso.ias.asce.exceptions

/**
 * Exception thrown bythe TF executor
 * when the required property is not found
 * 
 * @param name: the name of the missing property
 * @param cause: the cause
 */
class PropNotFoundException(name: String, cause: Throwable) extends Exception(name, cause){
  assert(Option[String](name).isDefined && !name.isEmpty())
  
  /**
   * Overloaded constructor with no cause
   * 
   * @param name: the name of the missing property
   */
  def this(name: String) = this(name,null)
}