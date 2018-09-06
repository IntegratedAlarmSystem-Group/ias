package org.eso.ias.dasu

import org.eso.ias.logging.IASLogger
import org.eso.ias.cdb.CdbReader
import org.eso.ias.cdb.json.JsonReader
import org.eso.ias.cdb.json.CdbFiles
import org.eso.ias.cdb.json.CdbJsonFiles
import org.eso.ias.cdb.pojos.DasuDao
import org.eso.ias.dasu.topology.Topology

import scala.collection.JavaConverters
import org.eso.ias.cdb.pojos.DasuDao
import org.eso.ias.types.Identifier
import org.eso.ias.types.IdentifierType
import org.eso.ias.cdb.pojos.AsceDao
import org.eso.ias.asce.ComputingElement
import org.eso.ias.asce.ComputingElementState
import org.eso.ias.asce.AsceStates
import org.eso.ias.types.InOut
import org.eso.ias.dasu.publisher.OutputPublisher
import org.eso.ias.types.IASValue
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
 * The Distributed Alarm System Unit (DASU).
 * 
 * The DASU, once notified of new inputs received from the BSDB (or other sources),
 * forwards the IASIOs to the ASCEs to produce the output.
 * If no new inputs arrived the DASU generate the output anyhow to notify that the DASU is alive.
 * At a first glance it seems enough to check the validity of the last set of inputs to assign 
 * the validity of the output.
 * 
 * The DASU is initialized in the constructor: to let it start processing events,
 * the start() method must be called.
 * 
 * The DASU must update the output even if it does not receive any input
 * to refresh the output and send it to the BSDB.
 * The automatic refresh of the output when no new inputs arrive is not active by default
 * but need to be activated by calling enableAutoRefreshOfOutput(true).
 * 
 * Newly received inputs are immediately processed unless they arrive so often
 * to need the CPU running at 100%. In this case the DASU delayed the evaluation
 * of the output collecting the inputs intil the throttling time interval elapses.
 * 
 * @constructor create a DASU with the given identifier
 * @param dasuIdentifier the identifier of the DASU
 * @param autoSendTimeInterval the refresh rate (seconds) to automatically re-send the last calculated 
 *                    output even if it did not change
 * @param tolerance the max delay (secs) before declaring an input unreliable
 */
abstract class Dasu(
    val dasuIdentifier: Identifier, 
    val autoSendTimeInterval: Integer,
    val tolerance: Integer) extends InputsListener {
  require(autoSendTimeInterval>0)
  require(tolerance>=0)
  
  /** The logger */
  private val logger = IASLogger.getLogger(this.getClass)
  
  /** The ID of the DASU */
  val id = dasuIdentifier.id
  
  /** 
   *  True if the DASU has been generated from a template,
   *  False otherwise 
   */
  val fromTemplate: Boolean = dasuIdentifier.fromTemplate
  
  /**
   * The number of the instance if the DASU has been generated
   * from a template; empty otherwise
   */
  lazy val templateInstance: Option[Int] = dasuIdentifier.templateInstance
  
  /** 
   *  Auto send time interval in milliseconds
   */
  val autoSendTimeIntervalMillis = TimeUnit.MILLISECONDS.convert(autoSendTimeInterval.toLong, TimeUnit.SECONDS)
  
  /** 
   *  The tolerance in milliseconds
   */
  val toleranceMillis = TimeUnit.MILLISECONDS.convert(tolerance.toLong, TimeUnit.SECONDS)
  
  /**
   * The minimum allowed refresh rate when a flow of inputs arrive (i.e. the throttiling) 
   * is given by [[TimeScheduler.DefaultMinAllowedRefreshRate]] 
   * if not overridden by a java property
   */
  val throttling = {
    val prop = Option(System.getProperties.getProperty(Dasu.MinAllowedRefreshRatePropName))
    prop.map(s => Try(s.toInt).getOrElse(Dasu.DefaultMinAllowedRefreshRate)).getOrElse(Dasu.DefaultMinAllowedRefreshRate).abs.toLong
  }
  logger.debug("Output calculation throttling of DASU [{}] set to {}",id,throttling.toString())
      
  
  /** The IDs of the inputs of the DASU */
  def getInputIds(): Set[String]
  
  /** The IDs of the ASCEs running in the DASU  */
  def getAsceIds(): Set[String]
  
  /** 
   *  Start getting events from the inputs subscriber
   *  to produce the output
   */
  def start(): Try[Unit]
  
  /**
   * Enable/disable the automatic update of the output
   * in case no new inputs arrive.
   * 
   * Most likely, the value of the output remains the same 
   * while the validity could change.
   */
  def enableAutoRefreshOfOutput(enable: Boolean)
  
  /**
   * Updates the output with the inputs received
   * 
   * @param iasios the inputs received
   * @see InputsListener
   */
  override def inputsReceived(iasios: Set[IASValue[_]])
  
  /**
   * Release all the resources before exiting
   */
  def cleanUp()
}

/** Companion object */
object Dasu {
  /** The minimum possible refresh rate */
  val DefaultMinAllowedRefreshRate = 250
  
  /** The name of the java property to set the minimum allowed refresh rate */
  val MinAllowedRefreshRatePropName = "ias.dasu.min.allowed.output.refreshrate"
}
