package org.eso.ias.prototype.input

import org.eso.ias.prototype.utils.ISO8601Helper
import org.eso.ias.prototype.input.java.OperationalMode
import org.eso.ias.prototype.input.java.IASTypes._
import org.eso.ias.prototype.input.java.IASTypes

/**
 * A  <code>InOut</code> holds the value of an input or output 
 * of the IAS.
 * Objects of this type constitutes both the input of ASCEs and the output 
 * they produce.
 * 
 * The type of a InOut can be a double, an integer, an
 * array of integers and many other customized types.
 * 
 * The actual value is an Option because there is no
 * value associated before it comes for example from the HW. 
 * Nevertheless the <code>InOut</code> exists
 * 
 * <code>InOut</code> is immutable.
 * 
 * @param actualValue: the actual value of this InOut (can be undefined) 
 * @param id: The unique ID of the monitor point
 * @param refreshRate: The expected refresh rate (msec) of this monitor point
 *                     (to be used to assess its validity)
 * @param mode: The operational mode
 * @param validity: The validity of the monitor point
 * @param iasType: is the IAS type of this InOut
 * 
 * @see IASType
 * 
 * @author acaproni
 */
case class InOut[A](
    value: Option[A],
    val id: Identifier,
    val refreshRate: Int,    
    val mode: OperationalMode,
    val validity: Validity.Value,
    val iasType: IASTypes) {
  require(Option[Identifier](id).isDefined,"The identifier must be defined")
  require(refreshRate>=InOut.MinRefreshRate,"Invalid refresh rate (too low): "+refreshRate)
  require(Option[Validity.Value](validity).isDefined,"Undefined validity is not allowed")
  require(Option[IASTypes](iasType).isDefined,"The type must be defined")
  
  val  actualValue: InOutValue[A] = new InOutValue(value)
  if (actualValue.value.isDefined) require(InOut.checkType(actualValue.value.get,iasType),"Type mismatch: ["+actualValue.value.get+"] is not "+iasType)
  
  /**
   * Auxiliary constructor for the case when the InOut has no value
   * associated
   * 
   * @param id: The unique ID of the monitor point
   * @param refreshRate: The expected refresh rate (msec) of this monitor point
   *                     (to be used to assess its validity)
 * @param iasType: is the IAS type of this InOut
   */
  def this(id: Identifier,
    refreshRate: Int,
    iasType: IASTypes) {
    this(None,id,refreshRate,OperationalMode.UNKNOWN,Validity.Unreliable,iasType)
  }
  
  override def toString(): String = {
    "Monitor point " + id.toString() +" of IAS type " +iasType+"\n\t" +  
    mode.toString() + "\n\t" +
    validity.toString() +"\n\t" +
    (if (actualValue.value.isEmpty) "No value" else "Value: "+actualValue.value.get.toString())
  }
  
  /**
   * Update the mode of the monitor point
   * 
   * @param newMode: The new mode of the monitor point
   */
  def updateMode(newMode: OperationalMode): InOut[A] = {
    if (newMode==mode) this
    else this.copy(mode=newMode)
  }
  
  /**
   * Update the value of the monitor point
   * 
   * @param newValue: The new value of the monitor point
   */
  def updateValue(newValue: Option[A]): InOut[A] = update(newValue,validity)
  
  /**
   * Update the value and validity of the monitor point
   * 
   * @param newValue: The new value of the monitor point
   * @param valid: the new validity
   * @return A new InOut with updated value and validity
   */
  def update(newValue: Option[A], valid: Validity.Value): InOut[A] = {
    if (newValue==actualValue.value && valid==validity) this 
    else InOut(newValue,id,refreshRate,mode,valid,iasType)
  }
  
  /**
   * Update the validity of the monitor point
   * 
   * @param valid: The new validity of the monitor point
   */
  def updateValidity(valid: Validity.Value): InOut[A] = {
    if (valid==validity) this
    else this.copy(validity=valid)
  }
}

/** 
 *  InOut companion object
 */
object InOut {
  
  /**
   * The min possible value for the refresh rate
   * If it is too short the value will be invalid most of the time; if too 
   * long it is not possible to understand if it has been properly refreshed or
   * the source is stuck/dead.
   */
  val MinRefreshRate = 50;

  /**
   * Check if the passed value has of the proper type
   * 
   * @param value: The value to check they type against the iasType
   * @param iasType: The IAS type
   */
  def checkType[T](value: T, iasType: IASTypes): Boolean = {
    if (value==None) true
    else iasType match {
      case IASTypes.LONG => value.isInstanceOf[Long]
      case IASTypes.INT => value.isInstanceOf[Int]
      case IASTypes.SHORT => value.isInstanceOf[Short]
      case IASTypes.BYTE => value.isInstanceOf[Byte]
      case IASTypes.DOUBLE => value.isInstanceOf[Double]
      case IASTypes.FLOAT => value.isInstanceOf[Float]
      case IASTypes.BOOLEAN =>value.isInstanceOf[Boolean]
      case IASTypes.CHAR => value.isInstanceOf[Char]
      case IASTypes.STRING => value.isInstanceOf[String]
      case IASTypes.ALARM =>value.isInstanceOf[AlarmValue]
      case _ => false
    }
  }
  
  def apply[T](id: Identifier,
    refreshRate: Int,
    iasType: IASTypes): InOut[T] = {
    InOut[T](None,id,refreshRate,OperationalMode.UNKNOWN,Validity.Unreliable,iasType)
  }

}