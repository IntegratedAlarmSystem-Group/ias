package org.eso.ias.supervisor.test

import org.eso.ias.cdb.pojos.{AsceDao, DasuDao, IasioDao}
import org.eso.ias.dasu.Dasu
import org.eso.ias.dasu.publisher.OutputPublisher
import org.eso.ias.dasu.subscriber.InputSubscriber
import org.eso.ias.logging.IASLogger
import org.eso.ias.types.*

import java.util.Objects
import java.util.concurrent.atomic.AtomicInteger
import scala.collection.mutable.ArrayBuffer
import scala.jdk.javaapi.CollectionConverters
import scala.util.{Success, Try}

/** 
 *  A mockup of the DASUs to run in the Supervisor without the 
 *  complexity of DASUs and ASCEs
 *  
 * @param dasuDao the DASU configuration
 * @param outputPublisher the publisher to send the output
 * @param inputSubscriber the subscriber getting events to be processed 
 */
class DasuMock(
  dasuIdentifier: Identifier,
  dasuDao: DasuDao,
  private val outputPublisher: OutputPublisher,
  private val inputSubscriber: InputSubscriber)
extends Dasu(dasuIdentifier,5,6) {
  
  /** The logger */
  private val logger = IASLogger.getLogger(this.getClass)
  
  logger.info("Building Mock-DASU [{}] with fullRuningId [{}]", dasuIdentifier.id,dasuIdentifier.fullRunningID)
  
  /** How many times cleanUp has been called */
  val numOfCleanUps = new AtomicInteger(0)
  
  /** How many times the Supervisor enabled the auto refresh of the output */
  val numOfEnableAutorefresh = new AtomicInteger(0)
  
  /** How many times the Supervisor disabled the auto refresh of the output */
  val numOfDisableAutorefresh = new AtomicInteger(0)
  
  /** How many times the Supervisor started the DASU */
  val numOfStarts = new AtomicInteger(0)
  
  /**
   * All the inputs received so far
   */
  val inputsReceivedFromSuperv: ArrayBuffer[String] = ArrayBuffer.empty[String]
  
  /**
   * unexpected inputs received so far: if everything works well
   * this should always be empty otherwise the
   * Supervisor sent a IASIO to a DASU that does not need it
   */
  val unexpectedInputsReceived: ArrayBuffer[String] = ArrayBuffer.empty[String]
  
  /**
   * The configuration of the ASCEs that run in the DASU
   */
  val asceDaos: Seq[AsceDao] = CollectionConverters.asScala(dasuDao.getAsces).toList
  
  /** The inputs of the DASU */
  val inputsOfTheDasu: Set[String] = {
    val inputs = asceDaos.foldLeft(Set.empty[IasioDao])( (set, asce) => set++CollectionConverters.asScala(asce.getInputs))
    inputs.map(_.getId)
  }
  logger.info("{} inputs required by Mock_DASU [{}]: {}",
    inputsOfTheDasu.size.toString,
    dasuIdentifier.id,
    inputsOfTheDasu.mkString(", "))
  
  /** The output published when inputs are received */
  val output: IASValue[_] = {
    val asceId = new Identifier("ASCE_ID_RUNTIME_GENERATED",IdentifierType.ASCE,dasuIdentifier)
    val outputId = new Identifier(dasuDao.getOutput.getId,IdentifierType.IASIO,asceId)
    IASValue.build(
      Alarm.getInitialAlarmState.set(),
      OperationalMode.OPERATIONAL,
      IasValidity.RELIABLE,
      outputId.fullRunningID,
      IASTypes.ALARM,
      null,
      null,
      null,
      null,
      null,
      null,
      System.currentTimeMillis(),
      null,
      null)

  }

  logger.info("Mock-DASU [{}] built", dasuIdentifier.id)
  
  /**
   * Updates the output with the inputs received
   * and simulate the sending of the output
   * 
   * @param iasios the inputs received
   * @see InputsListener
   */
  override def inputsReceived(iasios: Iterable[IASValue[_]]): Unit = {
    iasios.foreach(iasio => {
      val id = iasio.id
      inputsReceivedFromSuperv.append(id)
      if (!getInputIds().contains(id)) unexpectedInputsReceived.append(id)
    })
      
    val depIds = iasios.filter(value => getInputIds().contains(value.id)).map(_.fullRunningId)
      
    // Publish the simulated output
    outputPublisher.publish(output.updateFullIdsOfDependents(CollectionConverters.asJavaCollection(depIds)))
  }

  /**
   * ACK the alarm if the ASCE that produces it runs in this DASU.
   *
   * The DASU delegates the acknowledgment to the ASCE that produces the alarm
   *
   * @param alarmIdentifier the identifier of the alarm to ACK
   * @return true if the alarm has been ACKed, false otherwise
   * @see See [[org.eso.ias.asce.ComputingElement.ack()]]
   */
  override def ack(alarmIdentifier: Identifier): Boolean = {
    Objects.requireNonNull(alarmIdentifier)
    DasuMock.logger.info("DusuMock [{}]: ACKing {} ", id, alarmIdentifier.fullRunningID)
    true
  }
  
  /** The inputs of the DASU */
  def getInputIds(): Set[String] = inputsOfTheDasu
  
  /** The IDs of the ASCEs running in the DASU  */
  def getAsceIds(): Set[String] = asceDaos.map(_.getId).toSet
  
  /** 
   *  Start getting events from the inputs subscriber
   *  to produce the output
   */
  def start(): Try[Unit] = {
    numOfStarts.incrementAndGet()
    Success(())
  }
  
  /**
   * Enable/Disable the automatic update of the output
   * in case no new inputs arrive.
   * 
   * Most likely, the value of the output remains the same 
   * while the validity could change.
   */
  override def enableAutoRefreshOfOutput(enable: Boolean): Unit = {
    if (enable) {
      numOfEnableAutorefresh.incrementAndGet()
    } else {
      numOfDisableAutorefresh.incrementAndGet()
    }
  }
  
  /**
   * Release all the resources before exiting
   */
  def cleanUp(): Unit = numOfCleanUps.incrementAndGet()
}

/**
 * Companion object
 * 
 * The companion provides the factory method used by the Supervisor
 * to build a DASU.
 *  
 */
object DasuMock {
  /** The logger */
  private val logger = IASLogger.getLogger(this.getClass)
  
  /** 
   *  Factory method used by the Supervisor to build the DASU
   */
  def apply(
      dasuDao: DasuDao, 
      supervidentifier: Identifier, 
      outputPublisher: OutputPublisher,
      inputSubscriber: InputSubscriber): DasuMock = {
    
    require(Option(dasuDao).isDefined)
    require(Option(supervidentifier).isDefined)
    require(Option(outputPublisher).isDefined)
    require(Option(inputSubscriber).isDefined)
   
    val dasuId = dasuDao.getId
    
    val dasuIdentifier = new Identifier(dasuId,IdentifierType.DASU,supervidentifier)
    
    new DasuMock(dasuIdentifier,dasuDao,outputPublisher,inputSubscriber)
  }  
}
