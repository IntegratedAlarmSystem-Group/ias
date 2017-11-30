package org.eso.ias.dasu

import org.ias.prototype.logging.IASLogger
import org.eso.ias.cdb.CdbReader
import org.eso.ias.cdb.json.JsonReader
import org.eso.ias.cdb.json.CdbFiles
import org.eso.ias.cdb.json.CdbJsonFiles
import org.eso.ias.cdb.pojos.DasuDao
import org.eso.ias.dasu.topology.Topology

import scala.collection.JavaConverters
import org.eso.ias.cdb.pojos.DasuDao
import org.eso.ias.prototype.input.Identifier
import org.eso.ias.prototype.input.java.IdentifierType
import org.eso.ias.cdb.pojos.AsceDao
import org.eso.ias.prototype.compele.ComputingElement
import org.eso.ias.prototype.compele.ComputingElementState
import org.eso.ias.prototype.compele.AsceStates
import org.eso.ias.prototype.input.InOut
import org.eso.ias.kafkautils.SimpleStringProducer
import org.eso.ias.dasu.publisher.OutputPublisher
import org.eso.ias.prototype.input.java.IASValue
import org.eso.ias.prototype.input.JavaConverter
import org.eso.ias.dasu.executorthread.ScheduledExecutor
import scala.util.Try
import java.util.concurrent.atomic.AtomicLong
import java.util.Properties
import scala.collection.mutable.{Map => MutableMap}
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.atomic.AtomicReference
import java.util.concurrent.TimeUnit
import org.eso.ias.dasu.subscriber.InputsListener
import org.eso.ias.dasu.subscriber.InputSubscriber
import scala.util.Failure
import scala.util.Success

/**
 * The Distributed Alarm System Unit or DASU.
 * 
 * The DASU, once notified of new inputs received from the BSDB (or other sources),
 * sends the IASIOs to the ASCEs to produce the output.
 * If no new inputs arrived the DASU generate the output anyhow to notify that the DASU is alive.
 * At a first glance it seems enough to check the validity of the last set of inputs to assign 
 * the validity of the output.
 * 
 * The DASU is initialized in the constructor but to let it start processing events,
 * the start() method must be called.
 * 
 * The DASU must update the output even if it does not receive any input
 * to refresh the output and send it to the BSDB.
 * After generating the output, the DASU schedules a timer task for refreshing
 * the output. This task must be executed only if no inputs arrive before otherwiise
 * must be cancelled and anew scheduled.
 * The calculation of the point in time to refresh the output is delegated to [[TimeScheduler]].
 * The refresh of the output when no inputs arrive must be explicitly activate
 * and can be suspended resumed at any time.
 * 
 * The automatic refresh of the output when no new input arrive is not active by default
 * but need to be activated by calling enableAutoRefreshOfOutput()
 * 
 * Newly received inputs are immediately processed unless they arrive so often
 * to risk to have the CPU running at 100%.
 * Normally the DASU has a thread that refreshes the output even if there are no inputs:
 * it must be explicitly started by invoking autoUpdateOfOutput that will normally be done by the 
 * Supervisor.
 * In the case that the automatic refresh rate of the input has been activated, newly received
 * inputs are immediately processed only if the next refresh is not scheduled before
 * the minimum allowed refresh rate. If it is the case then the inputs are buffered 
 * and will be processed later.
 * This is a very easy strategy that avoids rescheduling the refresh after 
 * each publication of the output. 
 * [[TimeScheduler]] was thought to implement a more complex heuristic but
 * we will not use it unless we will need it.  
 * 
 * @constructor create a DASU with the given identifier
 * @param id is the identifier of the DASU
 * @param outputPublisher the publisher to send the output
 * @param inputSubscriber the subscriber getting events to be processed 
 * @param cdbReader the CDB reader to get the configuration of the DASU from the CDB

 */
class Dasu(
    val id: String,
    private val outputPublisher: OutputPublisher,
    private val inputSubscriber: InputSubscriber,
    cdbReader: CdbReader)
    extends InputsListener with Runnable {
  require(Option(outputPublisher).isDefined,"Invalid output publisher")
  require(Option(inputSubscriber).isDefined,"Invalid input subscriber")
  
  /** The logger */
  private val logger = IASLogger.getLogger(this.getClass)
  
  /** The identifier of the DASU
   *  
   *  In this version it has no parent identifier because
   *  the Supervisor has not yet been implemented
   */
  val dasuIdentifier: Identifier = new Identifier(id,IdentifierType.DASU,None)
  
  logger.info("Reading CDB configuration of [{}] DASU",dasuIdentifier.toString())
  
  // Read configuration from CDB
  val dasuDao = {
    val dasuOptional = cdbReader.getDasu(id)
    require(dasuOptional.isPresent(),"DASU ["+id+"] configuration not found on cdb")
    dasuOptional.get
  }
  // TODO: release CDB resources
  logger.debug("DASU [{}] configuration red from CDB",id)
  
  /**
   * The configuration of the ASCEs that run in the DASU
   */
  val asceDaos = JavaConverters.asScalaSet(dasuDao.getAsces).toList
  
  // Are there ASCEs assigned to this DASU?
  require(dasuDao.getAsces.size()>0,"No ASCE found for DASU "+id)
  logger.info("DASU [{}]: will load and run {} ASCEs",id,""+dasuDao.getAsces.size())
  
  // The ID of the output generated by the DASU
  val dasuOutputId = dasuDao.getOutput().getId()
  
  // Build the topology
  val dasuTopology: Topology = new Topology(
      id,
      dasuOutputId,
      asceDaos)
  logger.debug("DASU [{}]: topology built",id)
  logger.debug(dasuTopology.toString())
  
  // Instantiate the ASCEs
  val asces: Map[String, ComputingElement[_]] = {
    val addToMapFunc = (m: Map[String, ComputingElement[_]], asce: AsceDao) => {
      val propsForAsce = new Properties()
      asce.getProps.forEach(p => propsForAsce.setProperty(p.getName, p.getValue))
      m + (asce.getId -> ComputingElement(asce,dasuIdentifier,propsForAsce))
    }
    asceDaos.foldLeft(Map[String,ComputingElement[_]]())(addToMapFunc)
  }
  logger.info("DASU [{}] ASCEs loaded: [{}]",id, asces.keys.mkString(", "))
    
  // Activate the ASCEs
  val ascesInitedOk=asces.valuesIterator.map(asce => asce.initialize()).forall(s => s==AsceStates.InputsUndefined)
  assert(ascesInitedOk,"At least one ASCE did not pass the initialization phase")
  logger.info("DASU [{}]: ASCEs initialized",id)
  
  // The ASCE that produces the output of the DASU
  val asceThatProducesTheOutput = dasuTopology.asceProducingOutput(dasuOutputId)
  require(asceThatProducesTheOutput.isDefined && !asceThatProducesTheOutput.isEmpty,"ASCE producing output not found")
  logger.info("The output [{}] of the DASU [{}] is produced by [{}] ASCE",dasuOutputId,id,asceThatProducesTheOutput.get)
  
  // Get the required refresh rate i.e. the rate to produce the output 
  // of the DASU that is given by the ASCE that produces such IASIO
  val refreshRate = asceDaos.find(_.getOutput.getId==dasuOutputId).map(asce => asce.getOutput().getRefreshRate())
  require(refreshRate.isDefined,"Refresh rate not found!")
  logger.info("The DASU [{}] produces the output [{}] at a rate of {}ms",id,dasuOutputId,""+refreshRate.get)
  
  /**
   * Values received in input from plugins or other DASUs (BSDB)
   * and not yet processed
   */
  val notYetProcessedInputs: MutableMap[String,IASValue[_]] = MutableMap()
  
  /** The thread executor service */
  val scheduledExecutor = new ScheduledExecutor(id)
  
  /** 
   *  The helper to schedule the next refresh of the output
   *  when no inputs have been received
   */
  val timeScheduler: TimeScheduler = new TimeScheduler(dasuDao)
  
  /**
   * The point in time when the output has been generated
   * for the last time
   */
  val lastUpdateTime = new AtomicLong(Long.MinValue)
  
  /** True if the automatic refresh of the output has been enabled */
  val timelyRefreshing = new AtomicBoolean(false)
  
  /** The task that refreshes the output */
  val refreshTask: AtomicReference[ScheduledFuture[_]] = new AtomicReference[ScheduledFuture[_]]()
  
  logger.debug("DASU [{}] initializing the publisher", id)
  val outputPublisherInitialized = outputPublisher.initialize()
  outputPublisherInitialized match {
    case Failure(f) => logger.error("DASU [{}] failed to initialize the publisher: NO output will be produced", id,f)
    case Success(s) => logger.info("DASU [{}] publisher successfully initialized",id)
  }
  logger.debug("DASU [{}] initializing the subscriber", id)
  
  val inputSubscriberInitialized = inputSubscriber.initialize()
  inputSubscriberInitialized match {
    case Failure(f) => logger.error("DASU [{}] failed to initialize the subscriber: NO input will be processed", id,f)
    case Success(s) => logger.info("DASU [{}] subscriber successfully initialized",id)
  }
  
  addsShutDownHook()
  logger.info("DASU [{}] built", id)
  
  /**
   * Propagates the inputs received from the BSDB to each of the ASCEs
   * in the DASU generating the output of the entire DASU.
   * 
   * THis method runs after the refresh time interval elapses.
   * All the iasios collected in the time interval will be passed to the first level of the ASCEs
   * and up till the last ASCE generates the output of the DASU itself.
   * Each ASCE runs the TF and produces another output to be propagated to the next level.
   * The last level is the only one ASCE that produces the output of the DASU
   * to be sent to the BSDB.
   * 
   * @param iasios the IASIOs received from the BDSB in the last time interval
   * @return the IASIO to send back to the BSDB
   */
  def propagateIasios(iasios: Set[IASValue[_]]): Option[IASValue[_]] = {
      
      // Updates one ASCE i.e. runs its TF passing the inputs
      // Return the output of the ASCE
      def updateOneAsce(asceId: String, asceInputs: Set[IASValue[_]]): Option[IASValue[_]] = {
        
        val requiredInputs = dasuTopology.inputsOfAsce(asceId)
        assert(requiredInputs.isDefined,"No inputs required by "+asceId)
        val inputs: Set[IASValue[_]] = asceInputs.filter( p => requiredInputs.get.contains(p.id))
        // Get the ASCE with the given ID 
        val asceOpt = asces.get(asceId)
        assert(asceOpt.isDefined,"ASCE "+asceId+" NOT found!")
        val asce: ComputingElement[_] = asceOpt.get
        val asceOutputOpt: Option[InOut[_]] = asce.update(inputs)._1
        asceOutputOpt.map (inout => JavaConverter.inOutToIASValue(inout))
      }
      
      // Run the TFs of all the ASCEs in one level
      // Returns the inputs plus all the outputs produced by the ACSEs
      def updateOneLevel(asces: Set[String], levelInputs: Set[IASValue[_]]): Set[IASValue[_]] = {
        
        asces.foldLeft(levelInputs) ( 
          (s: Set[IASValue[_]], id: String ) => {
            val output = updateOneAsce(id, levelInputs) 
            if (output.isDefined) s+output.get
            else s})
      }
      
      val outputs = dasuTopology.levels.foldLeft(iasios){ (s: Set[IASValue[_]], ids: Set[String]) => s ++ updateOneLevel(ids, s) }
      
      // TODO: update the validity!!!
      outputs.find(_.id==dasuOutputId)
  }
  
  /**
   * Updates the output with the inputs received
   * 
   * @param iasios the inputs received
   * @see InputsListener
   */
  override def inputsReceived(iasios: Set[IASValue[_]]) = synchronized {
    
    // Merge the inputs with the buffered ones to keep ony the last update
    iasios.foreach(iasio => notYetProcessedInputs.put(iasio.id,iasio))
    if (!timelyRefreshing.get || // Send immediately 
        refreshTask.get().getDelay(TimeUnit.MILLISECONDS)<=0 || // Timer task
        // Or execute only if does not happen more often then the minAllowedRefreshRate:
        (
            // The delay from last execution is greater then minAllowedRefreshRate
            System.currentTimeMillis()-lastUpdateTime.get()>=timeScheduler.minAllowedRefreshRate &&
            // Next timer task will happen later then minAllowedRefreshRate
            refreshTask.get().getDelay(TimeUnit.MILLISECONDS)>timeScheduler.minAllowedRefreshRate
        )) {
      val before = System.currentTimeMillis()
      val newOutput = propagateIasios(notYetProcessedInputs.values.toSet)
      val after = System.currentTimeMillis()
      lastUpdateTime.set(after)
      notYetProcessedInputs.clear()
      // We ignore the next execution time provided by the TimeScheduler
      // with the disadvantage that the TimeScheduler takes into account
      // the average execution time to produce the output
      // We call getNextRefreshTime because it publish statistics 
      // that could help debugging
      timeScheduler.getNextRefreshTime(after-before)
      
      newOutput.foreach( output => {
        outputPublisher.publish(output) 
        logger.debug("DASU [{}]: output published",id)})
    }
  }
  
  /** 
   *  Start getting events from the inputs subscriber
   *  to produce the output
   */
  def start() = {
    if (inputSubscriberInitialized.isSuccess) {
      val started = inputSubscriber.start(this, dasuTopology.dasuInputs)
      started match {
        case Failure(f) => logger.error("DASU [{}] failed to start getting events: NO input will be processed", id,f)
        case Success(s) => logger.info("DASU [{}] now ready to process events",id)
      }
    }
  }
  
  /** The task to refresh the output when no new inputs have been received */
  override def run() = {
    inputsReceived(Set())
  }
  
  /** Cancel the timer task */
  private def cancelTimerTask() =  synchronized {
    val oldTask: Option[ScheduledFuture[_]] = Option(refreshTask.getAndSet(null))
    oldTask.foreach(task => task.cancel(false))
    
  }
  
  /**
   * Deactivate the automatic update of the output
   * in case no new inputs arrive.
   */
  def disableAutoRefreshOfOutput() = synchronized {
    val isEnabled = timelyRefreshing.getAndSet(false)
    if (isEnabled) {
      cancelTimerTask()
      logger.info("Dasu [{}]: automatic refresh of output disabled",id)
    }
  }
  
  /**
   * Activate the automatic update of the output
   * in case no new inputs arrive.
   * 
   * Most likely, the value of the output remains the same 
   * while the validity could change.
   */
  def enableAutoRefreshOfOutput() = synchronized {
    val alreadyActive = timelyRefreshing.getAndSet(true)
    if (alreadyActive) {
      logger.warn("DASU [{}]: automatic refresh of output already ative",id)
    } else {
      cancelTimerTask()
      val newTask=scheduledExecutor.scheduleWithFixedDelay(
          this, 
          timeScheduler.normalizedRefreshRate, 
          timeScheduler.normalizedRefreshRate,
          TimeUnit.MILLISECONDS)
      refreshTask.set(newTask)
      logger.info("Dasu [{}]: automatic refresh of output enabled at intervals of {} msecs",
          id,
          timeScheduler.normalizedRefreshRate.toString())
    }
  }
  
  /** Adds a shutdown hook to cleanup resources before exiting */
  def addsShutDownHook() = {
      Runtime.getRuntime().addShutdownHook(new Thread() {
        override def run() = {
          logger.info("DASU [{}]: releasing resources", id)
          logger.debug("DASU [{}]: stopping the auto-refresh of the output", id)
          Try(disableAutoRefreshOfOutput())
          logger.debug("DASU [{}]: releasing the subscriber", id)
          Try(inputSubscriber.cleanUp())
          logger.debug("DASU [{}]: releasing the publisher", id)
          Try(outputPublisher.cleanUp())
          logger.info("DASU [{}]: shutted down",id)
        }
    })
  }
}

object Dasu {
  /** The time interval to log statistics (minutes) */
  val DeafaultStatisticsTimeInterval = 10
  
  /** The name of the java property to set the statistics generation time interval */
  val StatisticsTimeIntervalPropName = "ias.dasu.stats.timeinterval"
  
  /** The actual time interval to log statistics (minutes) */
  val StatisticsTimeInterval: Int = {
    val prop = Option(System.getProperties.getProperty(StatisticsTimeIntervalPropName))
    prop.map(s => Try(s.toInt).getOrElse(DeafaultStatisticsTimeInterval)).getOrElse(DeafaultStatisticsTimeInterval).abs
  }
  
}