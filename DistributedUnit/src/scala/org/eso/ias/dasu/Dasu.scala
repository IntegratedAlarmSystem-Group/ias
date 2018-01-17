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
 * the output. This task must be executed only if no inputs arrive before otherwise
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
 * @param dasuIdentifier the identifier of the DASU
 */
abstract class Dasu(val dasuIdentifier: Identifier) extends InputsListener {
  
  /** The ID of the DASU */
  val id = dasuIdentifier.id
  
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
   * Deactivate the automatic update of the output
   * in case no new inputs arrive.
   */
  def disableAutoRefreshOfOutput()
  
  /**
   * Activate the automatic update of the output
   * in case no new inputs arrive.
   * 
   * Most likely, the value of the output remains the same 
   * while the validity could change.
   */
  def enableAutoRefreshOfOutput()
  
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