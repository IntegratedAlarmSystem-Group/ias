package org.eso.ias.extras.info

import com.typesafe.scalalogging.Logger
import org.eso.ias.cdb.CdbReader
import org.eso.ias.heartbeat.HeartbeatProducerType
import org.eso.ias.logging.IASLogger

import java.util
import java.util.Optional
import scala.jdk.javaapi.{CollectionConverters, OptionConverters}

/**
 * The tools to run, as read from the CDB
 *
 * Builds a list of IDs of the tools defined in the CDB for each type
 *
 * @param cdbReader The CDB reader (already initialized)
 */
class CdbDefinedTools(val cdbReader: CdbReader) {

  /** Convert the java structs returned by the reader to scala native data structures */
  private def convertToScalaList(data: Optional[java.util.Set[String]]): List[String] = {
    val scalaOpt = OptionConverters.toScala(data)
    scalaOpt.map(set => CollectionConverters.asScala(set).toList.sorted).getOrElse(Nil)
  }

  val pluginIds: List[String] = convertToScalaList(cdbReader.getPluginIds)
  val SupervisorIds: List[String] = convertToScalaList(cdbReader.getSupervisorIds)
  val getClientIds: List[String] = convertToScalaList(cdbReader.getClientIds)
  val sinkIds =  List[String]()
  val coreToolsIds = List[String]()
  val converterIds = List[String]()

  /** The frequency to submit HBs in the IAS (msecs) */
  val hbFrequency: Option[Int] =  OptionConverters.toScala(cdbReader.getIas.map(ias => ias.getHbFrequency*1000))

  /** Associate the HB type to the IDs of the tools */
  val idsByType: Map[HeartbeatProducerType, List[String]] = Map(
    HeartbeatProducerType.SUPERVISOR -> SupervisorIds,
    HeartbeatProducerType.PLUGIN -> pluginIds,
    HeartbeatProducerType.CLIENT -> getClientIds,
    HeartbeatProducerType.CONVERTER -> converterIds,
    HeartbeatProducerType.SINK -> sinkIds,
    HeartbeatProducerType.CORETOOL -> coreToolsIds
  )

  /** return the IDs of the given HB type */
  def getIds(hbType: HeartbeatProducerType): List[String] = idsByType(hbType)

  def idEmpty(hbType: HeartbeatProducerType): Boolean = getIds(hbType).isEmpty
}

/** Companion object */
object CdbDefinedTools {

  /** The logger */
  val logger: Logger = IASLogger.getLogger(CdbDefinedTools.getClass)

  val hbType: Array[HeartbeatProducerType] = HeartbeatProducerType.values()
  val types = (for tp <- hbType yield tp).sorted
}
