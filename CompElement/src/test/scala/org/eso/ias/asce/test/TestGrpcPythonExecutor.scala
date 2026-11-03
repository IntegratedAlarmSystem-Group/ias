package org.eso.ias.asce.test

import org.eso.ias.asce.transfer.{GrpcPythonExecutor, IasIO, PythonTransfer, TransferFunctionLanguage, TransferFunctionSetting}
import org.eso.ias.asce.{AsceStates, CompEleThreadFactory, ComputingElement}
import org.eso.ias.logging.IASLogger
import org.eso.ias.types.*
import org.scalatest.flatspec.AnyFlatSpec

import java.util.Properties
import scala.collection.mutable.{Map => MutableMap}
import scala.util.Try

class TestGrpcPythonExecutor extends AnyFlatSpec {

  private val logger = IASLogger.getLogger(this.getClass)

  // ---- Identifiers used throughout the tests ----

  val supervId = new Identifier("SupervId", IdentifierType.SUPERVISOR, None)
  val dasId = new Identifier("DAS-ID", IdentifierType.DASU, supervId)
  val compID = new Identifier("COMP-ID", IdentifierType.ASCE, Option(dasId))
  val outId = new Identifier("OutputId", IdentifierType.IASIO, Option(compID))
  val threadFactory = new CompEleThreadFactory("TestRunningId")
  val validityThresholdInSecs = 2

  behavior of "GrpcPythonExecutor constructor"

  it must "succeed with valid parameters and default server address" in {
    val props = new Properties()
    val exec = new GrpcPythonExecutor[java.lang.Long](
      "ASCE1",
      "ASCE1:runningId",
      1000L,
      props,
      "TestTF.TestTransferFunction"
    )
    assert(exec.serverAddr == "localhost:50051")
  }

  it must "use custom server address from pyTfServerAddr property" in {
    val props = new Properties()
    props.setProperty("pyTfServerAddr", "myhost:9999")
    val exec = new GrpcPythonExecutor[java.lang.Long](
      "ASCE2",
      "ASCE2:runningId",
      1000L,
      props,
      "TestTF.TestTransferFunction"
    )
    assert(exec.serverAddr == "myhost:9999")
  }

  it must "fail when pythonClassName is null" in {
    val props = new Properties()
    var thrown = false
    try {
      new GrpcPythonExecutor[java.lang.Long](
        "ASCE3",
        "ASCE3:runningId",
        1000L,
        props,
        null
      )
    } catch {
      case _: IllegalArgumentException => thrown = true
    }
    assert(thrown, "Expected IllegalArgumentException for null pythonClassName")
  }

  it must "fail when pythonClassName is empty" in {
    val props = new Properties()
    var thrown = false
    try {
      new GrpcPythonExecutor[java.lang.Long](
        "ASCE4",
        "ASCE4:runningId",
        1000L,
        props,
        ""
      )
    } catch {
      case _: IllegalArgumentException => thrown = true
    }
    assert(thrown, "Expected IllegalArgumentException for empty pythonClassName")
  }

  behavior of "GrpcPythonExecutor in full ASCE lifecycle"

  it must "mark ASCE as TFBroken when server is unreachable during initialize" in {
    logger.info("Testing ASCE initialize failure when server unreachable")

    val inputID = "MPointID"
    val mpId = new Identifier(inputID, IdentifierType.IASIO, Option(compID))
    val mp = InOut.asInput(mpId, IASTypes.LONG)
      .updateValue(Some(1L))
      .updateProdTStamp(System.currentTimeMillis())
    val inputsMPs: MutableMap[String, InOut[?]] = MutableMap(inputID -> mp)

    val output: InOut[Alarm] = InOut.asOutput(outId, IASTypes.ALARM)

    val props = new Properties()
    // Use a port that is definitely not listening
    props.setProperty("pyTfServerAddr", "localhost:59999")

    val pythonTFSetting = new TransferFunctionSetting(
      "TestTF.TestTransferFunction",
      TransferFunctionLanguage.python,
      None,
      threadFactory
    )

    val comp: ComputingElement[Alarm] = new ComputingElement[Alarm](
      compID,
      output,
      inputsMPs.values.toSet,
      pythonTFSetting,
      validityThresholdInSecs,
      props
    ) with PythonTransfer[Alarm]

    val state = comp.initialize()
    assert(
      state == AsceStates.TFBroken,
      s"Expected TFBroken but got $state (server should be unreachable)"
    )

    comp.shutdown()
    Thread.sleep(500)
    logger.info("ASCE initialize failure tested")
  }

  it must "create IasIO-wrapped executor via TransferFunctionSetting" in {
    logger.info("Testing TransferFunctionSetting creates GrpcPythonExecutor")

    val inputID = "MPointID"
    val mpId = new Identifier(inputID, IdentifierType.IASIO, Option(compID))
    val mp = InOut.asInput(mpId, IASTypes.LONG)
      .updateValue(Some(42L))
      .updateProdTStamp(System.currentTimeMillis())
    val inputsMPs: MutableMap[String, InOut[?]] = MutableMap(inputID -> mp)

    val output: InOut[java.lang.Long] = InOut.asOutput(outId, IASTypes.LONG)

    val props = new Properties()
    props.setProperty("pyTfServerAddr", "localhost:59998")

    val pythonTFSetting = new TransferFunctionSetting(
      "TestTF.TestTransferFunction",
      TransferFunctionLanguage.python,
      None,
      threadFactory
    )

    val comp: ComputingElement[java.lang.Long] = new ComputingElement[java.lang.Long](
      compID,
      output,
      inputsMPs.values.toSet,
      pythonTFSetting,
      validityThresholdInSecs,
      props
    ) with PythonTransfer[java.lang.Long]

    val state = comp.initialize()
    assert(
      state == AsceStates.TFBroken,
      s"Expected TFBroken but got $state"
    )

    val inputs = Map(inputID -> mp)
    val result: Try[InOut[java.lang.Long]] = comp.transfer(inputs, compID, output)

    assert(
      result.isFailure || comp.getState() == AsceStates.TFBroken,
      "Transfer should fail or ASCE should be TFBroken when server is unreachable"
    )

    comp.shutdown()
    Thread.sleep(500)
    logger.info("TransferFunctionSetting GrpcPythonExecutor creation tested")
  }

  it must "pass server address property through to GrpcPythonExecutor" in {
    val props = new Properties()
    props.setProperty("pyTfServerAddr", "custom-server:12345")

    val executor = new GrpcPythonExecutor[java.lang.Integer](
      "ASCE5",
      "ASCE5:run",
      5000L,
      props,
      "MyTF"
    )

    assert(executor.serverAddr == "custom-server:12345")
  }

}
