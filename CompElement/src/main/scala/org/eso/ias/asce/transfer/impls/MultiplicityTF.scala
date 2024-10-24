package org.eso.ias.asce.transfer.impls

import java.util.Properties
import com.typesafe.scalalogging.Logger
import org.eso.ias.asce.exceptions.{PropNotFoundException, WrongPropValue}
import org.eso.ias.asce.transfer.{IasIO, IasioInfo, ScalaTransferExecutor}
import org.eso.ias.logging.IASLogger
import org.eso.ias.types.IASTypes.*
import org.eso.ias.types.{Alarm, OperationalMode, Priority, IasValidity}

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
  * @param cEleId: the ID of the ASCE
  * @param cEleRunningId: the runningID of the ASCE
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
    if (propStr.isEmpty) {
      throw new PropNotFoundException(MultiplicityTF.ThresholdPropName)
    } else if (propStr.get.isEmpty) {
        throw new WrongPropValue(MultiplicityTF.ThresholdPropName)
    } else {
      try {
        val theThreshold=propStr.get.toInt
        if (theThreshold<1) {
           throw new WrongPropValue(MultiplicityTF.ThresholdPropName,theThreshold.toString)
        } else {
          theThreshold
        }
      } catch {
        case NonFatal(t) => throw new WrongPropValue(MultiplicityTF.ThresholdPropName,propStr.get,t)
      }
    }
  }
  
  /**
   * The priority of the alarm from the java property or the default if not defined
   */
  val priorityFromCDB: Priority =
    Option(props.getProperty(MultiplicityTF.alarmPriorityPropName)).map(Priority.valueOf(_)).getOrElse(Priority.getDefaultPriority)

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
  def shutdown(): Unit = {}

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
    grouped.view.mapValues(_.map(_._2).toList.mkString(",")).toMap
  }


  /**
    * Set the validity constraint to apply to the TF
    * 
    * The validity depnds on the validity of the inputs that contribute
    * to the determination of the output 
    * @see https://github.com/IntegratedAlarmSystem-Group/ias/issues/201
    *
    * @param isSet True if the output is set, Flase if cleared
    * @param setAlarms The IDs of the alarms in input that are SET
    * @param clearedAlarms The IDs of the alarms in input that are SET
    * @return the IDs of the alarm to use to set the validity of the output
    */
  def buildValidityConstraint(
    isSet: Boolean, 
    setAlarms: Iterable[IasIO[Alarm]], 
    clearedAlarms: Iterable[IasIO[Alarm]]): Option[Set[String]] = {

      val validSetAlarms = setAlarms.filter(alarm => {alarm.validity==IasValidity.RELIABLE}).toSet
      val validUnsetAlarms = clearedAlarms.filter(alarm => {alarm.validity==IasValidity.RELIABLE}).toSet

      val totAlarmsSz = setAlarms.size+clearedAlarms.size

      if (isSet && validSetAlarms.size>=threshold) Some(validSetAlarms.map(_.id))
      else if (!isSet && validUnsetAlarms.size>totAlarmsSz-threshold) Some(validUnsetAlarms.map(_.id))
      else None
    }
  
  /**
   * @see ScalaTransferExecutor#eval
   */
  def eval(compInputs: Map[String, IasIO[?]], actualOutput: IasIO[Alarm]): IasIO[Alarm] = {

    // Get the active (SET) alarms in input
    val activeAlarms: Iterable[IasIO[Alarm]] = compInputs.values.filter(input =>{
      input.value.isDefined && input.value.get.asInstanceOf[Alarm].isSet()
    }).map(_.asInstanceOf[IasIO[Alarm]])

    // Get the inactive (CLEARED) alarms in input
    val inactiveAlarms: Iterable[IasIO[Alarm]] = compInputs.values.filter(input =>{
      input.value.isDefined && input.value.get.asInstanceOf[Alarm].isCleared()
    }).map(_.asInstanceOf[IasIO[Alarm]])

    val actualAlarm = actualOutput.value.getOrElse(Alarm.getInitialAlarmState(priorityFromCDB))

    val newAlarm: Alarm = actualAlarm.setIf(activeAlarms.size>=threshold)

    // The properties of the output
    val props = if (newAlarm.isSet) {
      val idOfActiveAlarms = activeAlarms.map(_.id).mkString(",")
      getPropsOfOutput(activeAlarms.map(_.props)) + (MultiplicityTF.inputAlarmsSetPropName->idOfActiveAlarms)
    } else {
      Map.empty[String,String]
    }

    val mode = if (newAlarm.isSet) {
      getOutputMode(activeAlarms.map(_.mode))
    } else {
      getOutputMode(compInputs.values.map(_.mode))
    }

    val validityConstraints = buildValidityConstraint(newAlarm.isSet(), activeAlarms, inactiveAlarms)

    actualOutput.updateValue(newAlarm).updateMode(mode).updateProps(props).setValidityConstraint(validityConstraints)
  }
}

object MultiplicityTF {

  /** The logger */
  private val logger: Logger = IASLogger.getLogger(MultiplicityTF.getClass)
  
  /** The name of the property with the integer value of the threshold */
  val ThresholdPropName: String ="org.eso.ias.tf.mutliplicity.threshold"
  
  /** The name of the property to set the priority of the alarm */
  val alarmPriorityPropName: String = "org.eso.ias.tf.alarm.priority"

  /**
    * The name of the property with the IDs of the alarms in input that are set
    * and activate the output.
    *
    * This is a property of the output that can be useful for clients like the display
    */
  val inputAlarmsSetPropName: String = "IdsOfAlarmsSet"
}
