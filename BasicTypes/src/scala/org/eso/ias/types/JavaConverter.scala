
package org.eso.ias.types

import org.eso.ias.types.IASTypes._
import java.util.Optional

/**
 * Helper methods to convert from java to scala and vice-versa.
 * 
 * @author acaproni
 */
object JavaConverter {
  
  /**
   * Convert a scala InOut in a java IASValue.
   * 
   * The validity of the IASValue can be either the validity
   * received from the BSDB (the IASIO is the input of a ASCE) or the one
   * inherited from the inputs (the IASIO is the output of the ASCE)
   * 
   * @param io: the IASIO to convert to java IASValue
   * @param validity: the validity of the iASValue
   * @return The java value version of the passed HIO 
   */
  def inOutToIASValue[T](io: InOut[_]): IASValue[_] = {
    require(Option(io).isDefined)
    io.toIASValue()
  }
  
  /**
   * Update a IASIO with the passed IASValue
   * 
   * @param iasio: the IASIO to update
   * @param iasValue: the value to update the passed IASIO
   * @return The IASIO updated with the passed value
   */
  def updateInOutWithIasValue[T](iasio: InOut[T], iasValue: IASValue[_]): InOut[_] = {
    assert(Option(iasio).isDefined)
    assert(Option(iasValue).isDefined)
    
    iasio.update(iasValue)
    
  }
  
  /**
   * Convert a java Optional to a scala Option
   */
  def toScalaOption[T](jOpt: Optional[T]): Option[T] = if (jOpt.isPresent) Some(jOpt.get()) else None
}
