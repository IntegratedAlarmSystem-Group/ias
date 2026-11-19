package org.eso.ias.asce.transfer

import org.eso.ias.generated.protobuf.tfservice.PyTfServiceGrpc
import org.eso.ias.generated.protobuf.tfservice.PytfService
import org.eso.ias.types.InOut
import io.grpc.Deadline
import io.grpc.ManagedChannel
import io.grpc.ManagedChannelBuilder
import org.slf4j.LoggerFactory
import scala.jdk.CollectionConverters._

import java.util.Properties
import java.util.concurrent.TimeUnit
import scala.util.Try

/**
  * gRPC-based executor for Python transfer functions.
  *
  * Delegates TF initialization, evaluation, and shutdown to an external
  * PyTfServer process via gRPC.  The server address is provided explicitly
  * at construction time.
  *
  * @param cEleId: the ID of the ASCE
  * @param cEleRunningId: the running ID of the ASCE
  * @param validityTimeFrame: the time frame (msec) to invalidate monitor points
  * @param props: the properties for the executor
  * @param pythonClassName: the fully qualified name of the Python TF class
  * @param serverAddr: the address of the PyTfServer (host:port) — required
  */
class GrpcPythonExecutor[T](
    cEleId: String,
    cEleRunningId: String,
    validityTimeFrame: Long,
    props: Properties,
    pythonClassName: String,
    val serverAddr: String
) extends PythonTransferExecutor[T](cEleId, cEleRunningId, validityTimeFrame, props) {

  private val logger = LoggerFactory.getLogger(classOf[GrpcPythonExecutor[T]])

  require(pythonClassName != null && pythonClassName.nonEmpty, "Missing python class name")
  require(serverAddr != null && serverAddr.nonEmpty, "Missing PyTfServer address")

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

    val deadline = Deadline.after(GrpcPythonExecutor.rpcTimeoutSeconds, TimeUnit.SECONDS)
    val resp = stub.withDeadline(deadline).eval(request)

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

    val deadline = Deadline.after(GrpcPythonExecutor.rpcTimeoutSeconds, TimeUnit.SECONDS)
    val resp = stub.withDeadline(deadline).initialize(requestBuilder.build())

    if (!resp.getSuccess) {
      throw new Exception(s"gRPC Initialize failed for ASCE $compElementRunningId: ${resp.getError}")
    }

    logger.info("GrpcPythonExecutor initialized for ASCE {} (success={})",
      compElementRunningId, resp.getSuccess)
  }

  override def shutdown(): Unit = {
    try {
      val deadline = Deadline.after(GrpcPythonExecutor.rpcTimeoutSeconds, TimeUnit.SECONDS)
      val resp = stub.withDeadline(deadline).shutdown(
        PytfService.ShutdownRequest.newBuilder()
          .setAsceRunningId(compElementRunningId)
          .build()
      )

      if (!resp.getSuccess) {
        logger.warn("gRPC Shutdown returned error for ASCE {}: {}", compElementRunningId, resp.getError)
      }
    } catch {
      case e: Exception =>
        logger.error("Failed to send Shutdown RPC to PyTfServer for ASCE {}: {}", compElementRunningId, e.getMessage)
    }

    channel.shutdown()
    if (!channel.awaitTermination(5, java.util.concurrent.TimeUnit.SECONDS)) {
      logger.error("gRPC channel did not terminate within 5s for ASCE {}", compElementRunningId)
    }
    logger.info("gRPC channel shut down for ASCE {}", compElementRunningId)
  }
}

object GrpcPythonExecutor {
  val RpcTimeoutProperty = "org.eso.ias.rpc.timeout"
  /**
    * Default timeout in seconds for gRPC calls to PyTfServer.
    * Customizable via the Java system property `org.eso.ias.rpc.timeout`.
    */
  def rpcTimeoutSeconds: Long = {
    Try(System.getProperty(RpcTimeoutProperty).toLong).getOrElse(5)
  }

  /**
    * Ping the PyTfServer at the given address to check if it is reachable.
    *
    * @param serverAddr the server address in host:port format
    * @return true if the server replies with success, false otherwise
    */
  def checkServerRunning(serverAddr: String): Boolean = {
    val parts = serverAddr.split(":")
    val host = parts(0)
    val port = parts(1).toInt
    val channel = ManagedChannelBuilder.forAddress(host, port)
      .usePlaintext()
      .build()
    try {
      val stub = PyTfServiceGrpc.newBlockingStub(channel)
      val deadline = Deadline.after(rpcTimeoutSeconds, TimeUnit.SECONDS)
      val resp = stub.withDeadline(deadline).ping(PytfService.PingRequest.getDefaultInstance)
      resp.getSuccess
    } catch {
      case _: Exception => false
    } finally {
      channel.shutdownNow()
    }
  }

  def apply[T](
      cEleId: String,
      cEleRunningId: String,
      validityTimeFrame: Long,
      props: Properties,
      pythonClassName: String,
      serverAddr: String
  ): GrpcPythonExecutor[T] = {
    new GrpcPythonExecutor[T](cEleId, cEleRunningId, validityTimeFrame, props, pythonClassName, serverAddr)
  }
}
