package org.eso.ias.asce.exceptions

/**
 * Exception for mismatch between the IDs of the IASIOs
 * of the constraint and the IDs of the inputs of the ASCE
 * 
 * @param asceId the ID of the ASCE
 * @param constraint the validity constraint
 * @param inputIds the IDs of the inputs
 * @param cause the cause of the error
 */
class ValidityConstraintsMismatchException(
    asceId: String,
    constraint: Iterable[String], 
    inputIds: Iterable[String], 
    cause: Throwable)
extends Exception(
    "ASCE ["+asceId+"]: validity constraints ("+constraint.mkString(",")+") mismatch with inputs ("+inputIds.mkString(",")+")",
    cause) {
  
  /**
   * Overloaded constructor without cause
   * 
   * @param asceId the ID of the ASCE
   * @param constraint the validity constraint
   * @param inputIds the IDs of the inputs
   */
  def this(asceId: String,constraint: Iterable[String],inputIds: Iterable[String]) = this(asceId,constraint,inputIds,null)
  
}