package org.eso.ias.monitor.alarmpublisher

import org.eso.ias.types.IASValue

/**
  * The trait of the alarm publisher.
  * Implementers of this trait allows to publish alarms in different ways like for example
  * the BSDB, webosckets or other means.
  *
  * [[MonitorAlarmPublisher]] decouples the implemnters from the sends ([[org.eso.ias.monitor.MonitorAlarmsProducer]]
  * so that different implementations cab be easily used
  */
trait MonitorAlarmPublisher {

  /** Preparae the publisher to send data */
  def setUp()

  /** Closes the publisher: no alarms will be sent afetr closing */
  def tearDown()

  /** Send the passed values */
  def push(iasio: Array[IASValue[_]])

  /** Flush the values to force immediate sending */
  def flush()

}
