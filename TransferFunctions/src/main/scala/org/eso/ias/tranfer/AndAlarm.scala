package org.eso.ias.transfer

import java.util.Properties
import com.typesafe.scalalogging.Logger
import org.eso.ias.logging.IASLogger
import org.eso.ias.types.IASTypes.*
import org.eso.ias.asce.transfer.{IasIO, IasioInfo, ScalaTransferExecutor}
import org.eso.ias.types.{Alarm, OperationalMode, Priority, IasValidity}
import org.eso.ias.asce.exceptions.{TypeMismatchException, UnexpectedNumberOfInputsException}

/**
  * AndAlarm transfer function takes n alarms in input and returns another alarm 
  * that is set if all the alarms in input are set.
  * 
  * Note that this TF can be replaced by the Multiplicty TF with the "threshold" property 
  * set to the number of inputs.
  * 
  * @param cEleId: the ID of the ASCE
  * @param cEleRunningId: the runningID of the ASCE
  * @param validityTimeFrame: The time frame (msec) to invalidate monitor points
  * @param props: the user defined properties
  * @author acaproni
  */
class AndAlarm (cEleId: String, cEleRunningId: String, validityTimeFrame: Long, props: Properties) 
extends ScalaTransferExecutor[Alarm](cEleId,cEleRunningId,validityTimeFrame,props) {

    /**
     * If set the logic is inverted
     *
     * By default it is false because of the definition of java.lang.Boolean.getBoolean
     */
    val invert: Boolean = Option(props.getProperty(AndAlarm.InvertLogicPropName)).
        map(java.lang.Boolean.valueOf(_).booleanValue()).
        getOrElse(AndAlarm.DefaultLogicPropValue.booleanValue())

    /**
    * The priority of the alarm from the java property or the default if not defined
    */
    val priorityFromCDB: Priority =
        Option(props.getProperty(AndAlarm.alarmPriorityPropName)).map(Priority.valueOf(_)).getOrElse(Priority.getDefaultPriority)

    /**
    * Check that all the inputs and the output are alarms
    *
    * @param inputsInfo The IDs and types of the inputs
    * @param outputInfo The Id and type of thr output
    **/
    override def initialize(inputsInfo: Set[IasioInfo], outputInfo: IasioInfo): Unit = {
        AndAlarm.logger.debug("Initializing")
        if (inputsInfo.size<2) {
            throw new UnexpectedNumberOfInputsException(1, inputsInfo.size)
        }

        val types = inputsInfo.map(_.iasioType)
        require(types.size==1, "Inputs have different types!")
        // Are the inputs alarm?
        if (types.head != ALARM) {
            throw new TypeMismatchException("Input types not ALARM: " + types.head)
        }

        // Is the output an alarm?
        if (outputInfo.iasioType != ALARM) {
            throw new TypeMismatchException("Output type is not ALARM: " + outputInfo.iasioType)
        }

        
        AndAlarm.logger.info("The AndAlarm TF accepts {} alarm inputs", inputsInfo.size)
        AndAlarm.logger.debug("Initialized")
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
    * @return the properties to assign to the ouput
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

        // The alarm in output is set if there are no inactive alarms in input
        val cond: Boolean = inactiveAlarms.size==0
        val newAlarm = if (!invert) actualAlarm.setIf(cond) else actualAlarm.setIf(!cond)

        // The properties of the output
        val props = if (newAlarm.isSet) {
            val idOfActiveAlarms = activeAlarms.map(_.id).mkString(",")
            getPropsOfOutput(activeAlarms.map(_.props)) + (AndAlarm.inputAlarmsSetPropName->idOfActiveAlarms)
        } else {
            Map.empty[String,String]
        }

        val mode = if (newAlarm.isSet) {
            getOutputMode(activeAlarms.map(_.mode))
        } else {
            getOutputMode(compInputs.values.map(_.mode))
        }

        actualOutput.updateValue(newAlarm).updateMode(mode).updateProps(props)
    }
  
}

object AndAlarm {
    /** The logger */
    private val logger: Logger = IASLogger.getLogger(AndAlarm.getClass)

    /** The name of the property to set the priority of the alarm in output*/
    val alarmPriorityPropName: String = "org.eso.ias.transfer.alarm.priority"   

    /**
    * The name of the property with the IDs of the alarms in input that are set
    * and activate the output.
    *
    * This is a property of the output that can be useful for clients like the display
    */
    val inputAlarmsSetPropName: String = "IdsOfAlarmsSet"

    /** The name of the boolean property to invert the logic */
    val InvertLogicPropName: String = "org.eso.ias.tf.andalarm.invert"

    /** By default the logic is not inverted */
    val DefaultLogicPropValue: Boolean = false
}
