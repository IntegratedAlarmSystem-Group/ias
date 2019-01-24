package org.eso.ias.heartbeat

/**
 * HbPublisher trait for sending heartbeat
 * 
 * Implementers of this trait sends the heartbeat to the publisher framework,
 * a file or any other 
 */
trait HbMsgSerializer {
  
  /**
   * Serialize the HB message to publish in a string 
   * 
   * @param hb the heartbeat to publish
   * @param status the status of the tool
   * @paran additionalProeprties a map of additional properties
   * @param timestamp the timestamp to associate to the message
   * @return A string representation of the message and the timestamp
   */
  def serializeToString(
      hb: Heartbeat,
      status: HeartbeatStatus, 
      additionalProeprties: Map[String,String],
      timestamp: Long): String
  
  /** 
   *  Parse the passed string to return a tuple with the
   *  fields of the HB mesage
   *  
   *  @param A string representation of the message with the timestamp
   *  @return a tuple with the full running id, the status, properties and the timestamp
   */
  def deserializeFromString(hbMessage: String): 
    Tuple4[Heartbeat,HeartbeatStatus, Map[String,String], Long]
}