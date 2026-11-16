package org.eso.ias.asce.transfer

import org.eso.ias.generated.protobuf.tfservice.PyTfServiceGrpc
import org.eso.ias.generated.protobuf.tfservice.PytfService
import org.eso.ias.types.InOut
import io.grpc.ManagedChannel
import io.grpc.ManagedChannelBuilder
import org.slf4j.LoggerFactory
import scala.jdk.CollectionConverters._

import java.util.Properties

class GrpcPythonExecutor[T](
    cEleId: String,
    cEleRunningId: String,
    validityTimeFrame: Long,
    props: Properties,
    pythonClassName: String
) extends PythonTransferExecutor[T](cEleId, cEleRunningId, validityTimeFrame, props) {

  private val logger = LoggerFactory.getLogger(classOf[GrpcPythonExecutor[T]])

  private val SERVER_ADDRESS_PROPERTY = "pyTfServerAddr"
  private val DEFAULT_SERVER_ADDRESS = "localhost:50051"

  require(pythonClassName != null && pythonClassName.nonEmpty, "Missing python class name")

  val serverAddr: String = props.getProperty(SERVER_ADDRESS_PROPERTY, DEFAULT_SERVER_ADDRESS)

  private val channel: ManagedChannel = ManagedChannelBuilder.forTarget(serverAddr)
    .usePlaintext()
    .build()

  private val stub: PyTfServiceGrpc.PyTfServiceBlockingStub = PyTfServiceGrpc.newBlockingStub(channel)

  logger.info(
    "GrpcPythonExecutor built for ASCE {}: connected to {}, python class: {}",
    compElementRunningId, serverAddr, pythonClassName
  )

  override def eval(
      compInputs: Map[String, IasIO[?]],
      actualOutput: IasIO[T]
  ): IasIO[T] = {
    val compInputsProto = compInputs.map { case (k, v) =>
      (k, ProtoUtil.iasIOFrom(v.inOut, v.iasType))
    }.asJava

    val actualOutputProto = ProtoUtil.iasIOFrom(actualOutput.inOut, actualOutput.iasType)

    val request = PytfService.EvalRequest.newBuilder()
      .setAsceRunningId(compElementRunningId)
      .putAllCompInputs(compInputsProto)
      .setActualOutput(actualOutputProto)
      .build()

    val resp = stub.eval(request)

    if (!resp.getSuccess) {
      throw new Exception(s"gRPC Eval failed for ASCE $compElementRunningId: ${resp.getError}")
    }

    logger.debug("GrpcPythonExecutor eval for ASCE {} completed, success={}",
      compElementRunningId, resp.getSuccess)

    val resultInOut = ProtoUtil.iasIOTo(resp.getResult)
    new IasIO[T](resultInOut.asInstanceOf[InOut[T]])
  }

  override def initialize(
      inputsInfo: Set[IasioInfo],
      outputInfo: IasioInfo
  ): Unit = {
    val inputsInfoList = inputsInfo.toSeq.map(ProtoUtil.iasioInfoFrom).asJava
    val outputInfoProto = ProtoUtil.iasioInfoFrom(outputInfo)
    val propsMap = props.entrySet().asScala.map { e => (e.getKey.toString, e.getValue.toString) }.toMap.asJava

    val templateInstanceStr = if (getTemplateInstance.isPresent) {
      Some(getTemplateInstance.get.toString)
    } else {
      None
    }

    val requestBuilder = PytfService.InitializeRequest.newBuilder()
      .setAsceRunningId(compElementRunningId)
      .setAsceId(compElementId)
      .setPythonClassName(pythonClassName)
      .setValidityTimeFrame(validityTimeFrame)
      .putAllProps(propsMap)
      .addAllInputsInfo(inputsInfoList)
      .setOutputInfo(outputInfoProto)

    templateInstanceStr.foreach(templateInstanceStr =>
      requestBuilder.setTemplateInstance(templateInstanceStr)
    )

    val resp = stub.initialize(requestBuilder.build())

    if (!resp.getSuccess) {
      throw new Exception(s"gRPC Initialize failed for ASCE $compElementRunningId: ${resp.getError}")
    }

    logger.info("GrpcPythonExecutor initialized for ASCE {} (success={})",
      compElementRunningId, resp.getSuccess)
  }

  override def shutdown(): Unit = {
    val resp = stub.shutdown(
      PytfService.ShutdownRequest.newBuilder()
        .setAsceRunningId(compElementRunningId)
        .build()
    )

    if (!resp.getSuccess) {
      logger.warn("gRPC Shutdown returned error for ASCE {}: {}", compElementRunningId, resp.getError)
    }

    logger.info("GrpcPythonExecutor shut down for ASCE {} (success={})",
      compElementRunningId, resp.getSuccess)
  }
}

object GrpcPythonExecutor {
  def apply[T](
      cEleId: String,
      cEleRunningId: String,
      validityTimeFrame: Long,
      props: Properties,
      pythonClassName: String
  ): GrpcPythonExecutor[T] = {
    new GrpcPythonExecutor[T](cEleId, cEleRunningId, validityTimeFrame, props, pythonClassName)
  }
}
