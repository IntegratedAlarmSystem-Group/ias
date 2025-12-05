package org.eso.ias.kafkaneo.test.consumer

import org.eso.ias.kafkaneo.consumer.ConsumerHelper
import org.eso.ias.logging.IASLogger
import org.scalatest.flatspec.AnyFlatSpec

/** test the [[ConsumerHelper]] */
class ConsumerHelperTest extends AnyFlatSpec {
  /** The logger */
  private val logger = IASLogger.getLogger(this.getClass)

  "A ConsumerHelper" should "Get the group id" in {
    val servers = "host:9095, host2:9091"
    val p = ConsumerHelper.setupProps("MyGruopID", Map.empty, servers, "KDser", "VDser")

    assert(p.get("group.id")=="MyGruopID")
    assert(p.get("bootstrap.servers")==servers)

    assert(p.get("key.deserializer")=="KDser")
    assert(p.get("value.deserializer")=="VDser")
  }
  
  

}
