package org.eso.ias.types

import IdentifierType._

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
   * The prefix of a identifier generated from a template:
   * it is something like ID[!#n!] where n is the n-th 
   * instance of the template egnerated from ID 
   */
  val templatedIdPrefix = "[!#"
  
  /**
   * The suffix of an identifier built from 
   * a template
   */
  val templateSuffix = "!]"
  
  /**
   * The regular expression of the fullRunning ID
   */
  val fullRunningIdRegExp = {
    val idRegExp = """[^:^\(^\).]+"""
    val coupleRegExp = """\("""+idRegExp+"+:"+idRegExp+"""\)"""
    coupleRegExp + "(@"+coupleRegExp+")*"
  }
  
  /**
   * The regular expression of a template 
   * that matches with strings like [!#4343!]
   * 
   * This is used to get the number of an instance
   */
  val templateRegExp = """\[!#\d+!\]""".r
  
  /**
   *  The regular expression for a identifier
   *  generated from a template
   */
  val templatedIdRegExp =  (".+"+templateRegExp.regex).r
  
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
   * Check if the passed identifier is generated from a
   * template i.e. if id matches with templatedIdRegExp
   * 
   * @param id the identifier to check
   */
  def isTemplatedIdentifier(id : String): Boolean = {
    require(Option(id).isDefined)
    templatedIdRegExp.findAllIn(id).length==1
  }
  
  /**
   * Check if the given type supports template
   */
  def canBeTemplated(idType: IdentifierType): Boolean = {
    require(Option(idType).isDefined)
    
    idType match {
      case IASIO | DASU | ASCE => true
      case _ => false
    }
  }
  
  /**
   * Build the identifier from a string and a instance number.
   * The instance number, if present, denotes that the identifier
   * is an instance of a template; if the instance is not
   * defined then the identifier is not generated out
   * of a template.
   * 
   * @param id: the identifier
   * @param instance: the number of the templated instance, if defined
   * @return the id for the instance instance of a template
   */
  def buildIdFromTemplate(
      id: String, 
      instance: Option[Int]): String = {
    require(Option(id).isDefined && !id.trim().isEmpty(),"Invalid identifier")
    require(Option(instance).isDefined)
    if (instance.isDefined) {
      require(!Identifier.isTemplatedIdentifier(id),id+" is already generated from a template")
      "%s%s%d%s".format(id,Identifier.templatedIdPrefix,instance.get,Identifier.templateSuffix)
    } else id
  }
  
  /**
   * Build the identifier from a string and a instance number.
   * 
   * @param id: the identifier
   * @param instance: the number of the templated instance
   * @return the id for the instance instance of a template
   */
  def buildIdFromTemplate(
      id: String, 
      instance: Int): String = {
    buildIdFromTemplate(id,Option(instance))
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
 * The ID, in turn, is a couple with unique identifier plus the type of the identifier.
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
 * There are 2 possible ways to generate a IASIO: getting a monitored value of alarm from
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
 * Dedicated auxiliary constructors support the generation of replicated items, 
 * being them ASCEs, DASUs or IASIOs, from and identifier and the the given instance number
 * by appending to the passed ID, a suffix followed by the number of the instance and the suffix.
 * The constructor does not make any check to ensure that the passed instance number
 * matches with the definition of the template in the CDB.
 * The method buildFromTemplate(String, Option[Int]) builds an identifier from a template.
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
 * @param id: The not null nor empty identifier
 * @param iDType: The type of the identifier
 * @param parentID: The identifier of the parent
 */
class Identifier(
    val id: String,
    val idType: IdentifierType, 
    val parentID: Option[Identifier]) 
extends {
  require (Option(id).isDefined,"Invalid null ID")
  require(!id.isEmpty(),"Invalid empty identifier")
  require(id.indexOf(Identifier.separator) == -1,"Invalid character "+Identifier.separator+" in identifier "+id)
  require(id.indexOf(Identifier.coupleSeparator) == -1,"Invalid character "+Identifier.coupleSeparator+" in identifier "+id)
  require(id.indexOf(Identifier.coupleGroupSuffix) == -1,"Invalid character "+Identifier.coupleGroupSuffix+" in identifier "+id)
  require(id.indexOf(Identifier.coupleGroupPrefix) == -1,"Invalid character "+Identifier.coupleGroupPrefix+" in identifier "+id)
  require(Option(idType).isDefined,"Invalid identifier type")
  require(isValidParentType(idType, parentID), "Invalid parent for "+idType)
  
  // The pattern of the template must appear at most once in the id
  require(Identifier.templatedIdRegExp.findAllIn(id).length<=1,"Template pattern mismatch in "+id)
  
  // True if the ID is generated from a template
  val fromTemplate = Identifier.isTemplatedIdentifier(id)
  if (fromTemplate) require(Identifier.canBeTemplated(idType),idType+" does not support template")
  
  // The number of the instance or empty if the
  // identifier is not generated from a template
  lazy val templateInstance: Option[Int] = {
    // Extract the instance from a string like [!#333!]
    def extractInstance(temp: String): Int = temp.split("#")(1).split("!")(0).toInt
    
    val templates = Identifier.templateRegExp.findAllMatchIn(id).map(_.matched).toList
    
    // The identifier can be generated by at most one template 
    assert(templates.length<=1,"Templated mismatch identifier "+id)
    
    val v = Option(templates).filter(_.nonEmpty).map(a => a.head)
    v.map(extractInstance(_)) 
  }
   
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
   * Auxiliary constructor, of a non-templated identifier
   * mostly to ease java/scala inter-operability.
   * 
   * @param id: the not <code>null</code> nor empty identifier
   * @param idType the not <code>null</code> nor empty type of the identifier
   * @parm parent the parent of the identifier, can be <code>null</code>
   */
  def this(id: String, idType: IdentifierType, parent: Identifier) = {
    this(id,idType,Option(parent))
  }
  
  /**
   * Auxiliary constructor of a non-templated identifier without parent, 
   * mostly to ease java/scala inter-operability.
   * 
   * @param id: the not <code>null</code> nor empty identifier
   * @param idType the not <code>null</code> type of the identifier
   */
  def this(id: String, idType: IdentifierType) = {
    this(id,idType,None)
  }
  
  /**
   * Auxiliary constructor of possibly templated identifier
   * 
   * @param id: the not <code>null</code> nor empty identifier
   * @param instance the number of the instance of a templated identifier, if defined
   * @param idType the not <code>null</code> type of the identifier
   * @param parent the parent of the identifier, can be <code>null</code>
   */
  def this(id: String, instance: Option[Int], idType: IdentifierType, parentID: Option[Identifier]) {
    this(Identifier.buildIdFromTemplate(id,instance),idType,parentID)
  }
  
  /**
   * Auxiliary constructor of possibly templated identifier: this auxiliary constructor
   * is meant to ease the construction of an identifier from java
   * 
   * @param id: the not <code>null</code> nor empty identifier
   * @param idType the not <code>null</code> type of the identifier
   * @param instance the number of the instance of a templated identifier, if not null
   * @param parent the parent of the identifier, can be <code>null</code>
   */
  def this(id: String, idType: IdentifierType, instance: Int, parentID: Identifier) {
    this(id,Option(instance),idType,Option(parentID))
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
   * the id of the given type, if any.
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
  
  /** 
   *  canEqual method checks the class of the passed
   *  @param other the object to compare
   */
  def canEqual(other: Any): Boolean = other.isInstanceOf[Identifier]
  
  /**
   * Override equals
   * 
   * For equality it is enough to check the fullRuningId
   */
  override def equals(other: Any): Boolean = {
    other match {
      case that: Identifier =>
        (that canEqual this) && fullRunningID==that.fullRunningID
      case _ => false
    }
  }
  
  /**
   * Override hashCode
   */
  override def hashCode: Int =  fullRunningID.##
}