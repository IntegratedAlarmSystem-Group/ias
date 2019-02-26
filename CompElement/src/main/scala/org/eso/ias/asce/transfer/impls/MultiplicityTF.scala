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
  * of the alarms in input uneless a priority is passed by setting the alarm priority property
  * in the CDB.
  *
  * Since #153, the multiplicity TF propagates the properties of the inputs to the output:
  * - only the properties of the inputs that are SET are propagated in the output
  * - it the same property is present in more inputs, their values will be merged in the output
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

  /**
    * The mode of the ouptut depends on the modes of the inputs:
    * it is UNKNOW if the modes of the inputs differ, otherwise it is
    * the common operational mode of the inputs.
    *
    * @param modes the operational modes of the inputs
    * @return the mode to assign to the output
    */
  def getOutputMode(modes: Iterable[OperationalMode]): OperationalMode = {
    val setOfModes = modes.toSet
    if (setOfModes.size==1) setOfModes.head
    else OperationalMode.UNKNOWN
  }

  /**
    * Get and return the properties of the output by the properties of the  inputs
    *
    * @param props the properties of the inputs that are set
    * @return th eproperties to assign to the ouput
    */
  def getPropsOfOutput(props: Iterable[Map[String,String]]): Map[String, String] = {

    // convert maps to seq, to keep duplicate keys and concat
    // If props is Seq(Map(k1 -> A, k2 -> B), Map(k2 -> C, k3 -> D))
    // then merged is  List((k1,A), (k2,B), (k2,C), (k3,D))
    val merged: Seq[(String, String)] = props.foldLeft(Seq.empty[(String,String)]) { (z, prop) => z++prop.toSeq }

    // group by key
    // grouped is  Map(k2 -> List((k2,B), (k2,C)), k1 -> List((k1,A)), k3 -> List((k3,D)))
    val grouped = merged.groupBy(_._1)

    // Final cleanup
    // returns  Map(k2 -> B,C, k1 -> A, k3 -> D)
    grouped.mapValues(_.map(_._2).toList.mkString(","))
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
