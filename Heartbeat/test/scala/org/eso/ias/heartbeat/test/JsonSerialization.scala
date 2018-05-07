package org.eso.ias.heartbeat.test

import org.scalatest.FlatSpec
import org.eso.ias.heartbeat.serializer.HbJsonSerializer
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
    
    // The timestamp
    val now = System.currentTimeMillis()
    
    val hbStatus = HeartbeatStatus.RUNNING
    
    val strMsg = serializer.serializeToString(supervIdentifier.fullRunningID, hbStatus, Map.empty,now)
    assert(!strMsg.isEmpty())
    
    println(strMsg)
    
    val (frId,status,props,deserializedTStamp) = serializer.deserializeFromString(strMsg)
    assert(deserializedTStamp==now)
    assert(status==hbStatus)
    assert(frId==supervIdentifier.fullRunningID)
    assert(props.isEmpty)
  }
  
  it must "serialize/deserialize a message with additional properties" in {
    val addProps: Map[String, String] = Map("alma.cl" -> "value1", "org.eso.ias.prop2" -> "123")
    val hbStatus = HeartbeatStatus.STARTING_UP
    
    // The timestamp
    val now = System.currentTimeMillis()
    
    val strMsg = serializer.serializeToString(
        supervIdentifier.fullRunningID, 
        hbStatus,
        addProps,
        now)
    assert(!strMsg.isEmpty())
    
    println(strMsg)
    
    val (frId,status,props,deserializedTStamp) = serializer.deserializeFromString(strMsg)
    assert(deserializedTStamp==now)
    
    assert(frId==supervIdentifier.fullRunningID)
    assert(status==hbStatus)
    assert(props==addProps)
    
    
  }
  
}