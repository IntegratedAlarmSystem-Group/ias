package org.eso.ias.monitor

import java.util
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.{Executors, ScheduledExecutorService, ThreadFactory, TimeUnit}

import com.typesafe.scalalogging.Logger
import org.eso.ias.logging.IASLogger
import org.eso.ias.monitor.alarmpublisher.MonitorAlarmPublisher
import org.eso.ias.types._

/**
  * The alarms producer that periodically publishes
  * the alarms defined in [[MonitorAlarm]].
  *
  * In this version the alarms are sent to the Kafka core topic as [[org.eso.ias.types.IASValue]].
  *
  * TODO: send the alarms directly to the webserver so that it works even if kafka is down
  *
  * TODO: this process sends IASValue as if they have been produiced by a plugin
  *       It can be improved as the monitor is not a pluging neither a DASU
  *
  * @param brokers Kafka broker for the producer
  * @param id The id of the consumer
  * @param refreshRate The refresh rate (seconds) to peridically send alarms
  */
class MonitorAlarmsProducer(val producer: MonitorAlarmPublisher, val refreshRate: Long) extends Runnable {
  require(Option(producer).isDefined,"Undefined plublisher of alarms")
  require(refreshRate>0,"Invalid refresh rate "+refreshRate)

  /** The factory to generate the periodic thread */
  private val factory: ThreadFactory = new ThreadFactory {
    override def newThread(runnable: Runnable): Thread = {
      val t: Thread = new Thread(runnable,"HbMonitor-AlarmProducer-Thread")
      t.setDaemon(true)
      t
    }
  }

  /** The executor to periodically sends the alarms */
  private val schedExecutorSvc: ScheduledExecutorService = Executors.newSingleThreadScheduledExecutor(factory)

  /**
    * The identifier of the monitored system
    *
    * Needed to simulate submisison by plugin
    */
  private val monSystemIdentifier = new Identifier("IAS",IdentifierType.MONITORED_SOFTWARE_SYSTEM,None)

  /**
    * The identifier of the plugin
    *
    * Needed to simulate submisison by plugin
    */
  private val pluginIdentifier = new Identifier("IasMonitorPlugin",IdentifierType.PLUGIN,Some(monSystemIdentifier))

  /**
    * The identifier of the converter
    *
    * Needed to simulate submisison by plugin
    */
  private val converterIdentifier = new Identifier("SimulatedConverter",IdentifierType.CONVERTER,Some(pluginIdentifier))

  /** Empty set for the dependant IDs */
  private val emptySet = new util.HashSet[String]()

  /** Empty property map */
  private val emptyProps = new util.HashMap[String,String]()

  /** Set if the object has been started */
  private val started = new AtomicBoolean(false)

  /** Set if the object has been closed */
  private val closed = new AtomicBoolean(false)

  /** Start sending alarms */
  def start(): Unit = {
    val alreadyStarted = started.getAndSet(true)
    val alreadyClosed = closed.get

    (alreadyStarted, alreadyClosed) match {
      case (false, false) =>
        MonitorAlarmsProducer.logger.debug("Starting up the producer")
        producer.setUp()
        MonitorAlarmsProducer.logger.debug("Starting up the thread at a rate of {} secs",refreshRate)
        schedExecutorSvc.scheduleAtFixedRate(this,refreshRate,refreshRate,TimeUnit.SECONDS)
        MonitorAlarmsProducer.logger.info("Started")
      case (_, true) => MonitorAlarmsProducer.logger.error("Cannot be started: already closed")
      case (true, _) => MonitorAlarmsProducer.logger.error("Already started")
    }

  }

  /** Stops sending alarms and frees resources */
  def shutdown(): Unit = {
    val alreadyStarted = started.get()
    val alreadyShutDown = closed.getAndSet(true)

    (alreadyStarted, alreadyShutDown) match {
      case (_, true) => MonitorAlarmsProducer.logger.warn("Already shut down")
      case (false, false) => MonitorAlarmsProducer.logger.warn("Cannot shut down: not initialized")
      case (true, false) =>
        MonitorAlarmsProducer.logger.debug("Shutting down")
        MonitorAlarmsProducer.logger.debug("Stopping thread to send alarms")
        schedExecutorSvc.shutdown()
        MonitorAlarmsProducer.logger.debug("Closing the Kafka IASIOs producer")
        producer.tearDown()
        MonitorAlarmsProducer.logger.info("Shut down")
    }
  }

  /**
    * Build the IASValue to send to the BSDB
    *
    * @param id The identifier of the alarm
    * @param value the alarm state and priority
    * @param prop property with the list of faulty IDs
    * @return the IASValue to send to the BSDB
    */
  def buildIasValue(id: String, value: Alarm, prop: String): IASValue[_] = {

    val identifier = new Identifier(id,IdentifierType.IASIO,Some(converterIdentifier))

    val now = System.currentTimeMillis()

    val props =
      if (Option(prop).isEmpty || prop.isEmpty)
        emptyProps
    else {
      val p = new util.HashMap[String,String]()
        p.put(MonitorAlarmsProducer.faultyIdsPropName,prop)
        p
    }

    IASValue.build(
      value,
      OperationalMode.OPERATIONAL,
      IasValidity.RELIABLE,
      identifier.fullRunningID,
      IASTypes.ALARM,
      now-4,
      now-3,
      now-2,
      now-1,
      now,
      null, // sentToBsdbTStamp
      null, // readFromBsdbTStamp
      null, // dasuProductionTStamp
      emptySet, // dependentsFullrunningIds
      props) // properties

  }

  /** Periodically sends the alarms defined in [[org.eso.ias.monitor.MonitorAlarm]] */
  override def run(): Unit = {
    MonitorAlarmsProducer.logger.debug("Sending alarms to the BSDB")
    if (!closed.get()) {
      val iasValues = MonitorAlarm.values().map( a => {
        MonitorAlarmsProducer.logger.debug("Sending {} with activation status {} and faulty IDs {}",
          a.id,
          a.getAlarm,
          a.getProperties)
        buildIasValue(a.id,a.getAlarm,a.getProperties)
      })

      producer.push(iasValues)
      producer.flush()
      MonitorAlarmsProducer.logger.debug("Sent {} alarms to the BSDB",iasValues.length)
    }

  }
}

/** Companion object */
object MonitorAlarmsProducer {
  /** The logger */
  val logger: Logger = IASLogger.getLogger(MonitorAlarmsProducer.getClass)

  /** The name of the property with the fault IDs */
  val faultyIdsPropName = "faultyIDs"
}
