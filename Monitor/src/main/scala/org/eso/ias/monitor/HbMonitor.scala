package org.eso.ias.monitor

import java.util.concurrent.{Executors, ScheduledExecutorService, ThreadFactory, TimeUnit}

import com.typesafe.scalalogging.Logger
import org.eso.ias.heartbeat.consumer.{HbListener, HbMsg}
import org.eso.ias.logging.IASLogger
import org.eso.ias.types.IdentifierType._
import org.eso.ias.types.{Alarm, Identifier}

import scala.collection.mutable.{Map => MutableMap}

/**
  * Monitors the HBs of
  * - plugins
  * - converters
  * - clients
  * - sink connectors (Kafka connectors does not publish HBs)
  * - supervisor
  *
  * [[HbMonitor()]] gets the HBs using a HB consumer and checks
  * the time when they have been produced and received against the passed threshold to
  * generate alarms.
  *
  * A different alarm is generated depending on the type of the missing HB: the IDs of the
  * missing HBs are passed ass properties
  *
  * When a HB is received, an entry is updated in the relative map.
  * A thread runs periodically to check if all the HBs have been received and,
  * if any of them is missing, sets an alarm.
  * The entry in the map is a boolean used as a watch dog: the thread periodically resets the watch dog
  * and checks if some of the boolean have not been set.
  *
  * Notification of alarms is done indirectly by setting the state of the alarm in the [[MonitorAlarm]].
  *
  * TODO: at the present, the copnverters do not publish a fullRunnibngId but their plain ID
  *       this needs to be changed when [[https://github.com/IntegratedAlarmSystem-Group/ias/issues/145 #145]]
  *       will be fixed
  *
  * @param pluginIds The IDs of the plugins whose IDs must be monitored
  * @param converterIds The IDs of the converters whose IDs must be monitored
  * @param clientIds The IDs of the clients whose IDs must be monitored
  * @param sinks The IDs of the sink connectors whose IDs must be monitored
  *              Kafka sink connectors does not publish HBs and must be monitored elsewhere;
  *              however the IAs has sink conenctors like the email sender that publishes HBs
  * @param supervisorIds The IDs of the supervisors whose IDs must be monitored
  * @param threshold An alarm is emitted if the HB has not been received before the threshold elapses
  *                  (in seconds)
  * @param pluginsAlarmpriority the priority of the alarm for faulty plugins
  * @param convertersAlarmpriority the priority of the alarm for faulty converters
  * @param clientsAlarmpriority the priority of the alarm for faulty clients
  * @param sinksAlarmpriority the priority of the alarm for faulty sink connectors
  * @param supervisorsAlarmpriority the priority of the alarm for faulty supervisors
  */
class HbMonitor(
                 val pluginIds: Set[String],
                 val converterIds: Set[String],
                 val clientIds: Set[String],
                 val sinks: Set[String],
                 val supervisorIds: Set[String],
                 val threshold: Long,
                 val pluginsAlarmpriority: Alarm=Alarm.getSetDefault,
                 val convertersAlarmpriority: Alarm=Alarm.getSetDefault,
                 val clientsAlarmpriority: Alarm=Alarm.getSetDefault,
                 val sinksAlarmpriority: Alarm=Alarm.getSetDefault,
                 val supervisorsAlarmpriority: Alarm=Alarm.getSetDefault) extends HbListener with Runnable {
  require(threshold>0,"Invalid negative threshold")
  require(Option(pluginIds).isDefined)
  require(Option(converterIds).isDefined)
  require(Option(clientIds).isDefined)
  require(Option(sinks).isDefined)
  require(Option(supervisorIds).isDefined)

  /** The map to store the last HB of plugins */
  private val pluginsHbMsgs: MutableMap[String, Boolean] = MutableMap.empty

  /** The map to store the last HB of converters */
  private val convertersHbMsgs: MutableMap[String, Boolean] = MutableMap.empty

  /** The map to store the last HB of clients */
  private val clientsHbMsgs: MutableMap[String, Boolean] = MutableMap.empty

  /** The map to store the last HB of sink connectors */
  private val sinksHbMsgs: MutableMap[String, Boolean] = MutableMap.empty

  /** The map to store the last HB of supervisor */
  private val supervisorsHbMsgs: MutableMap[String, Boolean] = MutableMap.empty

  /** A list with all the maps of HB */
  private val hbMaps= List(pluginsHbMsgs,convertersHbMsgs,sinksHbMsgs,clientsHbMsgs,supervisorsHbMsgs)

  /** The factory to generate the periodic thread */
  private val factory: ThreadFactory = new ThreadFactory {
    override def newThread(runnable: Runnable): Thread = {
      val t: Thread = new Thread("HbMonitor-Thread")
      t.setDaemon(true)
      t
    }
  }

  /** The executor to periodically run the thread */
  private val schedExecutorSvc: ScheduledExecutorService = Executors.newSingleThreadScheduledExecutor(factory)

  def start(): Unit = {
    HbMonitor.logger.debug("Starting up")
    schedExecutorSvc.scheduleWithFixedDelay(this,threshold,threshold,TimeUnit.SECONDS)
    HbMonitor.logger.debug("Thread scheduled every {} seconds",threshold)
    HbMonitor.logger.info("Started up")
  }

  def shutdown(): Unit = {
    HbMonitor.logger.debug("Shutting down")
    schedExecutorSvc.shutdown()
    HbMonitor.logger.info("Shut down")
  }

  /**
    * An heartbeat has been consumed from the HB topic
    *
    * @param hbMsg The HB consumed from the HB topic
    */
  override def hbReceived(hbMsg: HbMsg): Unit = synchronized {
    HbMonitor.logger.debug("HB received from {}",hbMsg.id)
    if (Identifier.checkFullRunningIdFormat(hbMsg.id)) {
      // The converter due to a bug sends its ID instead of
      // its fullRunningId
      // @see #145
      convertersHbMsgs.put(hbMsg.id,true)
    } else {
      val identifier = Identifier(hbMsg.id)
      identifier.idType match {
        case PLUGIN => pluginsHbMsgs.put(identifier.id,true)
        case SUPERVISOR => supervisorsHbMsgs.put(identifier.id,true)
        case SINK => sinksHbMsgs.put(identifier.id,true)
        case CLIENT => clientsHbMsgs.put(identifier.id,true)
        case idType => HbMonitor.logger.warn("Unknown HB type to monitor: {} from fullRunningId {}",idType,hbMsg.id)
      }
    }
  }

  /** The thread that periodically checks for missing HBs */
  override def run(): Unit = synchronized {
    HbMonitor.logger.debug("Checking reception of HBs")

    // Return the IDs of the alarms in the map that have not been updated
    // These IDs are sent as properties in the alarms
    def faultyIds(m: MutableMap[String, Boolean]): List[String] = m.filterKeys(k => !m(k)).keys.toList

    // Update the passed alarm
    def updateAlarm(alarm: MonitorAlarm, faultyIds: List[String], priority: Alarm=Alarm.getSetDefault): Unit =
      alarm.set(priority,faultyIds.mkString(","))

    val faultyPlugins = updateAlarm(MonitorAlarm.PLUGIN_DEAD,faultyIds(pluginsHbMsgs),pluginsAlarmpriority)
    val faultySupervisors = updateAlarm(MonitorAlarm.SUPERVISOR_DEAD,faultyIds(supervisorsHbMsgs),supervisorsAlarmpriority)
    val faultyConverters = updateAlarm(MonitorAlarm.CONVERTER_DEAD,faultyIds(convertersHbMsgs),convertersAlarmpriority)
    val faultySinkConnectors = updateAlarm(MonitorAlarm.SINK_DEAD,faultyIds(sinksHbMsgs),sinksAlarmpriority)
    val faultyClients = updateAlarm(MonitorAlarm.CLIENT_DEAD,faultyIds(clientsHbMsgs),clientsAlarmpriority)

    // reset the maps to be ready for the next iteration
    hbMaps.foreach(m => m.keySet.foreach(k => m(k)=false))
  }
}

/** Companion object */
object HbMonitor {
  /** The logger */
  val logger: Logger = IASLogger.getLogger(HbMonitor.getClass)
}
