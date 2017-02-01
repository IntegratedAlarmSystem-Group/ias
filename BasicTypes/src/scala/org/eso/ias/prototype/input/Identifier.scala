package org.eso.ias.prototype.input

/**
 * Companion object
 */
object Identifier {
  /**
   * The separator between the ID and the parentID to build the runningID
   */
  val separator = ':'
}

/**
 * The immutable Identifier is composed of the uniqueID plus the ID of the parent.
 *
 * The parent of an item is the "owner" of the item itself:
 * <UL>
 * 	<LI> the owner of a monitor point is the alarm system component
 *       that receive the monitor point as input
 *  <LI> the owner of a component is the DAS where the component runs
 *  <LI> A DAS has no parent because nobody owns a DAS but they all
 *       collaborate to the functioning of the whole alarm system 
 * </UL>
 * 
 * This definition allows to have the same monitor point or component
 * connected to different part of system: they have the same ID but differs from 
 * the parent ID.
 * 
 * At run-time, to avoid traversing the parents, a unique ID is generated
 * in form of a string. 
 * 
 * The class is Iterable by definition as it contains a reference to the
 * Identifier of the parent.
 * 
 * @param id: The unique identifier
 * @param parentID: The identifier of the parent
 */
class Identifier(val id: Some[String], val parentID: Option[Identifier]) 
extends Ordered[Identifier] with Iterable[Identifier] {
  require (!id.get.isEmpty)
  require(id.get.indexOf(Identifier.separator) == -1)
  require(isAcyclic)
   
  /**
   * The runningID, composed of the id plus the id of the parents
   * allows to uniquely identify at object at run time
   */
  val runningID = buildRunningID(Option[Identifier](this))
  
  def iterator: Iterator[Identifier] = {
    var last = Option[Identifier](this)
    new Iterator[Identifier]() {
      def hasNext: Boolean = last!=None
      def next(): Identifier = {
        val toRet= last
        last = last.get.parentID
        toRet.get
      }
    }
  }
  
  /**
   * Build the running ID from the passed id and the Identifier's of the parents.
   * 
   * @param theID: the IDentifier to build the runningID
   * @return The running identifier of the passed identifier
   */
  private def buildRunningID(theID: Option[Identifier]): String = {
    if (theID.get.parentID==None) theID.get.id.get
    else theID.get.id.get + Identifier.separator + buildRunningID(theID.get.parentID)
  }
  
  /**
   * The list is ordered with this element being the first 
   * element of the list and the last parent being the last
   * item of the list
   * 
   * @return The ID as a list of identifiers
   */
  def asListOfIdentiers(): List[Identifier] = iterator.toList
  
  /**
   * Check if the same ID appears more then once
   * between the parents: if it is the case then there is a cyclic
   * between the dependencies.
   * 
   * @param theID: the IDentifier to check for cycles
   */
  def isAcyclic(): Boolean = {
    if (parentID==None) true
    else {
      val ids = asListOfIdentiers
      !ids.exists { id => id.asListOfIdentiers().tail.contains(id) }
    }
  }
  
  /**
   * Result of comparing this with operand that.
   * 
   * The comparison is delegated to the comparing of the runnigIDs strings.
   * 
   * @param that: The Identifier to compare with this one.
   * @see Ordered
   */
  def compare(that: Identifier) = this.runningID.compare(that.runningID)
  
  override def toString = runningID
}