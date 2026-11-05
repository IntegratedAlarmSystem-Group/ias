package org.eso.ias.asce.test

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import org.eso.ias.types.*
import org.eso.ias.types.ProtoUtil as BasicProtoUtil
import org.eso.ias.asce.transfer.{IasioInfo, ProtoUtil}
import org.eso.ias.generated.protobuf.iastypes.IasTypes
import org.eso.ias.generated.protobuf.tfservice.PytfService

class TestProtobufIasIO extends AnyFlatSpec with Matchers {

  private def mkIasIOId(name: String): Identifier = {
    val supervId = new Identifier("SupervId", IdentifierType.SUPERVISOR, None)
    val dasuId = new Identifier("dasuId", IdentifierType.DASU, Option(supervId))
    val asceId = new Identifier("asceId", IdentifierType.ASCE, Option(dasuId))
    new Identifier(name, IdentifierType.IASIO, Option(asceId))
  }

  behavior of "Proto IasIO"

  it must "round trip IasIO with all fields set" in {
    val id = mkIasIOId("testIO")
    val iasType = IASTypes.DOUBLE
    val inOut = InOut.asOutput[Double](id, iasType)
      .updateMode(OperationalMode.OPERATIONAL)
      .updateFromInputsValidity(Validity(IasValidity.RELIABLE))
      .updateValue(Some(42.5))
      .updateProdTStamp(1234567890L)
      .updateProps(Map("key1" -> "val1", "key2" -> "val2"))
      .updateSentToBsdbTStamp(9876543210L)

    val proto = ProtoUtil.iasIOFrom(inOut, iasType)
    val back = ProtoUtil.iasIOTo(proto)

    back.id.id shouldBe id.id
    back.mode shouldBe OperationalMode.OPERATIONAL
    back.iasType shouldBe iasType
    back.getValidity shouldBe Validity(IasValidity.RELIABLE)
    back.value.get.asInstanceOf[Double] shouldBe 42.5
    back.productionTStamp shouldBe Some(1234567890L)
    back.props shouldBe Some(Map("key1" -> "val1", "key2" -> "val2"))
    back.sentToBsdbTStamp shouldBe Some(9876543210L)
    back.idsOfDependants shouldBe None
  }

  it must "round trip IasIO with no value (None)" in {
    val id = mkIasIOId("emptyIO")
    val iasType = IASTypes.LONG
    val inOut = InOut.asOutput[Long](id, iasType)

    val proto = ProtoUtil.iasIOFrom(inOut, iasType)
    val back = ProtoUtil.iasIOTo(proto)

    back.value shouldBe None
    back.iasType shouldBe IASTypes.LONG
  }

  it must "round trip IasIO with Alarm type" in {
    val id = mkIasIOId("alarmIO")
    val iasType = IASTypes.ALARM
    val alarm = Alarm.getInitialAlarmState().set()
    val inOut = InOut.asOutput[Alarm](id, iasType)
      .updateValue(Some(alarm))
      .updateMode(OperationalMode.DEGRADED)

    val proto = ProtoUtil.iasIOFrom(inOut, iasType)
    val back = ProtoUtil.iasIOTo(proto)

    back.value.get.asInstanceOf[Alarm] shouldBe alarm
    back.mode shouldBe OperationalMode.DEGRADED
  }

  it must "treat zero timestamps as absent" in {
    val id = mkIasIOId("zeroIO")
    val iasType = IASTypes.INT
    val inOut = InOut.asOutput[Int](id, iasType)

    val proto = ProtoUtil.iasIOFrom(inOut, iasType)
    proto.getProductionTStamp shouldBe 0

    val back = ProtoUtil.iasIOTo(proto)
    back.productionTStamp shouldBe None
  }

  it must "round trip IasIO with array value" in {
    val id = mkIasIOId("arrIO")
    val iasType = IASTypes.ARRAYOFDOUBLES
    val arr = NumericArray.valueOf(NumericArray.NumericArrayType.DOUBLE, "1.0, 2.0, 3.0")
    val inOut = InOut.asOutput[NumericArray](id, iasType)
      .updateValue(Some(arr))

    val proto = ProtoUtil.iasIOFrom(inOut, iasType)
    val back = ProtoUtil.iasIOTo(proto)

    val resArr = back.value.get.asInstanceOf[NumericArray]
    resArr.size() shouldBe 3
    resArr.get(0).doubleValue() shouldBe 1.0
    resArr.get(1).doubleValue() shouldBe 2.0
    resArr.get(2).doubleValue() shouldBe 3.0
  }

  it must "round trip IasIO with all timestamps" in {
    val id = mkIasIOId("tsIO")
    val iasType = IASTypes.STRING
    val inOut = InOut.asOutput[String](id, iasType)
      .updateValue(Some("test"))
      .updateProdTStamp(1000L)
      .updateSentToBsdbTStamp(2000L)

    val proto = ProtoUtil.iasIOFrom(inOut, iasType)

    proto.getProductionTStamp shouldBe 1000L
    proto.getSentToBsdbTStamp shouldBe 2000L
    proto.getReadFromMonSysTStamp shouldBe 0
    proto.getSentToConverterTStamp shouldBe 0
    proto.getReceivedFromPluginTStamp shouldBe 0
    proto.getConvertedProductionTStamp shouldBe 0
    proto.getReadFromBsdbTStamp shouldBe 0

    val back = ProtoUtil.iasIOTo(proto)
    back.productionTStamp shouldBe Some(1000L)
    back.sentToBsdbTStamp shouldBe Some(2000L)
    back.readFromMonSysTStamp shouldBe None
  }

  it must "round trip IasIO with INT type via int64Value narrowing" in {
    val id = mkIasIOId("intIO")
    val iasType = IASTypes.INT
    val inOut = InOut.asOutput[Int](id, iasType)
      .updateValue(Some(Integer.valueOf(42)))

    val proto = ProtoUtil.iasIOFrom(inOut, iasType)
    proto.getValue.hasInt64Value() shouldBe true

    val back = ProtoUtil.iasIOTo(proto)
    back.value.get.asInstanceOf[Int] shouldBe 42
  }

  it must "round trip IasIO with SHORT and BYTE types" in {
    val shortId = mkIasIOId("shortIO")
    val shortIn = InOut.asOutput[Short](shortId, IASTypes.SHORT)
      .updateValue(Some(Int.box(100).shortValue()))

    val shortProto = ProtoUtil.iasIOFrom(shortIn, IASTypes.SHORT)
    val shortBack = ProtoUtil.iasIOTo(shortProto)
    shortBack.value.get.asInstanceOf[Short] shouldBe 100.toShort

    val byteId = mkIasIOId("byteIO")
    val byteIn = InOut.asOutput[Byte](byteId, IASTypes.BYTE)
      .updateValue(Some(Int.box(50).byteValue()))

    val byteProto = ProtoUtil.iasIOFrom(byteIn, IASTypes.BYTE)
    val byteBack = ProtoUtil.iasIOTo(byteProto)
    byteBack.value.get.asInstanceOf[Byte] shouldBe 50.toByte
  }

  behavior of "Proto IasioInfo"

  it must "round trip IasioInfo" in {
    val info = IasioInfo("testMP", IASTypes.DOUBLE)
    val proto = ProtoUtil.iasioInfoFrom(info)
    val back = ProtoUtil.iasioInfoTo(proto)

    back.iasioId shouldBe "testMP"
    back.iasioType shouldBe IASTypes.DOUBLE
  }

  it must "round trip multiple IasioInfo values" in {
    val infos = List(
      IasioInfo("mp1", IASTypes.LONG),
      IasioInfo("mp2", IASTypes.STRING),
      IasioInfo("mp3", IASTypes.ARRAYOFDOUBLES),
      IasioInfo("mp4", IASTypes.ALARM)
    )

    infos.foreach { info =>
      val proto = ProtoUtil.iasioInfoFrom(info)
      val back = ProtoUtil.iasioInfoTo(proto)
      back.iasioId shouldBe info.iasioId
      back.iasioType shouldBe info.iasioType
    }
  }
}
