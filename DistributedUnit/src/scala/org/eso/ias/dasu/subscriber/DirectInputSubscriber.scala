package org.eso.ias.dasu.publisher

import org.eso.ias.dasu.subscriber.InputSubscriber
import scala.collection.mutable.{HashSet => MutableSet}
import scala.util.Success
import scala.util.Try
import org.eso.ias.dasu.subscriber.InputsListener
import org.eso.ias.types.IASValue
import org.eso.ias.logging.IASLogger

/** 
 *  The input subscriber to send event to the listener
 *  
 *  This subscriber, especially useful for testing, allows to programamtically 
 *  submit inputs that will be immediately sent to the listener.
 */
class DirectInputSubscriber extends InputSubscriber {
  
  /** The logger */
  val logger = IASLogger.getLogger(this.getClass)
  
  /** The listener of events */
  private var listener: Option[InputsListener] = None

  /** The set of inputs accepted by the DASU */
  private val acceptedInputs = MutableSet[String]()

  /** The producer has been initialized */
  @volatile private var initialized = false

  /** The producer has been closed */
  @volatile private var closed = false

  /**
    * @return true if it has been initialized; false otherwise
    */
  def isInitialized = initialized

  /**
    * @return true if it has been closed; false otherwise
    */
  def isClosed = closed
    
  /** Initialize */
  def initializeSubscriber(): Try[Unit] = {
    initialized=true
    Success(())
  }

  /** CleanUp and release the resources */
  def cleanUpSubscriber(): Try[Unit] = {
    closed=true
    Success(())
  }
  
  /**
   * Start to get events and forward them to the listener.
   * 
   * IASIOs whose ID is not in the acceptedInputs set are discarded.
   * 
   * @param listener the listener of events
   * @param acceptedInputs the IDs of the inputs accepted by the listener
   * 											(if empty accepts all the inputs)
   */
  def startSubscriber(listener: InputsListener, acceptedInputs: Set[String]): Try[Unit] = {
    if (acceptedInputs.nonEmpty) {
      logger.info("Starting subscriber with accepted IDs {}",acceptedInputs.mkString(", "))
    } else {
      logger.info("Starting subscriber accepting  all IDs")
    }
    this.listener=Option(listener)
    this.acceptedInputs++=acceptedInputs
    Success(())
  }
  
  /** 
   *  Send the passed inputs to the listener 
   *  @param iasios the inputs to be processed by the DASU
   */
  def sendInputs(iasios: Set[IASValue[_]]) = {
    require(Option(iasios).isDefined)
    val iasiosToSend = {
      if (acceptedInputs.nonEmpty) iasios.filter(iasio => acceptedInputs.contains(iasio.id))
      else iasios
    }
    this.listener.foreach(l => l.inputsReceived(iasiosToSend))
  }
}
