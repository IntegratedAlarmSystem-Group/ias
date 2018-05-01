package org.eso.ias.heartbeat

import scala.collection.JavaConverters
import org.eso.ias.types.Identifier

/**
 * The heartbeat is the message repeatedly sent by each tool of the core
 * to notify about its computational phase and healtiness.
 * <P> 
 * The message is composed of the full running ID of the tool, its
 * computational phase and optionally some properties.
 * The message effectively published contains also the timestamp.
 * 
 * The message sent to the publisher subscriber framework includes a time stamp
 * added at the time of sending.
 * 
 * The HbMessage is immutable.
 * 
 * @param fullRunningId The full running ID of the tool publishing the heartbeat
 * @param hbState the state
 * @param additionalProps the additional properties
 * @author acaproni
 *
 */
case class HbMessage(
    fullRunningId: String,
    hbState: HeartbeatStatus,
    additionalProps: Map[String,String]) {
  require(Option(fullRunningId).isDefined && !fullRunningId.isEmpty(),"Invalid full running ID")
  require(Option(hbState).isDefined,"The HB state must be defined")
  require(Option(additionalProps).isDefined,"The properties must be defined")
  
  /**
   * Auxiliary constructor
   * 
   * @param id The identifier of the tool publishing the heartbeat
	 * @param hbState the initial state
	 * @param additionalProps additional properties
   */
  def this(id: Identifier, hbState: HeartbeatStatus,additionalProps: Map[String,String]) = {
    this(id.fullRunningID,hbState,additionalProps)
  }
  
  /**
   * Auxiliary constructor
   * 
   * @param id The identifier of the tool publishing the heartbeat
	 * @param hbState the initial state
   */
  def this(id: Identifier, hbState: HeartbeatStatus) = {
    this(id.fullRunningID,hbState,Map.empty[String,String])
  }
  
  /**
   * Auxiliary constructor
   * 
   * @param id The identifier of the tool publishing the heartbeat
	 * @param hbState the initial state
   */
  def this(fullRunningId: String, hbState: HeartbeatStatus) = {
    this(fullRunningId,hbState,Map.empty[String,String])
  }
    
  /**
	 * Set the new state
	 * 
	 * @param newState
	 * @return A new Heartbeat object with the passed state
	 */
  def setHbState(newState: HeartbeatStatus) = {
    require(Option(hbState).isDefined,"Invalid empty HB state")
    this.copy(hbState=newState)
  }
  
  /**
	 * Set the properties
	 * 
	 * @param newProps the properties
	 * @return A new  object with the passed properties
	 */
  def setHbProps(newProps: Map[String,String]) = {
    this.copy(additionalProps=newProps)
  }
  
  /**
	 * Set the properties
	 * 
	 * @param newProps the properties
	 * @return A new object with the passed properties
	 */
  def setHbProps(newProps: java.util.Map[String,String]) = {
    val temp = Option(newProps).map(map => JavaConverters.mapAsScalaMap(newProps).toMap).getOrElse(Map.empty)
    this.copy(additionalProps=temp)
  }

  /** Return the propertise as a java map */
  def getPropsAsJavaMap(): java.util.Map[String,String] = JavaConverters.mapAsJavaMap(additionalProps)

  
}