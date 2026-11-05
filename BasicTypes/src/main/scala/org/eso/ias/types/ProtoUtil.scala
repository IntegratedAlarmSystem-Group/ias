package org.eso.ias.types

import org.eso.ias.generated.protobuf.iastypes.IasTypes.{
  IASType => ProtoIASType,
  Validity => ProtoValidity,
  OperationalMode => ProtoOperationalMode,
  AlarmState => ProtoAlarmState,
  Priority => ProtoPriority
}
import org.eso.ias.generated.protobuf.iastypes.IasTypes
import scala.jdk.CollectionConverters._

/**
  * Protobuf conversion utilities for basic ASCE types.
  *
  * This object handles the serialization and deserialization of the
  * fundamental building blocks: type enums (IASType, Validity, OperationalMode,
  * AlarmState, Priority), Alarm, and the polymorphic Value (oneof) message.
  *
  * ASCE-specific messages (IasIO, IasioInfo) are converted by
  * [[org.eso.ias.asce.transfer.ProtoUtil]] in CompElement, which delegates
  * basic conversions to this object.
  */
object ProtoUtil {

  // ---- IASType ----

  /** Converts a domain [[IASTypes]] enum constant to its protobuf counterpart. */
   def IASTypeFrom(t: IASTypes): ProtoIASType = t match {
     case IASTypes.LONG => IasTypes.IASType.LONG
     case IASTypes.INT => IasTypes.IASType.INT
     case IASTypes.SHORT => IasTypes.IASType.SHORT
     case IASTypes.BYTE => IasTypes.IASType.BYTE
     case IASTypes.DOUBLE => IasTypes.IASType.DOUBLE
     case IASTypes.FLOAT => IasTypes.IASType.FLOAT
     case IASTypes.BOOLEAN => IasTypes.IASType.BOOLEAN
     case IASTypes.CHAR => IasTypes.IASType.CHAR
     case IASTypes.STRING => IasTypes.IASType.STRING
     case IASTypes.TIMESTAMP => IasTypes.IASType.TIMESTAMP
     case IASTypes.ARRAYOFDOUBLES => IasTypes.IASType.ARRAYOFDOUBLES
     case IASTypes.ARRAYOFLONGS => IasTypes.IASType.ARRAYOFLONGS
     case IASTypes.ALARM => IasTypes.IASType.ALARM
   }

   /** Converts a protobuf IASType back to the domain [[IASTypes]] enum. */
   def IASTypeTo(p: ProtoIASType): IASTypes = p match {
     case IasTypes.IASType.LONG => IASTypes.LONG
     case IasTypes.IASType.INT => IASTypes.INT
     case IasTypes.IASType.SHORT => IASTypes.SHORT
     case IasTypes.IASType.BYTE => IASTypes.BYTE
     case IasTypes.IASType.DOUBLE => IASTypes.DOUBLE
     case IasTypes.IASType.FLOAT => IASTypes.FLOAT
     case IasTypes.IASType.BOOLEAN => IASTypes.BOOLEAN
     case IasTypes.IASType.CHAR => IASTypes.CHAR
     case IasTypes.IASType.STRING => IASTypes.STRING
     case IasTypes.IASType.TIMESTAMP => IASTypes.TIMESTAMP
     case IasTypes.IASType.ARRAYOFDOUBLES => IASTypes.ARRAYOFDOUBLES
     case IasTypes.IASType.ARRAYOFLONGS => IASTypes.ARRAYOFLONGS
     case IasTypes.IASType.ALARM => IASTypes.ALARM
     case _ => throw new IllegalStateException(s"Unrecognized proto IASType: $p")
   }

  // ---- Validity ----

  /** Converts a domain [[IasValidity]] to its protobuf counterpart. */
  def ValidityFrom(v: IasValidity): ProtoValidity = v match {
    case IasValidity.RELIABLE => IasTypes.Validity.RELIABLE
     case IasValidity.UNRELIABLE => IasTypes.Validity.UNRELIABLE
   }

   /** Converts a protobuf Validity back to the domain [[IasValidity]]. */
   def ValidityTo(p: ProtoValidity): IasValidity = p match {
     case IasTypes.Validity.RELIABLE => IasValidity.RELIABLE
     case IasTypes.Validity.UNRELIABLE => IasValidity.UNRELIABLE
    case _ => throw new IllegalStateException(s"Unrecognized proto Validity: $p")
  }

  // ---- OperationalMode ----

  /** Converts a domain [[OperationalMode]] to its protobuf counterpart. */
  def OperationalModeFrom(m: OperationalMode): ProtoOperationalMode = m match {
    case OperationalMode.CLOSING => IasTypes.OperationalMode.CLOSING
     case OperationalMode.DEGRADED => IasTypes.OperationalMode.DEGRADED
     case OperationalMode.INITIALIZATION => IasTypes.OperationalMode.INITIALIZATION
     case OperationalMode.MAINTENANCE => IasTypes.OperationalMode.MAINTENANCE
     case OperationalMode.OPERATIONAL => IasTypes.OperationalMode.OPERATIONAL
     case OperationalMode.SHUTTEDDOWN => IasTypes.OperationalMode.SHUTTEDDOWN
     case OperationalMode.STARTUP => IasTypes.OperationalMode.STARTUP
     case OperationalMode.UNKNOWN => IasTypes.OperationalMode.UNKNOWN
     case OperationalMode.MALFUNCTIONING => IasTypes.OperationalMode.MALFUNCTIONING
   }

   /** Converts a protobuf OperationalMode back to the domain enum. */
   def OperationalModeTo(p: ProtoOperationalMode): OperationalMode = p match {
     case IasTypes.OperationalMode.CLOSING => OperationalMode.CLOSING
     case IasTypes.OperationalMode.DEGRADED => OperationalMode.DEGRADED
     case IasTypes.OperationalMode.INITIALIZATION => OperationalMode.INITIALIZATION
     case IasTypes.OperationalMode.MAINTENANCE => OperationalMode.MAINTENANCE
     case IasTypes.OperationalMode.OPERATIONAL => OperationalMode.OPERATIONAL
     case IasTypes.OperationalMode.SHUTTEDDOWN => OperationalMode.SHUTTEDDOWN
     case IasTypes.OperationalMode.STARTUP => OperationalMode.STARTUP
     case IasTypes.OperationalMode.UNKNOWN => OperationalMode.UNKNOWN
     case IasTypes.OperationalMode.MALFUNCTIONING => OperationalMode.MALFUNCTIONING
    case _ => throw new IllegalStateException(s"Unrecognized proto OperationalMode: $p")
  }

  // ---- AlarmState ----

  /** Converts a domain [[AlarmState]] to its protobuf counterpart. */
  def AlarmStateFrom(s: AlarmState): ProtoAlarmState = s match {
    case AlarmState.CLEAR_ACK => IasTypes.AlarmState.CLEAR_ACK
     case AlarmState.CLEAR_UNACK => IasTypes.AlarmState.CLEAR_UNACK
     case AlarmState.SET_UNACK => IasTypes.AlarmState.SET_UNACK
     case AlarmState.SET_ACK => IasTypes.AlarmState.SET_ACK
   }

   /** Converts a protobuf AlarmState back to the domain enum. */
   def AlarmStateTo(p: ProtoAlarmState): AlarmState = p match {
     case IasTypes.AlarmState.CLEAR_ACK => AlarmState.CLEAR_ACK
     case IasTypes.AlarmState.CLEAR_UNACK => AlarmState.CLEAR_UNACK
     case IasTypes.AlarmState.SET_UNACK => AlarmState.SET_UNACK
     case IasTypes.AlarmState.SET_ACK => AlarmState.SET_ACK
    case _ => throw new IllegalStateException(s"Unrecognized proto AlarmState: $p")
  }

  // ---- Priority ----

  /** Converts a domain [[Priority]] to its protobuf counterpart. */
  def PriorityFrom(p: Priority): ProtoPriority = p match {
    case Priority.LOW => IasTypes.Priority.LOW
     case Priority.MEDIUM => IasTypes.Priority.MEDIUM
     case Priority.HIGH => IasTypes.Priority.HIGH
     case Priority.CRITICAL => IasTypes.Priority.CRITICAL
   }

   /** Converts a protobuf Priority back to the domain enum. */
   def PriorityTo(p: ProtoPriority): Priority = p match {
     case IasTypes.Priority.LOW => Priority.LOW
     case IasTypes.Priority.MEDIUM => Priority.MEDIUM
     case IasTypes.Priority.HIGH => Priority.HIGH
     case IasTypes.Priority.CRITICAL => Priority.CRITICAL
    case _ => throw new IllegalStateException(s"Unrecognized proto Priority: $p")
  }

  // ---- Alarm ----

  /** Converts a domain [[Alarm]] to its protobuf representation. */
  def alarmFrom(domainAlarm: Alarm): IasTypes.Alarm = {
    IasTypes.Alarm.newBuilder()
      .setState(AlarmStateFrom(domainAlarm.alarmState))
      .setPriority(PriorityFrom(domainAlarm.priority))
      .build()
  }

  /** Converts a protobuf Alarm back to the domain [[Alarm]] using `Alarm.valueOf`. */
  def alarmTo(proto: IasTypes.Alarm): Alarm = {
    val stateStr = AlarmStateTo(proto.getState).name()
    val prioStr = PriorityTo(proto.getPriority).name()
    Alarm.valueOf(stateStr + ":" + prioStr)
  }

  // ---- Value (oneof) ----

  /**
    * Converts a boxed value of the given IAS type into a protobuf `Value`
    * message. INT/SHORT/BYTE/TIMESTAMP are stored as `int64Value`, FLOAT as
    * `doubleValue`, CHAR as `stringValue`. Arrays and Alarm are converted
    * recursively.
    *
    * @param value the boxed value to serialize
    * @param iasType the IAS type determining which oneof field to set
    * @return the protobuf Value message
    */
  def valueFrom(value: Any, iasType: IASTypes): IasTypes.Value = {
    val builder = IasTypes.Value.newBuilder()
    iasType match {
      case IASTypes.LONG => builder.setInt64Value(value.asInstanceOf[java.lang.Long])
      case IASTypes.INT => builder.setInt64Value(value.asInstanceOf[java.lang.Integer].longValue())
      case IASTypes.SHORT => builder.setInt64Value(value.asInstanceOf[java.lang.Short].longValue())
      case IASTypes.BYTE => builder.setInt64Value(value.asInstanceOf[java.lang.Byte].longValue())
      case IASTypes.DOUBLE => builder.setDoubleValue(value.asInstanceOf[java.lang.Double])
      case IASTypes.FLOAT => builder.setDoubleValue(value.asInstanceOf[java.lang.Float].doubleValue())
      case IASTypes.BOOLEAN => builder.setBoolValue(value.asInstanceOf[java.lang.Boolean])
      case IASTypes.CHAR => builder.setStringValue(value.asInstanceOf[java.lang.Character].toString)
      case IASTypes.STRING => builder.setStringValue(value.asInstanceOf[String])
      case IASTypes.TIMESTAMP => builder.setInt64Value(value.asInstanceOf[java.lang.Long])
      case IASTypes.ARRAYOFDOUBLES =>
        val arr = value.asInstanceOf[NumericArray]
        val doubleList = new java.util.ArrayList[java.lang.Double]()
        arr.array.asScala.foreach(n => doubleList.add(n.doubleValue()))
        builder.setDoubleArrayValue(IasTypes.DoubleArray.newBuilder().addAllValues(doubleList))
      case IASTypes.ARRAYOFLONGS =>
        val arr = value.asInstanceOf[NumericArray]
        val longList = new java.util.ArrayList[java.lang.Long]()
        arr.array.asScala.foreach(n => longList.add(n.longValue()))
        builder.setInt64ArrayValue(IasTypes.Int64Array.newBuilder().addAllValues(longList))
      case IASTypes.ALARM =>
        builder.setAlarmValue(alarmFrom(value.asInstanceOf[Alarm]))
    }
    builder.build()
  }

  /**
    * Deserializes a protobuf `Value` message into a boxed Scala/Java value
    * according to the provided IAS type. Applies narrowing casts for INT,
    * SHORT, BYTE (from int64) and FLOAT (from double).
    *
    * @param proto the protobuf Value message
    * @param iasType the IAS type determining how to interpret the oneof
    * @return the boxed value appropriate for the IAS type
    * @throws IllegalStateException if the proto Value has no case set
    */
  def valueTo(proto: IasTypes.Value, iasType: IASTypes): Any = {
    if (proto.getValueCase == IasTypes.Value.ValueCase.VALUE_NOT_SET) {
      throw new IllegalStateException(s"Value not set for IASType $iasType")
    }
    iasType match {
      case IASTypes.LONG => proto.getInt64Value
      case IASTypes.INT => proto.getInt64Value.intValue()
      case IASTypes.SHORT => proto.getInt64Value.shortValue()
      case IASTypes.BYTE => proto.getInt64Value.byteValue()
      case IASTypes.DOUBLE => proto.getDoubleValue
      case IASTypes.FLOAT => proto.getDoubleValue.floatValue()
      case IASTypes.BOOLEAN => proto.getBoolValue
      case IASTypes.CHAR => proto.getStringValue.charAt(0)
      case IASTypes.STRING => proto.getStringValue
      case IASTypes.TIMESTAMP => proto.getInt64Value
      case IASTypes.ARRAYOFDOUBLES =>
        val doubleList = new java.util.ArrayList[java.lang.Double]()
        proto.getDoubleArrayValue.getValuesList.asScala.foreach(d => doubleList.add(d: java.lang.Double))
        new NumericArray(NumericArray.NumericArrayType.DOUBLE, doubleList)
      case IASTypes.ARRAYOFLONGS =>
        val longList = new java.util.ArrayList[java.lang.Long]()
        proto.getInt64ArrayValue.getValuesList.asScala.foreach(l => longList.add(l: java.lang.Long))
        new NumericArray(NumericArray.NumericArrayType.LONG, longList)
      case IASTypes.ALARM => alarmTo(proto.getAlarmValue)
    }
  }
}
