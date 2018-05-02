package org.eso.ias.heartbeat

import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import org.eso.ias.logging.IASLogger
import java.util.concurrent.atomic.AtomicReference
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledFuture

/**
 * The HbEngine periodically sends the message 
 * to the publisher subscriber framework.
 * 
 * Users of this class shall only notify of changes
 * in the status message.
 * 
 * @param frequency the frequency to send the heartbeat
 * @param timeUnit the time unit of the frequency
 * @param publisher publish HB messages
 */
class HbEngine(
    frequency: Long, 
    timeUnit: TimeUnit,
    publisher: HbProducer) extends Runnable {
  require(Option(timeUnit).isDefined)
  require(Option(publisher).isDefined)
  
  /** The logger */
  val logger = IASLogger.getLogger(classOf[HbEngine])
  
  /** Signal if the object has been closed */
  val closed = new AtomicBoolean(false)
  
  /** Shutdown hook */
  val shutDownThread=addsShutDownHook()
  
  /** The message to send */
  val hbStatusMessage = new AtomicReference[HbMessage]()
  
  /** The periodic feature that push the heartbeat */
  private val feature = new AtomicReference[ScheduledFuture[_]]()
  
  /**
   * Start periodic sending of the heartbeat with the given
   * initial status message
   */
  def start(initialMsg: HbMessage) = synchronized {
    logger.debug("Initializing the Heartbeat engine")
    updateHbState(initialMsg)
    logger.debug("Initializing the HB publisher")
    publisher.init()
    logger.debug("HB publisher initialized")
    val executorService = Executors.newSingleThreadScheduledExecutor();
    feature.set(executorService.scheduleWithFixedDelay(this, frequency, frequency, timeUnit))
    logger.info("Heartbeat engine started with a frequency of {} {}",frequency.toString(),timeUnit.toString())
  }
  
  /** Adds a shutdown hook to cleanup resources before exiting */
  private def addsShutDownHook(): Thread = {
    val t = new Thread("Shutdown hook for HB engine") {
        override def run() = {
          shutdown()
        }
    }
    Runtime.getRuntime().addShutdownHook(t)
    t
  }
  
  /**
   * Update the Hb status message
   * 
   * @param newStateMsg The new state message
   */
  def updateHbState(newStateMsg: HbMessage) {
    require(Option(newStateMsg).isDefined,"Cannot update with an empty HB message")
    hbStatusMessage.set(newStateMsg)
  }
  
  /**
   * Update the state message with the passed state
   * 
   * This method does not allow to update the addiotional properties.
   * 
   * @param hbState The new state message
   */
  def updateHbState(hbState: HeartbeatStatus) {
    require(Option(hbState).isDefined,"Cannot update with an empty HB state")
    val actualMessage = getActualHbStatusMessage
    assert(actualMessage.isDefined,"Not initialized?")
    actualMessage.foreach( msg => updateHbState(msg.setHbState(hbState)))
  }
  
  /** 
   *  Return the actual status message
   *  
   *  It is empty if the engine has not yet been initialized
   */
  def getActualHbStatusMessage: Option[HbMessage] = Option(hbStatusMessage.get)
  
  /**
   * Stops sending the heartbeat and free the resources
   */
  def shutdown() = synchronized {
    val wasAlreadyClosed = closed.getAndSet(true)
    if (!wasAlreadyClosed) {
      logger.debug("HB engine shutting down")
      Option(feature.get).foreach( f => f.cancel(false))
      logger.debug("HB engine is shutting down the publisher")
      publisher.shutdown()
      logger.info("HB engine down")
    }
  }
  
  /**
   * Sends the heartbeat to the publisher
   */
  override def run() {
    assert(Option(hbStatusMessage.get).isDefined)
    if (!closed.get) {
      publisher.send(hbStatusMessage.get, System.currentTimeMillis())
    }
  }
}