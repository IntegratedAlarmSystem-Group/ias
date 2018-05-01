package org.eso.ias.heartbeat.publisher

import org.eso.ias.heartbeat.HbMsgSerializer
import org.eso.ias.heartbeat.HbMessage
import com.fasterxml.jackson.databind.ObjectMapper
import org.eso.ias.heartbeat.serializer.HeartbeatMessagePojo
import org.eso.ias.heartbeat.HeartbeatStatus
import org.eso.ias.utils.ISO8601Helper

/**
 * Serialize/deserialize HB messages into JSON strings
 */
class HbJsonSerializer extends HbMsgSerializer {
  
  /** Jackson mapper */
  val mapper: ObjectMapper = new ObjectMapper()
  
  /**
   * Sends the heartbeat with the passed timestamp
   * 
   * @param hbMessage theheartbeat to send
   * @param timestamp the timestamp to associate to the message
   * @return A string representation of the message and the timestamp
   */
  def serializeToString(hbMessage: HbMessage, timestamp: Long): String = {
    mapper.writeValueAsString(new HeartbeatMessagePojo(hbMessage,timestamp))
  }
  
  /** 
   *  Parse the passed string to return a tuple with the
   *  HB message and the timestamp
   *  
   *  @param hbStrMessage A string representation of the message with the timestamp
   *  @return a tuple with the message and the timestamp
   */
  def deserializeFromString(hbStrMessage: String): Tuple2[HbMessage, Long] = {
    val pojo: HeartbeatMessagePojo = mapper.readValue(hbStrMessage, classOf[HeartbeatMessagePojo])
    val hbMessage = new HbMessage(pojo.getFullRunningId,HeartbeatStatus.valueOf(pojo.getState))
    (hbMessage,ISO8601Helper.timestampToMillis(pojo.getTimestamp))
  }
  
  
}