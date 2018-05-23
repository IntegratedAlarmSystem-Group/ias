package org.eso.ias.supervisor.test

import org.eso.ias.dasu.Dasu
import org.eso.ias.dasu.topology.Topology
import org.eso.ias.types.IASValue
import scala.util.Try
import org.eso.ias.types.Identifier
import org.eso.ias.dasu.publisher.OutputPublisher
import org.eso.ias.dasu.subscriber.InputSubscriber
import org.eso.ias.cdb.CdbReader
import org.eso.ias.cdb.pojos.DasuDao
import org.eso.ias.cdb.pojos.AsceDao
import org.eso.ias.types.IdentifierType
import java.util.concurrent.atomic.AtomicInteger
import scala.util.Success
import scala.collection.mutable.ArrayBuffer
import org.eso.ias.logging.IASLogger
import scala.collection.JavaConverters
import org.eso.ias.types.Alarm
import org.eso.ias.types.OperationalMode
import org.eso.ias.types.IasValidity
import org.eso.ias.types.IASTypes
import java.util.HashSet
import org.eso.ias.cdb.pojos.IasioDao


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
extends Dasu(dasuIdentifier,5,1) {
  
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
  val asceDaos = JavaConverters.asScalaSet(dasuDao.getAsces).toList
  
  /** The inputs of the DASU */
  val inputsOfTheDasu: Set[String] = {
    val inputs = asceDaos.foldLeft(Set.empty[IasioDao])( (set, asce) => set++JavaConverters.collectionAsScalaIterable(asce.getInputs))
    inputs.map(_.getId)
  }
  logger.info("{} inputs required by Mock_DASU [{}]: {}", inputsOfTheDasu.size.toString(), dasuIdentifier.id,inputsOfTheDasu.mkString(", "))
  
  /** The output published when inputs are received */
  val output = {
    val asceId = new Identifier("ASCE_ID_RUNTIME_GENERATED",IdentifierType.ASCE,dasuIdentifier)
    val outputId = new Identifier(dasuDao.getOutput.getId,IdentifierType.IASIO,asceId)
    IASValue.build(
      Alarm.SET_MEDIUM,
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
  override def inputsReceived(iasios: Set[IASValue[_]]) {
    iasios.foreach(iasio => {
      val id = iasio.id
      inputsReceivedFromSuperv.append(id)
      if (!getInputIds().contains(id)) unexpectedInputsReceived.append(id)
      })
      
      val depIds = iasios.filter(value => getInputIds().contains(value.id)).map(_.fullRunningId)
      
      // Publish the simulated output
      outputPublisher.publish(output.updateFullIdsOfDependents(JavaConverters.setAsJavaSet(depIds)))
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
    new Success(())
  }
  
  /**
   * Enable/Disable the automatic update of the output
   * in case no new inputs arrive.
   * 
   * Most likely, the value of the output remains the same 
   * while the validity could change.
   */
  override def enableAutoRefreshOfOutput(enable: Boolean) = {
    if (enable) {
      numOfEnableAutorefresh.incrementAndGet()
    } else {
      numOfDisableAutorefresh.incrementAndGet()
    }
  }
  
  /**
   * Release all the resources before exiting
   */
  def cleanUp() = numOfCleanUps.incrementAndGet();
}

/**
 * Companion object
 * 
 * The companion provides the factory method used by the Supervisor
 * to build a DASU.
 *  
 */
object DasuMock {
  
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
