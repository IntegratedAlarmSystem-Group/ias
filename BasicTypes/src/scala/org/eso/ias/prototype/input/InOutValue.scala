package org.eso.ias.prototype.input

import org.eso.ias.prototype.utils.ISO8601Helper

/**
 * The value of the HIO is associated 
 * to a timestamp corresponding to the update time of 
 * the value.
 * 
 * @param theValue: the value (can be None)
 * @param timestamp: the time when this value has been set
 */
case class InOutValue[T](val value: Option[_ >: T]) {
  
  /**
   * The instant in time when this object has been created
   */
  val timestamp: Long = System.currentTimeMillis()
  
  override def toString(): String = { 
    "Value "+value.toString() +
    " updated at "+ISO8601Helper.getTimestamp(timestamp)
  }
  
  /**
   * Redefine the hashCode in terms of the value
   * @see #equals(other: Any)
   */
  override def hashCode = 13*31+value.##
  
  /**
   * In IAS semantic 2 values are equal if and only
   * if their values are the same.
   * The timestamp is not used for comparison. 
   * 
   * @see #hashCode
   */
  override def equals(other: Any): Boolean = {
    other match {
      case InOutValue(a) => value==a
      case _ => false
    }
  }
}