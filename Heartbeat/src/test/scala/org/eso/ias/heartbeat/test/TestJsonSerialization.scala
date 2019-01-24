package org.eso.ias.heartbeat.test

import org.eso.ias.heartbeat.serializer.HbJsonSerializer
import org.eso.ias.heartbeat.{Heartbeat, HeartbeatProducerType, HeartbeatStatus}
import org.scalatest.FlatSpec

class TestJsonSerialization extends FlatSpec {
  
  /** The JSON serializer to test */
  val serializer = new HbJsonSerializer

  /** The ID of th esupervisor */
  val supervId = "SupervisorID"
  
  /** The HB of the supervisor */
  val supervHeartbeat = Heartbeat(HeartbeatProducerType.SUPERVISOR,supervId)

  behavior of "The HB JSON serializer"

  it must "serialize/deserialize messages into JSON strings" in {

    // The timestamp
    val now = System.currentTimeMillis()

    val hbStatus = HeartbeatStatus.RUNNING

    val strMsg = serializer.serializeToString(supervHeartbeat, hbStatus, Map.empty,now)
    assert(!strMsg.isEmpty())

    println(strMsg)

    val (hb,status,props,deserializedTStamp) = serializer.deserializeFromString(strMsg)
    assert(deserializedTStamp==now)
    assert(status==hbStatus)
    assert(hb.hbType==supervHeartbeat.hbType)
    assert(hb.name==supervId)
    assert(props.isEmpty)
  }

  it must "serialize/deserialize a message with additional properties" in {
    val addProps: Map[String, String] = Map("alma.cl" -> "value1", "org.eso.ias.prop2" -> "123")
    val hbStatus = HeartbeatStatus.STARTING_UP

    // The timestamp
    val now = System.currentTimeMillis()

    val strMsg = serializer.serializeToString(
        supervHeartbeat,
        hbStatus,
        addProps,
        now)
    assert(!strMsg.isEmpty())

    println(strMsg)

    val (hb,status,props,deserializedTStamp) = serializer.deserializeFromString(strMsg)
    assert(deserializedTStamp==now)

    assert(hb.hbType==supervHeartbeat.hbType)
    assert(hb.name==supervId)
    assert(status==hbStatus)
    assert(props==addProps)
  }
  
}