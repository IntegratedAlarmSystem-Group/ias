package org.eso.ias.asce.transfer.impls

import java.util.Properties

import org.eso.ias.asce.exceptions.{PropNotFoundException, WrongPropValue}
import org.eso.ias.asce.transfer.{IasIO, IasioInfo, ScalaTransferExecutor}
import org.eso.ias.logging.IASLogger
import org.eso.ias.types.IASTypes._
import org.eso.ias.types.{Alarm, OperationalMode}

import scala.util.control.NonFatal

/**
  * Implements the Multiplicity transfer function.
  *
  * The IASIOs in input to this TF are only alarms.
  * The alarm generate by this TF activates when the number
  * of alarms in input is equal or greater then the threshold retrieved
  * from the properties.
  *
  * The priority of the alarm produced by this TF is the highest of the priorities
  * of the alarms in input unelss a priority is passed by setting the alarm priority property
  * in the CDB
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
   * The priority of the alarm from the java prorty or None if not defined
   */
  val alarmFromCDB: Option[Alarm] =
    Option(props.getProperty(MultiplicityTF.alarmPriorityPropName)).map(Alarm.valueOf(_))
  alarmFromCDB.foreach( alarm=> require(alarm!=Alarm.CLEARED, "The alarm for multiplicty must be SET"))

  /**
    * Check that all the inputs and the output are alarms
    *
   * @param inputsInfo The IDs and types of the inputs
   * @param outputInfo The Id and type of thr output
   **/
  override def initialize(inputsInfo: Set[IasioInfo], outputInfo: IasioInfo): Unit = {
    MultiplicityTF.logger.debug("Initializing")
    val types = inputsInfo.map(_.iasioType)
    require(types.size==1 && types.head==ALARM,"All inputs must be ALARM")
    require(outputInfo.iasioType==ALARM,"The output must be an ALARM")

    MultiplicityTF.logger.info("The TF accepts {} inputs with a threshold of {}", inputsInfo.size,threshold)
    require(inputsInfo.size>=threshold, "Too few inputs to activate this TF")
    MultiplicityTF.logger.debug("Initialized")
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

    // Get the active alarms in input
    val activeAlarms= compInputs.values.filter(input =>{
      input.value.isDefined && input.value.get.asInstanceOf[Alarm].isSet
    }).map(_.asInstanceOf[IasIO[Alarm]])

    val newAlarm = if (activeAlarms.size>=threshold) {
      alarmFromCDB.getOrElse({
        Alarm.fromPriority(activeAlarms.map(_.value.get.priorityLevel.get()).max)
      })
    } else Alarm.cleared()

    actualOutput.updateValue(newAlarm).updateMode(getOutputMode(compInputs.values.map(_.mode)))
  }
}

object MultiplicityTF {

  /** The logger */
  private val logger = IASLogger.getLogger(MultiplicityTF.getClass)
  
  /** The name of the property with the integer value of the threshold */
  val ThresholdPropName="org.eso.ias.tf.mutliplicity.threshold"
  
  /** The name of the property to set the priority of the alarm */
  val alarmPriorityPropName = "org.eso.ias.tf.alarm.priority"
}
