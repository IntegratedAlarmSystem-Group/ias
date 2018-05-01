package org.eso.ias.heartbeat

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
   * @param hbMessage theheartbeat to send
   * @param timestamp the timestamp to associate to the message
   */
  def send(hbMessage: HbMessage, timestamp: Long) = {
    require(Option(hbMessage).isDefined,"Cannot pubislh an empty HB")
     val strToSend = serializer.serializeToString(hbMessage, System.currentTimeMillis())
     push(strToSend)
  }
  
  /**
   * Push the string
   */
  def push(hbAsString: String)
  
}