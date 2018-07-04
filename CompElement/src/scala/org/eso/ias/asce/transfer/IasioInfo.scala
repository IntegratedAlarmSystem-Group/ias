package org.eso.ias.asce.transfer

import org.eso.ias.types.IASTypes

/**
  * The association of ID and type to be passed to the initialize method
  * of the transfer function
  *
  * @param iasioIs
  * @param iasioType
  */
class IasioInfo(val iasioIs: String, iasioType: IASTypes) {
  require(Option(iasioIs).isDefined,"Invalid undefined id")
  require(Option(iasioType).isDefined,"Invalid undefined id")
}
