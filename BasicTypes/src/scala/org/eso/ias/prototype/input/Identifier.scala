package org.eso.ias.prototype.input

import org.eso.ias.prototype.input.java.IdentifierType

/**
 * Companion object
 */
object Identifier {
  /**
   * The separator between the ID and the parentID to build the runningID
   */
  val separator = "@"
}

/**
 * The immutable Identifier is a recursive data structure composed of a unique ID 
 * plus the ID of the parent.
 * The ID, in turn, is a couple with unique identifier plus the type of the identifier
 * 
 * The identifier (id,type) is the unique identifier of an element of the core.
 * Even if not intuitive (and strongly discouraged), it is possible to have different elements 
 * like for example a DASU and an ASCE with the same ids because their types differ.
 *
 * The parent of an item is the "owner" of the item itself:
 * <UL>
 * 	<LI> the owner of a monitor point is the monitored system that produced it
 *  <LI> the owner of a component is the DASU where the component runs
 *  <LI> the owner of the DASU is the Supervisor where the DASU runs
 *  <LI> the Supervisor has no owner, being a the top level of the inclusion
 * </UL>
 * 
 * Another way to think of an Identifier is a way to describe the IAS components
 * that contribute to generate a IASIO. The identifier of all the other IAS components
 * can be deducted in the same way.
 * There are 2 possible way to generate a IASIO: getting a monitored value of alarm from
 * a remote monitored system and inject into the core or the output of a ASCE/DASU.
 * <BR>
 * In the former case the IAS components that collaborate to produce the IASIOs are
 * the monitored system, the plugin who gets the value, the converter that translated 
 * the value in a IAS data structure. 
 * In the latter case the components that collaborate to generate a IASIO are the 
 * the Supervisor, the DASU and the ASCE.
 * <BR>
 * There are therefore only 2 possible types of identifiers, for the 2 cases described upon:
 * <UL>
 * 	<LI>Monitored system->->Plugin->Converter->IASIO
 * 	<LI>Supervisor->DASU->ASCE->IASIO
 * </UL> 
 * 
 * The constructor checks if the Identifier that is going to build is compatible
 * with the type of the passed parent. 
 * 
 * This definition of the identifier shows the deployment because the parentID depends
 * on where/how an element is calculated deployed. 
 * The id of an item is supposed to be unique in the system: the deployment information
 * is mostly useful for debugging as it allows to follow the computation path
 * of a IASIO.
 * 
 * At run-time, to avoid traversing the parents, a unique ID is generated
 * in form of a string. 
 * 
 * The class is Iterable by definition as it contains a reference to the
 * Identifier of the parent.
 *  
 * @constructor Builds an identifier with a ID, a type and its parent  
 * @param id: The unique identifier
 * @param iDType: The type of the identifier
 * @param parentID: The identifier of the parent
 */
class Identifier(val id: Some[String], val idType: Some[IdentifierType], val parentID: Option[Identifier]) 
extends {
  require (!id.get.isEmpty)
  require(id.get.indexOf(Identifier.separator) == -1,"Invalid character "+Identifier.separator+" in identifier "+id.get)
  require(isValidParentType(idType.get, parentID), "Invalid parent for "+idType.get)
   
  /**
   * The runningID, composed of the id plus the id of the parents
   * allows to uniquely identify at object at run time but note
   * that it brings deployment information so it cannot be used
   * as a unique identifier because changing the deployment would trigger 
   * a change of the ID.
   * 
   * The runningID is composed of the IDs only i.e. without
   * the types
   */
  lazy val runningID = buildRunningID(Option[Identifier](this))
  
  /**
   * The fullRunning is the complete version of the runningID that 
   * includes also the type of each ID.
   * 
   * It brings deployment information for which aoolies the same consideration of
   * runningID
   * 
   * @see runningID
   */
  lazy val fullRunningID=buildFullRunningID(Option[Identifier](this));
  
  /**
   * Check if the passed identifier is compatible
	 * with the type of the passed parent.
   * 
   * As described in the constructor each type has a subset of types
   * that can be set as parent.
   * 
   * @param theType: the type whose compatibility must be checked
   * @parm parent Its parent
   */
  def isValidParentType(theType: IdentifierType, parent: Option[Identifier]): Boolean = {
    parent.fold(theType.parents.length==0)(pId => theType.parents.contains(pId.idType.get))
  }
  
  /**
   * Build a string representation of the identifier and its parents
   * formatting each identifier with the passed method
   * 
   * @param theID The identifier to build the string representation
   * @param format: the function to customize the output of the identifier
   */
  private def buildFormattedID(theID: Option[Identifier], format: (Identifier)=>String): String = {
    theID.fold("")( aId => aId.parentID.fold(format(aId))(pId => buildFormattedID(Some(pId),format)+Identifier.separator+format(aId)))
  }
  
  /**
   * Build the full running ID from the passed id and its the parents.
   * 
   * @param theID: the Identifier to build the runningID
   * @return The running identifier of the passed identifier
   */
  private def buildFullRunningID(theID: Option[Identifier]): String = {
    buildFormattedID(theID, (ide: Identifier) => "("+ide.id.get+","+ide.idType.get.toString()+")")
  }
  
  /**
   * Build the running ID from the passed id and its parents.
   * 
   * @param theID: the Identifier to build the runningID
   * @return The running identifier of the passed identifier
   */
  private def buildRunningID(theID: Option[Identifier]): String = {
    buildFormattedID(theID, (ide: Identifier) => ide.id.get)
  }
  
  override def toString = fullRunningID
}