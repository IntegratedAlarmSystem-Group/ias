package org.eso.ias.dasu

import org.eso.ias.logging.IASLogger
import org.eso.ias.types.Identifier
import org.eso.ias.types.IasValueStringSerializer
import org.eso.ias.types.IasValueJsonSerializer
import org.eso.ias.types.IASValue
import scala.collection.mutable.{Map=> MutableMap}
import org.eso.ias.kafkautils.KafkaUtilsException
import org.eso.ias.kafkautils.SimpleStringConsumer
import org.eso.ias.kafkautils.SimpleStringConsumer.KafkaConsumerListener
import org.eso.ias.kafkautils.SimpleStringConsumer.StartPosition
import org.eso.ias.converter.ConverterKafkaStream
import scala.util.Try
import java.util.Properties
import org.eso.ias.kafkautils.KafkaHelper

/**
 * Objects of this class gets IASIOs from the BSDB.
 * 
 * The receiver gets all the IASIOs published in the BSDB queues
 * and discards those not required by the DASU.
 * The filtering is based on the IDs of the IASValues: 
 * the JSON string is converted in a IASValue then its identifier
 * is checked against the possible inputs and finally accepted or discarded.
 * This is not the most efficient way as it requires to parse JSON strings
 * to decide if a value must be accepted or rejected.
 * 
 * IasiosReceiver objects store the IASValue received in the last time interval
 * in a map so that the last received values, override the oldest ones.
 * 
 * @param acceptedInputs the inputs accepted by the DASU 
 *        i.e. the inputs of all the ASCEs it contains
 * @param dasuIdentifier the identifier of the DASU to which 
 *        this receiver provides the inputs
 */
class IasiosReceiver(
    val acceptedInputs: Set[String],
    val dasuIdentifier: Identifier) extends KafkaConsumerListener {
  require(Option(acceptedInputs).isDefined && acceptedInputs.size>0,"Invalid set of accepted inputs")
  
  /** The logger */
  val logger = IASLogger.getLogger(this.getClass)
  
  /**
   * The serializer/deserializer to convert the string
   * received by the BSDB in a IASValue
   */
  val serializer: IasValueStringSerializer = new IasValueJsonSerializer()
  
  /** The inputs received in the last time frame  */
  val receivedInputs: MutableMap[String,IASValue[_]] = MutableMap()
  
  logger.debug("Receiver of DASU [{}] is initializing the consumer of events from the BSDB",dasuIdentifier.id)
  /** The  consumer receing events from the BSDB */
  val consumer: SimpleStringConsumer = new SimpleStringConsumer(
      KafkaHelper.DEFAULT_BOOTSTRAP_BROKERS,
      KafkaHelper.IASIOs_TOPIC_NAME,
      dasuIdentifier.id)
  val props: Properties = System.getProperties()
  props.put("group.id",dasuIdentifier.id)
  consumer.setUp(props)
  logger.info("Receiver of DASU [{}]: consumer of events from the BSDB initialized",dasuIdentifier.id)
  
  /**
   * Receives filter and store the IASIOs received from
   * the BSDB
   */
  override def stringEventReceived(event: String) {
    val iasValue: IASValue[_] = serializer.valueOf(event)
    
    if (acceptedInputs.contains(iasValue.id)) {
      receivedInputs.synchronized {
        receivedInputs.put(iasValue.id,iasValue)
      }
    }
  }
  
  /**
   * Return the inputs received by the BSDB in the last time interval.
   * 
   * It clears the map of inputs as it does not want to return IASIOs
   * received before the time interval.
   * 
   * @return the inputs received in the last time interval
   */
  def getInputs(): Set[IASValue[_]] = {
    receivedInputs.synchronized {
      val ret = receivedInputs.values.toSet
      receivedInputs.clear()
      ret
    }
  }
  
  /**
   * Start receiving events.
   * 
   * A method of the java consumer is called to start receiving events
   * from the BSDB. start() return a Try because the java method throws 
   * an exception in case of failure.
   * 
   * @return a Try indicating success or failure
   */
  def start(): Try[Unit] = {
    logger.info("Receiver of DASU [{}]: will ask the consumer to get events from BSDB",dasuIdentifier.id)
    Try[Unit](consumer.startGettingEvents(StartPosition.END,this))
  }
  
  /**
   * Stop receiving events
   */
  def shutdown() {
    logger.debug("Shutting down")
    consumer.tearDown()
    logger.info("Shutted down")
  }
  
}

/** Companion object */
object IasiosReceiver {
  val DEFAULT_BOOTSTRAP_SERVERS="localhost:9092"
}
