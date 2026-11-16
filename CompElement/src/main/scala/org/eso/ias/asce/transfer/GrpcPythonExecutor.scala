package org.eso.ias.asce.transfer

import org.eso.ias.generated.protobuf.tfservice.PyTfServiceGrpc
import org.eso.ias.generated.protobuf.tfservice.PytfService
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
    val message = compElementRunningId + " eval"
    val resp = stub.ping(PytfService.PingRequest.newBuilder().setMessage(message).build())

    if (!resp.getSuccess) {
      throw new Exception(s"gRPC Ping failed during eval for ASCE $compElementRunningId")
    }

    logger.debug("GrpcPythonExecutor eval for ASCE {} pinged, success={}",
      compElementRunningId, resp.getSuccess)

    actualOutput
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
