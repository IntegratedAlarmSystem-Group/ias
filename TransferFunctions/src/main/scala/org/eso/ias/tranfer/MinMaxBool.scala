package org.eso.ias.transfer

import org.eso.ias.asce.transfer.{IasIO, IasioInfo, ScalaTransferExecutor}

import java.util.Properties
import org.eso.ias.asce.exceptions.{PropsMisconfiguredException, UnexpectedNumberOfInputsException, TypeMismatchException}
import org.eso.ias.types.IASTypes.*

/**
 * MinMaxBool implements a Min/Max threshold like MinMaxThresholdTF but produces a boolean instead of an alarm.
 * The output is true if the value of the input is over the high threshold or under the low threshold, otherwise it is false.
 * 
 * The value, read from the input, is set in a property of the output.
 * 
 * To be generic, the value of the properties and that of the IASIO 
 * are converted to double before being evaluated.
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
class MinMaxBool(cEleId: String, cEleRunningId: String, validityTimeFrame:Long, props: Properties) 
extends ScalaTransferExecutor[Boolean](cEleId,cEleRunningId,validityTimeFrame, props) {
  
  /**
   * The (high) alarm is activated when the value of the IASIO 
   * is greater then HighON
   */
  lazy val highOn: Double = getValue(props, MinMaxBool.highOnPropName, Double.MaxValue)
  
  /**
   * if the (high) alarm is active and the value of the IASIO
   * goes below HighOFF, then the alarm is deactivated
   */
  lazy val highOff: Double = getValue(props, MinMaxBool.highOffPropName, highOn)
  
  /**
   * the (low) alarm is activated when the value of the IASIO is
   * lower then LowON
   */
  lazy val lowOn: Double =  getValue(props, MinMaxBool.lowOnPropName, Double.MinValue)
  
  /**
   * if the (low) alarm is active and the value of the IASIO
   * becomes greater then LowOFF, then the alarm is deactivated
   */
  lazy val lowOff: Double = getValue(props, MinMaxBool.lowOffPropName, lowOn)

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
   * @param inputsInfo The IDs and types of the inputs
   * @param outputInfo The Id and type of thr output
   */
  def initialize(inputsInfo: Set[IasioInfo],outputInfo: IasioInfo): Unit = {
    if (highOn<highOff) {
      throw new PropsMisconfiguredException(
        Map(MinMaxBool.highOnPropName->highOn.toString(), MinMaxBool.highOffPropName->highOff.toString()))
    }
    if (lowOff<lowOn) {
      throw new PropsMisconfiguredException(
        Map(MinMaxBool.lowOnPropName->lowOn.toString(), MinMaxBool.lowOffPropName->lowOff.toString()))
    }
    if (lowOff>highOff) {
      throw new PropsMisconfiguredException(
        Map(MinMaxBool.lowOffPropName->lowOff.toString(), MinMaxBool.highOffPropName->highOff.toString()))
    }

    if (inputsInfo.size!=1) {
      throw new UnexpectedNumberOfInputsException(1, inputsInfo.size)
    }

    val inputInfo = inputsInfo.head
    if (inputInfo.iasioType!=DOUBLE &&  
        inputInfo.iasioType!=FLOAT && 
        inputInfo.iasioType!=LONG && 
        inputInfo.iasioType!=INT && 
        inputInfo.iasioType!=SHORT && 
        inputInfo.iasioType!=BYTE) {
      throw new TypeMismatchException(inputInfo.iasioId, inputInfo.iasioType, List(DOUBLE, FLOAT, LONG, INT, SHORT, BYTE))
    }

    if (outputInfo.iasioType!=BOOLEAN) {
      throw new TypeMismatchException(outputInfo.iasioId, outputInfo.iasioType, BOOLEAN)
    }
  }
  
  /**
   * @see TransferExecutor#shutdown()
   */
  def shutdown(): Unit = {}
  
  /**
   * @see ScalaTransferExecutor#eval
   */
  override def eval(compInputs: Map[String, IasIO[?]], actualOutput: IasIO[Boolean]): IasIO[Boolean] = {
    if (compInputs.size!=1) {
      throw new UnexpectedNumberOfInputsException(compInputs.size,1)
    }
    if (actualOutput.iasType!=BOOLEAN) {
      throw new TypeMismatchException(actualOutput.fullRunningId,actualOutput.iasType,BOOLEAN)
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
    // defined (i.e. it is Optional.empty). In that case the variable
    // is initialized to false
    val wasTrue: Boolean = actualOutput.value.getOrElse(false).asInstanceOf[Boolean]
 
    // The condition is true if the value is over the limits (high on and low on)
    // but remains set is the old values was set and the value is
    // between high on and high off or between low on and low off
    val condition: Boolean = 
      (doubleValue>=highOn || doubleValue<=lowOn) ||
      wasTrue && (doubleValue>=highOff || doubleValue<=lowOff)

    val props: Map[String, String] = Map(MinMaxBool.actualValuePropName->doubleValue.toString())

    actualOutput.updateValue(condition).updateProps(props)
  }
}

object MinMaxBool {
  
 /** The name of the HighOn property */
  val highOnPropName = "org.eso.ias.tf.minmaxbool.highOn"
  
  /** The name of the HighOff property  */
  val highOffPropName = "org.eso.ias.tf.minmaxbool.highOff"
  
  /** The name of the lowOn property */
  val lowOnPropName = "org.eso.ias.tf.minmaxbool.lowOn"
  
  /** The name of the lowOff property  */
  val lowOffPropName = "org.eso.ias.tf.minmaxbool.lowOff" 

  /** The name of the property with the actual value of the input */
  val actualValuePropName = "actualValue"
}
