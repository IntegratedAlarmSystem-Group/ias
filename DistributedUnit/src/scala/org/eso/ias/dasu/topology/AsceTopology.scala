package org.eso.ias.dasu.topology

import org.eso.ias.cdb.pojos.AsceDao
import scala.collection.JavaConverters.collectionAsScalaIterable

/** The view of an ASCE based on its IASIOs in input
 *  and the generated output.
 * 
 * @constructor build the ACSE view for topology
 * @param identifier the identifier of the ASCE
 * @param inputs the identifier of the IASIOs in input
 * @param output the identifier of the IASIO produced by the ASCE
 * @author acaproni
 */
class AsceTopology(
    val identifier: String, 
    val inputs: Set[String], 
    val output: String) {
  require(Option(identifier).isDefined)
  require(identifier.size>0,"Invalid ASCE identifier")
  require(Option(inputs).isDefined)
  require(inputs.size>0,"An ASCE must have inputs")
  require(Option(output).isDefined)
  require(output.size>0,"Invalid output identifier")
  
  // Consistency check
  require(!inputs.contains(output),"The same IASIO can't be input and output of the same ASCE")
  
  /** Builds a AcseTopology from its CDB configuration
   * 
   * @param asceDao the configuration of the ASCE red from the CDB
   */
  def this(asceDao: AsceDao) = {
    this(
        asceDao.getId(),
        collectionAsScalaIterable(asceDao.getInputs()).map(iasid => iasid.getId).toSet,
        asceDao.getOutput().getId())
  }
  
  /** Check if this ASCE is connected to the passed one
   *  
   *  @param asce the ASCE to check for connection
   *  @return true if this ACSE is connected to the passed one
   */
  def isConnectedToAsce(asce: AsceTopology): Boolean = {
    asce.inputs.contains(output)
  }
  
  /**
   * Check if the passed IASIO is a required input
   * 
   * @parm iasioId the ID of the IASIO to check
   * @return true if the IASIO with the passed ID is an input of the ASCE
   */
  def isRequiredInput(iasioId: String) = {
    inputs.contains(iasioId)
  }
  
  override def toString = {
    val ret = new StringBuilder("ASCE [")
    ret.append(identifier)
    ret.append("]:  output=")
    ret.append(output)
    ret.append(", inputs=")
    ret.append(inputs.toList.sorted.mkString(", "))
    ret.toString()
  }
}