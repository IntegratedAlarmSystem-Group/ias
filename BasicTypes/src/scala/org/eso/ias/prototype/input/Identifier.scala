package org.eso.ias.prototype.input

import org.eso.ias.prototype.input.java.IdentifierType
import org.eso.ias.prototype.input.java.IASTypes

/**
 * Companion object
 */
object Identifier {
  
  /**
   * The separator between the ID and the parentID of the
   * runningID and the fullRunningID
   */
  val separator = "@"
  
  /**
   * In the fullRunningID, the id and the type are grouped
   * together to form a couple
   * 
   * coupleGroupPrefix is the prefix to group them in a string
   */
  val coupleGroupPrefix = "("
  
  /**
   * In the fullRunningID, the id and the type are grouped
   * together to form a couple
   * 
   * coupleGroupSuffix is the suffix to group them in a string
   */
  val coupleGroupSuffix = ")"
  
  /**
   * In the fullRunningID, the id and the type are grouped
   * together to form a couple
   * 
   * coupleSeparator is the separator between them in a string
   */
  val coupleSeparator = ":"
  
  /**
   * The regular expression of the fullRunning ID
   */
  val fullRunningIdRegExp = {
    val idRegExp = """[^:^\(^\).]+"""
    val coupleRegExp = """\("""+idRegExp+"+:"+idRegExp+"""\)"""
    coupleRegExp + "(@"+coupleRegExp+")*"
  }
  
  /**
   * Check if the passed full running id string has the proper format
   * 
   * @param frid The full running id string to check
   */
  def checkFullRunningIdFormat(frid: String): Boolean = {
    require(Option(frid).isDefined)
    frid.matches(fullRunningIdRegExp)
  }
  
  /**
   * Factory method to to build a identifier
   * from the passed fullRunningId string.
   * 
   * This method creates all the Identifiers specified in the
   * string and link them in the chain of parents.
   * 
   * @param fullRunningId the not null nor empty fullRunningId
   * @return a new Identifier
   */
  def apply(fullRunningId: String): Identifier = {
    require(Option(fullRunningId).isDefined)
    require(checkFullRunningIdFormat(fullRunningId),"Invalid fullRunningId format")
    
    // The id and type of each element passed in the parameter
    val identifiersDescr=fullRunningId.split(separator)
    
    identifiersDescr.foldLeft(None: Option[Identifier])( (id, couple) => {
      val cleanedCouple=couple.substring(Identifier.coupleGroupPrefix.size, couple.size-Identifier.coupleGroupSuffix.size)
      // The 2 parts of the couple (id and type)
      val identParts = cleanedCouple.split(Identifier.coupleSeparator)
      val idStr = identParts(0)
      val typeStr = identParts(1)
      Option(new Identifier(idStr,IdentifierType.valueOf(typeStr),id))
    }).get
  }
  
  /**
   * Extractor
   */
  def unapply(id: Identifier): String = {
    require(Option(id).isDefined)
    id.fullRunningID
  }
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
class Identifier(val id: String, val idType: IdentifierType, val parentID: Option[Identifier]) 
extends {
  require (Option(id).isDefined,"Invalid null Dasu ID")
  require(!id.isEmpty(),"Invalid empty identifier")
  require(id.indexOf(Identifier.separator) == -1,"Invalid character "+Identifier.separator+" in identifier "+id)
  require(id.indexOf(Identifier.coupleSeparator) == -1,"Invalid character "+Identifier.coupleSeparator+" in identifier "+id)
  require(id.indexOf(Identifier.coupleGroupSuffix) == -1,"Invalid character "+Identifier.coupleGroupSuffix+" in identifier "+id)
  require(id.indexOf(Identifier.coupleGroupPrefix) == -1,"Invalid character "+Identifier.coupleGroupPrefix+" in identifier "+id)
  require(Option(idType).isDefined,"Invalid identifier type")
  require(isValidParentType(idType, parentID), "Invalid parent for "+idType)
   
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
   * Auxiliary constructor, mostly to ease java/scala inter-operability.
   * 
   * It delegates to the constructor
   * 
   * @param id: the not <code>null</code> nor empty identifier
   * @param idType the not <code>null</code> nor empty type of the identifier
   * @parm parent the parent of the identifier, can be <code>null</code>
   * @return a new Identifier
   */
  def this(id: String, idType: IdentifierType, parent: Identifier) = {
    this(id,idType,Option(parent))
  }
  
  /**
   * Auxiliary constructor of an identifier without parent, 
   * mostly to ease java/scala inter-operability.
   * 
   * It delegates to the constructor
   * 
   * @param id: the not <code>null</code> nor empty identifier
   * @param idType the not <code>null</code> nor empty type of the identifier
   * @return a new Identifier
   */
  def this(id: String, idType: IdentifierType) = {
    this(id,idType,None)
  }
  
  /**
   * Check if the passed identifier is compatible
	 * with the type of the passed parent.
   * 
   * As described in the constructor each type has a subset of types
   * that can be set as parent.
   * 
   * @param theType: the type whose compatibility must be checked
   * @param parent Its parent
   */
  def isValidParentType(theType: IdentifierType, parent: Option[Identifier]): Boolean = {
    (theType, parent) match {
      case (IdentifierType.IASIO, None) => true // Unknown parent of IASIOs in the ASCE before they arrive
      case (x, None) => x.parents.length==0
      case (x, y) => x.parents.contains(y.get.idType)
    }
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
    buildFormattedID(theID, (ide: Identifier) => 
      Identifier.coupleGroupPrefix +
      ide.id +
      Identifier.coupleSeparator + 
      ide.idType.toString() +
      Identifier.coupleGroupSuffix)
  }
  
  /**
   * Build the running ID from the passed id and its parents.
   * 
   * @param theID: the Identifier to build the runningID
   * @return The running identifier of the passed identifier
   */
  private def buildRunningID(theID: Option[Identifier]): String = {
    buildFormattedID(theID, (ide: Identifier) => ide.id)
  }
  
  override def toString = fullRunningID
  
  /**
   * Search and return in this Identifier or in its parent
   * the id of the given type,, if any.
   * 
   * @return the ID of the identifier of the given type 
   */
  def getIdOfType(idTypeToSearch: IdentifierType): Option[String] = {
     require(Option(idTypeToSearch).isDefined,"Cannot search for an undefined identifier type")
     
     if (idTypeToSearch==idType) {
       Some(id)
     } else {
       parentID.flatMap(p => p.getIdOfType(idTypeToSearch))
     }
  }
}