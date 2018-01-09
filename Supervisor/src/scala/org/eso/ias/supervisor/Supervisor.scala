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

/**
 * A Supervisor is the container to run several DASUs into the same JVM.
 * 
 * The Supervisor gets IASIOs from a InputSubscriber and publisches
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
 * are used by DASUs and Supervisors in this way a DASu can be easily tested
 * directly connected to Kafka (for example) without the need to have
 * it running into a Supervisor.
 * 
 * @param id the identifier of the Supervisor
 * @param cdbReader the reader to get the configuration from the CDB
 */
class Supervisor(
    val id: String,
    private val outputPublisher: OutputPublisher,
    private val inputSubscriber: InputSubscriber,
    cdbReader: CdbReader) extends InputSubscriber with OutputPublisher {
  require(Option(id).isDefined && !id.isEmpty,"Invalid Supervisor identifier")
  require(Option(outputPublisher).isDefined,"Invalid output publisher")
  require(Option(inputSubscriber).isDefined,"Invalid input subscriber")
  
  /** The logger */
  val logger = IASLogger.getLogger(Supervisor.getClass)
  logger.info("Building Supervisor [{}]",id)
  
  /**
   * The DASUs to run in the Supervisor
   */
  val dasus = JavaConverters.asScalaSet(cdbReader.getDasusForSupervisor(id))
  require(dasus.size>0,"No DASUs to run in Supervisor "+id)
  logger.info("{} DASUs to run in [{}]: {}",dasus.size.toString(),id,dasus.map(d => d.getId()).mkString(", "))
  
  // Get the list of DASus to activate from CDB
  // Activate the DASUs and get the list of IASIOs the want to receive
  // Connect to the input kafka topic passing the IDs of the IASIOs
  
  // Start the loop:
  // - get IASIOs, forward to the DASUs
  // - publish the IASOs produced by the DASU in the kafka topic
  
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
   * The Supervisor has its own subscriber get events from: the list of
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
  def startSubscriber(listener: InputsListener, acceptedInputs: Set[String]): Try[Unit] = new Success(())
}

object Supervisor {
  
  
  
  def main(args: Array[String]) = {
    require(!args.isEmpty,"Missing identifier in command line")
    val supervisorId = args(0)
    
    val cdbFiles: CdbFiles = new CdbJsonFiles("../test")
    val reader: CdbReader = new JsonReader(cdbFiles)
  }
}