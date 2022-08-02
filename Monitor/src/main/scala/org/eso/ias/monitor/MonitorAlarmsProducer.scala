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
  * @param publisher the publisher of alarms
  * @param refreshRate The refresh rate (seconds) to periodically send alarms
  * @param id The identifier of the Monitor
  */
class MonitorAlarmsProducer(val publisher: MonitorAlarmPublisher, val refreshRate: Long, id: String) extends Runnable {
  require(Option(publisher).isDefined,"Undefined publisher of alarms")
  require(refreshRate>0,"Invalid refresh rate "+refreshRate)
  require(Option(id).isDefined && id.nonEmpty,"Invalid empty Monitor identifier")

  /** The identifier of the Monitor tool */
  val monitorIdentifier =  new Identifier(id,IdentifierType.CORETOOL,None)

  /** The factory to generate the periodic thread */
  private val factory: ThreadFactory = new ThreadFactory {
    override def newThread(runnable: Runnable): Thread = {
      val t: Thread = new Thread(runnable,"HbMonitor-AlarmProducer-Thread")
      t.setDaemon(true)
      t
    }
  }

  /** The executor to periodically send the alarms */
  private val schedExecutorSvc: ScheduledExecutorService = Executors.newSingleThreadScheduledExecutor(factory)

  /** Empty set for the dependant IDs */
  private val emptySetOfDependant = new util.HashSet[String]()

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
        publisher.setUp()
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
        publisher.tearDown()
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
    val identifier = new Identifier(id,IdentifierType.IASIO,Some(monitorIdentifier))

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
      null,
       now, // production timestamp
      null,
      null,
      null, // converted timestamp
      null, // sent to BSDB timestamp
      null, // read from BSDB TStamp
      emptySetOfDependant, // dependentsFullrunningIds
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

      publisher.push(iasValues)
      publisher.flush()
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
