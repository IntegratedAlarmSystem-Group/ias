package org.eso.ias.asce.exceptions

/**
 * Exception for mismatch between the IDs of the IASIOs
 * of the constraint and the IDs of the inputs of the ASCE
 * 
 * @param constraint the validity constraint
 * @param inputIds the IDs of the inputs
 * @param cause the cause of the error
 */
class ValidityConstraintsMismatchException(
    constraint: Set[String], 
    inputIds: Set[String], 
    cause: Throwable)
extends Exception(
    "Validity constraints ("+constraint.mkString(",")+") mismatch with inputs ("+inputIds.mkString(",")+")",
    cause) {
  
  /**
   * Overloaded constructor without cause
   * 
   * @param constraint the validity constraint
   * @param inputIds the IDs of the inputs
   */
  def this(constraint: Set[String],inputIds: Set[String]) = this(constraint,inputIds,null)
  
}