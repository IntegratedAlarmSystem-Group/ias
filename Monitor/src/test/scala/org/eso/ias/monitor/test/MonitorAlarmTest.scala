package org.eso.ias.monitor.test

import org.eso.ias.logging.IASLogger
import org.eso.ias.monitor.MonitorAlarm
import org.eso.ias.types.{Alarm, Priority}
import org.scalatest.flatspec.AnyFlatSpec

/**
  * Test the [[org.eso.ias.monitor.MonitorAlarm]], in particular the GLOBAL
  * multiplicity
  */
class MonitorAlarmTest extends AnyFlatSpec {

  /** The logger */
  val logger = IASLogger.getLogger(classOf[MonitorAlarmTest])

  /** All the MonitorAlarm alarms */
  val monitorAlarms: List[MonitorAlarm] = (MonitorAlarm.values).toList

  behavior of " The MonitorAlarm"

  it must "set/clear alarms" in {
    monitorAlarms.filterNot(_==MonitorAlarm.GLOBAL).foreach(alarm => {
      alarm.setPriority(Priority.CRITICAL)
      alarm.set("id")
      val returned = alarm.getAlarm
      val prop = alarm.getProperties
      assert(returned.isSet)
      assert(returned.priority==Priority.CRITICAL)
      assert(prop=="id")
    })

    monitorAlarms.filterNot(_==MonitorAlarm.GLOBAL).foreach(alarm => {
      alarm.clear()
      val returned = alarm.getAlarm
      val prop = alarm.getProperties
      assert(!returned.isSet)
      assert(prop.isEmpty)
    })
  }



  it must "not allow to set/clear GLOBAL" in {
    assertThrows[UnsupportedOperationException] {
      MonitorAlarm.GLOBAL.set("id")
    }
    assertThrows[UnsupportedOperationException] {
      MonitorAlarm.GLOBAL.clear()
    }
  }

  behavior of "MonitorAlarm.GLOBAL"

  it must "be a multiplicity alarm" in {
    // Clear all the alarms
    monitorAlarms.filterNot(_==MonitorAlarm.GLOBAL).foreach(_.clear())
    assert(!MonitorAlarm.GLOBAL.getAlarm.isSet)

    MonitorAlarm.CLIENT_DEAD.set("id")
    assert(MonitorAlarm.GLOBAL.getAlarm.isSet)
  }

  it must "get the higher priority of the alarms" in {
    // Clear all the alarms
    monitorAlarms.filterNot(_==MonitorAlarm.GLOBAL).foreach(_.clear())

    MonitorAlarm.CLIENT_DEAD.setPriority(Priority.LOW)
    MonitorAlarm.CLIENT_DEAD.set("id")
    assert(MonitorAlarm.GLOBAL.getAlarm.isSet)
    assert(MonitorAlarm.GLOBAL.getAlarm.priority==Priority.LOW)

    MonitorAlarm.CONVERTER_DEAD.setPriority(Priority.MEDIUM)
    MonitorAlarm.CONVERTER_DEAD.set("id")
    assert(MonitorAlarm.GLOBAL.getAlarm.isSet)
    assert(MonitorAlarm.GLOBAL.getAlarm.priority==Priority.MEDIUM)

    MonitorAlarm.PLUGIN_DEAD.setPriority(Priority.HIGH)
    MonitorAlarm.PLUGIN_DEAD.set("id")
    assert(MonitorAlarm.GLOBAL.getAlarm.isSet)
    assert(MonitorAlarm.GLOBAL.getAlarm.priority==Priority.HIGH)

    MonitorAlarm.SUPERVISOR_DEAD.setPriority(Priority.CRITICAL)
    MonitorAlarm.SUPERVISOR_DEAD.set("id")
    assert(MonitorAlarm.SUPERVISOR_DEAD.getAlarm.isSet)
    assert(MonitorAlarm.SUPERVISOR_DEAD.getAlarm.priority==Priority.CRITICAL)


    assert(MonitorAlarm.GLOBAL.getAlarm.isSet)
    assert(MonitorAlarm.GLOBAL.getAlarm.priority==Priority.CRITICAL)
  }

  it must "get the IDs of the faulty alarms" in {
    // Clear all the alarms
    monitorAlarms.filterNot(_==MonitorAlarm.GLOBAL).foreach(_.clear())
    assert(MonitorAlarm.GLOBAL.getProperties.isEmpty)

    MonitorAlarm.CLIENT_DEAD.setPriority(Priority.LOW)
    MonitorAlarm.CLIENT_DEAD.set("idClient1,idClient2")
    assert(MonitorAlarm.GLOBAL.getProperties=="idClient1,idClient2")

    MonitorAlarm.CONVERTER_DEAD.setPriority(Priority.MEDIUM)
    MonitorAlarm.CONVERTER_DEAD.set("idConverter")
    MonitorAlarm.PLUGIN_DEAD.setPriority(Priority.HIGH)
    MonitorAlarm.PLUGIN_DEAD.set("idPlugin1,idPlugin2,idPlugin3")
    MonitorAlarm.SUPERVISOR_DEAD.setPriority(Priority.CRITICAL)
    MonitorAlarm.SUPERVISOR_DEAD.set("idSuperv1,idSuperv2")
    MonitorAlarm.SINK_DEAD.set("idSink")

    // The Ids of all the faulty alarms
    val faultyIds = "idClient1,idClient2,idConverter,idPlugin1,idPlugin2,idPlugin3,idSuperv1,idSuperv2,idSink".
      split(",").
      toSet

    assert(MonitorAlarm.GLOBAL.getProperties.split(",").toSet==faultyIds)
  }

}
