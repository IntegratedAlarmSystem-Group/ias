package org.eso.ias.dasu.publisher

import org.eso.ias.prototype.input.java.IASValue
import scala.util.Try
import org.eso.ias.kafkautils.KafkaIasiosProducer
import java.util.Properties
import org.ias.prototype.logging.IASLogger
import org.eso.ias.prototype.input.java.IasValueJsonSerializer
import org.eso.ias.kafkautils.KafkaHelper

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
  require(Option(dasuId).isDefined && !dasuId.isEmpty())
  require(Option(kafkaProducer).isDefined)
  require(Option(props).isDefined)
  
  
  
  /** The logger */
  private val logger = IASLogger.getLogger(this.getClass)
  
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
   * @param aisio the not null IASIO to publish
   * @return a try to let the caller aware of errors publishing
   */
  override def publish(iasio: IASValue[_]): Try[Unit]  = {
    Try {
      kafkaProducer.push(iasio,null,iasio.id)
    }
  }
}

object KafkaPublisher {
  /** 
   *  Factory method with more fine control over the options
   *  
   * @param dasuId the identifier of the DASU
   * @param kafkaTopic the kafka topic to send the output to
   * @param kafkaServers kafka servers
   * @param props additional set of properties
   */
  def apply(dasuId: String, kafkaTopic: String, kafkaServers: String, props: Properties): KafkaPublisher = {
    val kafkaStringProducer = new KafkaIasiosProducer(kafkaServers,kafkaTopic,dasuId+"Producer",new IasValueJsonSerializer())
    new KafkaPublisher(dasuId,kafkaStringProducer,props)
  }
  
  /**
   * Factory method with default topic name and servers list
   *  
   * @param dasuId the identifier of the DASU
   * @param props additional set of properties
   */
  def apply(dasuId: String, props: Properties): KafkaPublisher = {
    apply(dasuId,KafkaHelper.IASIOs_TOPIC_NAME,KafkaHelper.DEFAULT_BOOTSTRAP_BROKERS,props)
  }
}