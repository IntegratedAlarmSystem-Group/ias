package org.eso.ias.cdb.cdbchecker

import com.typesafe.scalalogging.Logger
import org.eso.ias.cdb.pojos.{DasuDao, DasuToDeployDao, TemplateInstanceIasioDao}
import org.eso.ias.cdb.topology.{AsceTopology, DasuTopology}
import org.eso.ias.logging.IASLogger
import org.eso.ias.types.Identifier

import scala.jdk.javaapi.CollectionConverters

/**
  * Check and report cycles in the IAS by delegating to
  * classes in the cdb topology package
  *
  * @param mapOfDasus all the DASUs defined in the CDB
  * @param mapOfDasusToDeploy The DASUs to deploy in each Supervisor
  */
class CdbCyclesChecker(
                        val mapOfDasus: Map[String, DasuDao],
                        val mapOfDasusToDeploy: Map[String, Set[DasuToDeployDao]]) {

  /**
    * Check if the ASCEs defined in the passed DasuDao
    * create cycle
    *
    * @param dasu The dasu to check
    * @return true if there are cycles; false otherwise
    */
  def isDasuACyclic(dasu: DasuDao): Boolean = {
    require(Option(dasu).isDefined)
    CdbCyclesChecker.logger.debug("Checking cyclicity of DASU [{}]",dasu.getId)
    // The ASCE to deploy in the DASU
    val asces = CollectionConverters.asScala(dasu.getAsces).toList
    // The topology of the ASCEs to run in the DASU
    val ascesTopology = asces.map(asceDao => {

      // Does this ASCE have template inputs
      val templatedInputs: Set[TemplateInstanceIasioDao] = CollectionConverters.asScala(asceDao.getTemplatedInstanceInputs).toSet
      val templatedInputsIds = templatedInputs.map(ti =>
        Identifier.buildIdFromTemplate(ti.getIasio.getId, ti.getInstance()))

      new AsceTopology(
        asceDao.getId,
        CollectionConverters.asScala(asceDao.getIasiosIDs).toSet++templatedInputsIds,
        asceDao.getOutput.getId)
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
