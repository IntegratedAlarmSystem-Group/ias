package org.eso.ias.cdb.checker

import java.util.Optional

import org.eso.ias.cdb.CdbReader
import org.eso.ias.cdb.json.{CdbJsonFiles, JsonReader}
import org.eso.ias.cdb.pojos._
import org.eso.ias.cdb.rdb.RdbReader
import org.eso.ias.logging.IASLogger
import org.eso.ias.supervisor.Supervisor

import scala.collection.JavaConverters
import scala.util.{Failure, Success, Try}

/**
  * Che CdbChecker checks for problems in the CDB
  * in either JSON or RDB format.
  *
  * The main task is to check if the graph is -acyclic
  * (@see  [[https://github.com/IntegratedAlarmSystem-Group/ias/issues issue #70]])
  * but reports also other problems or inconsistency like unused IASIOs just to give an example.
  * While the RDB is more robust against such problems, the JSON implementation is more weak
  * and this tool could help.
  *
  * @param jsonCdbPath The path of the JSON CDB, if empty checks the structure of the RDB
  */
class CdbChecker(val jsonCdbPath: Option[String]) {
  Option(jsonCdbPath).orElse(throw new IllegalArgumentException("Invalid null jsonCdbPath"))

  /** The reader of the the JSON of RDB CDB */
  val reader: CdbReader = {
    jsonCdbPath match {
      case None => new RdbReader
      case Some(path) =>
        if (path.isEmpty) new IllegalArgumentException("Invalid empy CDB PATH")
        val cdbJSonFiles = new CdbJsonFiles(path)
        new JsonReader(cdbJSonFiles)
    }
  }

  // Are there errors in the IAS?
  val iasDaoOpt: Option[IasDao] = {
    val iasDaoOptional = reader.getIas
    if (iasDaoOptional.isPresent) Some(iasDaoOptional.get()) else None
  }
  iasDaoOpt.foreach(ias => CdbChecker.logger.info("IAS read"))
  val iasError = checkIas(iasDaoOpt)

  /** The map of transferfunctions where the key is the class of the TF */
  val mapOfTfs: Map[String, TransferFunctionDao] = {
    val tfsOptional = reader.getTransferFunctions
    val tfs: Set[TransferFunctionDao] = if (!tfsOptional.isPresent) Set.empty else {
      JavaConverters.asScalaSet(tfsOptional.get()).toSet
    }
    tfs.foldLeft(Map.empty[String,TransferFunctionDao])( (z, tf) => z+(tf.getClassName -> tf))
  }
  CdbChecker.logger.info("Read {} transfer functions",mapOfTfs.size)

  /** The map of transfer functions where the key is the ID of the template */
  val mapOfTemplates: Map[String, TemplateDao] = {
    val templatesOptional = reader.getTemplates
    val templates: Set[TemplateDao] = if (!templatesOptional.isPresent) Set.empty else {
      JavaConverters.asScalaSet(templatesOptional.get()).toSet
    }
    templates.foldLeft(Map.empty[String,TemplateDao])( (z, t) => z+(t.getId() -> t))
  }
  CdbChecker.logger.info("Read {} templates",mapOfTemplates.size)

   /** The map of IASIOs where the key is the ID of the IASIOS */
  val mapOfIasios: Map[String, IasioDao] = {
    val iasiosOptional = reader.getIasios
    val iasios: Set[IasioDao] = if (!iasiosOptional.isPresent) Set.empty else {
      JavaConverters.asScalaSet(iasiosOptional.get()).toSet
    }
    iasios.foldLeft(Map.empty[String,IasioDao])( (z, i) => z+(i.getId() -> i))
  }
  CdbChecker.logger.info("Read {} IASIOs",mapOfIasios.size)

  /** Method that convert IDs of Supervisors, DASUs and ASCEs to String  */
  private[this] def convertIdsFromReader(idsFromCdb: Try[Optional[java.util.Set[String]]]): Set[String] = {
    idsFromCdb match {
      case Failure(e) =>
        CdbChecker.logger.error("Error getting IDs",e)
        Set.empty
      case Success(idsOptional) =>
        if (idsOptional.isPresent) JavaConverters.asScalaSet(idsOptional.get()).toSet else Set.empty
    }
  }

  /** The IDs of the supervisors */
  val idsOfSupervisors: Set[String] = {
    val tryToGetIds: Try[Optional[java.util.Set[String]]] = Try(reader.getSupervisorIds())
    convertIdsFromReader(tryToGetIds)
  }
  CdbChecker.logger.info("Read {} IDs of Supervisors",idsOfSupervisors.size)

  /** The IDs of the DASUs */
  val idsOfDasus: Set[String] = {
    val tryToGetIds: Try[Optional[java.util.Set[String]]] = Try(reader.getDasuIds())
    convertIdsFromReader(tryToGetIds)
  }
  CdbChecker.logger.info("Read {} IDs of DASUs",idsOfDasus.size)

  /** The IDs of the ASCEs */
  val idsOfAsces: Set[String] = {
    val tryToGetIds: Try[Optional[java.util.Set[String]]] = Try(reader.getAsceIds())
    convertIdsFromReader(tryToGetIds)
  }
  CdbChecker.logger.info("Read {} IDs of ASCEs",idsOfAsces.size)

  /**
    * The DASUs to deploy in each Supervisor
    * The key is the ID of teh Supervisor
    */
  val dasusToDeploy: Map[String, Set[DasuToDeployDao]] = {
    idsOfSupervisors.foldLeft(Map.empty[String,Set[DasuToDeployDao]])( (z,id) => {
      var tryToGetDasus = Try(reader.getDasusToDeployInSupervisor(id))
      tryToGetDasus match {
        case Success(set) =>
          var setOfDtd: Set[DasuToDeployDao] = JavaConverters.asScalaSet(set).toSet
          CdbChecker.logger.info("{} DASUs to deploy on Supervisor [{}]",setOfDtd.size.toString,id)
          z+(id -> setOfDtd)
        case Failure(f) =>
          CdbChecker.logger.error("Error getting DASUS of Supervisor [{}]",id,f)
          z
      }
    })
  }
  // Check al the DASUs to deploy
  for {
    setOfDTD <- dasusToDeploy.values
    dtd <- setOfDTD
  } checkDasuToDeploy(dtd)

  /**
    * Check if the passed template and instance are valid
    *
    * @param template The template as set in the DASU to deploy for example
    * @param instance the instance number
    * @return false in case of error, true otherwise
    */
  def checkTemplate(template: Option[TemplateDao], instance: Option[Integer]): Boolean = {
    (template.isDefined, instance.isDefined) match {
      case (true, false) =>
        CdbChecker.logger.error("Template is defined ({}) but instance number is not",  template.get.getId)
        false
      case (false, true) =>
        CdbChecker.logger.error("Instance is defined ({}) but there is not template",instance.get.toString)
        false
      case (false, false) => true
      case (true, true) =>
        val i = instance.get
        val min = template.get.getMin
        val max = template.get.getMax
        if (i<min || i>max) {
          CdbChecker.logger.error("Instance ({}) out of range[{}.{}]",i.toString,min.toString,max.toString)
          false
        } else {
          true
        }
    }
  }

  /** Check the DasuToDeploy */
  def checkDasuToDeploy(dtd: DasuToDeployDao): Boolean = {
    require(Option(dtd).isDefined)
    val dasu = Option(dtd.getDasu)
    if (dasu.isEmpty) {
      CdbChecker.logger.error("No  DASU for this DasuToDeploy)")
    }
    val dasuId: String = dasu.map(_.getId).getOrElse("?")

    val templateOk:Boolean = checkTemplate(Option(dtd.getTemplate),Option(dtd.getInstance()))
    if (!templateOk) CdbChecker.logger.error("Error in template definition of DauToDeploy [{}]",dasuId)

    !templateOk || dasu.isEmpty
  }


  /** Check if the IAS has been defined */
  def checkIas(iasDaoOpt: Option[IasDao]): Boolean = {
    iasDaoOpt match {
      case None =>
        CdbChecker.logger.error("IAS not found")
        false
      case Some(ias) =>
        var errorFound = false
        if (ias.getRefreshRate()<=0) {
          CdbChecker.logger.error("Refresh rate must be >0: {} found",ias.getRefreshRate())
          errorFound = true
        }

        if (ias.getTolerance()<=0) {
          CdbChecker.logger.error("Tolerance must be >0: {} found",ias.getTolerance())
          errorFound = true
        }

        if (ias.getHbFrequency()<=0) {
          CdbChecker.logger.error("HB frequency must be >0: {} found",ias.getHbFrequency())
          errorFound = true
        }

        val bsdb = Option(ias.getBsdbUrl())
        if (bsdb.isEmpty || bsdb.get.isEmpty) {
          CdbChecker.logger.error("BSDB URL must be defined and not empty")
          errorFound = true
        } else {
          // The BSDB URL must be a comma separated string of server:port strings
          val s = bsdb.get.split(",")
          s.foreach(url => {
            val serverPort = url.split(":")
            if (serverPort.size!=2) {
              CdbChecker.logger.error("Invalid BSDB URL format {}",url)
              errorFound = true
            } else {
              // Check if port is a valid integer
              val port: Try[Integer] = Try(Integer.valueOf(serverPort(1)))
              port match {
                case Failure(e) =>
                  CdbChecker.logger.error("Invalid BSDB port {}:{}",serverPort(0),serverPort(1))
                  errorFound = true
                case Success(p) =>
                  if (p<=0) {
                    CdbChecker.logger.error("Invalid BSDB port {}",p)
                    errorFound = true
                  }
              }
            }
          })
        }
        errorFound
    }


  }



}

object CdbChecker {
  val usage = """
      |USAGE: CdbChecker [-jcdb <PATH>]
      |-jcdb <PATH>: checks the JSON CDB at the PATH location
      |              if not present connects to the RDB
      |
      |Checks the RDB of JSON CDB.
    """.stripMargin

  /** The logger */
  val logger = IASLogger.getLogger(Supervisor.getClass)

  def main(args: Array[String]): Unit = {
    args.size match {
      case 0 =>
        // RDB
        val checker = new CdbChecker(None)
      case 2 =>
        // JSON
        val checker = new CdbChecker(Some(args(1)))
      case _ =>
        // Wrong command line args
        logger.error("Invalid command line")
        println(usage)
        System.exit(-1)
    }
  }
}
