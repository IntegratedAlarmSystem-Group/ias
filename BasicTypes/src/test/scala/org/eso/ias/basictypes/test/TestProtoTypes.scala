package org.eso.ias.basictypes.test

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import org.eso.ias.types.*
import org.eso.ias.types.ProtoUtil
import org.eso.ias.generated.protobuf.iastypes.IasTypes

class TestProtoTypes extends AnyFlatSpec with Matchers {

  // ---- Enum round-trip tests (delegate to ProtoUtil) ----

  behavior of "Proto IASType"

  it must "round trip all 13 IASTypes values to proto and back" in {
    val allTypes = IASTypes.values.toList
    allTypes.size shouldBe 13
    allTypes.foreach { iasType =>
      val proto = ProtoUtil.IASTypeFrom(iasType)
      val back = ProtoUtil.IASTypeTo(proto)
      back shouldBe iasType
    }
  }

  behavior of "Proto Validity"

  it must "round trip all 2 Validity values to proto and back" in {
    val allValues = IasValidity.values.toList
    allValues.size shouldBe 2
    allValues.foreach { v =>
      val proto = ProtoUtil.ValidityFrom(v)
      val back = ProtoUtil.ValidityTo(proto)
      back shouldBe v
    }
  }

  behavior of "Proto OperationalMode"

  it must "round trip all 9 OperationalMode values to proto and back" in {
    val allModes = OperationalMode.values().toList
    allModes.size shouldBe 9
    allModes.foreach { m =>
      val proto = ProtoUtil.OperationalModeFrom(m)
      val back = ProtoUtil.OperationalModeTo(proto)
      back shouldBe m
    }
  }

  behavior of "Proto AlarmState"

  it must "round trip all 4 AlarmState values to proto and back" in {
    val allStates = AlarmState.values().toList
    allStates.size shouldBe 4
    allStates.foreach { s =>
      val proto = ProtoUtil.AlarmStateFrom(s)
      val back = ProtoUtil.AlarmStateTo(proto)
      back shouldBe s
    }
  }

  behavior of "Proto Priority"

  it must "round trip all 4 Priority values to proto and back" in {
    val allPrios = Priority.values().toList
    allPrios.size shouldBe 4
    allPrios.foreach { p =>
      val proto = ProtoUtil.PriorityFrom(p)
      val back = ProtoUtil.PriorityTo(proto)
      back shouldBe p
    }
  }

  // ---- Alarm round-trip ----

  behavior of "Proto Alarm"

  it must "round trip Alarm values through proto" in {
    val alarms = List(
      Alarm.getInitialAlarmState(),
      Alarm.getInitialAlarmState(Priority.CRITICAL),
      Alarm.getInitialAlarmState().set(),
      Alarm.getInitialAlarmState().set().increasePriority(),
      Alarm.getInitialAlarmState().set().clear()
    )
    alarms.foreach { alarm =>
      val proto = ProtoUtil.alarmFrom(alarm)
      val back = ProtoUtil.alarmTo(proto)
      back shouldBe alarm
    }
  }

  // ---- Value round-trip (all 13 IAS types) ----

  behavior of "Proto Value"

  private def roundTripValue(value: Any, iasType: IASTypes): Unit = {
    val proto = ProtoUtil.valueFrom(value, iasType)
    val back = ProtoUtil.valueTo(proto, iasType)

    iasType match {
      case IASTypes.LONG =>
        back.asInstanceOf[Long] shouldBe value.asInstanceOf[Long]
      case IASTypes.INT =>
        back.asInstanceOf[Int] shouldBe value.asInstanceOf[Int]
      case IASTypes.SHORT =>
        back.asInstanceOf[Short] shouldBe value.asInstanceOf[Short]
      case IASTypes.BYTE =>
        back.asInstanceOf[Byte] shouldBe value.asInstanceOf[Byte]
      case IASTypes.DOUBLE =>
        back.asInstanceOf[Double] shouldBe value.asInstanceOf[Double]
      case IASTypes.FLOAT =>
        back.asInstanceOf[Float] shouldBe value.asInstanceOf[Float]
      case IASTypes.BOOLEAN =>
        back.asInstanceOf[Boolean] shouldBe value.asInstanceOf[Boolean]
      case IASTypes.CHAR =>
        back.asInstanceOf[Char] shouldBe value.asInstanceOf[Char]
      case IASTypes.STRING =>
        back.asInstanceOf[String] shouldBe value.asInstanceOf[String]
      case IASTypes.TIMESTAMP =>
        back.asInstanceOf[java.lang.Long] shouldBe value.asInstanceOf[java.lang.Long]
      case IASTypes.ARRAYOFDOUBLES =>
        val orig = value.asInstanceOf[NumericArray]
        val res = back.asInstanceOf[NumericArray]
        res.numericArrayType shouldBe NumericArray.NumericArrayType.DOUBLE
        res.size() shouldBe orig.size()
        for (i <- 0 until orig.size())
          res.get(i).doubleValue() shouldBe orig.get(i).doubleValue()
      case IASTypes.ARRAYOFLONGS =>
        val orig = value.asInstanceOf[NumericArray]
        val res = back.asInstanceOf[NumericArray]
        res.numericArrayType shouldBe NumericArray.NumericArrayType.LONG
        res.size() shouldBe orig.size()
        for (i <- 0 until orig.size())
          res.get(i).longValue() shouldBe orig.get(i).longValue()
      case IASTypes.ALARM =>
        back.asInstanceOf[Alarm] shouldBe value.asInstanceOf[Alarm]
    }
  }

  it must "round trip LONG" in {
    roundTripValue(42L, IASTypes.LONG)
    roundTripValue(0L, IASTypes.LONG)
    roundTripValue(-100L, IASTypes.LONG)
  }

  it must "round trip INT with narrowing from int64" in {
    roundTripValue(Integer.valueOf(42), IASTypes.INT)
    roundTripValue(Integer.valueOf(0), IASTypes.INT)
    roundTripValue(Integer.valueOf(-100), IASTypes.INT)
  }

  it must "round trip SHORT with narrowing from int64" in {
    roundTripValue(Int.box(42).shortValue, IASTypes.SHORT)
    roundTripValue(Int.box(0).shortValue, IASTypes.SHORT)
    roundTripValue(Int.box(-100).shortValue, IASTypes.SHORT)
  }

  it must "round trip BYTE with narrowing from int64" in {
    roundTripValue(Int.box(42).byteValue, IASTypes.BYTE)
    roundTripValue(Int.box(0).byteValue, IASTypes.BYTE)
    roundTripValue(Int.box(-100).byteValue, IASTypes.BYTE)
  }

  it must "round trip DOUBLE" in {
    roundTripValue(3.14, IASTypes.DOUBLE)
    roundTripValue(0.0, IASTypes.DOUBLE)
    roundTripValue(-1.5, IASTypes.DOUBLE)
  }

  it must "round trip FLOAT with narrowing from double" in {
    roundTripValue(3.14f, IASTypes.FLOAT)
    roundTripValue(0f, IASTypes.FLOAT)
    roundTripValue(-1.5f, IASTypes.FLOAT)
  }

  it must "round trip BOOLEAN" in {
    roundTripValue(true, IASTypes.BOOLEAN)
    roundTripValue(false, IASTypes.BOOLEAN)
  }

  it must "round trip CHAR as string" in {
    roundTripValue('A', IASTypes.CHAR)
    roundTripValue('z', IASTypes.CHAR)
    roundTripValue('\u0000', IASTypes.CHAR)
  }

  it must "round trip STRING" in {
    roundTripValue("hello", IASTypes.STRING)
    roundTripValue("", IASTypes.STRING)
    roundTripValue("with spaces and special chars @#$", IASTypes.STRING)
  }

  it must "round trip TIMESTAMP as int64" in {
    val ts = System.currentTimeMillis()
    roundTripValue(ts, IASTypes.TIMESTAMP)
    roundTripValue(0L, IASTypes.TIMESTAMP)
  }

  it must "round trip ARRAYOFDOUBLES" in {
    val empty = new NumericArray(NumericArray.NumericArrayType.DOUBLE)
    roundTripValue(empty, IASTypes.ARRAYOFDOUBLES)

    val arr = NumericArray.valueOf(NumericArray.NumericArrayType.DOUBLE, "1.1, 2.2, 3.3")
    roundTripValue(arr, IASTypes.ARRAYOFDOUBLES)
  }

  it must "round trip ARRAYOFLONGS" in {
    val empty = new NumericArray(NumericArray.NumericArrayType.LONG)
    roundTripValue(empty, IASTypes.ARRAYOFLONGS)

    val arr = NumericArray.valueOf(NumericArray.NumericArrayType.LONG, "1, 2, 3, 100")
    roundTripValue(arr, IASTypes.ARRAYOFLONGS)
  }

  it must "round trip ALARM" in {
    val alarm = Alarm.getInitialAlarmState().set().increasePriority()
    roundTripValue(alarm, IASTypes.ALARM)

    val clearAlarm = Alarm.getInitialAlarmState(Priority.LOW)
    roundTripValue(clearAlarm, IASTypes.ALARM)
  }

   it must "throw when value case is not set" in {
    val emptyValue = IasTypes.Value.newBuilder().build()
    assertThrows[IllegalStateException] {
      ProtoUtil.valueTo(emptyValue, IASTypes.LONG)
    }
  }
}
