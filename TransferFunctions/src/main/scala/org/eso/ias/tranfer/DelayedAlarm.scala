package org.eso.ias.tranfer

import com.typesafe.scalalogging.Logger
import org.eso.ias.asce.transfer.{IasIO, IasioInfo, ScalaTransferExecutor}
import org.eso.ias.logging.IASLogger
import org.eso.ias.types.{Alarm, IASTypes}

import java.util.Properties
import java.util.concurrent.TimeUnit
import scala.util.Try

/**
 * The exception thrown by this TF in case of 
 * malfunctions
 */
class DelayedAlarmException(msg: String) extends Exception(msg)

/**
 * DelayedAlarm processes the alarm input and produces another alarm
 * if the alarm in input remains steady active for a given amount of time.
 * In the same way, the produced alarm is cleared if the input stays steady 
 * cleared for a given amount of time.
 * 
 * There is no default value for the delays: if they are not provided
 * the TF will throw an exception.
 * 
 * The actual implementation of the TF does not allow a ASCE to run a TF 
 * at a given time rate or after a delay elapsed.
 * This method does its best but will not be precise.
 * Issue #84 of the IAS core is about this problem.
 * The best way to get the alarm close to the required time frame
 * is to tune the refresh rate of the Supervisor where this TF is executed.
 * 
 * @param cEleId: the ID of the ASCE
 * @param cEleRunningId: the runningID of the ASCE
 * @param validityTimeFrame: The time frame (msec) to invalidate monitor points
 * @param props: the user defined properties    
 * @author acaproni
 */
class DelayedAlarm(cEleId: String, cEleRunningId: String, validityTimeFrame: Long, props: Properties) 
extends ScalaTransferExecutor[Alarm](cEleId,cEleRunningId,validityTimeFrame,props) {

  /**
   * Get the value of the passed property, if defined
   */
  def getValue(propName: String): Option[Long] = {
    require(Option(propName).isDefined && !propName.isEmpty)
    val propValueStr = Option(props.getProperty(propName))
    propValueStr.flatMap( str => { Try[Long](TimeUnit.MILLISECONDS.convert(str.toInt,TimeUnit.SECONDS)).toOption })
  }
  
  /**
   * The string with the delay (seconds) before setting the output if the input was set 
   */
  val waitTimeToSet: Option[Long] = getValue(DelayedAlarm.delayToSetTimePropName)
  waitTimeToSet.foreach(wtts => DelayedAlarm.logger.debug("Time to set: {}",wtts))
  
  /**
   * The  string with the delay (seconds) before setting the output if the input was set 
   */
  val waitTimeToClear: Option[Long] = getValue(DelayedAlarm.delayToClearTimePropName)
  waitTimeToClear.foreach(wttc => DelayedAlarm.logger.debug("Time to clear: {}",wttc))

  /**
   * The point in time when the input changed its state
   * from SET to CLEAR or vice-versa
   */
  private var lastStateChangeTime: Long = 0L
  
  /**
   * The previously received alarm
   */
  private var lastInputValue: Option[Alarm]=None

  /**
    * Initialize the TF
    *
    * @param inputsInfo The IDs and types of the inputs
    * @param outputInfo The Id and type of the output
    **/
  override def initialize(inputsInfo: Set[IasioInfo], outputInfo: IasioInfo): Unit = {
    DelayedAlarm.logger.debug("Initializing TF of [{}]", cEleId)

    // This TF expects one and only one input
    if (inputsInfo.size!=1) {
	    throw new DelayedAlarmException("Expected only 1 input but got "+inputsInfo.size)
	  }
    // Is the input an alarm?
    if (inputsInfo.head.iasioType!=IASTypes.ALARM) {
      throw new DelayedAlarmException("Input type is not alarm: "+inputsInfo.head.iasioType)
    }


    if (waitTimeToSet.isEmpty || waitTimeToClear.isEmpty) {
      throw new DelayedAlarmException("Time to set and/or time to clear properties not provided")
    }
    if (waitTimeToSet.get<0 || waitTimeToClear.get<0) {
      throw new DelayedAlarmException("Time range below 0: delayToSet="+waitTimeToSet.get+", delayToClear="+waitTimeToClear.get)
    }
    if (waitTimeToSet.get==0 && waitTimeToClear.get==0) {
      throw new DelayedAlarmException("No delays set: delayToSet=0, delayToClear==0")
    }

    DelayedAlarm.logger.debug("TF of [{}] initialized with delayToSet=[{}], delayToClear=[{}]", 
        cEleId, waitTimeToSet.get.toString, waitTimeToClear.get.toString)
  }
  
  /**
   * @see TransferExecutor#shutdown()
   */
  override def shutdown(): Unit = {
    DelayedAlarm.logger.debug("TF of [{}] shut down", cEleId)
  }
  
  /**
	 * Produces the output of the component by evaluating the inputs.
	 * 
	 * @return the computed output of the ASCE
	 */
  override def eval(compInputs: Map[String, IasIO[?]], actualOutput: IasIO[Alarm]): IasIO[Alarm] = {
	  // Here waitTimeToClear and waitTimeToSet must be defined because if they are not
	  // an exception in thrown by initialize() and the execution of the TF never enabled
    assert(waitTimeToClear.isDefined && waitTimeToSet.isDefined)

    // Get the input
    val iasio = compInputs.values.head
    assert(iasio.value.isDefined)

	  // Is the output an alarm?
    if (actualOutput.iasType!=IASTypes.ALARM) {
      throw new DelayedAlarmException("Output type is not alarm: "+actualOutput.iasType)
    }

    val actualAlarmValue =  actualOutput.value.getOrElse(Alarm.getInitialAlarmState)

    if (lastInputValue.isEmpty) {
	    // Initialization: if the output was never activated then
	    // return a CLEARED alarm because the delay did not elapsed
	    lastStateChangeTime=System.currentTimeMillis()
	    lastInputValue=Some(iasio.value.get.asInstanceOf[Alarm])
	    actualOutput.updateValue(actualAlarmValue.clear())
	  } else {
      
      // Did the input change?
      if (iasio.value.get!=lastInputValue.get) {
        lastInputValue=Some(iasio.value.get.asInstanceOf[Alarm])
        lastStateChangeTime = System.currentTimeMillis()
      } 
      
      val delayFromLastChange = System.currentTimeMillis()-lastStateChangeTime
      
      if (
          (actualAlarmValue.isSet && delayFromLastChange<waitTimeToClear.get) ||
          (!actualAlarmValue.isSet && delayFromLastChange<waitTimeToSet.get)) {
        // Not enough time elapsed from the last time the input changed: 
        // the output remains the same
        actualOutput
      } else {
        // enough time elapsed without changes in the input:
        // shell the output change?
        if (iasio.value.get==actualAlarmValue) {
          actualOutput
        } else {
          actualOutput.updateValue(actualAlarmValue.setIf(iasio.value.get.asInstanceOf[Alarm].isSet)).updateProps(iasio.props)
        }
      }
    }
  }
}

object DelayedAlarm {
  
  /**
   * The logger
   */
  val logger: Logger = IASLogger.getLogger(DelayedAlarm.getClass)
  
  /**
   * The time to wait (seconds) before setting the alarm
   */
  val delayToSetTimePropName = "org.eso.ias.tf.delaiedthreshold.settime"
  
  /**
   * The time to wait (seconds) before clearing the alarm
   */
  val delayToClearTimePropName = "org.eso.ias.tf.delaiedthreshold.cleartime"
  
}
