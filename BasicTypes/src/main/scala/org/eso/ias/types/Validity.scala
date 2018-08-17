package org.eso.ias.types

import scala.language.implicitConversions

/**
 * The validity of an alarm or monitor point value is a measure of 
 * how much the value reflects actual situation of a monitored system.
 * 
 * Basically, if the value is produced propagated in time then it is reliable
 * otherwise is unreliable.
 * 
 * An invalid value or alarm is displaied with a proper color coding
 * (or other visualization strategy) to let the operator aware that
 * the information provided by the IAS might not reflect the actual situation.
 * 
 * The Validity is immutable 
 * 
 * @param validity the validity
 * @author acaproni
 */
class Validity(val iasValidity: IasValidity) extends Ordered[Validity] {
  
  
  /**
   * Check if the validity is "valid"
   * 
   * @return True if the passed validity is Reliable;
   * 			   False otherwise
   */
  def isValid(): Boolean = iasValidity==IasValidity.RELIABLE
  
  /**
   * Compare 2 validities by delegating to their reliabilityFactor
   */
  def compare(that: Validity) =  iasValidity.reliabilityFactor.compareTo(that.iasValidity.reliabilityFactor)
  
  /**
   * Compare this object with that one by delegating to the iasValidity
   * 
   * @return true if the receiver object is equivalent to the argument; false otherwise.
   */
  override def equals(that: Any): Boolean =
      that match {
          case that: Validity => this.iasValidity == that.iasValidity
          case _ => false
   }
  
  /** 
   *  Return the hash code of this object by delegating to iasValue  
   *  
   *  @return the hash code
   */
  override def hashCode: Int = iasValidity.hashCode()
  
  
  override def toString() = "Validity(IasValidity="+iasValidity.toString()+")"
}

object Validity {
  
  /**
   * Build a Validity from the the IasValidity
   */
  def apply(iasValidity: IasValidity): Validity = IasValidityToValidity(iasValidity)
  
  /**
   * Unapply simmetric to apply
   */
  def unapply(validity: Validity): IasValidity =  ValidityToIasValidity(validity)
  
  /**
   * Implicit conversion from Validity to IasValidity
   */
  implicit def ValidityToIasValidity(validity: Validity) = validity.iasValidity
  
  /**
   * Implicit conversion from  IasValidity to Validity
   */
  implicit def IasValidityToValidity(iasValidity: IasValidity) = new Validity(iasValidity)
  
  /**
   * Return the lowest validity between all
   * the validities of the passed iasValidities
   * 
   * @param iasValidities non empty set of validities to evaluate the new validity
   * @return the minimum validity between all the validities in the passed set
   */
  def minIasValidity(iasValidities: Set[IasValidity]) : IasValidity = {
    require(Option(iasValidities).isDefined,"Invalid set of validities")
    require(!iasValidities.isEmpty,"Cannot evaluate the min validity of an empty set of validities")
    
    minValidity(iasValidities.map( iasV => Validity(iasV))).iasValidity
  }
  
  /**
   * Return the lowest validity between all
   * the validities of the passed validities
   * 
   * @param validities the non empty set of validities to evaluate the new validity
   * @return the minimum validity between all the validities in the passed set
   */
  def minValidity(validities: Set[Validity]) : Validity = {
    require(Option(validities).isDefined,"Invalid set of validities")
    require(!validities.isEmpty,"Cannot evaluate the min validity of an empty set of validities")
    
    validities.min
  }
  
}
