package org.eso.ias.dasu

import org.eso.ias.prototype.input.InOut

/** A listener of inputs like the DASU */
trait InputsListener {
  /** 
   *  Notify about the reception of new inputs
   *  
   *  @param iasios the inputs received
   */
  def inputsReceived(iasios: Set[InOut[_]]);
}