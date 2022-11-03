package org.eso.ias.heartbeat.test

import org.eso.ias.heartbeat.{Heartbeat, HeartbeatProducerType}
import org.scalatest.flatspec.AnyFlatSpec

/**
  * Test the heartbeat
  */
class TestHeartbeat extends AnyFlatSpec {

  behavior of "The Heartbeat"

  it must "correctly build the objects from type and name" in {
    val name = "ToolName"
    val toolTp = HeartbeatProducerType.CLIENT

    val hb = Heartbeat(toolTp,name)

    assert(hb.name==name)
    assert(hb.hbType==toolTp)
    assert(hb.hostName.nonEmpty)
    assert(hb.stringRepr.nonEmpty)
  }

  it must "correctly build the objects from their string rpresentation" in {
    val name = "ToolName"
    val toolTp = HeartbeatProducerType.PLUGIN

    val hb = Heartbeat(toolTp,name)

    val hbFromStr = Heartbeat(hb.stringRepr)
    assert(hbFromStr.name==hb.name)
    assert(hbFromStr.hostName==hb.hostName)
    assert(hbFromStr.hbType==hb.hbType)
    assert(hbFromStr.stringRepr==hb.stringRepr)
  }

  it must "support all the tool types" in {
    val supportedTypes = HeartbeatProducerType.values()

    supportedTypes.foreach( tp => {
      val name = "toolNameame-"+tp.name()
      val hb = Heartbeat(tp,name)
      assert(hb.hbType==tp)
      assert(hb.name==name)
      assert(hb.hostName.nonEmpty)
    })
  }


}
