package org.eso.ias.heartbeat.test

import org.eso.ias.heartbeat.consumer.{HbListener, HbMsg}
import org.eso.ias.logging.IASLogger
import org.scalatest.FlatSpec

import scala.collection.mutable.ListBuffer

/**
  * Test the HB consumer
  *
  * The test
  * - instantiates a consumer that puts all the received events in the buffer
  * - produces HB events
  * - check if the events in the buffer matches with the events produced
  *
  */
class TestKafkaHbConsumer extends FlatSpec with HbListener {

  /** The logger */
  private val logger = IASLogger.getLogger(classOf[TestKafkaHbConsumer])

  private val buffer: ListBuffer[HbMsg] = new ListBuffer[HbMsg]

  def hbReceived(hbMsh: HbMsg): Unit = {
    require(Option(hbMsh).isDefined)
    buffer.append(hbMsh)
  }


}
