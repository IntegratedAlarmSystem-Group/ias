package org.eso.ias.basictypes.test

import org.scalatest.FlatSpec
import org.eso.ias.prototype.input.Validity
import org.eso.ias.prototype.input.java.IasValidity
import org.eso.ias.prototype.input.java.IasValidity._


/**
 * Test the Validity
 * 
 * @author acaproni
 */
class TestValidity extends FlatSpec {
  
  "Only Reliable" must "be valid" in {
    val values = IasValidity.values
    
    for (value <- values) {
      val validity = Validity(value)
      if (validity.iasValidity!=IasValidity.RELIABLE) assert(!validity.isValid())
      else assert(validity.isValid())
    }
  }
  
  "Validity.minValidity(Set)" must "always return the min validity" in {
    // Build few "interesting" lists to submit to Validity.min
    val l1= Set(RELIABLE)
    assert(Validity.minValidity(l1).get==Validity(RELIABLE))
    
    val l2= Set(UNRELIABLE)
    assert(Validity.minValidity(l2).get==Validity(UNRELIABLE))
    
    val l3= Set(RELIABLE,UNRELIABLE)
    assert(Validity.minValidity(l3).get==Validity(UNRELIABLE))
  }
  
}