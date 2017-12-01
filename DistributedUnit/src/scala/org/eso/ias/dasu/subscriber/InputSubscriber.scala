package org.eso.ias.dasu.subscriber

import scala.util.Try

/** 
 *  InputSubscriber get events from different sources
 *  and send them to the InputsListener 
 */
trait InputSubscriber {
  /** 
   *  Initialize the subscriber
   *  
   *  The get events, start() must be called 
   */
  def initialize(): Try[Unit]
  
  /** 
   *  CleanUp and release the resources 
   */
  def cleanUp(): Try[Unit]
  
  /**
   * Start to get events and forward them to the listener.
   * 
   * IASIOs whose ID is not in the acceptedInputs set are discarded.
   * 
   * @param listener the listener of events
   * @param acceptedInputs the IDs of the inputs accepted by the listener
   */
  def start(listener: InputsListener, acceptedInputs: Set[String]): Try[Unit]
}