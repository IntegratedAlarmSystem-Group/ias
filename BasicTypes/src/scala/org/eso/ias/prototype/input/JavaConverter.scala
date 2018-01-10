package org.eso.ias.prototype.input

import org.eso.ias.prototype.input.java.IASValue
import org.eso.ias.prototype.input.java.IASTypes._
import org.eso.ias.prototype.input.java.IasAlarm
import org.eso.ias.prototype.input.java.IasLong
import org.eso.ias.prototype.input.java.IASValueBase
import org.eso.ias.prototype.input.java.IasDouble
import org.eso.ias.prototype.input.java.IasInt
import org.eso.ias.prototype.input.java.IasShort
import org.eso.ias.prototype.input.java.IasByte
import org.eso.ias.prototype.input.java.IasFloat
import org.eso.ias.prototype.input.java.IasChar
import org.eso.ias.prototype.input.java.IasString
import org.eso.ias.prototype.input.java.IasBool
import org.eso.ias.prototype.input.java.AlarmSample

/**
 * Converter methods from java to scala and vice-versa.
 * 
 * @author acaproni
 */
object JavaConverter {
  
  /**
   * Convert a scala InOut in a java IASValue
   * 
   * @param hio: the HIO to convert to java IASValue
   * @return The java value version of the passed HIO 
   */
  def inOutToIASValue[T](io: InOut[_]): IASValue[_] = {
    require(Option[InOut[_]](io).isDefined)
    
    val ret = if (io.actualValue.value.isEmpty) {
      IASValue.buildIasValue(
          null, 
          Long.MinValue,io.mode,
          io.validity.iasValidity,
          io.id.fullRunningID,
          io.iasType)
    } else {
      IASValue.buildIasValue(
          io.actualValue.value.get, 
          io.actualValue.timestamp,
          io.mode,
          io.validity.iasValidity,
          io.id.fullRunningID,
          io.iasType)
    }
    ret
  }
  
  /**
   * Update a scala HIO with a IasValueBase inferring its type
   * 
   * @see #updateHIOWithIasValue[T](hio: HeteroInOut, iasValue: IASValue[T])
   */
  def updateHIOWithIasValue(hio: InOut[_], iasValue: IASValueBase): InOut[_] = {
    hio.iasType match {
      case LONG => updateHIOWithIasValue(hio.asInstanceOf[InOut[Long]], iasValue.asInstanceOf[IasLong])
      case INT => updateHIOWithIasValue(hio.asInstanceOf[InOut[Int]],iasValue.asInstanceOf[IasInt])
      case SHORT => updateHIOWithIasValue(hio.asInstanceOf[InOut[Short]],iasValue.asInstanceOf[IasShort])
      case BYTE => updateHIOWithIasValue(hio.asInstanceOf[InOut[Byte]],iasValue.asInstanceOf[IasByte])
      case DOUBLE => updateHIOWithIasValue(hio.asInstanceOf[InOut[Double]],iasValue.asInstanceOf[IasDouble])
      case FLOAT => updateHIOWithIasValue(hio.asInstanceOf[InOut[Float]],iasValue.asInstanceOf[IasFloat])
      case BOOLEAN => updateHIOWithIasValue(hio.asInstanceOf[InOut[Boolean]],iasValue.asInstanceOf[IasBool])
      case CHAR => updateHIOWithIasValue(hio.asInstanceOf[InOut[Char]],iasValue.asInstanceOf[IasChar])
      case STRING => updateHIOWithIasValue(hio.asInstanceOf[InOut[String]],iasValue.asInstanceOf[IasString])
      case ALARM=> updateHIOWithIasValue(hio.asInstanceOf[InOut[AlarmSample]],iasValue.asInstanceOf[IasAlarm])
      case _ => throw new UnsupportedOperationException("Unsupported IAS type: "+hio.iasType)
    }
  }
  
  
  /**
   * Update a scala HIO with a IasLong
   * 
   * @see #updateHIOWithIasValue[T](hio: HeteroInOut, iasValue: IASValue[T])
   */
  def updateHIOWithIasValue(hio: InOut[Long], iasValue: IasLong): InOut[Long] = {
    JavaConverter.updateHIOWithIasValue[Long](hio, iasValue.asInstanceOf[IASValue[Long]])
  }
  
  /**
   * Update a scala HIO with a IasInt
   * 
   * @see #updateHIOWithIasValue[T](hio: HeteroInOut, iasValue: IASValue[T])
   */
  def updateHIOWithIasValue(hio: InOut[Int], iasValue: IasInt): InOut[Int] = {
    JavaConverter.updateHIOWithIasValue[Int](hio, iasValue.asInstanceOf[IASValue[Int]])
  }
  
  /**
   * Update a scala HIO with a IasShort
   * 
   * @see #updateHIOWithIasValue[T](hio: HeteroInOut, iasValue: IASValue[T])
   */
  def updateHIOWithIasValue(hio: InOut[Short], iasValue: IasShort): InOut[Short] = {
    JavaConverter.updateHIOWithIasValue[Short](hio, iasValue.asInstanceOf[IASValue[Short]])
  }
  
  /**
   * Update a scala HIO with a IasByte
   * 
   * @see #updateHIOWithIasValue[T](hio: HeteroInOut, iasValue: IASValue[T])
   */
  def updateHIOWithIasValue(hio: InOut[Byte], iasValue: IasByte): InOut[Byte] = {
    JavaConverter.updateHIOWithIasValue[Byte](hio, iasValue.asInstanceOf[IASValue[Byte]])
  }
  
  /**
   * Update a scala HIO with a IasDouble
   * 
   * @see #updateHIOWithIasValue[T](hio: HeteroInOut, iasValue: IASValue[T])
   */
  def updateHIOWithIasValue(hio: InOut[Double], iasValue: IasDouble): InOut[Double] = {
    JavaConverter.updateHIOWithIasValue[Double](hio, iasValue.asInstanceOf[IASValue[Double]])
  }
  
  /**
   * Update a scala HIO with a IasFloat
   * 
   * @see #updateHIOWithIasValue[T](hio: HeteroInOut, iasValue: IASValue[T])
   */
  def updateHIOWithIasValue(hio: InOut[Float], iasValue: IasFloat): InOut[Float] = {
    JavaConverter.updateHIOWithIasValue[Float](hio, iasValue.asInstanceOf[IASValue[Float]])
  }
  
  /**
   * Update a scala HIO with a IasFloat
   * 
   * @see #updateHIOWithIasValue[T](hio: HeteroInOut, iasValue: IASValue[T])
   */
  def updateHIOWithIasValue(hio: InOut[Boolean], iasValue: IasBool): InOut[Boolean] = {
    JavaConverter.updateHIOWithIasValue[Boolean](hio, iasValue.asInstanceOf[IASValue[Boolean]])
  }
  
  /**
   * Update a scala HIO with a IasChar
   * 
   * @see #updateHIOWithIasValue[T](hio: HeteroInOut, iasValue: IASValue[T])
   */
  def updateHIOWithIasValue(hio: InOut[Char], iasValue: IasChar): InOut[Char] = {
    JavaConverter.updateHIOWithIasValue[Char](hio, iasValue.asInstanceOf[IASValue[Char]])
  }
  
  /**
   * Update a scala HIO with a IasString
   * 
   * @see #updateHIOWithIasValue[T](hio: HeteroInOut, iasValue: IASValue[T])
   */
  def updateHIOWithIasValue(hio: InOut[String], iasValue: IasString): InOut[String] = {
    JavaConverter.updateHIOWithIasValue[String](hio, iasValue.asInstanceOf[IASValue[String]])
  }
  
  /**
   * Update a scala HIO with a IasAlarm
   * 
   * @see #updateHIOWithIasValue[T](hio: HeteroInOut, iasValue: IASValue[T])
   */
  def updateHIOWithIasValue(hio: InOut[AlarmSample], iasValue: IasAlarm): InOut[AlarmSample] = {
    JavaConverter.updateHIOWithIasValue[AlarmSample](hio, iasValue.asInstanceOf[IASValue[AlarmSample]])
  }
  
  /**
   * Update a scala HeteroInOut with the passed java IASValue
   * 
   * @param hio: the HIO to update
   * @param iasValue: the java value to update the passed scala HIO
   * @return The hio updated with the passed java value
   */
  private def updateHIOWithIasValue[T](hio: InOut[T], iasValue: IASValue[T]): InOut[T] = {
    assert(Option[InOut[T]](hio).isDefined)
    assert(Option[IASValueBase](iasValue).isDefined)
    // Some consistency check
    if (hio.iasType!=iasValue.valueType) {
      throw new IllegalStateException("Type mismatch for HIO "+hio.id.runningID+": "+hio.iasType+"!="+iasValue.valueType)
    }
    if (hio.id.id!=iasValue.id) {
      throw new IllegalStateException("ID mismatch for HIO "+hio.id.runningID+": "+hio.id.id+"!="+iasValue.id)
    }
    if (hio.id.fullRunningID!=iasValue.fullRunningId) {
      throw new IllegalStateException("Running ID mismatch for HIO "+hio.id.fullRunningID+": "+hio.id.runningID+"!="+iasValue.fullRunningId)
    }
    // Finally, update the HIO
    hio.updateMode(iasValue.mode).updateValue(Option[T](iasValue.value))
  }
}
