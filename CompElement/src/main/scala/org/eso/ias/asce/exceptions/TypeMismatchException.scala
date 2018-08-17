package org.eso.ias.asce.exceptions

import org.eso.ias.types.IASTypes
import java.util.Collection
import scala.collection.JavaConverters

/**
 * Exception thrown by the TF executor when the type
 * of a HIOs does not match with the expected on
 * 
 * @param message: A message explaining the mismatch
 * @param actualType: the type of the HIO
 * @param expectedType: the expected type of the HIO
 */
class TypeMismatchException(message: String) 
extends Exception(message) {
  
  /**
   * Specialized constructor that takes the actual and expected type.
   * 
   * @param hioId: The ID of the mismatched HIO
   * @param actualType: the type of the HIO
   * @param expectedType: the expected type of the HIO
   */
  def this(hioId: String, actualType: IASTypes, expectedType: IASTypes) {
    this("Type mismatch for HIO "+hioId+": expected was "+expectedType+" but "+actualType+" found")
  }
  
  /**
   * Specialized constructor that takes the actual type
   * and a set of expected types
   * 
   * @param hioId: The ID of the mismatched HIO
   * @param actualType: the type of the HIO
   * @param expectedTypes: the possible types of the HIO
   */
  def this(hioId: String, actualType: IASTypes, expectedTypes: Iterable[IASTypes]) {
    this("Type mismatch for HIO "+hioId+": its state, "+actualType+", not in ("+expectedTypes.mkString(",")+")")
  }
  
  /**
   * Specialized constructor that takes the actual type
   * and a set of expected types for java.
   * 
   * @param hioId: The ID of the mismatched HIO
   * @param actualType: the type of the HIO
   * @param expectedType: the possible types of the HIO
   */
  def this(hioId: String, actualType: IASTypes, expectedTypes: Collection[IASTypes]) {
    this(hioId,actualType,JavaConverters.collectionAsScalaIterable[IASTypes](expectedTypes))
  }
  
}