package org.eso.ias.heartbeat.serializer

import org.eso.ias.heartbeat.HbMsgSerializer
import com.fasterxml.jackson.databind.ObjectMapper
import org.eso.ias.heartbeat.HeartbeatStatus
import org.eso.ias.utils.ISO8601Helper
import scala.collection.JavaConverters

/**
 * Serialize/deserialize HB messages into JSON strings
 */
class HbJsonSerializer extends HbMsgSerializer {
  
  /** Jackson mapper */
  private val mapper: ObjectMapper = new ObjectMapper()
  
  /**
   * Serialize the HB message to publish in a string 
   * 
   * @param fullRunningId full running id
   * @param status the status of the tool
   * @paran additionalProeprties a map of additional properties
   * @param timestamp the timestamp to associate to the message
   * @return A string representation of the message and the timestamp
   */
  def serializeToString(
      fullRunningId: String,
      status: HeartbeatStatus, 
      additionalProeprties: Map[String,String],
      timestamp: Long): String = {
    require(Option(fullRunningId).isDefined && !fullRunningId.isEmpty())
    require(Option(status).isDefined)
    require(Option(additionalProeprties).isDefined)
    
    val javaProps = JavaConverters.mapAsJavaMap(additionalProeprties)
    val pojo: HeartbeatMessagePojo = new HeartbeatMessagePojo(fullRunningId,status,javaProps,timestamp)
    
    
    mapper.writeValueAsString(pojo)
  }
  
  /** 
   *  Parse the passed string to return a tuple with the
   *  field of the HB mesage
   *  
   *  @param A string representation of the message with the timestamp
   *  @return a tuple with the full running id, the status, properties and the timestamp
   */
  def deserializeFromString(hbStrMessage: String): 
    Tuple4[String,HeartbeatStatus, Map[String,String], Long] = {
    val pojo: HeartbeatMessagePojo = mapper.readValue(hbStrMessage, classOf[HeartbeatMessagePojo])
    
    val props: Map[String, String] = Option(pojo.getProps) match {
      case None => Map.empty
      case Some(p) => 
        if (p.isEmpty()) Map.empty else JavaConverters.mapAsScalaMap(p).toMap
    }
    
    val timeStamp = ISO8601Helper.timestampToMillis(pojo.getTimestamp)
    
    ( pojo.getFullRunningId,
        pojo.getState,
        props,
        timeStamp)
  }
  
  
}