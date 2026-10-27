package org.eso.ias.asce.transfer

import org.eso.ias.types.Identifier

import java.util.{Optional, Set => JavaSet, Map => JavaMap, Properties}
import java.util.Objects

/**
 * The PythonTransferExecutor provides the interface
 * for the execution of transfer functions in Python.
 * It uses Java data structures (IasIOJ) for interop compatibility.
 *
 * @param cEleId: The id of the ASCE
 * @param cEleRunningId: the running ID of the ASCE
 * @param validityTimeFrame: The time frame (msec) to invalidate monitor points
 * @param props: The properties for the executor
 */
abstract class PythonTransferExecutor[T](
    cEleId: String,
    cEleRunningId: String,
    validityTimeFrame: Long,
    props: Properties
) extends TransferExecutor(cEleId, cEleRunningId, validityTimeFrame, props) {

  /**
   * This method is mostly to get templated input instances for which the
   * instance, not known to the ASCE, must be passed.
   *
   * If the instance is empty, it delegates to getValue(inputs, id).
   *
   * @param inputs the map of the inputs
   * @param id The (non templated) identifier of the value
   * @param instance the optional instance number for templated input instances
   * @return the IasIOJ of the given ID, or None if not found
   */
  protected def getValue(
      inputs: JavaMap[String, IasIOJ[?]],
      id: String,
      instance: Optional[Integer]
  ): Option[IasIOJ[?]] = {
    if (instance.isPresent) {
      val templatedID = Identifier.buildIdFromTemplate(id, instance.get())
      Option(inputs.get(templatedID))
    } else {
      getValue(inputs, id)
    }
  }

  /**
   * This method transparently returns a value from the passed ID,
   * being the ASCE templated or not.
   *
   * If the ASCE is not templated, it delegates to `inputs.get(id)`.
   *
   * If the ASCE is generated out of a template,
   * its inputs can or cannot be generated out of the same template. In the latter,
   * their identifiers must be enriched with the number of the instance.
   *
   * @param inputs the map of the inputs
   * @param id The (non templated) identifier of the value
   * @return the IasIOJ of the given ID, or None if not found
   */
  protected def getValue(
      inputs: JavaMap[String, IasIOJ[?]],
      id: String
  ): Option[IasIOJ[?]] = {
    Objects.requireNonNull(inputs, "Invalid map of inputs")
    Objects.requireNonNull(id, "Invalid IASIO ID")
    if (Identifier.isTemplatedIdentifier(id)) {
      throw new IllegalArgumentException("Templated IDs are forbidden here")
    }
    if (!isTemplated) {
      Option(inputs.get(id))
    } else {
      var ret: IasIOJ[?] = inputs.get(id)
      if (ret == null) {
        val instance = getTemplateInstance.get()
        val templatedID = Identifier.buildIdFromTemplate(id, instance)
        ret = inputs.get(templatedID)
      }
      Option(ret)
    }
  }

  /**
   * Produces the output of the component by evaluating the inputs.
   *
   * The IasIOJ is immutable. The easiest way to produce
   * the output to return is to execute the methods of the actualOutput
   * that return a new IasIOJ.
   *
   * @param compInputs: the inputs to the ASCE
   * @param actualOutput: the actual output of the ASCE
   * @return the computed value to set as output of the ASCE
   * @throws Exception in case of error
   */
  @throws[Exception]
  def eval(compInputs: JavaMap[String, IasIOJ[?]], actualOutput: IasIOJ[T]): IasIOJ[T]

  /**
   * Initialize the transfer function
   *
   * @param inputsInfo The IDs and types of the inputs
   * @param outputInfo The Id and type of the output
   * @throws Exception in case of errors
   */
  @throws[Exception]
  def initialize(inputsInfo: JavaSet[IasioInfo], outputInfo: IasioInfo): Unit

}
