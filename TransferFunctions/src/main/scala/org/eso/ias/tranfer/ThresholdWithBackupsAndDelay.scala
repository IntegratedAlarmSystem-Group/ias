package org.eso.ias.tranfer

import java.util.Properties
import java.util.concurrent.TimeUnit
import com.typesafe.scalalogging.Logger
import org.eso.ias.asce.exceptions.{PropsMisconfiguredException, TypeMismatchException}
import org.eso.ias.asce.transfer.{IasIO, IasioInfo, ScalaTransferExecutor}
import org.eso.ias.logging.IASLogger
import org.eso.ias.types.{Alarm, IasValidity, OperationalMode, Priority}
import org.eso.ias.types.IASTypes.*

/**
  * This class is very similar to the BackupSelector but in case of a failure
  * instead of the main monitor points checks if any of the others
  * is over (or below) the thresholds to generate an alarm.
  *
  * Monitor points in inputs are assumed to be numeric and for the calculation
  * are all converted to double.
  *
  * Thresholds are defined in the same way they are defined in the MinMaxThresholdTF.
  *
  * It is optionally possible to set a activation and deactivation delay like the one
  * provided by the DelayedAlarm: if not set no delay is implemented
  *
  * The ThresholdWithBackupsAndDelay generates an alarm if the value of the main monitor point
  * (or, if not available, one of the backups) is greater (lower) then the threshold.
  * If a delay is given, the generation or clearing of the alarm is done only after
  * the timeout elapses.
  *
  * @param asceId : the ID of the ASCE
  * @param asceRunningId: the runningID of the ASCE
  * @param validityTimeFrame: The time frame (msec) to invalidate monitor points
  * @param props: the user defined properties
  * @author acaproni
  */
class ThresholdWithBackupsAndDelay(asceId: String, asceRunningId: String, validityTimeFrame:Long, props: Properties)
  extends ScalaTransferExecutor[Alarm](asceId,asceRunningId,validityTimeFrame,props) {

  /** Delay to set the alarm (msecs) */
  val delayToSet: Long = {
    val seconds = getValue(props, ThresholdWithBackupsAndDelay.DelayToSetTimePropName,ThresholdWithBackupsAndDelay.DefaultDelayToSetTime).toInt
    TimeUnit.MILLISECONDS.convert(seconds,TimeUnit.SECONDS)
  }


  /** Delay to clear the alarm (msecs) */
  val delayToClear: Long = {
    val seconds = getValue(props, ThresholdWithBackupsAndDelay.DelayToClearTimePropName,ThresholdWithBackupsAndDelay.DefaultDelayToClearTime).toInt
    TimeUnit.MILLISECONDS.convert(seconds,TimeUnit.SECONDS)
  }

  /**
    * The (high) alarm is activated when the value of the IASIO
    * is greater then HighON
    */
  val highOn: Double = getValue(props, ThresholdWithBackupsAndDelay.HighOnPropName, Double.MaxValue)

  /**
    * if the (high) alarm is active and the value of the IASIO
    * goes below HighOFF, then the alarm is deactivated
    */
  val highOff: Double = getValue(props, ThresholdWithBackupsAndDelay.HighOffPropName, highOn)

  /**
    * the (low) alarm is activated when the value of the IASIO is
    * lower then LowON
    */
  val lowOn: Double =  getValue(props, ThresholdWithBackupsAndDelay.LowOnPropName, Double.MinValue)

  /**
    * if the (low) alarm is active and the value of the IASIO
    * becomes greater then LowOFF, then the alarm is deactivated
    */
  val lowOff: Double = getValue(props, ThresholdWithBackupsAndDelay.LowOffPropName, lowOn)

  /** The Ids of the main IASIOs against the backups */
  val idOfMainInput: Option[String] = Option(props.getProperty(ThresholdWithBackupsAndDelay.MaindIdPropName))

  /** The priority to SET */
  val alarmPriority: Priority = Priority.valueOf(
    props.getProperty(ThresholdWithBackupsAndDelay.AlarmPriorityPropName,ThresholdWithBackupsAndDelay.AlarmPriorityDefault.name()))

  /**
    * The previously calculated alarm
    * Due to the delay, it can be that this change is not immediately
    * sent to the BSDB
    */
  private var lastCalcAlarmState: Option[Alarm]=None

  /**
    * The point in time when the inputs changed the state
    * from SET to CLEAR or vice-versa.
    */
  private var lastStateChangeTimeRequest: Long = 0L

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
    * Initialize the TF
    *
    * @param inputsInfo The IDs and types of the inputs
    * @param outputInfo The Id and type of thr output
    **/
  override def initialize(inputsInfo: Set[IasioInfo], outputInfo: IasioInfo): Unit = {
    ThresholdWithBackupsAndDelay.logger.debug("TF of ASCE [{}] initializing", asceId)
    if (outputInfo.iasioType != ALARM) {
      throw new TypeMismatchException(outputInfo.iasioId, outputInfo.iasioType, ALARM)
    }

    if (delayToSet<0) {
      throw new PropsMisconfiguredException(Map(ThresholdWithBackupsAndDelay.DelayToSetTimePropName->delayToSet.toString))
    }
    if (delayToSet==0) ThresholdWithBackupsAndDelay.logger.debug("No delay to set the alarm")
    else ThresholdWithBackupsAndDelay.logger.info("Delay to set alarm at {} msecs",delayToSet.toString)

    if (delayToClear<0) {
      throw new PropsMisconfiguredException(Map(ThresholdWithBackupsAndDelay.DelayToClearTimePropName->delayToClear.toString))
    }
    if (delayToClear==0) ThresholdWithBackupsAndDelay.logger.debug("No delay to clear the alarm")
    else ThresholdWithBackupsAndDelay.logger.info("Delay to clear alarm at {} msecs",delayToClear.toString)

    if (highOn<highOff) {
      throw new PropsMisconfiguredException(
        Map(ThresholdWithBackupsAndDelay.HighOnPropName->highOn.toString,ThresholdWithBackupsAndDelay.HighOffPropName->highOff.toString))
    }

    if (lowOff<lowOn) {
      throw new PropsMisconfiguredException(
        Map(ThresholdWithBackupsAndDelay.LowOnPropName->lowOn.toString,ThresholdWithBackupsAndDelay.LowOffPropName->lowOff.toString))
    }
    if (lowOff>highOff) {
      throw new PropsMisconfiguredException(
        Map(ThresholdWithBackupsAndDelay.LowOffPropName->lowOff.toString,ThresholdWithBackupsAndDelay.HighOffPropName->highOff.toString))
    }

    if (idOfMainInput.isEmpty || idOfMainInput.get.isEmpty) {
      throw new PropsMisconfiguredException(
        Map(ThresholdWithBackupsAndDelay.MaindIdPropName->lowOff.toString))
    }

    ThresholdWithBackupsAndDelay.logger.info("TF of ASCE [{}]: ID of the main IASIO: [{}]",asceId,idOfMainInput.get)
    ThresholdWithBackupsAndDelay.logger.info("TF of ASCE [{}]: priority of alarm: [{}]",asceId,alarmPriority.name())

    ThresholdWithBackupsAndDelay.logger.info("TF of ASCE [{}] initialized", asceId)
  }

  /**
    * @see TransferExecutor#shutdown()
    */
  override def shutdown(): Unit = {
    ThresholdWithBackupsAndDelay.logger.debug("TF of ASCE [{}] shut down", asceId)
  }

  /**
    * Get and return the double in the passed iasio
    *
    * @return the double in the passed iasio, if any
    */
  def getDoubleValueOfIasio(iasio: IasIO[?]): Double = {
    iasio.value.map( value =>
      iasio.iasType match {
        case LONG => value.asInstanceOf[Long].toDouble
        case INT => value.asInstanceOf[Int].toDouble
        case SHORT => value.asInstanceOf[Short].toDouble
        case BYTE => value.asInstanceOf[Byte].toDouble
        case DOUBLE => value.asInstanceOf[Double]
        case FLOAT => value.asInstanceOf[Float].toDouble
        case _ => throw new TypeMismatchException(iasio.fullRunningId,iasio.iasType,List(LONG,INT,SHORT,BYTE,DOUBLE,FLOAT))
    }).get
  }

  /**
    * Get the value of the inputs taking into account the validity of
    * the main monitor point or, in case of failure, the values of the backups.
    *
    * The value is a list composed of the only main monitor point if it is reliable,
    * otherwise the list of the backups to consider, i.e. those that are reliable
    *
    * @param inputs the inputs
    * @return the values of the inputs, or empty if no IASIO is valid
    */
  def getIasiosWithBackup(inputs: Map[String, IasIO[?]]): List[IasIO[?]] = {
    val validInputs: List[IasIO[?]] = inputs.values.filter(
      iasio => iasio.validity==IasValidity.RELIABLE && iasio.mode==OperationalMode.OPERATIONAL).toList
    if (validInputs.map(_.id).contains(getIdentifier(idOfMainInput.get))) List(getValue(inputs,idOfMainInput.get).get)
    else validInputs
  }

  /**
    * Check if the output must be SET or CLEARED because
    * the value of the main input (or one of the backups) is
    * greater than the threshold
    *
    * @param wasSet true if the alarm was set
    * @param doubleValues the values of the IASIOs in input
    * @return true if the alarm must be set
    */
  def mustBeSetByThreshold(wasSet: Boolean, doubleValues: List[Double]): Boolean = {
    require(doubleValues.nonEmpty)

      (doubleValues.exists(_>=highOn) || doubleValues.exists(_<=lowOn)) ||
    wasSet && (doubleValues.exists(_>=highOff) || doubleValues.exists(_<=lowOff))
  }

  /**
    * Produces the output of the component by evaluating the inputs.
    *
    * @return the computed output of the ASCE
    */
  override def eval(compInputs: Map[String, IasIO[?]], actualOutput: IasIO[Alarm]): IasIO[Alarm] = {
    assert(compInputs.values.forall(_.value.isDefined)) // This should be ensured by ASCE
    assert(getValue(compInputs, idOfMainInput.get).isDefined,
      "ASCE ["+compElementId+"] ID of main input "+idOfMainInput.get+" not found: received inputs "+compInputs.keySet.mkString(","))


    // The actual Alarm
    val actualOutputAlarm: Alarm = actualOutput.value.getOrElse(Alarm.getInitialAlarmState)

    // If not yet initialized, assumed alarm not set
    val wasSet: Boolean = actualOutputAlarm.isSet

    ThresholdWithBackupsAndDelay.logger.debug("TF of ASCE[{}]: wasSet={}",asceId,wasSet)

    // Get the valid IASIOs if any
    val iasios: List[IasIO[?]] = getIasiosWithBackup(compInputs)
    ThresholdWithBackupsAndDelay.logger.debug("TF of ASCE[{}]: operational and valid iasios {}",asceId,iasios.map(_.id).mkString(","))

    /** The values of the valid and operational inputs */
    val iasioVals = iasios.map(iasio => getDoubleValueOfIasio(iasio))

    // True if the alarm must be SET
    val toBeSetByThreshold = {
      if (iasios.isEmpty) mustBeSetByThreshold(wasSet, List(getDoubleValueOfIasio(getValue(compInputs, idOfMainInput.get).get)))
      else mustBeSetByThreshold(wasSet, iasioVals)
    }
    // The alarm to set in the output by the Threshold only
    val requestedAlarmByThreshold: Alarm = actualOutputAlarm.setIf(toBeSetByThreshold)
    ThresholdWithBackupsAndDelay.logger.debug("TF of ASCE[{}]: requested by thershold={}",asceId,requestedAlarmByThreshold.toString)

    val newOutput: Alarm = {
      if (actualOutput.value.isEmpty) { // First iteration
        lastStateChangeTimeRequest = System.currentTimeMillis()
        lastCalcAlarmState = Some(requestedAlarmByThreshold)
        if (delayToSet>0) actualOutputAlarm // Always CLEAR at the beginning with delay
        else requestedAlarmByThreshold // Immediate activation
      } else if (actualOutputAlarm.isSet && requestedAlarmByThreshold.isSet) {
        // If the output matches with the new request state
        // then it does not change
        lastStateChangeTimeRequest = System.currentTimeMillis()
        lastCalcAlarmState = Some(requestedAlarmByThreshold)
        requestedAlarmByThreshold
      } else {
        val delayFromLastChange = System.currentTimeMillis()-lastStateChangeTimeRequest
        if ( (!toBeSetByThreshold && delayFromLastChange>=delayToClear) ||
        (toBeSetByThreshold && delayFromLastChange>=delayToSet)) {
          requestedAlarmByThreshold
        } else {
          actualOutputAlarm
        }
      }
    }

    // The property must report the higher or lowest value between the IASIOs
    // used to calculate if the value passed the threshold
    val buildProps: Map[String, String] = {
      if (iasios.exists(_.id==idOfMainInput.get)) {
        Map("value" -> getDoubleValueOfIasio(getValue(compInputs, idOfMainInput.get).get).toString)
      } else {
        Map("backups" -> iasioVals.mkString(","))
      }
    }

    val mode = {
      if (iasios.isEmpty) getValue(compInputs, idOfMainInput.get).get.mode
      else OperationalMode.OPERATIONAL // iasios filters bythis mode
    }

    val constraints: Option[Set[String]] = {
      if (iasios.isEmpty) None
      else Some(iasios.map(iasio => getIdentifier(iasio.id)).toSet)
    }


    actualOutput.updateValue(newOutput).updateProps(buildProps).setValidityConstraint(constraints).updateMode(mode)
  }


}

object ThresholdWithBackupsAndDelay {

  /** The logger */
  val logger: Logger = IASLogger.getLogger(ThresholdWithBackupsAndDelay.getClass)

  /** The name of the HighOn property */
  val HighOnPropName: String = "org.eso.ias.thresholdbackup.minmaxthreshold.highOn"

  /** The name of the HighOff property  */
  val HighOffPropName: String = "org.eso.ias.thresholdbackup.minmaxthreshold.highOff"

  /** The name of the lowOn property */
  val LowOnPropName: String = "org.eso.ias.thresholdbackup.minmaxthreshold.lowOn"

  /** The name of the lowOff property  */
  val LowOffPropName: String = "org.eso.ias.thresholdbackup.minmaxthreshold.lowOff"

  /** The name of the property to set the priority of the alarm */
  val AlarmPriorityPropName: String = "org.eso.ias.thresholdbackup.alarm.priority"

  /** The priority of the alarm generated by default */
  val AlarmPriorityDefault: Priority = Priority.getDefaultPriority

  /** The time to wait (seconds) before setting the alarm  */
  val DelayToSetTimePropName: String = "org.eso.ias.thresholdbackup.delaiedthreshold.settime"

  /** Delay to set is disabled by default */
  val DefaultDelayToSetTime =0

  /** The time to wait (seconds) before clearing the alarm  */
  val DelayToClearTimePropName: String = "org.eso.ias.thresholdbackup.delaiedthreshold.cleartime"

  /** Delay to clear is disabled by default */
  val DefaultDelayToClearTime =0

  /** The name of the property to set the ID of the main IASIO against the backups */
  val MaindIdPropName: String = "org.eso.ias.thresholdbackup.selector.mainInputId"
}
