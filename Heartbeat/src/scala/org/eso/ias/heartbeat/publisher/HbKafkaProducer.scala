package org.eso.ias.heartbeat.publisher

import org.eso.ias.heartbeat.HbProducer
import org.eso.ias.heartbeat.HbMsgSerializer
import org.eso.ias.kafkautils.SimpleStringProducer
import org.eso.ias.kafkautils.KafkaHelper
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Publish the HB in a kafka topic with a kafka 
 * 
 * @parm id the identifier will be used as client ID for kafka producer
 * @param kafkaServers the kafka borkers to connect to
 * @param serializer the serializer to transform HBs into strings 
 */
class HbKafkaProducer(
    id: String,
    kafkaServers: String,
    serializer: HbMsgSerializer) extends HbProducer(serializer) {
  require(Option(id).isDefined && !id.isEmpty(),"Invalid identifier")
  require(Option(kafkaServers).isDefined && !kafkaServers.isEmpty(),"Invalid list of kafka brokers")
  
  /** The kafka producer */
  val kafkaProducer = new SimpleStringProducer(kafkaServers,KafkaHelper.HEARTBEAT_TOPIC_NAME,id)
  
  val initialized = new AtomicBoolean(false)
  
  val closed = new AtomicBoolean(false)
  
  /** 
   *  Auxiliary constructor that takes the list of kafka brokers 
   *  from java properties.
   *  
   *  If the proeprties does not contain a list of kafkla brokers then the default is used
   *  
   * @parm id the identifier will be used as client ID for kafka producer
   * @param serializer the serializer to transform HBs into strings 
   */
  def this(id: String, serializer: HbMsgSerializer) {
    this(id,
        System.getProperties().getProperty(KafkaHelper.BROKERS_PROPNAME,KafkaHelper.DEFAULT_BOOTSTRAP_BROKERS),
        serializer)
  }
  
  /** Initialize the producer */
  override def init() = {
    if (!initialized.getAndSet(true)) {
      kafkaProducer.setUp()
    }
  }
  
  /** Shutdown the producer */
  override def shutdown() = {
    if (!closed.getAndSet(true)) {
      kafkaProducer.tearDown()
    }
  }
  
  /**
   * Push the string
   */
  override def push(hbAsString: String) {
    if (!closed.get) {
      kafkaProducer.push(hbAsString,null,id)
    }
  }
  
}