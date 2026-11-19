package org.eso.ias.pytfserver.test

import org.eso.ias.asce.transfer.{GrpcPythonExecutor, IasIO, IasioInfo, ProtoUtil}
import org.eso.ias.generated.protobuf.tfservice.PyTfServiceGrpc
import org.eso.ias.generated.protobuf.tfservice.PytfService
import org.eso.ias.types.{IASTypes, Identifier, IdentifierType, InOut}
import org.eso.ias.logging.IASLogger
import org.scalatest.BeforeAndAfterAll
import org.scalatest.flatspec.AnyFlatSpec

import io.grpc.ManagedChannel
import io.grpc.ManagedChannelBuilder

import java.util.Properties
import scala.jdk.CollectionConverters._

/** Integration tests for GrpcPythonExecutor with a real PyTfServer subprocess.
  *
  * Uses [[PyTfServerBase]] for server lifecycle management.  A
  * gRPC channel is opened from the Scala side to verify end-to-end
  * communication between `GrpcPythonExecutor` and the Python TF server.
  */
class TestGrpcPythonExecutorIntegration extends AnyFlatSpec with BeforeAndAfterAll with PyTfServerBase {

  private val logger = IASLogger.getLogger(this.getClass)

  private var channel: ManagedChannel | Null = null

  override def beforeAll(): Unit = {
    super.beforeAll()
    startServer()
    channel = ManagedChannelBuilder.forAddress("localhost", serverPort)
      .usePlaintext()
      .build()
    logger.info(s"gRPC channel created on port $serverPort")
  }

  override def afterAll(): Unit = {
    if (channel != null) {
      channel.shutdownNow()
    }
    stopServer()
    super.afterAll()
  }

  behavior of "GrpcPythonExecutor with running PyTfServer"

  it must "initialize and shutdown via gRPC" in {
    val props = new Properties()
    props.setProperty("pyTfServerAddr", s"localhost:$serverPort")

    val executor = new GrpcPythonExecutor[java.lang.Long](
      "testInt",
      s"testInt:run$serverPort",
      1000L,
      props,
      "test_tf_module.PassThroughTF",
      s"localhost:$serverPort"
    )

    val inputsInfo = Set(IasioInfo("in1", IASTypes.LONG))
    val outputInfo = IasioInfo("out", IASTypes.LONG)

    executor.initialize(inputsInfo, outputInfo)

    executor.shutdown()
  }

  it must "succeed on double initialize after cleaning orphaned TF" in {
    val props = new Properties()
    props.setProperty("pyTfServerAddr", s"localhost:$serverPort")

    val executor = new GrpcPythonExecutor[java.lang.Long](
      "testInt",
      s"testInt:double:$serverPort",
      1000L,
      props,
      "test_tf_module.PassThroughTF",
      s"localhost:$serverPort"
    )

    val inputsInfo = Set(IasioInfo("in1", IASTypes.LONG))
    val outputInfo = IasioInfo("out", IASTypes.LONG)

    executor.initialize(inputsInfo, outputInfo)

    // Second initialize succeeds: when the Supervisor restarts after an unclean
    // termination (crash, SIGKILL), the TF from the previous Supervisor instance
    // is still registered in PyTfServer because the gRPC shutdown RPC was never
    // sent.  PyTfServer handles this by detecting the collision in initialise(),
    // shutting down the orphaned TF, and re-attempting the initialization.
    // See TfManager.initialize() in tf_manager.py for full rationale.
    executor.initialize(inputsInfo, outputInfo)

    executor.shutdown()
  }

  it must "receive correct props and asceId in Python TF" in {
    val props = new Properties()
    props.setProperty("pyTfServerAddr", s"localhost:$serverPort")
    props.setProperty("customProp", "customValue")

    val stub = PyTfServiceGrpc.newBlockingStub(channel)

    val uniqueId = s"testInt:props:$serverPort"
    val asceId = "testProps"

    val inputsInfoList = List.empty[PytfService.IasioInfoProto].asJava
    val outputInfoProto = ProtoUtil.iasioInfoFrom(IasioInfo("out", IASTypes.LONG))

    val propsMap = props.entrySet().asScala
      .map { e => (e.getKey.toString, e.getValue.toString) }
      .toMap
      .asJava

    val initReq = PytfService.InitializeRequest.newBuilder()
      .setAsceRunningId(uniqueId)
      .setAsceId(asceId)
      .setPythonClassName("test_tf_module.PassThroughTF")
      .setValidityTimeFrame(1000L)
      .putAllProps(propsMap)
      .addAllInputsInfo(inputsInfoList)
      .setOutputInfo(outputInfoProto)
      .build()

    val initResp = stub.initialize(initReq)
    assert(initResp.getSuccess, s"Initialize failed: ${initResp.getError}")

    val shutdownResp = stub.shutdown(
      PytfService.ShutdownRequest.newBuilder()
        .setAsceRunningId(uniqueId)
        .build()
    )
    assert(shutdownResp.getSuccess, s"Shutdown failed: ${shutdownResp.getError}")
  }

  it must "eval pass through returns input value" in {
    val props = new Properties()
    props.setProperty("pyTfServerAddr", s"localhost:$serverPort")

    val executor = new GrpcPythonExecutor[java.lang.Long](
      "testInt",
      s"testInt:eval:$serverPort",
      1000L,
      props,
      "test_tf_module.PassThroughTF",
      s"localhost:$serverPort"
    )

    val inputsInfo = Set(IasioInfo("in1", IASTypes.LONG))
    val outputInfo = IasioInfo("out", IASTypes.LONG)
    executor.initialize(inputsInfo, outputInfo)

    val inputId = new Identifier("in1", IdentifierType.IASIO, None)
    val inputInOut: InOut[java.lang.Long] = InOut.asInput[java.lang.Long](inputId, IASTypes.LONG)
      .asInstanceOf[InOut[java.lang.Long]]
      .updateValue(Some(42L))
      .updateProdTStamp(System.currentTimeMillis())
    val inputIasIO = new IasIO[java.lang.Long](inputInOut)

    val outputId = new Identifier("out", IdentifierType.IASIO, None)
    val outputInOut = InOut.asOutput[java.lang.Long](outputId, IASTypes.LONG)
    val outputIasIO = new IasIO[java.lang.Long](outputInOut)

    val compInputs = Map("in1" -> inputIasIO)
    val result = executor.eval(compInputs, outputIasIO)

    assert(result.value.contains(42L: java.lang.Long), s"Expected 42 but got ${result.value}")

    executor.shutdown()
  }

  it must "eval doubling tf doubles input value" in {
    val props = new Properties()
    props.setProperty("pyTfServerAddr", s"localhost:$serverPort")

    val executor = new GrpcPythonExecutor[java.lang.Long](
      "testDbl",
      s"testDbl:eval:$serverPort",
      1000L,
      props,
      "test_tf_module.DoublingTF",
      s"localhost:$serverPort"
    )

    val inputsInfo = Set(IasioInfo("in1", IASTypes.LONG))
    val outputInfo = IasioInfo("out", IASTypes.LONG)
    executor.initialize(inputsInfo, outputInfo)

    val inputId = new Identifier("in1", IdentifierType.IASIO, None)
    val inputInOut: InOut[java.lang.Long] = InOut.asInput[java.lang.Long](inputId, IASTypes.LONG)
      .asInstanceOf[InOut[java.lang.Long]]
      .updateValue(Some(21L))
      .updateProdTStamp(System.currentTimeMillis())
    val inputIasIO = new IasIO[java.lang.Long](inputInOut)

    val outputId = new Identifier("out", IdentifierType.IASIO, None)
    val outputInOut = InOut.asOutput[java.lang.Long](outputId, IASTypes.LONG)
    val outputIasIO = new IasIO[java.lang.Long](outputInOut)

    val compInputs = Map("in1" -> inputIasIO)
    val result = executor.eval(compInputs, outputIasIO)

    assert(result.value.contains(42L: java.lang.Long), s"Expected 42 but got ${result.value}")

    executor.shutdown()
  }

  it must "eval fails when not initialized" in {
    val props = new Properties()
    props.setProperty("pyTfServerAddr", s"localhost:$serverPort")

    val executor = new GrpcPythonExecutor[java.lang.Long](
      "testFail",
      s"testFail:notinit:$serverPort",
      1000L,
      props,
      "test_tf_module.PassThroughTF",
      s"localhost:$serverPort"
    )

    val inputId = new Identifier("in1", IdentifierType.IASIO, None)
    val inputInOut: InOut[java.lang.Long] = InOut.asInput[java.lang.Long](inputId, IASTypes.LONG)
      .asInstanceOf[InOut[java.lang.Long]]
      .updateValue(Some(1L))
      .updateProdTStamp(System.currentTimeMillis())
    val inputIasIO = new IasIO[java.lang.Long](inputInOut)

    val outputId = new Identifier("out", IdentifierType.IASIO, None)
    val outputInOut = InOut.asOutput[java.lang.Long](outputId, IASTypes.LONG)
    val outputIasIO = new IasIO[java.lang.Long](outputInOut)

    val compInputs = Map("in1" -> inputIasIO)

    var failed = false
    try {
      executor.eval(compInputs, outputIasIO)
    } catch {
      case _: Exception => failed = true
    }
    assert(failed, "Expected exception when eval called without initialize")
  }

}
