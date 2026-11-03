package org.eso.ias.basictypes.test

import org.scalatest.flatspec.AnyFlatSpec
import org.eso.ias.types.{IASTypes, IasValidity, OperationalMode, AlarmState, Priority}
import com.google.protobuf.Descriptors.DescriptorValidationException
import org.eso.ias.generated.protobuf.iastypes.IasTypes.{IASType => ProtoIASType}

import org.eso.ias.generated.protobuf.iastypes.IasTypes.{Validity => ProtoValidity}

import org.eso.ias.generated.protobuf.iastypes.IasTypes.{OperationalMode => ProtoOperationalMode}

import org.eso.ias.generated.protobuf.iastypes.IasTypes.{AlarmState => ProtoAlarmState}

import org.eso.ias.generated.protobuf.iastypes.IasTypes.{Priority => ProtoPriority}

class TestProtoTypes extends AnyFlatSpec {

  behavior of "Proto IASType"

  it must "round trip all 13 IASTypes values to proto and back" in {
    val allTypes = IASTypes.values.toList
    assert(allTypes.size == 13)
    allTypes.foreach { iasType =>
      val proto = IASTypeFrom(iasType)
      val back = IASTypeTo(proto)
      assert(back == iasType, s"$iasType -> $proto -> $back")
    }
  }

  behavior of "Proto Validity"

  it must "round trip all 2 Validity values to proto and back" in {
    val allValues = IasValidity.values.toList
    assert(allValues.size == 2)
    allValues.foreach { validity =>
      val proto = ValidityFrom(validity)
      val back = ValidityTo(proto)
      assert(back == validity, s"$validity -> $proto -> $back")
    }
  }

  behavior of "Proto OperationalMode"

  it must "round trip all 9 OperationalMode values to proto and back" in {
    val allModes = OperationalMode.values().toList
    assert(allModes.size == 9)
    allModes.foreach { mode =>
      val proto = OperationalModeFrom(mode)
      val back = OperationalModeTo(proto)
      assert(back == mode, s"$mode -> $proto -> $back")
    }
  }

  behavior of "Proto AlarmState"

  it must "round trip all 4 AlarmState values to proto and back" in {
    val allStates = AlarmState.values().toList
    assert(allStates.size == 4)
    allStates.foreach { state =>
      val proto = AlarmStateFrom(state)
      val back = AlarmStateTo(proto)
      assert(back == state, s"$state -> $proto -> $back")
    }
  }

  behavior of "Proto Priority"

  it must "round trip all 4 Priority values to proto and back" in {
    val allPrios = Priority.values().toList
    assert(allPrios.size == 4)
    allPrios.foreach { prio =>
      val proto = PriorityFrom(prio)
      val back = PriorityTo(proto)
      assert(back == prio, s"$prio -> $proto -> $back")
    }
  }

  // IASType conversion
  private def IASTypeFrom(t: IASTypes): ProtoIASType = t match {
    case IASTypes.LONG => ProtoIASType.IASTYPE_LONG
    case IASTypes.INT => ProtoIASType.IASTYPE_INT
    case IASTypes.SHORT => ProtoIASType.IASTYPE_SHORT
    case IASTypes.BYTE => ProtoIASType.IASTYPE_BYTE
    case IASTypes.DOUBLE => ProtoIASType.IASTYPE_DOUBLE
    case IASTypes.FLOAT => ProtoIASType.IASTYPE_FLOAT
    case IASTypes.BOOLEAN => ProtoIASType.IASTYPE_BOOLEAN
    case IASTypes.CHAR => ProtoIASType.IASTYPE_CHAR
    case IASTypes.STRING => ProtoIASType.IASTYPE_STRING
    case IASTypes.TIMESTAMP => ProtoIASType.IASTYPE_TIMESTAMP
    case IASTypes.ARRAYOFDOUBLES => ProtoIASType.IASTYPE_ARRAYOFDOUBLES
    case IASTypes.ARRAYOFLONGS => ProtoIASType.IASTYPE_ARRAYOFLONGS
    case IASTypes.ALARM => ProtoIASType.IASTYPE_ALARM
  }

  private def IASTypeTo(p: ProtoIASType): IASTypes = p match {
    case ProtoIASType.IASTYPE_LONG => IASTypes.LONG
    case ProtoIASType.IASTYPE_INT => IASTypes.INT
    case ProtoIASType.IASTYPE_SHORT => IASTypes.SHORT
    case ProtoIASType.IASTYPE_BYTE => IASTypes.BYTE
    case ProtoIASType.IASTYPE_DOUBLE => IASTypes.DOUBLE
    case ProtoIASType.IASTYPE_FLOAT => IASTypes.FLOAT
    case ProtoIASType.IASTYPE_BOOLEAN => IASTypes.BOOLEAN
    case ProtoIASType.IASTYPE_CHAR => IASTypes.CHAR
    case ProtoIASType.IASTYPE_STRING => IASTypes.STRING
    case ProtoIASType.IASTYPE_TIMESTAMP => IASTypes.TIMESTAMP
    case ProtoIASType.IASTYPE_ARRAYOFDOUBLES => IASTypes.ARRAYOFDOUBLES
    case ProtoIASType.IASTYPE_ARRAYOFLONGS => IASTypes.ARRAYOFLONGS
    case ProtoIASType.IASTYPE_ALARM => IASTypes.ALARM
    case _ => throw new IllegalStateException(s"Unrecognized proto IASType: $p")
  }

  // Validity conversion
  private def ValidityFrom(v: IasValidity): ProtoValidity = v match {
    case IasValidity.RELIABLE => ProtoValidity.VALIDITY_RELIABLE
    case IasValidity.UNRELIABLE => ProtoValidity.VALIDITY_UNRELIABLE
  }

  private def ValidityTo(p: ProtoValidity): IasValidity = p match {
    case ProtoValidity.VALIDITY_RELIABLE => IasValidity.RELIABLE
    case ProtoValidity.VALIDITY_UNRELIABLE => IasValidity.UNRELIABLE
    case _ => throw new IllegalStateException(s"Unrecognized proto Validity: $p")
  }

  // OperationalMode conversion
  private def OperationalModeFrom(m: OperationalMode): ProtoOperationalMode = m match {
    case OperationalMode.CLOSING => ProtoOperationalMode.OPERATIONALMODE_CLOSING
    case OperationalMode.DEGRADED => ProtoOperationalMode.OPERATIONALMODE_DEGRADED
    case OperationalMode.INITIALIZATION => ProtoOperationalMode.OPERATIONALMODE_INITIALIZATION
    case OperationalMode.MAINTENANCE => ProtoOperationalMode.OPERATIONALMODE_MAINTENANCE
    case OperationalMode.OPERATIONAL => ProtoOperationalMode.OPERATIONALMODE_OPERATIONAL
    case OperationalMode.SHUTTEDDOWN => ProtoOperationalMode.OPERATIONALMODE_SHUTTEDDOWN
    case OperationalMode.STARTUP => ProtoOperationalMode.OPERATIONALMODE_STARTUP
    case OperationalMode.UNKNOWN => ProtoOperationalMode.OPERATIONALMODE_UNKNOWN
    case OperationalMode.MALFUNCTIONING => ProtoOperationalMode.OPERATIONALMODE_MALFUNCTIONING
  }

  private def OperationalModeTo(p: ProtoOperationalMode): OperationalMode = p match {
    case ProtoOperationalMode.OPERATIONALMODE_CLOSING => OperationalMode.CLOSING
    case ProtoOperationalMode.OPERATIONALMODE_DEGRADED => OperationalMode.DEGRADED
    case ProtoOperationalMode.OPERATIONALMODE_INITIALIZATION => OperationalMode.INITIALIZATION
    case ProtoOperationalMode.OPERATIONALMODE_MAINTENANCE => OperationalMode.MAINTENANCE
    case ProtoOperationalMode.OPERATIONALMODE_OPERATIONAL => OperationalMode.OPERATIONAL
    case ProtoOperationalMode.OPERATIONALMODE_SHUTTEDDOWN => OperationalMode.SHUTTEDDOWN
    case ProtoOperationalMode.OPERATIONALMODE_STARTUP => OperationalMode.STARTUP
    case ProtoOperationalMode.OPERATIONALMODE_UNKNOWN => OperationalMode.UNKNOWN
    case ProtoOperationalMode.OPERATIONALMODE_MALFUNCTIONING => OperationalMode.MALFUNCTIONING
    case _ => throw new IllegalStateException(s"Unrecognized proto OperationalMode: $p")
  }

  // AlarmState conversion
  private def AlarmStateFrom(s: AlarmState): ProtoAlarmState = s match {
    case AlarmState.CLEAR_ACK => ProtoAlarmState.ALARMSTATE_CLEAR_ACK
    case AlarmState.CLEAR_UNACK => ProtoAlarmState.ALARMSTATE_CLEAR_UNACK
    case AlarmState.SET_UNACK => ProtoAlarmState.ALARMSTATE_SET_UNACK
    case AlarmState.SET_ACK => ProtoAlarmState.ALARMSTATE_SET_ACK
  }

  private def AlarmStateTo(p: ProtoAlarmState): AlarmState = p match {
    case ProtoAlarmState.ALARMSTATE_CLEAR_ACK => AlarmState.CLEAR_ACK
    case ProtoAlarmState.ALARMSTATE_CLEAR_UNACK => AlarmState.CLEAR_UNACK
    case ProtoAlarmState.ALARMSTATE_SET_UNACK => AlarmState.SET_UNACK
    case ProtoAlarmState.ALARMSTATE_SET_ACK => AlarmState.SET_ACK
    case _ => throw new IllegalStateException(s"Unrecognized proto AlarmState: $p")
  }

  // Priority conversion
  private def PriorityFrom(p: Priority): ProtoPriority = p match {
    case Priority.LOW => ProtoPriority.PRIORITY_LOW
    case Priority.MEDIUM => ProtoPriority.PRIORITY_MEDIUM
    case Priority.HIGH => ProtoPriority.PRIORITY_HIGH
    case Priority.CRITICAL => ProtoPriority.PRIORITY_CRITICAL
  }

  private def PriorityTo(p: ProtoPriority): Priority = p match {
    case ProtoPriority.PRIORITY_LOW => Priority.LOW
    case ProtoPriority.PRIORITY_MEDIUM => Priority.MEDIUM
    case ProtoPriority.PRIORITY_HIGH => Priority.HIGH
    case ProtoPriority.PRIORITY_CRITICAL => Priority.CRITICAL
    case _ => throw new IllegalStateException(s"Unrecognized proto Priority: $p")
  }
}