package org.eso.ias.heartbeat

import org.eso.ias.heartbeat.serializer.HeartbeatMessagePojo

/**
 * Implementation of HbProducer pushes the HB status message
 * 
 * @param serializer The serializer to transform the HB state in a string
 */
abstract class HbProducer(val serializer: HbMsgSerializer) {
  require(Option(serializer).isDefined,"Invalid empty HB serializer")
  
  /** Initialize the producer */
  def init()
  
  /** Shutdown the producer */
  def shutdown()
  
  /**
   * Publish the HB message with the passed time stamp 
   * 
   * @param fullRunningId full running id
   * @param status the status of the tool
   * @paran additionalProeprties a map of additional properties
   */
  def send(fullRunningId: String,
      status: HeartbeatStatus, 
      additionalProperties: Map[String,String]) = {
    require(Option(fullRunningId).isDefined && !fullRunningId.isEmpty(),"Invalid null/empty full running ID")
    
     val strToSend = serializer.serializeToString(fullRunningId,status,additionalProperties,System.currentTimeMillis())
     push(strToSend)
  }
  
  /**
   * Push the string
   */
  def push(hbAsString: String)
  
}