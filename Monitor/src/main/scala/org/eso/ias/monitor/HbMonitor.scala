package org.eso.ias.monitor

import com.typesafe.scalalogging.Logger
import org.eso.ias.heartbeat.HeartbeatProducerType.*
import org.eso.ias.heartbeat.consumer.{HbKafkaConsumer, HbListener, HbMsg}
import org.eso.ias.logging.IASLogger
import org.eso.ias.types.Alarm

import java.util.concurrent.{Executors, ScheduledExecutorService, ThreadFactory, TimeUnit}
import scala.collection.mutable.Map as MutableMap

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
  * @param hbConsumer The consumer of HBs
  * @param pluginIds The IDs of the plugins whose IDs must be monitored
  * @param converterIds The IDs of the converters whose IDs must be monitored
  * @param clientIds The IDs of the clients whose IDs must be monitored
  * @param sinkIds The IDs of the sink connectors whose IDs must be monitored
  *              Kafka sink connectors does not publish HBs and must be monitored elsewhere;
  *              however the IAs has sink conenctors like the email sender that publishes HBs
  * @param supervisorIds The IDs of the supervisors whose IDs must be monitored
  * @param coreToolIds The IDs of the IAS core tools whose IDs must be monitored
  * @param threshold An alarm is emitted if the HB has not been received before the threshold elapses
  *                  (in seconds)
  * @param pluginsAlarmPriority the priority of the alarm for faulty plugins
  * @param convertersAlarmPriority the priority of the alarm for faulty converters
  * @param clientsAlarmPriority the priority of the alarm for faulty clients
  * @param sinksAlarmPriority the priority of the alarm for faulty sink connectors
  * @param supervisorsAlarmPriority the priority of the alarm for faulty supervisors
  */
class HbMonitor(
                 val hbConsumer: HbKafkaConsumer,
                 val pluginIds: Set[String],
                 val converterIds: Set[String],
                 val clientIds: Set[String],
                 val sinkIds: Set[String],
                 val supervisorIds: Set[String],
                 val coreToolIds: Set[String],
                 val threshold: Long,
                 val pluginsAlarmPriority: Alarm=Alarm.getSetDefault,
                 val convertersAlarmPriority: Alarm=Alarm.getSetDefault,
                 val clientsAlarmPriority: Alarm=Alarm.getSetDefault,
                 val sinksAlarmPriority: Alarm=Alarm.getSetDefault,
                 val supervisorsAlarmPriority: Alarm=Alarm.getSetDefault,
                 val coreToolAlarmPriority: Alarm = Alarm.getSetDefault) extends HbListener with Runnable {
  require(Option(hbConsumer).isDefined)
  require(threshold>0,"Invalid negative threshold")
  require(Option(pluginIds).isDefined)
  require(Option(converterIds).isDefined)
  require(Option(clientIds).isDefined)
  require(Option(sinkIds).isDefined)
  require(Option(supervisorIds).isDefined)
  require(Option(coreToolIds).isDefined)

  /** The map to store the last HB of plugins */
  private val pluginsHbMsgs: MutableMap[String, Boolean] = MutableMap.empty
  pluginIds.foreach(pluginsHbMsgs.put(_,false))
  HbMonitor.logger.debug("{} plugins to monitor: {}",pluginsHbMsgs.keySet.size,pluginsHbMsgs.keySet.mkString(","))

  /** The map to store the last HB of converters */
  private val convertersHbMsgs: MutableMap[String, Boolean] = MutableMap.empty
  converterIds.foreach(convertersHbMsgs.put(_,false))
  HbMonitor.logger.debug("{} converters to monitor: {}",convertersHbMsgs.keySet.size,convertersHbMsgs.keySet.mkString(","))

  /** The map to store the last HB of clients */
  private val clientsHbMsgs: MutableMap[String, Boolean] = MutableMap.empty
  clientIds.foreach(clientsHbMsgs.put(_,false))
  HbMonitor.logger.debug("{} clients to monitor: {}",clientsHbMsgs.keySet.size,clientsHbMsgs.keySet.mkString(","))

  /** The map to store the last HB of sink connectors */
  private val sinksHbMsgs: MutableMap[String, Boolean] = MutableMap.empty
  sinkIds.foreach(sinksHbMsgs.put(_,false))
  HbMonitor.logger.debug("{} sink clients to monitor: {}",sinksHbMsgs.keySet.size,sinksHbMsgs.keySet.mkString(","))

  /** The map to store the last HB of supervisors */
  private val supervisorsHbMsgs: MutableMap[String, Boolean] = MutableMap.empty
  supervisorIds.foreach(supervisorsHbMsgs.put(_,false))
  HbMonitor.logger.debug("{} supervisors to monitor: {}",supervisorsHbMsgs.keySet.size,supervisorsHbMsgs.keySet.mkString(","))

  /** The map to store the last HB of IAS core tools */
  private val coreToolsHbMsgs: MutableMap[String, Boolean] = MutableMap.empty
  coreToolIds.foreach(coreToolsHbMsgs.put(_,false))
  HbMonitor.logger.debug("{} IAS core tools to monitor: {}",coreToolsHbMsgs.keySet.size,coreToolsHbMsgs.keySet.mkString(","))

  /** A list with all the maps of HB */
  private val hbMaps= List(pluginsHbMsgs,convertersHbMsgs,sinksHbMsgs,clientsHbMsgs,supervisorsHbMsgs,coreToolsHbMsgs)

  /** The factory to generate the periodic thread */
  private val factory: ThreadFactory = new ThreadFactory {
    override def newThread(runnable: Runnable): Thread = {
      val t: Thread = new Thread(runnable,"HbMonitor-Thread")
      t.setDaemon(true)
      t
    }
  }

  /** The executor to periodically run the thread */
  private val schedExecutorSvc: ScheduledExecutorService = Executors.newSingleThreadScheduledExecutor(factory)

  def start(): Unit = {
    HbMonitor.logger.debug("Starting up")
    hbConsumer.addListener(this)
    hbConsumer.start()
    HbMonitor.logger.debug("HB consumer started")
    schedExecutorSvc.scheduleWithFixedDelay(this,threshold,threshold,TimeUnit.SECONDS)
    HbMonitor.logger.debug("Thread scheduled every {} seconds",threshold)
    HbMonitor.logger.info("Started")
  }

  def shutdown(): Unit = {
    HbMonitor.logger.debug("Shutting down")
    hbConsumer.shutdown()
    HbMonitor.logger.debug("HB consumer shut down")
    schedExecutorSvc.shutdown()
    HbMonitor.logger.info("Shut down")
  }

  /**
    * An heartbeat has been consumed from the HB topic
    *
    * @param hbMsg The HB consumed from the HB topic
    */
  override def hbReceived(hbMsg: HbMsg): Unit = synchronized {
    HbMonitor.logger.debug("HB received: {} of type {}",hbMsg.hb.stringRepr,hbMsg.hb.hbType)
    hbMsg.hb.hbType match {
      case PLUGIN => pluginsHbMsgs.put(hbMsg.hb.name,true)
      case SUPERVISOR => supervisorsHbMsgs.put(hbMsg.hb.name,true)
      case SINK => sinksHbMsgs.put(hbMsg.hb.name,true)
      case CONVERTER => convertersHbMsgs.put(hbMsg.hb.name,true)
      case CLIENT => clientsHbMsgs.put(hbMsg.hb.name,true)
      case CORETOOL => coreToolsHbMsgs.put(hbMsg.hb.name,true)
    }
  }

  /** The thread that periodically checks for missing HBs */
  override def run(): Unit = synchronized {
    HbMonitor.logger.debug("Checking reception of HBs")

    // Return the IDs of the alarms in the map that have not been updated
    // These IDs are set as properties in the alarms
    def faultyIds(m: MutableMap[String, Boolean]): List[String] = m.view.filterKeys(k => !m(k)).keys.toList

    // Update the passed alarm
    def updateAlarm(alarm: MonitorAlarm, faultyIds: List[String], priority: Alarm=Alarm.getSetDefault): Unit = {
      HbMonitor.logger.debug("Updating alarm {} with faulty ids {} and priority {}",
        alarm.id,
        faultyIds.mkString(","),
        priority)
      if (faultyIds.isEmpty) alarm.set(Alarm.cleared(), faultyIds.mkString(","))
      else alarm.set(priority, faultyIds.mkString(","))
    }

    updateAlarm(MonitorAlarm.PLUGIN_DEAD,faultyIds(pluginsHbMsgs),pluginsAlarmPriority)
    updateAlarm(MonitorAlarm.SUPERVISOR_DEAD,faultyIds(supervisorsHbMsgs),supervisorsAlarmPriority)
    updateAlarm(MonitorAlarm.CONVERTER_DEAD,faultyIds(convertersHbMsgs),convertersAlarmPriority)
    updateAlarm(MonitorAlarm.SINK_DEAD,faultyIds(sinksHbMsgs),sinksAlarmPriority)
    updateAlarm(MonitorAlarm.CLIENT_DEAD,faultyIds(clientsHbMsgs),clientsAlarmPriority)
    updateAlarm(MonitorAlarm.CORETOOL_DEAD,faultyIds(coreToolsHbMsgs),coreToolAlarmPriority)

    // reset the maps to be ready for the next iteration
    hbMaps.foreach(m => m.keySet.foreach(k => m(k)=false))
  }
}

/** Companion object */
object HbMonitor {
  /** The logger */
  val logger: Logger = IASLogger.getLogger(HbMonitor.getClass)
}
