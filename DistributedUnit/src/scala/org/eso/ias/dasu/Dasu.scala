package org.eso.ias.dasu

import org.ias.prototype.logging.IASLogger
import org.eso.ias.cdb.CdbReader
import org.eso.ias.cdb.json.JsonReader
import org.eso.ias.cdb.json.CdbFiles
import org.eso.ias.cdb.json.CdbJsonFiles
import org.eso.ias.cdb.pojos.DasuDao
import org.eso.ias.dasu.topology.Topology

import scala.collection.JavaConverters
import org.eso.ias.cdb.pojos.DasuDao
import org.eso.ias.prototype.input.Identifier
import org.eso.ias.prototype.input.java.IdentifierType

/**
 * The Distributed Alarm System Unit or DASU
 * 
 * @constructor create a DASU with the give identifier
 * @param is the identifier of the DASU 
 */
class Dasu(val id: String) {
  
  /** The logger */
  val logger = IASLogger.getLogger(Dasu.getClass);
  
  /** The identifier of the DASU
   *  
   *  In this version it has no parent identifier because
   *  the Supervisor has not yet been implemented
   */
  val dasuIdentifier: Identifier = new Identifier(id,IdentifierType.DASU,None)
  
  logger.info("Reading CDB configuration of [{}] DASU",dasuIdentifier)
  
  // Read configuration from CDB
  val cdbFiles: CdbFiles = new CdbJsonFiles("../test")
  val cdbReader: CdbReader = new JsonReader(cdbFiles)
  
  val dasuOptional = cdbReader.getDasu(id)
  require(dasuOptional.isPresent(),"DASU ["+id+"] configuration not found on cdb")
  val dasuDao: DasuDao = dasuOptional.get
  logger.debug("DASU [{}] configuration red from CDB",id)
  
  // Build the topology
  val dasuTopology: Topology = new Topology(
      id,
      dasuDao.getOutput().getId(),
      JavaConverters.asScalaSet(dasuDao.getAsces).toList)
  
  // -Get DASU configuration from CDB
  // - Build the topology of the ASCEs running in the DASU
  // - Build and activate the ASCEs
  
  // - Connect to the IASIOs queue (input and output)
  // - Start the loop:
  //  - Get IASIOs and forward them to the ASCEs
  //  - Get and send to the IASIO queue the output of the DASU
  
  
}

object Dasu {
  def main(args: Array[String]) {
    require(args.length==1,"DASU identifier missing in command line")
    
    
    val dasu = new Dasu(args(0))
  }
}