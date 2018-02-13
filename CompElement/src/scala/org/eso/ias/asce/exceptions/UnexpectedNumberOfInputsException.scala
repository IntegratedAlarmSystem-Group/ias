package org.eso.ias.asce.exceptions

/**
 * Exception thrown by the TF executor when the number of 
 * inputs does not match with what it expects
 * 
 * @param expected: Th eexpected number of HIOs
 * @param num: the actual number of HIOs
 */
class UnexpectedNumberOfInputsException(expected: Int, num: Int)
extends Exception("Got "+num+" HIOs instead of "+expected) {  
}