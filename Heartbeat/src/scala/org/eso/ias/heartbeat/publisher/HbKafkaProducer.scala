package org.eso.ias.heartbeat.publisher

import org.eso.ias.heartbeat.HbProducer
import org.eso.ias.heartbeat.HbMsgSerializer
import org.eso.ias.kafkautils.SimpleStringProducer
import org.eso.ias.kafkautils.KafkaHelper

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
  
  /** Initialize the producer */
  def init() = kafkaProducer.setUp()
  
  /** Shutdown the producer */
  def shutdown() = kafkaProducer.tearDown()
  
  /**
   * Push the string
   */
  def push(hbAsString: String) {
    kafkaProducer.push(hbAsString,null,id)
  }
  
}