package org.eso.ias.dasu.publisher

import org.eso.ias.types.IASValue

/**
 * The listener of events published by the output  publisher
 */
trait OutputListener {
  
  /** Notifies about a new output produced by the DASU */
  def outputEvent(output: IASValue[_])
  
  /** Notifies about a new output produced by the DASU 
   *  formatted as String
   */
  def outputStringifiedEvent(outputStr: String)
}