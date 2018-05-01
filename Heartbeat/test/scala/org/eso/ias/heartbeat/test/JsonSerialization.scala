package org.eso.ias.heartbeat.test

import org.scalatest.FlatSpec
import org.eso.ias.heartbeat.serializer.HbJsonSerializer
import org.eso.ias.heartbeat.HbMessage
import org.eso.ias.types.Identifier
import org.eso.ias.types.IdentifierType
import org.eso.ias.heartbeat.HeartbeatStatus

class JsonSerialization extends FlatSpec {
  
  /** The JSON serializer to test */
  val serializer = new HbJsonSerializer
  
  /** The identifier of the supervisor */
  val supervIdentifier = new Identifier("SupervisorID", IdentifierType.SUPERVISOR, None)
  
  behavior of "The HB JSON serializer"
  
  it must "serialize/deserialize messages into JSON strings" in {
    val msg = new HbMessage(supervIdentifier.fullRunningID,HeartbeatStatus.RUNNING)
    
    // The timestamp
    val now = System.currentTimeMillis()
    
    val strMsg = serializer.serializeToString(msg, now)
    assert(!strMsg.isEmpty())
    
    println(strMsg)
    
    val (deserializedMsg, deserializedTStamp) = serializer.deserializeFromString(strMsg)
    assert(deserializedTStamp==now)
    assert(deserializedMsg==msg)
  }
  
  it must "serialize/deserialize a message with additional properties" in {
    val addProps: Map[String, String] = Map("alma.cl" -> "value1", "org.eso.ias.prop2" -> "123")
    val msg = new HbMessage(supervIdentifier.fullRunningID,HeartbeatStatus.STARTING_UP,addProps)
    
    // The timestamp
    val now = System.currentTimeMillis()
    
    val strMsg = serializer.serializeToString(msg, now)
    assert(!strMsg.isEmpty())
    
    println(strMsg)
    
    val (deserializedMsg, deserializedTStamp) = serializer.deserializeFromString(strMsg)
    assert(deserializedTStamp==now)
    
    println(deserializedMsg.fullRunningId)
    println(deserializedMsg.hbState)
    println(deserializedMsg.additionalProps)
    
    assert(deserializedMsg==msg)
    
  }
  
}