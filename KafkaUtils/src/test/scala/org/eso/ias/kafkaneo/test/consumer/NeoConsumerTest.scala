package org.eso.ias.kafkaneo.test.consumer

import org.eso.ias.kafkaneo.consumer.Consumer
import org.eso.ias.logging.IASLogger
import org.scalatest.flatspec.AnyFlatSpec

class NeoConsumerTest extends AnyFlatSpec {

  /** The logger */
  private val logger = IASLogger.getLogger(this.getClass)

  behavior of "The Kafka Neo Consumer"

  it must "Add and remove listeners" in {
    logger.info("Testing listeners started")
    val consumer = new Consumer("Test1Group", "Test1Id")
    logger.info("Testing listeners done")
  }

}
