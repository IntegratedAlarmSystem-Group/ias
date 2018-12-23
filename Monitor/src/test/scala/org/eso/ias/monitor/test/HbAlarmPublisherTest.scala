package org.eso.ias.monitor.test

import java.util
import java.util.Collections
import java.util.concurrent.atomic.AtomicInteger

import com.typesafe.scalalogging.Logger
import org.eso.ias.logging.IASLogger
import org.eso.ias.monitor.MonitorAlarmsProducer
import org.eso.ias.types.IASValue
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
    iasValues.foreach(value => {
      val propStr = if (value.props.isPresent) {
        val map = value.props.get()
        val propValueOpt = Option(map.get(MonitorAlarmsProducer.faultyIdsPropName))
        propValueOpt.getOrElse("")
      } else ""
      MonitorAlarmsProducer.logger.info("Alarm received {} with value {} and properties {}",
        value.id,value.value.toString,value.props,propStr)
    })
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
  val refreshRate: Long = 1L

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

}
