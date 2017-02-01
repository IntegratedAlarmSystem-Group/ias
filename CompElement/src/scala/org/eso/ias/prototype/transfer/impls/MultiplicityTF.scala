package org.eso.ias.prototype.transfer.impls

import org.eso.ias.prototype.transfer.ScalaTransferExecutor
import org.eso.ias.prototype.input.java.IASTypes._
import java.util.Properties
import org.eso.ias.prototype.input.InOut
import org.eso.ias.prototype.compele.exceptions.PropNotFoundException
import org.eso.ias.prototype.compele.exceptions.WrongPropValue
import scala.util.control.NonFatal
import org.eso.ias.prototype.compele.exceptions.UnexpectedNumberOfInputsException
import org.eso.ias.prototype.compele.exceptions.TypeMismatchException
import org.eso.ias.prototype.compele.exceptions.TypeMismatchException
import org.eso.ias.prototype.input.AlarmValue
import org.eso.ias.prototype.input.AlarmState
import org.eso.ias.prototype.input.Clear
import org.eso.ias.prototype.input.Set

/**
 * Implements the Multiplicity transfer function.
 * 
 * The HIOs in input to this TF are only alarms.
 * The alarm generate by this TF activates when the number
 * of alarms in input is equal or greater then the threshold retrieved
 * from the properties. 
 * 
 * @author acaproni
 */
class MultiplicityTF (cEleId: String, cEleRunningId: String, props: Properties) 
extends ScalaTransferExecutor[AlarmValue](cEleId,cEleRunningId,props) {
  
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
   * @see TransferExecutor#shutdown()
   */
  def initialize() {
  }
  
  /**
   * @see TransferExecutor#shutdown()
   */
  def shutdown() {}
  
  /**
   * @see ScalaTransferExecutor#eval
   */
  def eval(compInputs: Map[String, InOut[_]], actualOutput: InOut[AlarmValue]): InOut[AlarmValue] = {
    if (compInputs.size<threshold) throw new UnexpectedNumberOfInputsException(compInputs.size,threshold)
    if (actualOutput.iasType!=ALARM) throw new TypeMismatchException(actualOutput.id.runningID,actualOutput.iasType,ALARM)
    for (hio <- compInputs.values
        if hio.iasType!=ALARM) throw new TypeMismatchException(actualOutput.id.runningID,hio.iasType,ALARM)
    
    // Get the number of active alarms in input
    var activeAlarms=0
    val numOfActiveAlarms = for {
      hio <- compInputs.values
      if (hio.iasType==ALARM)
      if (hio.actualValue.value.isDefined)
      alarmValue = hio.actualValue.value.get.asInstanceOf[AlarmValue]
      if (alarmValue.alarmState==AlarmState.Active)} activeAlarms=activeAlarms+1
    
    val event = if (activeAlarms>=threshold) new Set() else new Clear()
    val ret: Either[Exception,AlarmValue] = AlarmValue.transition(actualOutput.actualValue.value.get.asInstanceOf[AlarmValue], event)
    ret match {
      case Left(e) => throw e
      case Right(v) => actualOutput.updateValue(Some(v))
    }
  }
}

object MultiplicityTF {
  
  /**
   * The name of the property with the integer value of the threshold 
   */
  val ThresholdPropName="org.eso.ias.tf.mutliplicity.threshold"
}
