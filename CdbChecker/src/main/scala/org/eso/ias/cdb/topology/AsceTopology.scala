package org.eso.ias.cdb.topology

import org.eso.ias.cdb.pojos.AsceDao

import scala.jdk.javaapi.CollectionConverters

/** The view of an ASCE based on its IASIOs in input
 *  and the generated output.
 * 
 * @constructor build the ACSE view for topology
 * @param id the identifier of the ASCE
 * @param inputsIds the identifier of the IASIOs in input
 * @param outputId the identifier of the IASIO produced by the ASCE
 * @author acaproni
 */
class AsceTopology(
                    override val id: String,
                    override val inputsIds: Set[String],
                    override val outputId: String) extends OutputProducer {
  require(Option(id).isDefined)
  require(id.nonEmpty,"Invalid ASCE identifier")
  require(Option(inputsIds).isDefined)
  require(inputsIds.nonEmpty,"ASCE "+id+" has NO inputs")
  require(Option(outputId).isDefined)
  require(outputId.nonEmpty,"ASCE "+id+" has invalid output")

  // Consistency check
  require(!inputsIds.contains(outputId),"The same IASIO can't be input and output of the same ASCE "+id)

  /** Builds a AcseTopology from its CDB configuration
   *
   * @param asceDao the configuration of the ASCE red from the CDB
   */
  def this(asceDao: AsceDao) = {
    this(
        asceDao.getId(),
      CollectionConverters.asScala(asceDao.getInputs()).map(iasid => iasid.getId).toSet,
        asceDao.getOutput().getId())
  }

  /** Check if this ASCE is connected to the passed one
   *
   *  @param asce the ASCE to check for connection
   *  @return true if this ACSE is connected to the passed one
   */
  def isConnectedToAsce(asce: AsceTopology): Boolean = {
    asce.inputsIds.contains(outputId)
  }

  /**
   * Check if the passed IASIO is a required input
   *
   * @parm iasioId the ID of the IASIO to check
   * @return true if the IASIO with the passed ID is an input of the ASCE
   */
  def isRequiredInput(iasioId: String) = {
    inputsIds.contains(iasioId)
  }

  override def toString = {
    val ret = new StringBuilder("Topology of ASCE [")
    ret.append(id)
    ret.append("]:  output=")
    ret.append(outputId)
    ret.append(", inputs=")
    ret.append(inputsIds.toList.sorted.mkString(", "))
    ret.toString()
  }
}