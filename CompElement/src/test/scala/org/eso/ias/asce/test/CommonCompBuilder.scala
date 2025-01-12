package org.eso.ias.asce.test

import org.eso.ias.types.Identifier
import org.eso.ias.types.InOut
import scala.collection.mutable.{Map => MutableMap }
import org.eso.ias.types.Validity
import org.eso.ias.types.IASTypes
import org.eso.ias.types.IdentifierType
import org.eso.ias.types.IASValue
import org.eso.ias.types.OperationalMode
import org.eso.ias.types.IasValidity

/**
 * A common helper class to build data structures for testing
 *
 * @param dasuId: The ID of the DASU where the ASCE runs (to build the iD)
 * @param asceId: The ID of the ASCE
 * @param outputId: the ID of the output HIO
 * @param outputType: the type of the output
 * @param inputTypes: the type of the inputs used to generate the inputs
 */
class CommonCompBuilder(
    dasuId: String,
    asceId: String,
    outputId: String,
    outputType: IASTypes,
    inputTypes: Set[IASTypes]) {
  require(Option[String](dasuId).isDefined)
  require(Option[String](asceId).isDefined)
  require(Option[String](outputId).isDefined)
  require(Option[IASTypes](outputType).isDefined)
  require(Option[Set[IASTypes]](inputTypes).isDefined)

  // The thread factory used by the setting to async
  // initialize and shutdown the TF objects
  val threadFactory = new TestThreadFactory()

  
  // The ID of the SUPERVISOR and the DASU where the components runs
  val supervId = new Identifier("SupervId",IdentifierType.SUPERVISOR,None)
  val dasId = new Identifier(dasuId,IdentifierType.DASU,supervId)

  // The ID of the component running into the DASU
  val compID = new Identifier(asceId,IdentifierType.ASCE,Option(dasId))

  // The ID of the output generated by the component
  val outId = new Identifier(outputId,IdentifierType.IASIO, Some(compID))
  
  // Build the IASIO in output
  val output = InOut.asOutput(
    outId,
    outputType)

  // The Map of IASValues for updating the inputs to the ASCE
  val inputsMPs: Set[InOut[?]]  = inputTypes.zipWithIndex.map( a => {
     InOut.asInput(
         new Identifier(("INPUT-HIO-ID#"+a._2), IdentifierType.IASIO,compID),
         a._1)
  })
     
  val requiredInputIDs: Set[String] = inputsMPs.map(_.id.id)
  
  /**
   * Auxiliary constructor to help instantiating a component
   * with the given number of inputs of the same type
   * 
   * @param dasuId: The ID of the DASU where the ASCE runs (to build the iD)
   * @param asceId: The ID of the ASCE
   * @param outputId: the ID of the output HIO
   * @param outputType: the type of the output
   * @param numOfInputs: the number of inputs to generate
   * @param hioType: the type of the inputs
   */
  def this(
    dasuId: String,
    asceId: String,
    outputId: String,
    outputType: IASTypes,
    numOfInputs: Int,
    hioType: IASTypes) = {
    this(dasuId,asceId,outputId, outputType,List.fill(numOfInputs)(hioType).toSet)
  }
}
