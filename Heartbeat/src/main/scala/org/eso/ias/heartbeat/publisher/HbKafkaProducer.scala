package org.eso.ias.heartbeat.publisher

import java.util.concurrent.atomic.AtomicBoolean

import org.eso.ias.heartbeat.{HbMsgSerializer, HbProducer}
import org.eso.ias.kafkautils.{KafkaHelper, SimpleStringProducer}

/**
 * Publish the HB in a kafka topic with a kafka 
 *
 * @param kafkaProducer The kafka string producer to publish heartbeats
 * @parm id the identifier will be used as client ID for kafka producer
 * @param serializer the serializer to transform HBs into strings
 */
class HbKafkaProducer(
    val kafkaProducer : SimpleStringProducer,
    id: String,
    serializer: HbMsgSerializer) extends HbProducer(serializer) {
  require(Option(id).isDefined && !id.isEmpty(),"Invalid identifier")
  require(Option(kafkaProducer).isDefined,"Invalid string producer")
  
  val initialized = new AtomicBoolean(false)
  
  val closed = new AtomicBoolean(false)

  /** Initialize: nothing to do in this implementation */
  override def init() { initialized.set(true) }

  /** Shutdown: nothing to do in this implementation */
  override def shutdown() { closed.set(true) }

  /**
   * Push the string
   */
  override def push(hbAsString: String) {
    if (!closed.get) {
      kafkaProducer.push(hbAsString,KafkaHelper.HEARTBEAT_TOPIC_NAME,null,id)
    }
  }
  
}