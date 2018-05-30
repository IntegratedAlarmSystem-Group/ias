package org.eso.ias.basictypes.test

import org.scalatest.FlatSpec
import org.eso.ias.types.Validity
import org.eso.ias.types.IasValidity
import org.eso.ias.types.IasValidity._


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
  
  "Validity.minIasValidity(Set)" must "always return the min validity" in {
    // Build few "interesting" lists to submit to Validity.min
    val l1= Set(RELIABLE)
    assert(Validity.minIasValidity(l1)==RELIABLE)
    
    val l2= Set(UNRELIABLE)
    assert(Validity.minIasValidity(l2)==UNRELIABLE)
    
    val l3= Set(RELIABLE,UNRELIABLE)
    assert(Validity.minIasValidity(l3)==UNRELIABLE)
  }
  
  "Validity.minIasValidity(Set)" must "match with Validity.minValidity(Set)" in {
    val iasV1= Set(RELIABLE)
    val v1 = Set(Validity(RELIABLE))
    assert(Validity.minIasValidity(iasV1)==Validity.minValidity(v1).iasValidity)
    
    val iasV2 = Set(UNRELIABLE)
    val v2 = Set(Validity(UNRELIABLE))
    assert(Validity.minIasValidity(iasV2)==Validity.minValidity(v2).iasValidity)
    
    val iasV3 = Set(RELIABLE,UNRELIABLE)
    val v3 = Set(Validity(RELIABLE),Validity(UNRELIABLE))
    assert(Validity.minIasValidity(iasV3)==Validity.minValidity(v3).iasValidity) 
  }
  
}