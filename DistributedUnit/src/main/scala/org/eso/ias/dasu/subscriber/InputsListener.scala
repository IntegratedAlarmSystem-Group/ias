package org.eso.ias.dasu.subscriber

import org.eso.ias.types.IASValue

/** A listener of inputs like the DASU */
trait InputsListener {
  /** 
   *  Notify about the reception of new inputs
   *  
   *  @param iasios the inputs received
   */
  def inputsReceived(iasios: Set[IASValue[_]]);
}