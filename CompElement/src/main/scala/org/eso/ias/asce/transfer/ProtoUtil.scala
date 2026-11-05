package org.eso.ias.asce.transfer

import org.eso.ias.types.*
import org.eso.ias.types.ProtoUtil.*
import org.eso.ias.generated.protobuf.tfservice.PytfService
import org.eso.ias.generated.protobuf.iastypes.IasTypes
import scala.jdk.CollectionConverters._

/**
  * Protobuf conversion utilities for ASCE-specific types (IasIO, IasioInfo).
  *
  * This object delegates basic type conversions (enums, Value, Alarm) to
  * [[org.eso.ias.types.ProtoUtil]] in BasicTypes and only handles the
  * ASCE-level serialization of IasIO and IasioInfo messages defined in
  * the CompElement protobuf service.
  */
object ProtoUtil {

  /**
    * Converts an [[InOut]] together with its [[IASTypes]] into a protobuf
    * `IasIO` message. Timestamps that are undefined or zero are omitted from
    * the protobuf representation. Optional `value` and `props` fields are
    * only set when defined and non-empty.
    *
    * @param inOut the domain model to serialize
    * @param iasType the IAS type of the value carried by the InOut
    * @return the protobuf IasIO message
    */
  def iasIOFrom(inOut: InOut[?], iasType: IASTypes): PytfService.IasIO = {
    val builder = PytfService.IasIO.newBuilder()
      .setIasId(inOut.id.id)
      .setRunningId(inOut.id.fullRunningID)
      .setMode(OperationalModeFrom(inOut.mode))
      .setIasType(IASTypeFrom(iasType))
      .setValidity(ValidityFrom(inOut.getValidity.iasValidity))

    if (inOut.productionTStamp.isDefined && inOut.productionTStamp.get > 0)
      builder.setProductionTStamp(inOut.productionTStamp.get)
    if (inOut.readFromMonSysTStamp.isDefined && inOut.readFromMonSysTStamp.get > 0)
      builder.setReadFromMonSysTStamp(inOut.readFromMonSysTStamp.get)
    if (inOut.sentToConverterTStamp.isDefined && inOut.sentToConverterTStamp.get > 0)
      builder.setSentToConverterTStamp(inOut.sentToConverterTStamp.get)
    if (inOut.receivedFromPluginTStamp.isDefined && inOut.receivedFromPluginTStamp.get > 0)
      builder.setReceivedFromPluginTStamp(inOut.receivedFromPluginTStamp.get)
    if (inOut.convertedProductionTStamp.isDefined && inOut.convertedProductionTStamp.get > 0)
      builder.setConvertedProductionTStamp(inOut.convertedProductionTStamp.get)
    if (inOut.sentToBsdbTStamp.isDefined && inOut.sentToBsdbTStamp.get > 0)
      builder.setSentToBsdbTStamp(inOut.sentToBsdbTStamp.get)
    if (inOut.readFromBsdbTStamp.isDefined && inOut.readFromBsdbTStamp.get > 0)
      builder.setReadFromBsdbTStamp(inOut.readFromBsdbTStamp.get)
    if (inOut.props.isDefined && inOut.props.get.nonEmpty)
      builder.putAllProps(inOut.props.get.map { case (k, v) => (k, v) }.asJava)
    inOut.value.foreach(v => builder.setValue(valueFrom(v, iasType)))

    builder.build()
  }

  /**
    * Deserializes a protobuf `IasIO` message back into an [[InOut]].
    * Timestamps with value zero are treated as absent (`None`).
    * The resulting InOut is constructed as an output (fromInputsValidity set,
    * fromIasValueValidity empty).
    *
    * @param proto the protobuf IasIO message
    * @return a new InOut populated from the message fields
    */
  def iasIOTo(proto: PytfService.IasIO): InOut[Any] = {
    val iasType = IASTypeTo(proto.getIasType)
    val id = Identifier(proto.getRunningId)
    val mode = OperationalModeTo(proto.getMode)
    val validity = ValidityTo(proto.getValidity)

    val value: Option[Any] =
      if (proto.hasValue() && proto.getValue.getValueCase != IasTypes.Value.ValueCase.VALUE_NOT_SET)
        Some(valueTo(proto.getValue, iasType))
      else
        None

    val props: Option[Map[String, String]] =
      if (proto.getPropsCount > 0)
        Some(proto.getPropsMap.asScala.toMap)
      else
        None

    new InOut[Any](
      value = value,
      id = id,
      mode = mode,
      fromIasValueValidity = None,
      fromInputsValidity = Some(Validity(validity)),
      validityConstraint = None,
      iasType = iasType,
      readFromMonSysTStamp = if (proto.getReadFromMonSysTStamp > 0) Some(proto.getReadFromMonSysTStamp) else None,
      productionTStamp = if (proto.getProductionTStamp > 0) Some(proto.getProductionTStamp) else None,
      sentToConverterTStamp = if (proto.getSentToConverterTStamp > 0) Some(proto.getSentToConverterTStamp) else None,
      receivedFromPluginTStamp = if (proto.getReceivedFromPluginTStamp > 0) Some(proto.getReceivedFromPluginTStamp) else None,
      convertedProductionTStamp = if (proto.getConvertedProductionTStamp > 0) Some(proto.getConvertedProductionTStamp) else None,
      sentToBsdbTStamp = if (proto.getSentToBsdbTStamp > 0) Some(proto.getSentToBsdbTStamp) else None,
      readFromBsdbTStamp = if (proto.getReadFromBsdbTStamp > 0) Some(proto.getReadFromBsdbTStamp) else None,
      idsOfDependants = None,
      props = props
    )
  }

  /**
    * Converts an [[IasioInfo]] to its protobuf representation.
    *
    * @param info the IasioInfo to serialize
    * @return the protobuf IasioInfoProto message
    */
  def iasioInfoFrom(info: IasioInfo): PytfService.IasioInfoProto = {
    PytfService.IasioInfoProto.newBuilder()
      .setId(info.iasioId)
      .setIasType(IASTypeFrom(info.iasioType))
      .build()
  }

  /**
    * Deserializes a protobuf `IasioInfoProto` message into an [[IasioInfo]].
    *
    * @param proto the protobuf message
    * @return the domain IasioInfo
    */
  def iasioInfoTo(proto: PytfService.IasioInfoProto): IasioInfo = {
    IasioInfo(
      proto.getId,
      IASTypeTo(proto.getIasType)
    )
  }
}
