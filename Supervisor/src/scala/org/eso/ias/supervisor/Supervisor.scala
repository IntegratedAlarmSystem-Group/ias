package org.eso.ias.supervisor

import org.eso.ias.cdb.CdbReader
import org.ias.prototype.logging.IASLogger
import org.eso.ias.cdb.json.JsonReader
import org.eso.ias.cdb.json.CdbFiles
import org.eso.ias.cdb.json.CdbJsonFiles
import org.eso.ias.cdb.pojos.DasuDao
import scala.collection.JavaConverters
import org.eso.ias.dasu.subscriber.InputSubscriber
import org.eso.ias.dasu.publisher.OutputPublisher
import scala.util.Success
import scala.util.Try
import org.eso.ias.prototype.input.java.IASValue
import org.eso.ias.dasu.subscriber.InputsListener
import org.eso.ias.dasu.Dasu
import org.eso.ias.cdb.pojos.SupervisorDao
import org.eso.ias.prototype.input.java.IasValueJsonSerializer
import scala.util.Failure
import java.util.concurrent.atomic.AtomicBoolean
import org.eso.ias.prototype.input.Identifier
import org.eso.ias.dasu.DasuImpl
import org.eso.ias.dasu.publisher.KafkaPublisher
import org.eso.ias.dasu.subscriber.KafkaSubscriber
import org.eso.ias.prototype.input.java.IdentifierType
import java.util.concurrent.CountDownLatch

/**
 * A Supervisor is the container to run several DASUs into the same JVM.
 * 
 * The Supervisor blindly forward inputs to each DASU and sent the oupts to the BSDB 
 * without adding any other heuristic: things like for updating validities when an input
 * is not refreshed are not part of the Supervisor.
 * 
 * The Supervisor gets IASIOs from a InputSubscriber and publishes
 * IASValues to the BSDB by means of a OutputPublisher.
 * The Supervisor itself is the publisher and subscriber for the DASUs i.e.
 * the Supervisor acts as a bridge:
 *  * IASIOs read from the BSDB are forwarded to the DASUs that need them as input:
 *    the Supervisor has its own subscriber to receive values from the BSDB that 
 *    are then forwarded to each DASU for processing
 *  * values produced by the DASUs are forwarded to the BSDB: the DASUs publishes the output 
 *    they produce to the supervisor that, in turn, forward each of them to its own publisher.
 * 
 * The same interfaces, InputSubscriber and OutputPublisher, 
 * are used by DASUs and Supervisors in this way a DASU can be easily tested
 * directly connected to Kafka (for example) without the need to have
 * it running into a Supervisor.
 * 
 * DASUs are built by invoking the dasufactory passed in the constructor: 
 * test can let the Supervisor run with their mockup implementation of a DASU.
 * 
 * @param supervisorIdentifier the identifier of the Supervisor
 * @param outputPublisher the publisher to send the output
 * @param inputSubscriber the subscriber getting events to be processed 
 * @param cdbReader the CDB reader to get the configuration of the DASU from the CDB
 * @param dasuFactory: factory to build DASU 
 */

class Supervisor(
    val supervisorIdentifier: Identifier,
    private val outputPublisher: OutputPublisher,
    private val inputSubscriber: InputSubscriber,
    cdbReader: CdbReader,
    dasuFactory: (DasuDao, Identifier, OutputPublisher, InputSubscriber, CdbReader) => Dasu) 
    extends InputsListener with InputSubscriber with  OutputPublisher {
  require(Option(supervisorIdentifier).isDefined,"Invalid Supervisor identifier")
  require(Option(outputPublisher).isDefined,"Invalid output publisher")
  require(Option(inputSubscriber).isDefined,"Invalid input subscriber")
  require(Option(cdbReader).isDefined,"Invalid CDB reader")
  
  /** The ID of the Supervisor */
  val id = supervisorIdentifier.id
  
  /** The logger */
  val logger = IASLogger.getLogger(Supervisor.getClass)
  
  logger.info("Building Supervisor [{}] with fullRunningId [{}]",id,supervisorIdentifier.fullRunningID)
  
  // Get the configuration of the supervisor from the CDB
  val supervDao : SupervisorDao = {
    val supervDaoOpt = cdbReader.getSupervisor(id)
    require(supervDaoOpt.isPresent(),"Supervisor ["+id+"] configuration not found on cdb")
    supervDaoOpt.get
  }
  logger.info("Supervisor [{}] configuration retrived from CDB",id)
  
  /**
   * Gets the definitions of the DASUs to run in the Supervisor from the CDB
   */
  val dasuDaos: Set[DasuDao] = JavaConverters.asScalaSet(cdbReader.getDasusForSupervisor(id)).toSet
  require(dasuDaos.size>0,"No DASUs to run in Supervisor "+id)
  logger.info("Supervisor [{}], {} DASUs to run: {}",id,dasuDaos.size.toString(),dasuDaos.map(d => d.getId()).mkString(", "))
  
  // Initialize the consumer and exit in case of error 
  val inputSubscriberInitialized = inputSubscriber.initializeSubscriber()
  inputSubscriberInitialized match {
    case Failure(f) => logger.error("Supervisor [{}] failed to initialize the consumer", id,f);
                       System.exit(-1)
    case Success(s) => logger.info("Supervisor [{}] subscriber successfully initialized",id)
  }
  
  // Initialize the producer and exit in case of error 
  val outputProducerInitialized = outputPublisher.initializePublisher()
  outputProducerInitialized match {
    case Failure(f) => logger.error("Supervisor [{}] failed to initialize the producer", id,f);
                       System.exit(-2)
    case Success(s) => logger.info("Supervisor [{}] producer successfully initialized",id)
  }
  
  // Build all the DASUs
  val dasus: Map[String, Dasu] = dasuDaos.foldLeft(Map.empty[String,Dasu])((m, dasuDao) => m + (dasuDao.getId -> dasuFactory(dasuDao,supervisorIdentifier,this,this,cdbReader)))
  
  /**
   * The IDs of the DASUs instantiated in the Supervisor
   */
  val dasuIds = dasuDaos.map(_.getId)
  logger.info("Supervisor [{}] built {} DASUs: {}",id, dasus.size.toString(),dasuIds.mkString(", "))
  
  // TODO: close the cdbReader and free the resources (@see Issue #25)
  
  /**
   * Associate each DASU with the Set of inputs it needs.
   * 
   * the key is the ID of the DASU, the value is 
   * the set of inputs to send to the DASU
   */
  val iasiosToDasusMap: Map[String, Set[String]] = startDasus()
  logger.info("Supervisor [{}] associated IASIOs IDs to DASUs", id)
  
  val cleanedUp = new AtomicBoolean(false) // Avoid cleaning up twice
  val shutDownThread=addsShutDownHook()
  
  /** Flag to know if the Supervisor has been started */
  val started = new AtomicBoolean(false)
  
  logger.info("Supervisor [{}] built",id)
  
  /**
   * Start each DASU and gets the list of inputs it needs to forward to the ASCEs
   * 
   * Invoking start to a DASU triggers the initialization of its input subscriber
   * that it is implemented by this Supervisor so, ultimately, 
   * each DASU calls Supervisor#startSubscriber.
   * This method, calls Dasu#start() just to be and independent of the
   * implementation of Dasu#start() itself.
   */
  private def startDasus(): Map[String, Set[String]] = {
    dasus.values.foreach(_.start())
    
    val fun = (m: Map[String, Set[String]], d: Dasu) => m + (d.id -> d.getInputIds())
    dasus.values.foldLeft(Map.empty[String, Set[String]])(fun)
  }
  
  /**
   * Enable or diable the auto-refresh of the outputs in the DASUs
   * 
   * @param enable if true enable the autorefresh, otherwise disable the autorefresh
   */
  def enableAutoRefreshOfOutput(enable: Boolean) {
    if (enable) {
      dasus.values.foreach(dasu => dasu.enableAutoRefreshOfOutput())
    } else {
      dasus.values.foreach(dasu => dasu.disableAutoRefreshOfOutput())
    }
  }
  
  /**
   * Start the loop:
   * - get events from the BSDB
   * - forward events to the DASUs
   * 
   * @return Success if the there were no errors starting the supervisor, 
   *         Failure otherwise 
   */
  def start(): Try[Unit] = {
    val alreadyStarted = started.getAndSet(true) 
    if (!alreadyStarted) {
      logger.debug("Starting Supervisor [{}]",id)
      dasus.values.foreach(dasu => dasu.enableAutoRefreshOfOutput())
      val inputsOfSupervisor = dasus.values.foldLeft(Set.empty[String])( (s, dasu) => s ++ dasu.getInputIds())
      inputSubscriber.startSubscriber(this, inputsOfSupervisor).flatMap(s => Try(logger.debug("Supervisor [{}] started",id)))
    } else {
      logger.warn("Supervisor [{}] already started",id)
      new Failure(new Exception("Supervisor already started"))
    }
  }

  /**
   * Release all the resources
   */
  def cleanUp() = synchronized {

    val alreadyCleaned = cleanedUp.getAndSet(true)
    if (!alreadyCleaned) {
      logger.debug("Cleaning up supervisor [{}]", id)

      logger.debug("Releasing DASUs running in the supervisor [{}]", id)
      dasus.values.foreach(_.cleanUp)

      logger.debug("Supervisor [{}]: releasing the subscriber", id)
      Try(inputSubscriber.cleanUpSubscriber())
      logger.debug("Supervisor [{}]: releasing the publisher", id)
      Try(outputPublisher.cleanUpPublisher())
      logger.info("Supervisor [{}]: cleaned up", id)
    }
  }
  
    /** Adds a shutdown hook to cleanup resources before exiting */
  private def addsShutDownHook(): Thread = {
    val t = new Thread() {
        override def run() = {
          cleanUp()
        }
    }
    Runtime.getRuntime().addShutdownHook(t)
    t
  }
  
  /** 
   *  Notify the DASUs of new inputs received from the consumer
   *  
   *  @param iasios the inputs received
   */
  def inputsReceived(iasios: Set[IASValue[_]]) {
    
    val receivedIds = iasios.map(i => i.id)
    
    dasus.values.foreach(dasu => {
      val iasiosToSend = iasios.filter(iasio => iasiosToDasusMap(dasu.id).contains(iasio.id))
      val idsOfIasiosToSend = iasiosToSend.map(_.id)
      if (!iasiosToSend.isEmpty) {
        dasu.inputsReceived(iasiosToSend)
      }
    })
  }
  
  /** 
   *  The Supervisor acts as publisher for the DASU
   *  by forwarding IASIOs to its own publisher.
   *  The initialization has already been made by the supervisor 
   *  so this method, invoke by each DASU,
   *  does nothing and always return success. 
   *  
   *  @return Success or Failure if the initialization went well 
   *          or encountered a problem  
   */
  def initializePublisher(): Try[Unit] = new Success(())
  
  /**
   * The Supervisor acts as publisher for the DASU
   * by forwarding IASIOs to its own publisher.
   * The clean up will be done by by the supervisor on its own publisher 
   * so this method, invoked by each DASU, 
   * does nothing and always return success. 
   *  
   *  @return Success or Failure if the clean up went well 
   *          or encountered a problem  
   */
  def cleanUpPublisher(): Try[Unit] = new Success(())
  
  /**
   * The Supervisor acts as publisher for the DASU
   * by forwarding IASIOs to its own publisher.
   * 
   * @param iasio the not IASIO to publish
   * @return a try to let the caller aware of errors publishing
   */
  def publish(iasio: IASValue[_]): Try[Unit] = outputPublisher.publish(iasio)
  
  /** 
   *  The Supervisor has its own subscriber so this initialization,
   *  invoked by each DASU, does nothing but returning Success.
   */
  def initializeSubscriber(): Try[Unit] = new Success(())
  
  /** 
   *  The Supervisor has its own subscriber so this  clean up 
   *  invoked by each DASU, does nothing but returning Success. 
   */
  def cleanUpSubscriber(): Try[Unit] = new Success(())
  
  /**
   * The Supervisor has its own subscriber to get events from: the list of
   * IDs to be accepted is composed of the IDs accepted by each DASUs.
   * 
   * Each DASU calls this method when ready to accept IASIOs; the Supervisor
   * - uses the passedInputs to tune its list of accepted IDs.
   * - uses the passed listener to forward to each DAUS the IASIOs it receives  
   * 
   * 
   * @param listener the listener of events
   * @param acceptedInputs the IDs of the inputs accepted by the listener
   */
  def startSubscriber(listener: InputsListener, acceptedInputs: Set[String]): Try[Unit] = {
    new Success(())
  }
}

object Supervisor {

  /** 
   *  Application: run a Supervisor with the passed ID and 
   *  kafka producer and consumer.
   *  
   *  Kill to terminate.
   */
  def main(args: Array[String]) = {
    require(!args.isEmpty,"Missing identifier in command line")
    val supervisorId = args(0)
    
    val cdbFiles: CdbFiles = new CdbJsonFiles("../test")
    val reader: CdbReader = new JsonReader(cdbFiles)
    
    val outputPublisher: OutputPublisher = KafkaPublisher(supervisorId,System.getProperties)
    val inputsProvider: InputSubscriber = new KafkaSubscriber(supervisorId,System.getProperties)
    
    // The identifier of the supervisor
    val identifier = new Identifier(supervisorId, IdentifierType.SUPERVISOR, None)
    
    val factory = (dd: DasuDao, i: Identifier, op: OutputPublisher, id: InputSubscriber, cr: CdbReader) => DasuImpl(dd,i,op,id,cr)
    
    // Build the supervisor
    val supervisor = new Supervisor(identifier,outputPublisher,inputsProvider,reader,factory)
    
    val started = supervisor.start()
    started match {
      case Success(_) => val latch = new CountDownLatch(1); latch.await();
      case Failure(ex) => System.err.println("Error starting the supervisor: "+ex.getMessage)
    }
  }
}