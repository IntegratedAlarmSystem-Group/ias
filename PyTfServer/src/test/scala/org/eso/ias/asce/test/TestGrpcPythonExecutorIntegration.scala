package org.eso.ias.asce.test

import org.eso.ias.asce.transfer.{GrpcPythonExecutor, IasIO, IasioInfo, ProtoUtil}
import org.eso.ias.generated.protobuf.tfservice.PyTfServiceGrpc
import org.eso.ias.generated.protobuf.tfservice.PytfService
import org.eso.ias.types.{IASTypes, Identifier, IdentifierType, InOut}
import org.eso.ias.logging.IASLogger
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.BeforeAndAfterAll

import io.grpc.ManagedChannel
import io.grpc.ManagedChannelBuilder

import java.util.Properties
import scala.jdk.CollectionConverters._
import scala.util.Try

/** Integration tests for GrpcPythonExecutor with a real PyTfServer subprocess.
  *
  * These tests manage their own PyTfServer lifecycle: `iasPyTfServer.py` is
  * spawned on a random localhost TCP port in `beforeAll` and destroyed in
  * `afterAll`.  No external PyTfServer needs to be running beforehand.  A
  * gRPC channel is opened from the Scala side to verify end-to-end
  * communication between `GrpcPythonExecutor` and the Python TF server.
  *
  * '''Start:''' beforeAll spawns the server via `ProcessBuilder` with a
  * `PYTHONPATH` covering the build trees of CompElement, BasicTypes, Tools,
  * and the PyTfServer source and generated proto paths.  A 2-second sleep
  * follows, then a liveness check — if the process has already exited, an
  * `IllegalStateException` is thrown with the captured stdout/stderr.
  *
  * '''Terminate:''' afterAll calls `channel.shutdownNow()` and
  * `serverProcess.destroy()` with a 5-second wait.
  */
class TestGrpcPythonExecutorIntegration extends AnyFlatSpec with BeforeAndAfterAll {

  private val logger = IASLogger.getLogger(this.getClass)

  var serverProcess: Process = null
  var serverPort: Int = 0
  var channel: ManagedChannel = null

  /** Spawn the PyTfServer subprocess and open a gRPC channel. */
  override def beforeAll(): Unit = {
    serverPort = findFreePort()

    val iasRoot = System.getProperty("ias.root.build",
      new java.io.File(".").getCanonicalFile.getParentFile.getCanonicalPath)

    val pythonVersion = "python3.12"

    val compElementLib = s"$iasRoot/CompElement/build/lib/$pythonVersion/site-packages"
    val basicTypesLib = s"$iasRoot/BasicTypes/build/lib/$pythonVersion/site-packages"
    val toolsLib = s"$iasRoot/Tools/build/lib/$pythonVersion/site-packages"
    val pyTfServerSrc = s"$iasRoot/PyTfServer/src/main/python"
    val pyTfServerTest = s"$iasRoot/PyTfServer/src/test/python"
    val compElementGenProto = s"$iasRoot/CompElement/build/generated/python/IasProtobufTfserver"
    val basicTypesGenProto = s"$iasRoot/BasicTypes/build/generated/python/IasProtobufTypes"

    val pythonPath = List(
      compElementLib, basicTypesLib, toolsLib, pyTfServerSrc, pyTfServerTest,
      compElementGenProto, basicTypesGenProto
    ).mkString(":")

    val serverScript = s"$iasRoot/PyTfServer/src/main/python/iasPyTfServer.py"

    val env = System.getenv().asScala.toMap + ("PYTHONPATH" -> pythonPath)

    val cmd = List("python3", "-u", serverScript, "-p", serverPort.toString)
    logger.info(s"Starting PyTfServer on port $serverPort with PYTHONPATH=$pythonPath")

    val pb = new ProcessBuilder(cmd*)
    env.foreach { case (k, v) => pb.environment().put(k, v) }
    serverProcess = pb.start()

    Thread.sleep(2000)
    val isAlive = Try(serverProcess.exitValue()).isFailure
    if (!isAlive) {
      val err = scala.io.Source.fromInputStream(serverProcess.getInputStream).mkString
      val out = scala.io.Source.fromInputStream(serverProcess.getErrorStream).mkString
      throw new IllegalStateException(s"PyTfServer failed to start. stdout: $err stderr: $out")
    }
    logger.info(s"PyTfServer started successfully on port $serverPort")

    channel = ManagedChannelBuilder.forAddress("localhost", serverPort)
      .usePlaintext()
      .build()
  }

  /** Shut down the gRPC channel and destroy the server subprocess. */
  override def afterAll(): Unit = {
    if (channel != null) {
      channel.shutdownNow()
    }
    if (serverProcess != null) {
      serverProcess.destroy()
      Try(serverProcess.waitFor(5, java.util.concurrent.TimeUnit.SECONDS))
    }
  }

  /** Return a free TCP port on localhost using a zero-bound ServerSocket. */
  def findFreePort(): Int = {
    val serverSocket = new java.net.ServerSocket(0)
    val port = serverSocket.getLocalPort
    serverSocket.close()
    port
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
      "test_tf_module.PassThroughTF"
    )

    val inputsInfo = Set(IasioInfo("in1", IASTypes.LONG))
    val outputInfo = IasioInfo("out", IASTypes.LONG)

    executor.initialize(inputsInfo, outputInfo)

    executor.shutdown()
  }

  it must "fail on double initialize" in {
    val props = new Properties()
    props.setProperty("pyTfServerAddr", s"localhost:$serverPort")

    val executor = new GrpcPythonExecutor[java.lang.Long](
      "testInt",
      s"testInt:double:$serverPort",
      1000L,
      props,
      "test_tf_module.PassThroughTF"
    )

    val inputsInfo = Set(IasioInfo("in1", IASTypes.LONG))
    val outputInfo = IasioInfo("out", IASTypes.LONG)

    executor.initialize(inputsInfo, outputInfo)

    var failed = false
    try {
      executor.initialize(inputsInfo, outputInfo)
    } catch {
      case _: Exception => failed = true
    }
    assert(failed, "Expected exception on double initialize")

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
      "test_tf_module.PassThroughTF"
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
      "test_tf_module.DoublingTF"
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
      "test_tf_module.PassThroughTF"
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
