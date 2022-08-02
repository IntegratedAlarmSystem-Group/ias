package org.eso.ias.asce.test.transfer

import java.util.Properties

import org.eso.ias.asce.transfer.{IasIO, IasioInfo, ScalaTransferExecutor, TransferFunctionSetting}
import org.eso.ias.logging.IASLogger
import org.eso.ias.types.{Alarm, IASTypes}

/**
  * A transfer function to test if the component correctly detects
  * and inhibit misbehaving (slow) TFs.
  *
  * @param asceId: the ID of the ASCE
  * @param asceRunningId: the runningID of the ASCE
  * @param validityTimeFrame: The time frame (msec) to invalidate monitor points
  * @param props: the user defined properties
  * @see TransferExecutor
  */
class SimulatedSlowTF(
                       cEleId: String,
                       cEleRunningId: String,
                       validityTimeFrame: Long,
                       props: Properties) extends ScalaTransferExecutor[Alarm](cEleId,cEleRunningId,validityTimeFrame,props) {

  /** The time to wait to trigger the detection of the slowness in the ASCE */
  val timeToWait: Long = TransferFunctionSetting.MaxTolerableTFTime+100
  /**
    * Initialize the TF
    *
    * @param inputsInfo The IDs and types of the inputs
    * @param outputInfo The Id and type of thr output
    **/
  override def initialize(inputsInfo: Set[IasioInfo], outputInfo: IasioInfo): Unit = {
    require(inputsInfo.size==1)
    require(inputsInfo.head.iasioType==IASTypes.BOOLEAN)
    SimulatedSlowTF.logger.info("Scala TF intialized")
  }

  /**
   * Shut dwon
   *
   * @see TransferExecutor
   */
  override def shutdown(): Unit = {
    SimulatedSlowTF.logger.info("Scala TF shut down")
  }

  /**
    * Produces the output simulating a slowness.
    *
    * If the boolean input is truue the TF will take too long to
    * produce the output.
    * If false, returns immediately
    *
    * @param compInputs: the inputs to the ASCE
    * @param actualOutput: the actual output of the ASCE
    * @return the computed value to set as output of the ASCE
    */
  override def eval(compInputs: Map[String, IasIO[_]], actualOutput: IasIO[Alarm]): IasIO[Alarm] = {
    assert(compInputs.size == 1)
    val input = compInputs.values.head.asInstanceOf[IasIO[Boolean]]
    assert(input.value.isDefined)

    val inputValue = input.value.head

    if (inputValue) {
      SimulatedSlowTF.logger.info("Waiting to trigger slowness...")
      Thread.sleep(timeToWait)
      SimulatedSlowTF.logger.info("Waked up")
    }

    if (inputValue) actualOutput.updateValue(Alarm.getSetDefault)
    else actualOutput.updateValue(Alarm.CLEARED)
  }
}

object SimulatedSlowTF {
  /** The logger */
  private val logger = IASLogger.getLogger(this.getClass)
}
