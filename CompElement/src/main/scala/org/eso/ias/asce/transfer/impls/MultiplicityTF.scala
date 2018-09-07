package org.eso.ias.asce.transfer.impls

import org.eso.ias.asce.transfer.{IasIO, IasioInfo, ScalaTransferExecutor}
import org.eso.ias.types.IASTypes._
import java.util.Properties

import org.eso.ias.asce.exceptions.PropNotFoundException
import org.eso.ias.asce.exceptions.WrongPropValue

import scala.util.control.NonFatal
import org.eso.ias.asce.exceptions.UnexpectedNumberOfInputsException
import org.eso.ias.asce.exceptions.TypeMismatchException
import org.eso.ias.asce.exceptions.TypeMismatchException
import org.eso.ias.types.{Alarm, OperationalMode}

/**
 * Implements the Multiplicity transfer function.
 * 
 * The IASIOs in input to this TF are only alarms.
 * The alarm generate by this TF activates when the number
 * of alarms in input is equal or greater then the threshold retrieved
 * from the properties. 
 * 
 * @param asceId: the ID of the ASCE
 * @param asceRunningId: the runningID of the ASCE
 * @param validityTimeFrame: The time frame (msec) to invalidate monitor points
 * @param props: the user defined properties
 * @author acaproni
 */
class MultiplicityTF (cEleId: String, cEleRunningId: String, validityTimeFrame: Long, props: Properties) 
extends ScalaTransferExecutor[Alarm](cEleId,cEleRunningId,validityTimeFrame,props) {
  
  /**
   * A little bit too verbose but wanted to catch all the 
   * possible failures....
   * 
   * The threshold
   */
  lazy val threshold: Int = {
    val propStr = Option[String](props.getProperty(MultiplicityTF.ThresholdPropName))
    if (!propStr.isDefined) {
      throw new PropNotFoundException(MultiplicityTF.ThresholdPropName)
    } else if (propStr.get.isEmpty()) {
        throw new WrongPropValue(MultiplicityTF.ThresholdPropName)
    } else {
      try {
        val theThreshold=propStr.get.toInt
        if (theThreshold<1) {
           throw new WrongPropValue(MultiplicityTF.ThresholdPropName,theThreshold.toString())
        } else {
          theThreshold
        }
      } catch {
        case NonFatal(t) => throw new WrongPropValue(MultiplicityTF.ThresholdPropName,propStr.get,t)
      }
    }
  }
  
  /**
   * The priority of the alarm can be set defining a property; 
   * otherwise use the default
   */
  val alarmSet: Alarm = 
    Option(props.getProperty(MultiplicityTF.alarmPriorityPropName)).map(Alarm.valueOf(_)).getOrElse(Alarm.getSetDefault)

  /**
    * Check that all the inputs and the output are alarms
    *
   * @param inputsInfo The IDs and types of the inputs
   * @param outputInfo The Id and type of thr output
   **/
  override def initialize(inputsInfo: Set[IasioInfo], outputInfo: IasioInfo): Unit = {
    val types = inputsInfo.map(_.iasioType)
    require(types.size==1 && types.head==ALARM,"All inputs must be ALARM")
    require(outputInfo.iasioType==ALARM,"The output must be an ALARM")
  }
  
  /**
   * @see TransferExecutor#shutdown()
   */
  def shutdown() {}

  def getOutputMode(modes: Iterable[OperationalMode]): OperationalMode = {
    val setOfModes = modes.toSet
    if (setOfModes.size==1) setOfModes.head
    else OperationalMode.UNKNOWN
  }
  
  /**
   * @see ScalaTransferExecutor#eval
   */
  def eval(compInputs: Map[String, IasIO[_]], actualOutput: IasIO[Alarm]): IasIO[Alarm] = {
    if (compInputs.size<threshold) {
      throw new UnexpectedNumberOfInputsException(compInputs.size,threshold)
    }
    if (actualOutput.iasType!=ALARM) {
      throw new TypeMismatchException(actualOutput.fullRunningId,actualOutput.iasType,ALARM)
    }
    
    for (hio <- compInputs.values if hio.iasType!=ALARM) {
      throw new TypeMismatchException(actualOutput.fullRunningId,hio.iasType,ALARM)
    }
    
    // Get the number of active alarms in input
    var activeAlarms=0
    val numOfActiveAlarms = for {
      hio <- compInputs.values
      if (hio.value.isDefined)
      alarmValue = hio.value.get.asInstanceOf[Alarm]
      if (alarmValue.isSet())} activeAlarms=activeAlarms+1
    
      val newAlarm = if (activeAlarms>=threshold) alarmSet else Alarm.cleared()
    actualOutput.updateValue(newAlarm).updateMode(getOutputMode(compInputs.values.map(_.mode)))
  }
}

object MultiplicityTF {
  
  /** The name of the property with the integer value of the threshold */
  val ThresholdPropName="org.eso.ias.tf.mutliplicity.threshold"
  
  /** The name of the property to set the priority of the alarm */
  val alarmPriorityPropName = "org.eso.ias.tf.alarm.priority"
}