package org.eso.ias.asce.transfer

import org.eso.ias.types.Identifier

import java.util.{Optional, Properties}

/**
 * The PythonTransferExecutor provides the interface
 * for the execution of transfer functions in Python.
 * It uses Scala data structures (IasIO) like ScalaTransferExecutor.
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
    * @return the IasIO of the given ID, or None if not found
    */
  protected def getValue(
      inputs: Map[String, IasIO[?]],
      id: String,
      instance: Optional[Integer]
  ): Option[IasIO[?]] = {
    if (instance.isPresent) {
      val templatedID = Identifier.buildIdFromTemplate(id, instance.get())
      inputs.get(templatedID)
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
    * @return the IasIO of the given ID, or None if not found
    */
  protected def getValue(
      inputs: Map[String, IasIO[?]],
      id: String
  ): Option[IasIO[?]] = {
    if (Option(inputs).isEmpty) {
      throw new IllegalArgumentException("Invalid map of inputs")
    }
    if (Option(id).isEmpty || id.isEmpty) {
      throw new IllegalArgumentException("Invalid IASIO ID")
    }
    if (Identifier.isTemplatedIdentifier(id)) {
      throw new IllegalArgumentException("Templated IDs are forbidden here")
    }
    if (!isTemplated) {
      inputs.get(id)
    } else {
      inputs.get(id) match {
        case Some(value) => Some(value)
        case None =>
          val inst = getTemplateInstance.get()
          val templatedID = Identifier.buildIdFromTemplate(id, inst)
          inputs.get(templatedID)
      }
    }
  }

  /**
    * Produces the output of the component by evaluating the inputs.
    *
    * The IasIO is immutable. The easiest way to produce
    * the output to return is to execute the methods of the actualOutput
    * that return a new IasIO.
    *
    * @param compInputs: the inputs to the ASCE
    * @param actualOutput: the actual output of the ASCE
    * @return the computed value to set as output of the ASCE
    * @throws Exception in case of error
    */
  @throws[Exception]
  def eval(compInputs: Map[String, IasIO[?]], actualOutput: IasIO[T]): IasIO[T]

  /**
    * Initialize the transfer function
    *
    * @param inputsInfo The IDs and types of the inputs
    * @param outputInfo The Id and type of the output
    * @throws Exception in case of errors
    */
  @throws[Exception]
  def initialize(inputsInfo: Set[IasioInfo], outputInfo: IasioInfo): Unit

}
