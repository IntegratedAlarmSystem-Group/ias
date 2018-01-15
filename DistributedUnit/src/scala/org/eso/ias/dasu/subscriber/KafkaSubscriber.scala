package org.eso.ias.dasu.subscriber

import org.ias.prototype.logging.IASLogger
import org.eso.ias.prototype.input.java.IasValueJsonSerializer
import org.eso.ias.kafkautils.SimpleStringConsumer.KafkaConsumerListener
import java.util.Properties
import org.eso.ias.kafkautils.KafkaHelper
import scala.util.Try
import org.eso.ias.kafkautils.SimpleStringConsumer.StartPosition
import scala.collection.mutable.{HashSet => MutableSet}
import org.eso.ias.kafkautils.KafkaIasiosConsumer
import org.eso.ias.kafkautils.KafkaIasiosConsumer.IasioListener
import org.eso.ias.prototype.input.java.IASValue

/** 
 *  Read IASValues from the kafka queue 
 *  and forward them to the DASU for processing.
 *  
 *  KafkaSubscriber delegates to KafkaIasiosConsumer.
 *  
 *  @param dasuId the identifier of the DASU
 *  @param serversList the list of kafka brojkers to connect to
 *  @param topicName the name of the kafka topic to poll events from
 *  @param props additional properties
 */
class KafkaSubscriber(
    val dasuId: String, 
    serversList: String,
    topicName: String,
    val props: Properties) 
extends IasioListener with InputSubscriber {
  require(Option(dasuId).isDefined && !dasuId.isEmpty())
  require(Option(serversList).isDefined && !dasuId.isEmpty())
  require(Option(topicName).isDefined && !dasuId.isEmpty())
  require(Option(props).isDefined)
  
  props.setProperty("group.id", dasuId+"-GroupID")
  
  /** The logger */
  private val logger = IASLogger.getLogger(this.getClass)
  
  /** To serialize IASValues to JSON strings */
  private val jsonSerializer = new IasValueJsonSerializer()
  
  /** The Kafka consumer */
  private val kafkaConsumer = new KafkaIasiosConsumer(serversList, topicName, dasuId)
  
  /** The listener of events */
  private var listener: Option[InputsListener] = None
  
  /** The set of inputs accepted by the DASU */
  private val acceptedInputs = MutableSet[String]()
  
  /**
   * Auxiliary constructor with default topic and servers list
   * @param serversList the list of kafka brojkers to connect to
   * @param dasuId the identifier of the DASU
   * @param props additional properties
   */
  def this(dasuId: String, props: Properties) = {
    this(dasuId,KafkaHelper.DEFAULT_BOOTSTRAP_BROKERS,KafkaHelper.IASIOs_TOPIC_NAME,props)
  }
  
  /**
   * Auxiliary constructor with default servers list
   * 
   * @param dasuId the identifier of the DASU
   * @param topicName the name of the kafka topic to poll events from
   * @param props additional properties
   */
  def this(dasuId: String, topicName: String,props: Properties) = {
    this(dasuId,KafkaHelper.DEFAULT_BOOTSTRAP_BROKERS,topicName,props)
  }
  
  /**
	 * Process an event (a String) received from the kafka topic
	 * 
	 * @param event The string received in the topic
	 * @see KafkaConsumerListener
	 */
	override def iasioReceived(iasValue: IASValue[_]) = {
	  try {
	    if (acceptedInputs.contains(iasValue.id)) listener.foreach( l => l.inputsReceived(Set(iasValue)))
	  } catch {
	    case e: Exception => logger.error("Subscriber of DASU [{}] got an error processing event [{}]", dasuId,iasValue.toString(),e)
	  }
	}
  
  /** Initialize the subscriber */
  def initializeSubscriber(): Try[Unit] = {
    logger.info("Initializing subscriber of DASU [{}]",dasuId)
    Try{ 
      kafkaConsumer.setUp(props)
      logger.info("Subscriber of DASU [{}] intialized", dasuId)
    }
  }
  
  /** CleanUp and release the resources */
  def cleanUpSubscriber(): Try[Unit] = {
    logger.info("Cleaning up subscriber of DASU [{}]",dasuId)
    Try{
      kafkaConsumer.tearDown()
      logger.info("Subscriber of DASU [{}] cleaned up", dasuId)
    }
    
  }
  
  /**
   * Start to get events and forward them to the listener.
   * 
   * IASIOs whose ID is not in the acceptedInputs set are discarded.
   * 
   * @param listener the listener of events
   * @param acceptedInputs the IDs of the inputs accepted by the listener
   */
  def startSubscriber(listener: InputsListener, acceptedInputs: Set[String]): Try[Unit] = {
    require(Option(listener).isDefined)
    require(Option(acceptedInputs).isDefined)
    require(!acceptedInputs.isEmpty,"The list of IDs of accepted inputs can' be empty")
    this.acceptedInputs++=acceptedInputs
    this.listener = Option(listener)
    logger.info("The subscriber of DASU [{}] will start getting events",dasuId)
    Try {
      kafkaConsumer.startGettingEvents(StartPosition.END,this)
      logger.info("The subscriber of DASU [{}] is polling events from kafka",dasuId)
    }
  }
}