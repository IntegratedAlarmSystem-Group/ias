package org.eso.ias.dasu.subscriber

import java.util
import java.util.{Collection, Properties}

import org.eso.ias.kafkautils.KafkaStringsConsumer.StreamPosition
import org.eso.ias.kafkautils.SimpleKafkaIasiosConsumer.IasioListener
import org.eso.ias.kafkautils.{KafkaHelper, KafkaIasiosConsumer}
import org.eso.ias.logging.IASLogger
import org.eso.ias.types.{IASTypes, IASValue}

import scala.collection.JavaConverters
import scala.util.{Failure, Try}

/** 
  *  Read IASValues from the kafka queue
  *  and forward them to the listener for processing.
  *
  *  KafkaSubscriber delegates to KafkaIasiosConsumer and it is mostly
  *  a covenience class to use the java KafkaIasiosConsumer class from scala
  *
  *  Filtering by ID, passed in startSuscriber, is supported by delegating
  *  to the KafkaIasiosConsumer.
  *
  *  @param dasuId the identifier of the owner
  *  @param kafkaConsumer the Kafka consumer
  *  @param props additional properties
  *
  * @author acaproni
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
	  * @param iasValues The values read from the BSDB
	  * @see IasiosListener
	 */
	override def iasiosReceived(iasValues: Collection[IASValue[_]]): Unit = {
    assert(Option(iasValues).isDefined)
    val receivedIasios = JavaConverters.collectionAsScalaIterable(iasValues)
     KafkaSubscriber.logger.debug(("Subscriber of [{}] receeved {} events "),dasuId,receivedIasios.size)
    Try(listener.foreach( l => l.inputsReceived(receivedIasios))) match {
      case Failure(e) =>
        KafkaSubscriber.logger.error("Subscriber of [{}] got an exception processing events: up to {} values potentially lost!",
          dasuId,
          receivedIasios.size,
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
      kafkaConsumer.startGettingEvents(StreamPosition.END, this)
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
    
    
    val kafkaConsumer = new KafkaIasiosConsumer(kafkaBrokers,topic,dasuId+"Consumer", new util.HashSet[String](), new util.HashSet[IASTypes]())
    new KafkaSubscriber(dasuId,kafkaConsumer,props)
  }

}

