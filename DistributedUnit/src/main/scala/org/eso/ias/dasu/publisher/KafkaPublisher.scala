package org.eso.ias.dasu.publisher

import java.util.Properties

import org.eso.ias.kafkautils.{KafkaHelper, KafkaIasiosProducer, SimpleStringProducer}
import org.eso.ias.types.{IASValue, IasValueJsonSerializer}

import scala.util.Try

/** 
 *  Publishes the output to Kafka queues by delegating 
 *  to SimpleStringProducer.
 *  
 *  The id of the IASIO to published is used as key.
 *  
 *  @param dasuId te identifier of the DASU
 *  @param kafkaProducer the kafka producer
 *  @param props additional set of properties
 */ 
class KafkaPublisher private (
    val dasuId: String, 
    private val kafkaProducer: KafkaIasiosProducer,
    props: Properties) 
extends OutputPublisher {
  require(Option(dasuId).isDefined && !dasuId.isEmpty)
  require(Option(kafkaProducer).isDefined)
  require(Option(props).isDefined)

  /**
   *  Initialize the Kafka subscriber.
   *  
   *  @return Success or Failure if the initialization went well 
   *          or encountered a problem  
   */
  override def initializePublisher(): Try[Unit] = {
    Try(kafkaProducer.setUp(props))
  }
  
  /**
   * Release all the acquired kafka resources.
   */
  override def cleanUpPublisher(): Try[Unit] = {
    Try(kafkaProducer.tearDown())
  }
  
  /**
   * Publish the output to the kafka topic.
   * 
   * @param iasio the not null IASIO to publish
   * @return a try to let the caller aware of errors publishing
   */
  override def publish(iasio: IASValue[?]): Try[Unit]  = {
    Try {
      kafkaProducer.push(iasio,null,iasio.id)
    }
  }
}

object KafkaPublisher {
  /** 
   *  Factory method
   *  
   * @param dasuId the identifier of the DASU
   * @param kafkaTopic the kafka topic to send the output to; if empty uses defaults from KafkaHelper
   * @param stringProducer The string producer to push IASIOs into
   * @param props an optional set of properties
   */
  def apply(
      dasuId: String, 
      kafkaTopic: Option[String], 
      stringProducer: SimpleStringProducer,
      props: Option[Properties]): KafkaPublisher = {
    
    // Get the topic from the parameter or from the default  
    val topic = kafkaTopic.getOrElse(KafkaHelper.IASIOs_TOPIC_NAME)
    
    val kafkaStringProducer = new KafkaIasiosProducer(stringProducer, topic, new IasValueJsonSerializer())

    new KafkaPublisher(dasuId,kafkaStringProducer,props.getOrElse(new Properties()))
  }

}
