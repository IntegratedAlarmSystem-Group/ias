package org.eso.ias.monitor.test

import java.util
import java.util.Collections
import java.util.concurrent.atomic.AtomicInteger

import com.typesafe.scalalogging.Logger
import org.eso.ias.logging.IASLogger
import org.eso.ias.monitor.{MonitorAlarm, MonitorAlarmsProducer}
import org.eso.ias.types.{Alarm, IASValue}
import org.scalatest.{BeforeAndAfterEach, FlatSpec}

import scala.collection.JavaConverters

/**
  * Check that the alarms are effectively sent to the producer
  *
  * This class checks the functioning of the [[org.eso.ias.monitor.MonitorAlarmsProducer]] with
  * a [[AlarmPublisherListener]] listener
  *
  */
class HbAlarmPublisherTest extends FlatSpec with AlarmPublisherListener with BeforeAndAfterEach {

  /** The number of times setUp has been invoked */
  val setUpExecutions = new AtomicInteger()

  /** The number of times tearDown has been invoked */
  val tearDownExecutions = new AtomicInteger()

  /** The number of times flush has been invoked */
  val flushExecutions = new AtomicInteger()

  /** The alrms published */
  val alarmsReceived: util.List[IASValue[_]] = Collections.synchronizedList(new util.ArrayList[IASValue[_]]())

  /**
    * The alrams have been published
    *
    * @param iasValues the alarms published
    */
  override def alarmsPublished(iasValues: Array[IASValue[_]]): Unit = {
    alarmsReceived.addAll(JavaConverters.asJavaCollection(iasValues))
    iasValues.foreach(value => MonitorAlarmsProducer.logger.info("Alarm received [{}]",value.toString))
  }

  /**
    * setUp has been called in the producer
    */
  override def setUpInvoked(): Unit = setUpExecutions.incrementAndGet()

  /**
    * flush has been called in the producer
    */
  override def flushUpInvoked(): Unit = flushExecutions.incrementAndGet()

  /**
    * tearDown been called in the producer
    */
  override def tearDownInvoked(): Unit = tearDownExecutions.incrementAndGet()

  /** The logger */
  val logger: Logger = IASLogger.getLogger(classOf[HbAlarmPublisherTest])

  /** Refresh rate (seconds) */
  val refreshRate: Long = 2L

  /**
    * The producer sends alarms to this object
    */
  val producer = new AlarmPublisherListenerImpl(this)

  /** The object to test */
  var monitorAlarm: MonitorAlarmsProducer = _

  override protected def beforeEach(): Unit = {
    super.beforeEach()
    flushExecutions.set(0)
    setUpExecutions.set(0)
    tearDownExecutions.set(0)
    alarmsReceived.clear()

    monitorAlarm = new MonitorAlarmsProducer(producer,refreshRate)
  }

  override protected def afterEach(): Unit = super.afterEach()


  behavior of "The MonitorAlarmProducer"

  it must "get the refresh rate" in {
    assert(monitorAlarm.refreshRate==refreshRate)
  }

  it must "initialize" in {
    monitorAlarm.start()
    assert(setUpExecutions.get()==1)

    monitorAlarm.shutdown()
  }

  it must "initialize twice" in {
    monitorAlarm.start()
    monitorAlarm.start()
    assert(setUpExecutions.get()==1)

    monitorAlarm.shutdown()
  }

  it must "shutdown" in {
    monitorAlarm.start()
    monitorAlarm.shutdown()
    assert(tearDownExecutions.get()==1)
  }

  it must "not shutdown twice" in {
    monitorAlarm.start()
    monitorAlarm.shutdown()
    monitorAlarm.shutdown()
    assert(tearDownExecutions.get()==1)
  }

  it must "not shutdown if not initialized" in {
    monitorAlarm.shutdown()
    assert(tearDownExecutions.get()==0)
  }

  it must "not initialize if already closed" in {
    monitorAlarm.start()
    monitorAlarm.shutdown()
    monitorAlarm.start()
    assert(tearDownExecutions.get()==1)
    assert(setUpExecutions.get()==1)
  }

  it must "send all alarms at startup" in {
    monitorAlarm.start()
    Thread.sleep(refreshRate*1000+500)
    assert(alarmsReceived.size()==MonitorAlarm.values.size)

    monitorAlarm.shutdown()
  }

  it must "not send alarm after shutdown" in {
    monitorAlarm.start()
    Thread.sleep(refreshRate*1000+500)
    assert(alarmsReceived.size()==MonitorAlarm.values.size)

    monitorAlarm.shutdown()
    Thread.sleep(refreshRate*1000+500)
    assert(alarmsReceived.size()==MonitorAlarm.values.size)

  }

  it must "send the property and alarm activation" in {
    MonitorAlarm.CLIENT_DEAD.set(Alarm.SET_CRITICAL,"id")
    monitorAlarm.start()
    Thread.sleep(refreshRate*1000+500)
    assert(alarmsReceived.size()==MonitorAlarm.values.size)

    monitorAlarm.shutdown()

    alarmsReceived.forEach( alarm => {
      val al = Alarm.valueOf(alarm.value.toString)
      if (alarm.id==MonitorAlarm.CLIENT_DEAD.id || alarm.id==MonitorAlarm.GLOBAL.id) {
        assert(al==Alarm.SET_CRITICAL)

        val p = alarm.props
        assert(p.isPresent)
        val props = p.get()
        val fIds = Option(props.get(MonitorAlarmsProducer.faultyIdsPropName))
        assert(fIds.isDefined)
        assert(fIds.get=="id")
      } else {
        assert(!al.isSet)
      }
    })
  }

  it must "periodically send alarms" in {
    monitorAlarm.start()
    Thread.sleep(3*refreshRate*1000+500)

    monitorAlarm.shutdown()

    assert(alarmsReceived.size()==3*MonitorAlarm.values().size)
  }

}
