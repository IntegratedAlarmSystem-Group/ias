package org.eso.ias.basictypes.test

import org.scalatest.FlatSpec
import org.eso.ias.prototype.input.Validity

/**
 * Test the Validity
 * 
 * @author acaproni
 */
class TestValidity extends FlatSpec {
  
  "Only Reliable" must "be valid" in {
    val values = Validity.values
    
    for (value <- values) {
      if (!Validity.ValueOrdering.equiv(value,Validity.Reliable)) assert(!Validity.isValid(value))
    }
    assert(Validity.isValid(Validity.Reliable))
  }
  
  "Validity.min(list)" must "always return the min validity" in {
    // Build few "interesting" lists to submit to Validity.min
    val l1= List(Validity.Reliable,Validity.Reliable,Validity.Reliable,Validity.Reliable,Validity.Reliable)
    assert(Validity.min(l1)==Validity.Reliable)
    
    val l2= List(Validity.Unreliable,Validity.Unreliable,Validity.Unreliable,Validity.Unreliable)
    assert(Validity.min(l2)==Validity.Unreliable)
    
    val l3= List(Validity.Reliable,Validity.Unreliable,Validity.Reliable,Validity.Unreliable,Validity.Reliable)
    assert(Validity.min(l3)==Validity.Unreliable)
    
    val l4= List(Validity.Unreliable,Validity.Reliable,Validity.Reliable,Validity.Reliable,Validity.Reliable)
    assert(Validity.min(l4)==Validity.Unreliable)
    
    val l5= List(Validity.Reliable,Validity.Reliable,Validity.Reliable,Validity.Reliable,Validity.Unreliable)
    assert(Validity.min(l5)==Validity.Unreliable)
    
    val l6= List(Validity.Reliable,Validity.Reliable,Validity.Unreliable,Validity.Reliable,Validity.Reliable)
    assert(Validity.min(l6)==Validity.Unreliable)
    
  }
  
  it must "return Reliable for an empty list" in {
    assert(Validity.min(Nil)==Validity.Reliable)
  }
  
}