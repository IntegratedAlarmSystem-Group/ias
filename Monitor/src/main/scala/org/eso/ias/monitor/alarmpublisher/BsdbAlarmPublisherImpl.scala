package org.eso.ias.monitor.alarmpublisher
import org.eso.ias.kafkautils.{KafkaHelper, KafkaIasiosProducer, SimpleStringProducer}
import org.eso.ias.types.{IASValue, IasValueJsonSerializer}

import scala.jdk.javaapi.CollectionConverters

/**
  * Sends alarms to the BSDB by delegating to the [[KafkaIasiosProducer]]
  *
  * @param stringProducer The shared kafka string producer
  */
class BsdbAlarmPublisherImpl(val stringProducer: SimpleStringProducer) extends  MonitorAlarmPublisher {
  /** The producer of alarms */
  private val producer: KafkaIasiosProducer = new KafkaIasiosProducer(
    stringProducer,
    KafkaHelper.IASIOs_TOPIC_NAME,
    new IasValueJsonSerializer
  )

  /** Preparae the publisher to send data */
  override def setUp(): Unit = producer.setUp()

  /** Closes the publisher: no alarms will be sent afetr closing */
  override def tearDown(): Unit = producer.tearDown()

  /** Send the passed values to the BSDB */
  override def push(iasios: Array[IASValue[?]]): Unit = {
    require(Option(iasios).isDefined)
    if (iasios.nonEmpty) {
      producer.push(CollectionConverters.asJavaCollection(iasios))
    }
  }

  /** Flush the values to force immediate sending */
  override def flush(): Unit = producer.flush()
}
