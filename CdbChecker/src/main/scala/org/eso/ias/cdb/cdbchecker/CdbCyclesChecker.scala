package org.eso.ias.extras.cdbchecker

import com.typesafe.scalalogging.Logger
import org.eso.ias.cdb.pojos.{DasuDao, DasuToDeployDao}
import org.eso.ias.cdb.topology.{AsceTopology, DasuTopology}
import org.eso.ias.logging.IASLogger

import scala.collection.JavaConverters

/**
  * Check and report cycles in the IAS by delgating to
  * classes in the cdb topology package
  *
  * @param mapOfDasus all the DASUs defined in the CDB
  * @param mapOfDasusToDeploy The DASUs to deploy in each Supervisor
  */
class CdbCyclesChecker(
                        val mapOfDasus: Map[String, DasuDao],
                        val mapOfDasusToDeploy: Map[String, Set[DasuToDeployDao]]) {

  /**
    * Check if the ASCEs defined in the pased DasuDao
    * create cycle
    *
    * @param dasu The dasu to check
    * @return true if there are cycles; false otherwise
    */
  def isDasuACyclic(dasu: DasuDao): Boolean = {
    require(Option(dasu).isDefined)
    CdbCyclesChecker.logger.debug("Checking cyclicity of DASU [{}]",dasu.getId)
    // The ASCE to deply in the DASU
    val asces = JavaConverters.asScalaSet(dasu.getAsces).toList
    // The topology of the ASCEs to run in the DASU
    val ascesTopology = asces.map(asceDao => {
      new AsceTopology(asceDao.getId,JavaConverters.asScalaSet(asceDao.getIasiosIDs).toSet,asceDao.getOutput.getId)
    })
    new DasuTopology(ascesTopology,dasu.getId,dasu.getOutput.getId).isACyclic
  }

  /**
    * Check and returns the IDs of DASUs whose ASCEs generate a cycle
    *
    * @return The ids of the DASUs whose ASCEs define a cycle
    */
  def getDasusWithCycles(): Iterable[String] = {
    for {
      dasuDao <- mapOfDasus.values
      if !isDasuACyclic(dasuDao)
    } yield dasuDao.getId
  }

  /**
    * Check and returns the IDs of DASUs whose ASCEs generate a cycle
    *
    * @return The ids of the DASUs whose ASCEs define a cycle
    */
  def getDasusToDeployWithCycles()  = {
    val t = mapOfDasusToDeploy.values.flatten
    for {
      dasuDao <- mapOfDasusToDeploy.values.flatten.map(_.getDasu)
      if !isDasuACyclic(dasuDao)
    } yield dasuDao.getId
  }
}
object CdbCyclesChecker {
  /** The logger */
  val logger: Logger = IASLogger.getLogger(CdbCyclesChecker.getClass)
}
