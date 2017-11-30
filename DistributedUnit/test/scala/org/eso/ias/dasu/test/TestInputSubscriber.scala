package org.eso.ias.dasu.test

import org.eso.ias.dasu.subscriber.InputSubscriber
import scala.collection.mutable.{HashSet => MutableSet}
import scala.util.Success
import scala.util.Try
import org.eso.ias.dasu.subscriber.InputsListener
import org.eso.ias.prototype.input.java.IASValue

/** 
   *  The input subscriber to send event to the DASU   *  
   */
  class TestInputSubscriber extends InputSubscriber {
    
    /** The listener of events */
    private var listener: Option[InputsListener] = None
  
    /** The set of inputs accepted by the DASU */
    private val acceptedInputs = MutableSet[String]()
      
    /** Initialize */
    def initialize(): Try[Unit] = Success(())
  
    /** CleanUp and release the resources */
    def cleanUp(): Try[Unit] = Success(())
    
    /**
     * Start to get events and forward them to the listener.
     * 
     * IASIOs whose ID is not in the acceptedInputs set are discarded.
     * 
     * @param listener the listener of events
     * @param acceptedInputs the IDs of the inputs accepted by the listener
     */
    def start(listener: InputsListener, acceptedInputs: Set[String]): Try[Unit] = {
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
      this.listener.foreach(l => l.inputsReceived(iasios))
    }
  }