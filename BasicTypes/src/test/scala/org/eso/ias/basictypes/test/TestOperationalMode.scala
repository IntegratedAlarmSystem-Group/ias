package org.eso.ias.basictypes.test

import org.eso.ias.types.OperationalMode
import org.scalatest.flatspec.AnyFlatSpec

import scala.collection.JavaConverters

/**
  * Test the operational modes
  */
class TestOperationalMode extends AnyFlatSpec {
  behavior of "The operational mode"

  it must "throw an exception if the inputs is empty" in {

    val emptyInputs: List[OperationalMode] = List.empty

    assertThrows[IllegalArgumentException] {
      OperationalMode.getModeFromInputs(JavaConverters.asJavaIterable(emptyInputs))
    }

  }

  it must "return the operationl mode if inputs are all the same" in {
    val inputs = List(OperationalMode.INITIALIZATION,OperationalMode.INITIALIZATION,OperationalMode.INITIALIZATION)
    assert(OperationalMode.getModeFromInputs(JavaConverters.asJavaIterable(inputs))==OperationalMode.INITIALIZATION)
  }

  it must "return UNKNOWN if inputs differ" in {
    val inputs = List(OperationalMode.INITIALIZATION,OperationalMode.OPERATIONAL,OperationalMode.MAINTENANCE)
    assert(OperationalMode.getModeFromInputs(JavaConverters.asJavaIterable(inputs))==OperationalMode.UNKNOWN)
  }

  it must "return the fallback if inputs differ" in {
    val inputs = List(OperationalMode.INITIALIZATION,OperationalMode.OPERATIONAL,OperationalMode.MAINTENANCE)
    assert(OperationalMode.getModeFromInputs(JavaConverters.asJavaIterable(inputs),OperationalMode.DEGRADED)==OperationalMode.DEGRADED)
  }

}
