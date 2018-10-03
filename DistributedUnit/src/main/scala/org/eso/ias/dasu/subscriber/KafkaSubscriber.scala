package org.eso.ias.dasu.subscriber

import org.eso.ias.logging.IASLogger
import java.util.Properties

import org.eso.ias.kafkautils.KafkaHelper

import scala.util.{Failure, Try}
import org.eso.ias.kafkautils.SimpleStringConsumer.StartPosition
import org.eso.ias.kafkautils.KafkaIasiosConsumer
import org.eso.ias.kafkautils.KafkaIasiosConsumer.IasioListener
import org.eso.ias.types.IASValue

import scala.collection.JavaConverters

/** 
 *  Read IASValues from the kafka queue 
 *  and forward them to the listener for processing.
 *  
 *  KafkaSubscriber delegates to KafkaIasiosConsumer.
  *
  *  Filtering by ID, passed in startSuscriber, is supported by delegating
  *  to the KafkaIasiosConsumer.
 *  
 *  @param dasuId the identifier of the owner
 *  @param kafkaConsumer the Kafka consumer
 *  @param props additional properties
 */
class KafkaSubscriber(
    val dasuId: String, 
    private val kafkaConsumer: KafkaIasiosConsumer,
    val props: Properties) 
extends IasioListener with InputSubscriber {
  require(Option(dasuId).isDefined && !dasuId.isEmpty)
  require(Option(kafkaConsumer).isDefined && !dasuId.isEmpty)
  require(Option(props).isDefined)

  props.setProperty("client.id",dasuId)
  props.setProperty("group.id", dasuId+"-GroupID")
  
  /** The listener of events */
  private var listener: Option[InputsListener] = None

  /**
	  * Forward the IASValue received from the kafka topic
    * to the listener
	  *
	  * @param iasValue The value received in the topic
	  * @see KafkaConsumerListener
	 */
	override def iasioReceived(iasValue: IASValue[_]): Unit = {
      Try(listener.foreach( l => l.inputsReceived(Set(iasValue)))) match {
        case Failure(e) =>
          KafkaSubscriber.logger.error("Subscriber of [{}] got an exception processing event [{}]: value lost!",
            dasuId,
            iasValue.toString,
            e)
        case _ =>
      }
	}
  
  /** Initialize the subscriber */
  def initializeSubscriber(): Try[Unit] = {
    KafkaSubscriber.logger.debug("Initializing subscriber of [{}]",dasuId)
    Try{ 
      kafkaConsumer.setUp(props)
      KafkaSubscriber.logger.info("Subscriber of [{}] intialized", dasuId)
    }
  }
  
  /** CleanUp and release the resources */
  def cleanUpSubscriber(): Try[Unit] = {
    KafkaSubscriber.logger.debug("Cleaning up subscriber of [{}]",dasuId)
    Try{
      kafkaConsumer.tearDown()
      KafkaSubscriber.logger.info("Subscriber of [{}] cleaned up", dasuId)
    }
  }
  
  /**
   * Start to get events and forward them to the listener.
   * 
   * IASIOs whose ID is not in the acceptedInputs set are discarded.
   * 
   * @param listener the listener of events
   * @param acceptedInputs the IDs of the inputs accepted by the listener
   *                       (if empty accepts all the IasValues)
   */
  def startSubscriber(listener: InputsListener, acceptedInputs: Set[String]): Try[Unit] = {
    val newListener = Option(listener)
    require(newListener.isDefined)
    require(Option(acceptedInputs).isDefined)


    if (acceptedInputs.nonEmpty) {
      kafkaConsumer.addIdsToFilter(JavaConverters.setAsJavaSet(acceptedInputs))
      KafkaSubscriber.logger.info("New accepted IDs added by [{}]: {}",dasuId,acceptedInputs.mkString)

      val acceptedIDs =JavaConverters.asScalaSet(kafkaConsumer.getAcceptedIds)
      KafkaSubscriber.logger.info("Filter of IDs set in the subscriber of [{}]: {}",dasuId, acceptedIDs.mkString)
    } else {
      KafkaSubscriber.logger.info("New accepted IDs set by [{}]: ",dasuId)
    }

    this.listener = newListener

    Try {
      kafkaConsumer.startGettingEvents(StartPosition.END,this)
      KafkaSubscriber.logger.info("The subscriber of [{}] is polling events from kafka",dasuId)
    }
  }
}

/**  KafkaSubscriber companion object */
object KafkaSubscriber {
  /** The logger */
  private[KafkaSubscriber] val logger = IASLogger.getLogger(this.getClass)

  /** 
   *  Factory method
   *  
   * @param dasuId the identifier 
   * @param kafkaTopic the kafka topic to send the output to;
   *                   if empty uses defaults from KafkaHelper
   * @param kafkaServers kafka servers; 
   *                     overridden by KafkaHelper.BROKERS_PROPNAME java property, if present
   * @param props additional set of properties
   */
  def apply(
      dasuId: String, 
      kafkaTopic: Option[String], 
      kafkaServers: Option[String], 
      props: Properties): KafkaSubscriber = {
    
    // Get the topic from the parameter or from the default  
    val topic = kafkaTopic.getOrElse(KafkaHelper.IASIOs_TOPIC_NAME)
    
    val serversFromProps = Option( props.getProperty(KafkaHelper.BROKERS_PROPNAME))
    val kafkaBrokers = (serversFromProps, kafkaServers) match {
      case (Some(servers), _) => servers
      case ( None, Some(servers)) => servers
      case (_, _) => KafkaHelper.DEFAULT_BOOTSTRAP_BROKERS
    }
    
    
    val kafkaConsumer = new KafkaIasiosConsumer(kafkaBrokers,topic,dasuId+"Consumer")
    new KafkaSubscriber(dasuId,kafkaConsumer,props)
  }

}

