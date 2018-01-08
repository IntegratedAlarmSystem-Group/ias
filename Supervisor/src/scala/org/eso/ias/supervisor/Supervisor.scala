package org.eso.ias.supervisor

import org.eso.ias.cdb.CdbReader
import org.ias.prototype.logging.IASLogger

/**
 * A Supervisor is the container to run several DASUs into the same JVM.
 * 
 * @param id the identifier of the Supervisor
 * @param cdbReader the reader to get the configuration from the CDB
 */
class Supervisor (
    val id: String,
    cdbReader: CdbReader) {
  require(Option(id).isDefined && !id.isEmpty,"Invalid Supervisor identifier")
  
  // Get the list of DASus to activate from CDB
  // Activate the DASUs and get the list of IASIOs the want to receive
  // Connect to the input kafka topic passing the IDs of the IASIOs
  
  // Start the loop:
  // - get IASIOs, forward to the DASUs
  // - publish the IASOs produced by the DASU in the kafka topic
}

object Supervisor {
  
  /** The logger */
  private val logger = IASLogger.getLogger(Supervisor.getClass)
  
  def main(args: Array[String]) = {
    
  }
}