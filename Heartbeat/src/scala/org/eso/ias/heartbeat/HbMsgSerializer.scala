package org.eso.ias.heartbeat

/**
 * HbPublisher trait for sending heartbeat
 * 
 * Implementers of this trait sends the heartbeat to the publisher framework,
 * a file or any other 
 */
trait HbMsgSerializer {
  
  /**
   * Sends the heartbeat with the passed timestamp
   * 
   * @param hbMessage theheartbeat to send
   * @param timestamp the timestamp to associate to the message
   * @return A string representation of the message and the timestamp
   */
  def serializeToString(hbMessage: HbMessage, timestamp: Long): String
  
  /** 
   *  Parse the passed string to return a tuple with the
   *  HB mesage and the timestamp
   *  
   *  @param A string representation of the message with the timestamp
   *  @return a tuple with the message and the timestamp
   */
  def deserializeFromString(hbMessage: String): Tuple2[HbMessage, Long]
}