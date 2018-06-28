package org.eso.ias.dasu.subscriber

import org.eso.ias.logging.IASLogger
import org.eso.ias.types.IasValueJsonSerializer
import org.eso.ias.kafkautils.SimpleStringConsumer.KafkaConsumerListener
import java.util.Properties
import org.eso.ias.kafkautils.KafkaHelper
import scala.util.Try
import org.eso.ias.kafkautils.SimpleStringConsumer.StartPosition
import scala.collection.mutable.{HashSet => MutableSet}
import org.eso.ias.kafkautils.KafkaIasiosConsumer
import org.eso.ias.kafkautils.KafkaIasiosConsumer.IasioListener
import org.eso.ias.types.IASValue
import org.eso.ias.kafkautils.KafkaIasiosProducer

/** 
 *  Read IASValues from the kafka queue 
 *  and forward them to the listener for processing.
 *  
 *  KafkaSubscriber delegates to KafkaIasiosConsumer.
 *  
 *  @param dasuId the identifier of the owner
 *  @param serversList the list of kafka brojkers to connect to
 *  @param topicName the name of the kafka topic to poll events from
 *  @param props additional properties
 */
class KafkaSubscriber(
    val dasuId: String, 
    private val kafkaConsumer: KafkaIasiosConsumer,
    val props: Properties) 
extends IasioListener with InputSubscriber {
  require(Option(dasuId).isDefined && !dasuId.isEmpty())
  require(Option(kafkaConsumer).isDefined && !dasuId.isEmpty())
  require(Option(props).isDefined)
  
  props.setProperty("group.id", dasuId+"-GroupID")
  
  /** The logger */
  private val logger = IASLogger.getLogger(this.getClass)
  
  /** To serialize IASValues to JSON strings */
  private val jsonSerializer = new IasValueJsonSerializer()
  
  /** The listener of events */
  private var listener: Option[InputsListener] = None
  
  /** 
   *  The set of inputs accepted by the listener
   *  If empty accepts all the inputs
   */
  private val acceptedInputs = MutableSet[String]()

  
  /**
	 * Process an event (a String) received from the kafka topic
	 * 
	 * @param event The string received in the topic
	 * @see KafkaConsumerListener
	 */
	override def iasioReceived(iasValue: IASValue[_]) = {
	  try {
	    if (acceptedInputs.isEmpty || acceptedInputs.contains(iasValue.id)) {
	      listener.foreach( l => l.inputsReceived(Set(iasValue)))
	    }
	  } catch {
	    case e: Exception => logger.error("Subscriber of [{}] got an error processing event [{}]", dasuId,iasValue.toString(),e)
	  }
	}
  
  /** Initialize the subscriber */
  def initializeSubscriber(): Try[Unit] = {
    logger.info("Initializing subscriber of [{}]",dasuId)
    Try{ 
      kafkaConsumer.setUp(props)
      logger.info("Subscriber of [{}] intialized", dasuId)
    }
  }
  
  /** CleanUp and release the resources */
  def cleanUpSubscriber(): Try[Unit] = {
    logger.info("Cleaning up subscriber of [{}]",dasuId)
    Try{
      kafkaConsumer.tearDown()
      logger.info("Subscriber of [{}] cleaned up", dasuId)
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
    require(Option(listener).isDefined)
    require(Option(acceptedInputs).isDefined)
    if (acceptedInputs.nonEmpty) {
      logger.info("Starting subscriber with accepted IDs {}",acceptedInputs.mkString(", "))
    } else {
      logger.info("Starting subscriber accepting  all IDs")
    }
    this.acceptedInputs++=acceptedInputs
    this.listener = Option(listener)
    Try {
      kafkaConsumer.startGettingEvents(StartPosition.END,this)
      logger.info("The subscriber of [{}] is polling events from kafka",dasuId)
    }
  }
}

object KafkaSubscriber {
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
    val topic = kafkaTopic.getOrElse(KafkaHelper.IASIOs_TOPIC_NAME);
    
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