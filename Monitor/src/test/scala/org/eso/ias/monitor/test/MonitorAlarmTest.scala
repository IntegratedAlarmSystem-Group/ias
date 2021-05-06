package org.eso.ias.monitor.test

import org.eso.ias.logging.IASLogger
import org.eso.ias.monitor.MonitorAlarm
import org.eso.ias.types.Alarm
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
      alarm.set(Alarm.SET_CRITICAL,"id")
      val returned = alarm.getAlarm
      val prop = alarm.getProperties
      assert(returned==Alarm.SET_CRITICAL)
      assert(prop=="id")
    })

    monitorAlarms.filterNot(_==MonitorAlarm.GLOBAL).foreach(alarm => {
      alarm.clear()
      val returned = alarm.getAlarm
      val prop = alarm.getProperties
      assert(returned==Alarm.cleared())
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

    MonitorAlarm.CLIENT_DEAD.set(Alarm.SET_LOW,"id")
    assert(MonitorAlarm.GLOBAL.getAlarm==Alarm.SET_LOW)

    MonitorAlarm.CONVERTER_DEAD.set(Alarm.SET_MEDIUM,"id")
    assert(MonitorAlarm.GLOBAL.getAlarm==Alarm.SET_MEDIUM)

    MonitorAlarm.PLUGIN_DEAD.set(Alarm.SET_HIGH,"id")
    assert(MonitorAlarm.GLOBAL.getAlarm==Alarm.SET_HIGH)

    MonitorAlarm.SUPERVISOR_DEAD.set(Alarm.SET_CRITICAL,"id")
    assert(MonitorAlarm.GLOBAL.getAlarm==Alarm.SET_CRITICAL)
  }

  it must "get the IDs of the faulty alarms" in {
    // Clear all the alarms
    monitorAlarms.filterNot(_==MonitorAlarm.GLOBAL).foreach(_.clear())
    assert(MonitorAlarm.GLOBAL.getProperties.isEmpty)

    MonitorAlarm.CLIENT_DEAD.set(Alarm.SET_LOW,"idClient1,idClient2")
    assert(MonitorAlarm.GLOBAL.getProperties=="idClient1,idClient2")

    MonitorAlarm.CONVERTER_DEAD.set(Alarm.SET_MEDIUM,"idConverter")
    MonitorAlarm.PLUGIN_DEAD.set(Alarm.SET_HIGH,"idPlugin1,idPlugin2,idPlugin3")
    MonitorAlarm.SUPERVISOR_DEAD.set(Alarm.SET_CRITICAL,"idSuperv1,idSuperv2")
    MonitorAlarm.SINK_DEAD.set(Alarm.SET_MEDIUM,"idSink")

    // The Ids of all the faulty alarms
    val faultyIds = "idClient1,idClient2,idConverter,idPlugin1,idPlugin2,idPlugin3,idSuperv1,idSuperv2,idSink".
      split(",").
      toSet

    assert(MonitorAlarm.GLOBAL.getProperties.split(",").toSet==faultyIds)
  }

}
