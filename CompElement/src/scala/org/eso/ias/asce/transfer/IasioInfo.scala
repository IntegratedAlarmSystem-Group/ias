package org.eso.ias.asce.transfer

import org.eso.ias.types.IASTypes

/**
  * The association of ID and type to be passed to the initialize method
  * of the transfer function
  *
  * @param iasioId the ID of the IASIO
  * @param iasioType Th etype of the IASIO
  */
class IasioInfo(val iasioId: String, iasioType: IASTypes) {
  require(Option(iasioId).isDefined,"Invalid undefined id")
  require(Option(iasioType).isDefined,"Invalid undefined id")
}
