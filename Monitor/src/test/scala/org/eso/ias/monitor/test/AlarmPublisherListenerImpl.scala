package org.eso.ias.monitor.test

import org.eso.ias.monitor.alarmpublisher.MonitorAlarmPublisher
import org.eso.ias.types.IASValue

/** The listener of alarms sent to the producer */
trait AlarmPublisherListener {

  /**
    * The alrams have been published
    *
    * @param iasValues the alarms published
    */
  def alarmsPublished(iasValues: Array[IASValue[_]])

  /**
    * setUp has been called in the producer
    */
  def setUpInvoked()

  /**
    * flush has been called in the producer
    */
  def flushUpInvoked()

  /**
    * tearDown been called in the producer
    */
  def tearDownInvoked()
}

/**
  * Send alarms to the listener.
  *
  * This class is used for testing
  */
class AlarmPublisherListenerImpl(val listener: AlarmPublisherListener) extends MonitorAlarmPublisher {
  require(Option(listener).isDefined,"Undefined listener")

  /** Preparae the publisher to send data */
  override def setUp(): Unit = listener.setUpInvoked()

  /** Closes the publisher: no alarms will be sent afetr closing */
  override def tearDown(): Unit = listener.tearDownInvoked()

  /** Send the passed values */
  override def push(iasios: Array[IASValue[_]]): Unit = listener.alarmsPublished(iasios)

  /** Flush the values to force immediate sending */
  override def flush(): Unit = listener.flushUpInvoked()
}
