package org.eso.ias.heartbeat.serializer

import com.fasterxml.jackson.databind.ObjectMapper
import org.eso.ias.heartbeat.{HbMsgSerializer, Heartbeat, HeartbeatStatus}
import org.eso.ias.utils.ISO8601Helper

import java.util
import scala.collection.JavaConverters
import scala.jdk.javaapi.CollectionConverters

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
  override def serializeToString(
      hb: Heartbeat,
      status: HeartbeatStatus, 
      additionalProeprties: Map[String,String],
      timestamp: Long): String = {
    require(Option(hb).isDefined)
    require(Option(status).isDefined)
    require(Option(additionalProeprties).isDefined)
    
    val javaProps: util.Map[String, String] = CollectionConverters.asJava(additionalProeprties)
    val pojo: HeartbeatMessagePojo = new HeartbeatMessagePojo(hb.stringRepr,status,javaProps,timestamp)
    
    
    mapper.writeValueAsString(pojo)
  }
  
  /** 
   *  Parse the passed string to return a tuple with the
   *  field of the HB mesage
   *  
   *  @param A string representation of the message with the timestamp
   *  @return a tuple with the full running id, the status, properties and the timestamp
   */
  override def deserializeFromString(hbStrMessage: String):
    Tuple4[Heartbeat,HeartbeatStatus, Map[String,String], Long] = {
    val pojo: HeartbeatMessagePojo = mapper.readValue(hbStrMessage, classOf[HeartbeatMessagePojo])
    
    val props:  Map[String,String] = Option(pojo.getProps) match {
      case None => Map.empty
      case Some(p) => 
        if (p.isEmpty()) Map.empty else CollectionConverters.asScala(p).toMap
    }
    
    val timeStamp = ISO8601Helper.timestampToMillis(pojo.getTimestamp)
    
    ( Heartbeat(pojo.getHbStringrepresentation),
        pojo.getState,
        props,
        timeStamp)
  }
  
  
}