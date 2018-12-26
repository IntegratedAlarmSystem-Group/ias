package org.eso.ias.monitor.alarmpublisher
import org.eso.ias.kafkautils.{KafkaHelper, KafkaIasiosProducer}
import org.eso.ias.types.{IASValue, IasValueJsonSerializer}

import scala.collection.JavaConverters

/**
  * Sends alarms to the BSDB by delegating to the [[KafkaIasiosProducer]]
  *
  * @param brokers Kafka broker for the producer
  * @param id The id of the consumer
  */
class BsdbAlarmPublisherImpl(val brokers: String, val id: String) extends  MonitorAlarmPublisher {
  /** The producer of alarms */
  private val producer: KafkaIasiosProducer = new KafkaIasiosProducer(
    brokers,
    KafkaHelper.IASIOs_TOPIC_NAME,
    id,
    new IasValueJsonSerializer
  )

  /** Preparae the publisher to send data */
  override def setUp(): Unit = producer.setUp()

  /** Closes the publisher: no alarms will be sent afetr closing */
  override def tearDown(): Unit = producer.tearDown()

  /** Send the passed values to the BSDB */
  override def push(iasios: Array[IASValue[_]]): Unit = {
    require(Option(iasios).isDefined)
    if (iasios.nonEmpty) {
      producer.push(JavaConverters.asJavaCollection(iasios))
    }
  }

  /** Flush the values to force immediate sending */
  override def flush(): Unit = producer.flush()
}
