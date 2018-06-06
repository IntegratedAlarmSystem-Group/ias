package org.eso.ias.asce.transfer.impls

import org.eso.ias.asce.transfer.ScalaTransferExecutor
import java.util.Properties
import org.eso.ias.asce.exceptions.PropsMisconfiguredException
import org.eso.ias.asce.exceptions.UnexpectedNumberOfInputsException
import org.eso.ias.types.IASTypes._
import org.eso.ias.asce.exceptions.TypeMismatchException
import MinMaxThresholdTF._
import org.eso.ias.types.Alarm
import org.eso.ias.asce.transfer.IasIO

/**
 * The TF implementing a Min/Max threshold TF  (there is also
 * a java implementation for comparison).
 * 
 * The alarm is activated when the alarm is higher then
 * the max threshold or when it is lower then the low threshold.
 * 
 * We could call this alarm a "Non-nominal temperature" because it is 
 * equally set if the temperature is too low or is too high but
 * cannot distinguish between the 2 cases.
 * 
 * If we want to distinguish between the 2 cases,  we need 2 ASCEs having 
 * the same input, one checking for the high value and the other checking 
 * for the low value.
 * 
 * To be generic, the value of the properties and that of the IASIO 
 * are converted to double.
 * 
 * The value of the Min and Max thresholds are passed as properties:
 * <UL>
 * 	<LI>HighON: the (high) alarm is activated when the value of the IASIO 
 *              is greater then HighON
 *  <LI>HighOFF: if the (high) alarm is active and the value of the IASIO
 *               goes below HighOFF, then the alarm is deactivated
 *  <LI>LowOFF: if the (low) alarm is active and the value of the IASIO
 *               becomes greater then LowOFF, then the alarm is deactivated
 *  <LI>LowON: the (low) alarm is activated when the value of the IASIO is
 *             lower then LowON
 *         
 * @param asceId: the ID of the ASCE
 * @param asceRunningId: the runningID of the ASCE
 * @param validityTimeFrame: The time frame (msec) to invalidate monitor points
 * @param props: the user defined properties    
 * @author acaproni
 */
class MinMaxThresholdTF(cEleId: String, cEleRunningId: String, validityTimeFrame:Long, props: Properties) 
extends ScalaTransferExecutor[Alarm](cEleId,cEleRunningId,validityTimeFrame, props) {
  
  /**
   * The (high) alarm is activated when the value of the IASIO 
   * is greater then HighON
   */
  lazy val highOn: Double = getValue(props, highOnPropName, Double.MaxValue)
  
  /**
   * if the (high) alarm is active and the value of the IASIO
   * goes below HighOFF, then the alarm is deactivated
   */
  lazy val highOff: Double = getValue(props, highOffPropName, highOn)
  
  /**
   * the (low) alarm is activated when the value of the IASIO is
   * lower then LowON
   */
  lazy val lowOn: Double =  getValue(props, lowOnPropName, Double.MinValue)
  
  /**
   * if the (low) alarm is active and the value of the IASIO
   * becomes greater then LowOFF, then the alarm is deactivated
   */
  lazy val lowOff: Double = getValue(props, lowOffPropName, lowOn)
  
  /**
   * The priority of the alarm can be set defining a property; 
   * otherwise use the default
   */
  val alarmSet: Alarm = 
    Option(props.getProperty(alarmPriorityPropName)).map(Alarm.valueOf(_)).getOrElse(Alarm.getSetDefault) 
  
  
  /**
   * Get the value of a property from the passed properties.
   * 
   * @param props: The properties to look for the property with 
   *               the given name
   * @param propName: the name of the property
   * @param default: the value to return if the property is not defined 
   *                 in the passed properties
   */
  def getValue(props: Properties, propName: String, default: Double): Double = {
    val propStr = Option[String](props.getProperty(propName))
    if (propStr.isDefined) {
      propStr.get.toDouble
    } else {
      default
    }
  }
  
  
  
  /**
   * Initialize the TF by getting the four properties
   * (being the properties lazy, they will be initialized here.
   * 
   * This method merely checks if the values of the properties are coherent
   * with the definitions given above.
   * 
   * @see TransferExecutor#initialize()
   */
  def initialize() {
    if (highOn<highOff) {
      throw new PropsMisconfiguredException(Map(highOnPropName->highOn.toString(),highOffPropName->highOff.toString()))
    }
    if (lowOff<lowOn) {
      throw new PropsMisconfiguredException(Map(lowOnPropName->lowOn.toString(),lowOffPropName->lowOff.toString()))
    }
    if (lowOff>highOff) {
      throw new PropsMisconfiguredException(Map(lowOffPropName->lowOff.toString(),highOffPropName->highOff.toString()))
    }
  }
  
  /**
   * @see TransferExecutor#shutdown()
   */
  def shutdown() {}
  
  /**
   * @see ScalaTransferExecutor#eval
   */
  def eval(compInputs: Map[String, IasIO[_]], actualOutput: IasIO[Alarm]): IasIO[Alarm] = {
    if (compInputs.size!=1) {
      throw new UnexpectedNumberOfInputsException(compInputs.size,1)
    }
    if (actualOutput.iasType!=ALARM) {
      throw new TypeMismatchException(actualOutput.fullRunningId,actualOutput.iasType,ALARM)
    }
    
    // Get the input
    val iasio = compInputs.values.head
    
    val doubleValue: Double = iasio.iasType match {
      case LONG => iasio.value.get.asInstanceOf[Long].toDouble
      case INT => iasio.value.get.asInstanceOf[Int].toDouble
      case SHORT => iasio.value.get.asInstanceOf[Short].toDouble
      case BYTE => iasio.value.get.asInstanceOf[Byte].toDouble
      case DOUBLE => iasio.value.get.asInstanceOf[Double]
      case FLOAT => iasio.value.get.asInstanceOf[Float].toDouble
      case _ => throw new TypeMismatchException(iasio.fullRunningId,iasio.iasType,List(LONG,INT,SHORT,BYTE,DOUBLE,FLOAT))
    }
    
    // It cope with the case that the value of the actual output is not 
    // defined (i.e. it is Optional.empty. In that case the variable
    // is initialized to false 
    val wasSet = actualOutput.value.map(_!=Alarm.cleared()).getOrElse(false)
 
    // The condition is true if the value is over the limits (high on and low on)
    // but remains set is the old values was set and the value is
    // between high on and hiogh off or between low on and low off
    val condition = 
      (doubleValue>=highOn || doubleValue<=lowOn) ||
      wasSet && (doubleValue>=highOff || doubleValue<=lowOff)
    val newValue = if (condition) alarmSet else Alarm.cleared()
    actualOutput.updateValue(newValue).updateMode(iasio.mode).updateProps(Map("actualValue"->doubleValue.toString()))
  }
  
}

object MinMaxThresholdTF {
  
 /** The name of the HighOn property */
  val highOnPropName = "org.eso.ias.tf.minmaxthreshold.highOn"
  
  /** The name of the HighOff property  */
  val highOffPropName = "org.eso.ias.tf.minmaxthreshold.highOff"
  
  /** The name of the lowOn property */
  val lowOnPropName = "org.eso.ias.tf.minmaxthreshold.lowOn"
  
  /** The name of the lowOff property  */
  val lowOffPropName = "org.eso.ias.tf.minmaxthreshold.lowOff" 
  
  /** The name of the property to set the priority of the alarm */
  val alarmPriorityPropName = "org.eso.ias.tf.alarm.priority"
}
