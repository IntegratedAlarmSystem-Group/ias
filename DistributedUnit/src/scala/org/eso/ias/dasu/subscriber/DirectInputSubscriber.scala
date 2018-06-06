package org.eso.ias.dasu.publisher

import org.eso.ias.dasu.subscriber.InputSubscriber
import scala.collection.mutable.{HashSet => MutableSet}
import scala.util.Success
import scala.util.Try
import org.eso.ias.dasu.subscriber.InputsListener
import org.eso.ias.types.IASValue
import org.eso.ias.logging.IASLogger

/** 
 *  The input subscriber to send event to the DASU
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
    
  /** Initialize */
  def initializeSubscriber(): Try[Unit] = Success(())

  /** CleanUp and release the resources */
  def cleanUpSubscriber(): Try[Unit] = Success(())
  
  /**
   * Start to get events and forward them to the listener.
   * 
   * IASIOs whose ID is not in the acceptedInputs set are discarded.
   * 
   * @param listener the listener of events
   * @param acceptedInputs the IDs of the inputs accepted by the listener
   */
  def startSubscriber(listener: InputsListener, acceptedInputs: Set[String]): Try[Unit] = {
    logger.info("Starting subscriber with accepted IDs {}",acceptedInputs.mkString(", "))
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
    val iasiosToSend = iasios.filter(iasio => acceptedInputs.contains(iasio.id))
    this.listener.foreach(l => l.inputsReceived(iasiosToSend))
  }
}
