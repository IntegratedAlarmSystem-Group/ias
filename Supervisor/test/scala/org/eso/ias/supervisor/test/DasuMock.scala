package org.eso.ias.supervisor.test

import org.eso.ias.dasu.Dasu
import org.eso.ias.dasu.topology.Topology
import org.eso.ias.prototype.input.java.IASValue
import scala.util.Try
import org.eso.ias.prototype.input.Identifier
import org.eso.ias.dasu.publisher.OutputPublisher
import org.eso.ias.dasu.subscriber.InputSubscriber
import org.eso.ias.cdb.CdbReader
import org.eso.ias.cdb.pojos.DasuDao
import org.eso.ias.cdb.pojos.AsceDao
import org.eso.ias.prototype.input.java.IdentifierType
import java.util.concurrent.atomic.AtomicInteger
import scala.util.Success
import scala.collection.mutable.ArrayBuffer
import org.ias.prototype.logging.IASLogger
import scala.collection.JavaConverters
import org.eso.ias.prototype.input.java.AlarmSample
import org.eso.ias.prototype.input.java.OperationalMode
import org.eso.ias.prototype.input.java.IasValidity
import org.eso.ias.prototype.input.java.IASTypes


/** 
 *  A mockup of the DASUs to run in the Supervisor without the 
 *  complexity of DASUs and ASCEs
 *  
 * @param the identifier of the DASU
 * @param outputPublisher the publisher to send the output
 * @param inputSubscriber the subscriber getting events to be processed 
 * @param cdbReader the CDB reader to get the configuration of the DASU from the CDB
 */
class DasuMock(
    dasuIdentifier: Identifier,
    private val outputPublisher: OutputPublisher,
    private val inputSubscriber: InputSubscriber,
    cdbReader: CdbReader,
    outputIdentifier: Identifier)
extends Dasu(dasuIdentifier) {
  
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
  
  /** DASU configuration from CDB */
  lazy val dasuDao = {
    val dasuOptional = cdbReader.getDasu(dasuIdentifier.id)
    require(dasuOptional.isPresent(), "DASU [" + dasuIdentifier.id+ "] configuration not found on cdb")
    logger.debug("DASU [{}] configuration read from CDB", dasuIdentifier.id)
    dasuOptional.get
  }
  
  /**
   * The configuration of the ASCEs that run in the DASU
   */
  lazy val asceDaos = JavaConverters.asScalaSet(dasuDao.getAsces).toList
  
  /** The inputs of the DASU */
  val inputsOfTheDasu: Set[String] = getInputsFromCDB(cdbReader)
  logger.info("{} inputs required by Mock_DASU [{}]: {}", inputsOfTheDasu.size.toString(), dasuIdentifier.id,inputsOfTheDasu.mkString(", "))
  
  /** The output published when inputs are received */
  val output = IASValue.buildIasValue(
      AlarmSample.SET, 
      System.currentTimeMillis(), 
      OperationalMode.OPERATIONAL, 
      IasValidity.RELIABLE, 
      outputIdentifier.fullRunningID, 
      IASTypes.ALARM)
  
  
  // TODO release cdb resources
      
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
      
      // Publish the simulated output
      outputPublisher.publish(output)
  }

  /**
   * Get the inputs of the DASU from the CDB
   * 
   * It reads the inputs from the ASCEs running in the DASU
   * 
   * @param reader The CDB reader
   * @return the inputs of the DASU
   */
  private def getInputsFromCDB(reader: CdbReader): Set[String] = {
    asceDaos.foldLeft(Set.empty[String])( (s, aDao) => {
      val asceInputs = JavaConverters.collectionAsScalaIterable(aDao.getInputs).map(i => i.getId).toSet
      logger.info("Inputs of ASCE [{}] running in Mock_DASU [{}]: {}", aDao.getId, dasuIdentifier.id,asceInputs.mkString(", "))
      s ++ asceInputs
      })
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
   * Deactivate the automatic update of the output
   * in case no new inputs arrive.
   */
  def disableAutoRefreshOfOutput() = numOfDisableAutorefresh.incrementAndGet()
  
  /**
   * Activate the automatic update of the output
   * in case no new inputs arrive.
   * 
   * Most likely, the value of the output remains the same 
   * while the validity could change.
   */
  def enableAutoRefreshOfOutput() = numOfEnableAutorefresh.incrementAndGet()
  
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
      inputSubscriber: InputSubscriber,
      cdbReader: CdbReader): DasuMock = {
    
    require(Option(dasuDao).isDefined)
    require(Option(supervidentifier).isDefined)
    require(Option(outputPublisher).isDefined)
    require(Option(inputSubscriber).isDefined)
    require(Option(cdbReader).isDefined)
   
    val dasuId = dasuDao.getId
    
    val dasuIdentifier = new Identifier(dasuId,IdentifierType.DASU,supervidentifier)
    
    val asceId = new Identifier("ASCE_ID_RUNTIME_GENERATED",IdentifierType.ASCE,dasuIdentifier)
    
    val outputId = new Identifier(dasuDao.getOutput.getId,IdentifierType.IASIO,asceId)
    
    new DasuMock(dasuIdentifier,outputPublisher,inputSubscriber,cdbReader,outputId)
  }  
}
