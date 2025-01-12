package org.eso.ias.heartbeat

import java.util.concurrent.atomic.{AtomicBoolean, AtomicReference}
import java.util.concurrent.{Executors, ScheduledFuture, TimeUnit}

import org.eso.ias.logging.IASLogger

import scala.collection.mutable.{Map => MutableMap}

/**
 * The HbEngine periodically sends the message 
 * to the publisher subscriber framework.
 * 
 * Users of this class shall only notify of changes
 * in the status message.
 * 
 * It is a singleton to be sure that the same 
 * tool does not send the HB twice.
 * 
 * The initial status is STARTING_UP; then it must be updated by
 * calling [[updateHbState]] to follow the
 * computational phases of the tool that sends the HB.
 * 
 * @param hb the heartbeat of the tool
 * @param frequency the frequency to send the heartbeat (seconds)
 * @param publisher publish HB messages
 */
class HbEngine private[heartbeat] (
    val hb: Heartbeat,
    val frequency: Long,
    val publisher: HbProducer) extends Runnable {
  require(Option(hb).isDefined)
  require(Option(publisher).isDefined)

  /** Signal if the object has been started */
  private val started = new AtomicBoolean(false)
  
  /** Signal if the object has been closed */
  private val closed = new AtomicBoolean(false)
  
  /** Shutdown hook */
  val shutDownThread=addsShutDownHook()
  
  /** 
   *  The intial status set by default.
   *  Can be overridden with [[HbEngine.start[HeartbeatStatus]]]
   */
  val initialStatusDefault: HeartbeatStatus = HeartbeatStatus.STARTING_UP
  
  /** The actual status */
  private val hbStatus = new AtomicReference[HeartbeatStatus](initialStatusDefault)
  
  /** Additional properties  */
  private val props: MutableMap[String,String] = MutableMap.empty
  
  /** The periodic feature that push the heartbeat */
  private val feature = new AtomicReference[ScheduledFuture[?]]()

  /**
    * @return true if the HB has been started; false otherwise
    */
  def isStarted = started.get()

  /**
    * @return true if the HB has been closed; false otherwise
  */
  def isClosed = closed.get()
  
  /**
   * Start periodic sending of the heartbeat with the 
   * default initial status 
   */
  def start(): Unit = synchronized {
    if (!started.getAndSet(true)) {
      HbEngine.logger.debug("Initializing the Heartbeat engine")
      HbEngine.logger.debug("Initializing the HB publisher")
      publisher.init()
      HbEngine.logger.debug("HB publisher initialized")
      val executorService = Executors.newSingleThreadScheduledExecutor();
      feature.set(executorService.scheduleWithFixedDelay(this, frequency, frequency, TimeUnit.SECONDS))
      HbEngine.logger.info("Heartbeat engine started with a frequency of {} seconds",frequency.toString())
    } else {
      HbEngine.logger.warn("HB engine Already started")
    }
  }
  
  /**
   * Start periodic sending of the heartbeat with the given
   * initial status 
   * 
   * @param hbState The initial state of the process
   */
  def start(hbState: HeartbeatStatus): Unit = synchronized {
    require(Option(hbState).isDefined,"An intial state is needed")
    updateHbState(hbState)
    start()
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
   * @param newStateMsg The new state 
   */
  def updateHbState(newStateMsg: HeartbeatStatus): Unit = {
    hbStatus.set(newStateMsg)
  }
  
  /** 
   *  Return the actual status message
   *  
   *  It is empty if the engine has not yet been initialized
   */
  def getActualStatus: HeartbeatStatus = hbStatus.get
  
  /**
   * Stops sending the heartbeat and free the resources
   */
  def shutdown() = synchronized {
    if (!closed.getAndSet(true)) {
      HbEngine.logger.debug("HB engine shutting down")
      Option(feature.get).foreach( f => f.cancel(false))
      HbEngine.logger.debug("HB engine is shutting down the publisher")
      publisher.shutdown()
      HbEngine.logger.info("HB engine down")
    }
  }
  
  /**
   * Sends the heartbeat to the publisher
   */
  override def run(): Unit = {
    HbEngine.logger.debug("Sending HB")
    assert(started.get,"HB engine not initialized")
    if (!closed.get) {
      publisher.send(hb,hbStatus.get,props.toMap)
    }
  }
  
  /**
   * Add a property to attach to the HB message
   * 
   * @param key the key of the property
   * @param value he value of the property
   * @return the previous value of the key; 
   *         empty if a value with the given key does not exist 
   */
  def addProperty(key: String, value: String): Option[String] = {
    require(Option(key).isDefined && !key.isEmpty(),"Invalid null/empty key")
    require(Option(value).isDefined && !value.isEmpty(),"Invalid null/empty value")
    
    props.put(key,value)
  }
  
  /**
   * Remove a the property with the given key, if it exists
   * 
   * @param key the key of the property to remove
   * @return the value previously associated with the given key, if it exists
   */
  def removeProperty(key: String): Option[String] = {
    require(Option(key).isDefined && !key.isEmpty(),"Invalid null/empty key")
    props.remove(key)
  }
  
  /** Remove all the properties from the map */
  def clearProps(): Unit = {
    props.clear()
  }
}

/**
 * Companion object
 * 
 */
object HbEngine {

  /** The logger */
  val logger = IASLogger.getLogger(classOf[HbEngine])
  
  /** The singleton */
  var engine: Option[HbEngine] = None
  
  /**
    * Return the HbEngine singleton
    *
    * @param name the name of the tool
    * @param toolType the type of the tool
    * @param frequency the frequency to publish HBs in the topic
    * @param publisher the publisher that sends the HBs
    * @return the HbEngine
    */
  def apply(
    name: String,
    toolType: HeartbeatProducerType,
    frequency: Long,
    publisher: HbProducer) = {
       engine match {
         case None =>
            engine = Some(new HbEngine(Heartbeat(toolType,name),frequency,publisher))
            engine.get
         case Some(hbEng) => hbEng
       }
  }
  
  /**
    * Alias more familiar to java developers that delegates
    * to [[apply()]]
    *
    * @param name the name of the tool
    * @param toolType the type of the tool
    * @param frequency the frequency to publish HBs in the topic
    * @param publisher the publisher that sends the HBs
    * @return the HbEngine
    */
  def getInstance(
      name: String,
      toolType: HeartbeatProducerType,
      frequency: Long,
      publisher: HbProducer)() = apply(name,toolType,frequency,publisher)
  
}