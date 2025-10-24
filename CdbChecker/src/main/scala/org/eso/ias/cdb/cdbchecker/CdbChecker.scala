package org.eso.ias.cdb.cdbchecker

import java.util.Optional
import com.typesafe.scalalogging.Logger
import org.apache.commons.cli.{CommandLine, CommandLineParser, DefaultParser, HelpFormatter, Options}
import org.eso.ias.cdb.{CdbReader, CdbReaderFactory}
import org.eso.ias.cdb.pojos._
import org.eso.ias.logging.IASLogger
import org.eso.ias.types.Identifier

import scala.collection.JavaConverters
import scala.jdk.javaapi.CollectionConverters
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
  * @param args the arguments in the command line to open the right CDB
  */
class CdbChecker(args: Array[String]) {
  require(Option(args).isDefined,"Invalid empty command line arguments")

  /** The reader of the JSON of RDB CDB */
  val reader: CdbReader = CdbReaderFactory.getCdbReader(args)

  

  /**
    * Check for errors
    *
    * @return True in case of errors; false otherwise
    */
  def check(): Boolean = {

    // True at least one error is found by the checks
    var errorsFound = false

    reader.init()

      // Are there errors in the IAS?
    val iasDaoOpt: Option[IasDao] = {
      val iasDaoOptional = reader.getIas
      if (iasDaoOptional.isPresent) Some(iasDaoOptional.get()) else None
    }
    iasDaoOpt.foreach(ias => CdbChecker.logger.info("IAS read"))
    val iasError: Boolean = checkIas(iasDaoOpt)

    /** The map of transfer functions: the key is the class of the TF */
    val mapOfTfs: Map[String, TransferFunctionDao] = {
      val tfsOptional = reader.getTransferFunctions
      val tfs: Set[TransferFunctionDao] = if (!tfsOptional.isPresent) Set.empty else {
        CollectionConverters.asScala(tfsOptional.get()).toSet
      }
      tfs.foldLeft(Map.empty[String,TransferFunctionDao])( (z, tf) => z+(tf.getClassName -> tf))
    }
    CdbChecker.logger.info("Read {} transfer functions",mapOfTfs.size)

    /** The map of templates where the key is the ID of the template */
    val mapOfTemplates: Map[String, TemplateDao] = {
      val templatesOptional = reader.getTemplates
      val templates: Set[TemplateDao] = if (!templatesOptional.isPresent) Set.empty else {
        CollectionConverters.asScala(templatesOptional.get()).toSet
      }
      templates.foldLeft(Map.empty[String,TemplateDao])( (z, t) => z+(t.getId -> t))
    }
    CdbChecker.logger.info("Read {} templates",mapOfTemplates.size)

    /**
       * The map of IASIOs where the key is the ID of the IASIOs
       *
       * These IASIOs do not take templates into account
       */
    val mapOfIasios: Map[String, IasioDao] = {
      val iasiosOptional = reader.getIasios
      val iasios: Set[IasioDao] = if (!iasiosOptional.isPresent) Set.empty else {
        CollectionConverters.asScala(iasiosOptional.get()).toSet
      }
      iasios.foldLeft(Map.empty[String,IasioDao])( (z, i) => z+(i.getId -> i))
    }
    CdbChecker.logger.info("Read {} IASIOs",mapOfIasios.size)

    /** The IDs of the IASIOs read from the CDB */
    val idsOfIasios: Set[String] = mapOfIasios.values.map(_.getId).toSet

    /** Method to convert IDs of Supervisors, DASUs and ASCEs to String  */
    def convertIdsFromReader(idsFromCdb: Try[Optional[java.util.Set[String]]]): Set[String] = {
      idsFromCdb match {
        case Failure(e) =>
          CdbChecker.logger.error("Error getting IDs",e)
          errorsFound = true
          Set.empty
        case Success(idsOptional) =>
          if (idsOptional.isPresent) CollectionConverters.asScala(idsOptional.get()).toSet else Set.empty
      }
    }

    /** The IDs of the supervisors */
    val idsOfSupervisors: Set[String] = {
      val tryToGetIds: Try[Optional[java.util.Set[String]]] = Try(reader.getSupervisorIds)
      convertIdsFromReader(tryToGetIds)
    }
    CdbChecker.logger.info("Read {} IDs of Supervisors: {}",idsOfSupervisors.size,idsOfSupervisors.mkString(","))

    /** Map of Supervisors, the key is the ID of the supervisor */
    val mapOfSupervisors: Map[String, SupervisorDao] = {
      idsOfSupervisors.foldLeft(Map.empty[String, SupervisorDao])( (z,id) => {
        val attempt = Try(reader.getSupervisor(id))
        attempt match {
          case Success(supervOptional) =>
            if (supervOptional.isPresent) {
              z+(id -> supervOptional.get())
            } else {
              CdbChecker.logger.error("Supervisor [{}] not found in CDB",id)
              errorsFound = true
              z
            }
          case Failure(f) =>
            CdbChecker.logger.error("Error getting Supervisor [{}] from CDB:",id,f)
            errorsFound = true
            z
        }
      })
    }

    /** The IDs of the DASUs read from the CDB */
    val idsOfDasus: Set[String] = {
      val tryToGetIds: Try[Optional[java.util.Set[String]]] = Try(reader.getDasuIds)
      convertIdsFromReader(tryToGetIds)
    }
    CdbChecker.logger.info("Read {} IDs of DASUs",idsOfDasus.size)

    /** The map of DasuDao; the key is the id of the DASU */
    val mapOfDasus: Map[String, DasuDao] = {
      idsOfDasus.foldLeft(Map.empty[String, DasuDao])( (z,id) => {
        val attempt = Try(reader.getDasu(id))
        attempt match {
          case Success(dasuOptional) =>
            if (dasuOptional.isPresent) {
              z+(id -> dasuOptional.get())
            } else {
              CdbChecker.logger.error("DASU [{}] not found in CDB",id)
              errorsFound = true
              z
            }
          case Failure(f) =>
            CdbChecker.logger.error("Error getting DASU [{}] from CDB:",id,f)
            errorsFound = true
            z
        }
      })
    }

    /** The IDs of the ASCEs */
    val idsOfAsces: Set[String] = {
      val tryToGetIds: Try[Optional[java.util.Set[String]]] = Try(reader.getAsceIds)
      convertIdsFromReader(tryToGetIds)
    }
    CdbChecker.logger.info("Read {} IDs of ASCEs",idsOfAsces.size)

    val mapOfAsces: Map[String, AsceDao] = {
      idsOfAsces.foldLeft(Map.empty[String, AsceDao])( (z,id) => {
        val attempt = Try(reader.getAsce(id))
        attempt match {
          case Success(asceOptional) =>
            if (asceOptional.isPresent) {
              z+(id -> asceOptional.get())
            } else {
              CdbChecker.logger.error("ASCE [{}] not found in CDB",id)
              errorsFound = true
              z
            }
          case Failure(f) =>
            CdbChecker.logger.error("Error getting ASCE [{}] from CDB:",id,f)
            errorsFound = true
            z
        }
      })
    }

    /**
      * The DASUs to deploy in each Supervisor
      * The key is the ID of the Supervisor
      */
    val mapOfDasusToDeploy: Map[String, Set[DasuToDeployDao]] = {
      idsOfSupervisors.foldLeft(Map.empty[String,Set[DasuToDeployDao]])( (z,id) => {
        val tryToGetDasus = Try(reader.getDasusToDeployInSupervisor(id))
        tryToGetDasus match {
          case Success(set) =>
            val setOfDtd: Set[DasuToDeployDao] = CollectionConverters.asScala(set).toSet
            CdbChecker.logger.info("{} DASUs to deploy on Supervisor [{}]: {}",
              setOfDtd.size.toString,
              id,
              setOfDtd.map(_.getDasu.getId).mkString(",")
            )
            // Normalize the DASUs to transform template input instances and
            // templates into concrete values
            z+(id -> setOfDtd)
          case Failure(f) =>
            CdbChecker.logger.error("Error getting DASUs of Supervisor [{}]:",id,f)
            errorsFound = true
            z
        }
      })
    }

    // Check if all the Supervisors have at least one DASU to deploy
    mapOfSupervisors.values.foreach( supervisorDao => {
      val dasusToDeployInSupervisor= supervisorDao.getDasusToDeploy
      if (dasusToDeployInSupervisor.isEmpty) {
        CdbChecker.logger.error("Supervisor [{}] has no DASU to run",supervisorDao.getId)
        errorsFound = true
      } else {
        CdbChecker.logger.debug("{} DASUs to deploy in {} Supervisor",
          supervisorDao.getDasusToDeploy.size(),
          supervisorDao.getId)
      }
    })

    // Check all the DASUs to deploy
    for {
      setOfDTD <- mapOfDasusToDeploy.values
      dtd <- setOfDTD
    } {
      errorsFound = errorsFound || checkDasuToDeploy(dtd)
      }

    // The IDs of all the DASUs to deploy (the id contains the instance of the template, if defined)
    val idsOfDasusToDeploy: Set[String] = (for {
      setOfDTD <- mapOfDasusToDeploy.values
      dtd <- setOfDTD
      dasu = Option(dtd.getDasu)
      id = dasu.map(_.getId)
      if id.isDefined
    } yield id.get).toSet

    // Check if each DASU to deploy corresponds to a DASU
    // taking into account that the DASU to deploy can be templated while the DASU is not
    idsOfDasusToDeploy.foreach(idtd => {
      if (!idsOfDasus.contains(Identifier.getBaseId(idtd))) {
        CdbChecker.logger.error("DASU to deploy [{}] does not correspond to any DASU: must be fixed",idtd)
        errorsFound = true
      }
    })

    // Check the DASUs
    for {
      id <- idsOfDasus
      dasu = mapOfDasus.get(id)
      if dasu.isDefined
    } errorsFound = errorsFound || checkDasu(dasu.get, idsOfIasios, mapOfTemplates)

    /** The IDs of the ASCEs to run in each DASU */
    val mapOfAscesOfDasus: Map[String, Set[String]] = buildMapOfAscesOfDasus(idsOfDasus, mapOfDasus)

    /** The IDs of the ASCEs instantiated by all the DASUs */
    val ascesOfDasus: Set[String] = mapOfAscesOfDasus.values.foldLeft(Set.empty[String])(( z,asces) => z++asces)

    // Is there any ASCE not instantiated by any DASU?
    for {
      asce <- idsOfAsces
      if !ascesOfDasus.contains(asce)
    } {
      CdbChecker.logger.error("ASCE [{}] not deployed in any DASU: can be removed",asce)
      errorsFound = true
    }

    // Are all the ASCE defined in the CDB?
    for {
      asce <- ascesOfDasus
      if !idsOfAsces.contains(asce)
    } {
      CdbChecker.logger.error("ASCE [{}] not defined in the CDB",asce)
      errorsFound = true
    }

    // Check the ASCEs
    for {
      asceId <- ascesOfDasus
      if idsOfAsces.contains(asceId)
      asce = mapOfAsces.get(asceId)
      if asce.isDefined
    } errorsFound = errorsFound || checkAsce(asce.get, idsOfDasus, mapOfTfs, idsOfIasios, mapOfTemplates)

    // Check for cycles
    val cyclesChecker= new CdbCyclesChecker(mapOfDasus,mapOfDasusToDeploy)
    val tryDasuWithCycles: Try[Iterable[String]] = Try(cyclesChecker.getDasusWithCycles())
    tryDasuWithCycles match {
      case Success(dasuWithCycles) =>
        if (dasuWithCycles.nonEmpty) {
          CdbChecker.logger.error("Found DASUs with cycles: {}",dasuWithCycles.mkString(","))
          errorsFound = true
        } else {
          CdbChecker.logger.info("NO cycles found in the DASUs")
        }
        val dasuTodeployWithCycles: Iterable[String] = cyclesChecker.getDasusToDeployWithCycles()
        if (dasuTodeployWithCycles.nonEmpty) {
          CdbChecker.logger.error("Found DASUs to deploy with cycles: {}",dasuTodeployWithCycles.mkString(","))
          errorsFound = true
        } else {
          CdbChecker.logger.info("NO cycles found in the DASUs")
        }
      case Failure(exception) =>
        CdbChecker.logger.error("Error getting the DASUs with cycles (fix to check for cycles):",exception)
        errorsFound = true
    }

    CdbChecker.logger.debug("Checking for duplicated IDs of tools (Supervisors, clients, plugins)...")
    // Check for duplicated IDs of tools
    val idsOfPlugins: List[String] = {
      val idsOptional = reader.getPluginIds
      if (idsOptional.isPresent) CollectionConverters.asScala(idsOptional.get()).toList
      else List.empty[String]
    }
    CdbChecker.logger.debug("Ids of plugins: {}",idsOfPlugins.mkString(","))
    val idsOfClients: List[String] = {
      val idsOptional = reader.getClientIds
      if (idsOptional.isPresent) CollectionConverters.asScala(idsOptional.get()).toList
      else List.empty[String]
    }
    CdbChecker.logger.debug("Ids of clients: {}",idsOfClients.mkString(","))
    val duplicatedIdsOfTools: Option[List[String]] = {
      val listOfIds: List[String] = idsOfPlugins:::idsOfClients:::idsOfSupervisors.toList
      listOfIds.groupBy(x => listOfIds.count(_==x)>1).get(true)
    }
    if (duplicatedIdsOfTools.isEmpty) {
      CdbChecker.logger.info("No duplication of IDs between Supervisors, Clients and Plugins")
    } else {
      // Removes duplicates in the list
      val dupIds = duplicatedIdsOfTools.get.toSet

      def formatMsg(tp: String, ids: List[String]) = tp+ " ["+ids.mkString(",")+"]"

      dupIds.foreach(duplicatedId => {
        val supervContainDupMsg =
          if (idsOfSupervisors.contains(duplicatedId)) formatMsg("Supervisors",idsOfSupervisors.toList)
          else ""
        val pluginContainDupMsg =
          if (idsOfPlugins.contains(duplicatedId)) formatMsg("Plugins",idsOfPlugins)
          else ""
        val clientContainDupMsg =
          if (idsOfClients.contains(duplicatedId)) formatMsg("Clients",idsOfClients)
          else ""
        CdbChecker.logger.error("Duplicated id [{}] found: check {} {} {} ",
          duplicatedId,supervContainDupMsg,pluginContainDupMsg,clientContainDupMsg)
        errorsFound = true
      })

    }


    CdbChecker.logger.debug("Shutting down the CDB reader")
    reader.shutdown()
    return errorsFound
  }

  /**
    * Build the map of the ASCEs to run in each DASU
    * The key is the ID of the DASU, the value is the set of IDs of ASCEs
    * to run in the DASU
    *
    * @param dasuIds The IDs of the DASUs read from the CDB
    * @param dasusMap The map of DasuDao where the key is the ID of the DASU
    * @return the map of the ASCEs to run in each DASU
    */
  def buildMapOfAscesOfDasus(dasuIds: Set[String], dasusMap: Map[String, DasuDao]): Map[String,Set[String]] = {
    dasuIds.foldLeft(Map.empty[String, Set[String]])((z, idOfDasu) => {
      dasusMap.get(idOfDasu) match {
        case Some(d) =>
          val asceIds = CollectionConverters.asScala(d.getAscesIDs).toSet
          z + (idOfDasu -> asceIds)
        case None =>
          CdbChecker.logger.error("Error getting DASU [{}]:", idOfDasu)
          z
      }
    })
  }

  /**
    * Check if there are errors in the ASCE
    *
    * @param asceDao The AsceDao to check for error
    * @param dasuIds The IDs of the DASUs read from the CDB
    * @param ttfsMap The map of transfer functions: the key is the class of the TF
    * @param iasioIds The IDDs of the IASIOs
    * @param templatesMap The map of templates where the key is the ID of the template
    * @return true in case of errors; false otherwise
    */
  def checkAsce(
    asceDao: AsceDao, 
    dasuIds: Set[String], 
    tffsMap: Map[String, TransferFunctionDao],
    iasioIds: Set[String],
    templatesMap: Map[String, TemplateDao]): Boolean = {
    require(Option(asceDao).isDefined)

    var errorsFound = false

    val id = Option(asceDao.getId)

    // This should never happen because the ID is the identifier
    if (id.isEmpty || id.get.isEmpty) {
      CdbChecker.logger.error("This DASU has no ID")
      errorsFound = true
    }
    CdbChecker.logger.trace("Checking ASCE [{}]",id.getOrElse("?"))

    val dasu = Option(asceDao.getDasu)
    if (dasu.isEmpty) {
      CdbChecker.logger.error("No DASU for ASCE [{}]",id.getOrElse("?"))
    } else {
      val dasuId = dasu.get.getId
      if (!dasuIds.contains(dasuId)) {
        CdbChecker.logger.error("ASCE [{}] will be deployied in DASU [{}} that is not defined in CDB",
          id.getOrElse("?"),
          dasuId)
        errorsFound = true
      }
    }

    val tf = asceDao.getTransferFunction
    if (!tffsMap.keySet.contains(tf.getClassName)) {
      CdbChecker.logger.error("TF {} of ASCE [{}] not defined in CDB",
        tf.getClassName,
        id.getOrElse("?"))
      errorsFound = true
    }

    // Are all the inputs defined?
    val inputs = CollectionConverters.asScala(asceDao.getIasiosIDs).toSet
    val templatedInputs = CollectionConverters.asScala(asceDao.getTemplatedInstanceInputs).toSet
    if (inputs.isEmpty && templatedInputs.isEmpty) {
      CdbChecker.logger.error("No inputs neither templated inputs defined for ASCE [{}]",id.getOrElse("?"))
      errorsFound = true
    }

    inputs.foreach(inputId => {
      if (!iasioIds.contains(inputId)) CdbChecker.logger.error("Input [{}] not defined for ASCE [{}]",
        inputId,
        id.getOrElse("?"))
    })

    templatedInputs.foreach( tii =>{
      val inputId = tii.getIasio.getId
      val template = tii.getTemplateId
      val instance = tii.getInstance()
      if (!iasioIds.contains(inputId)) CdbChecker.logger.error("Templated instance input [{}] not defined for ASCE [{}]",
        inputId,
        id.getOrElse("?"))


      val templateDao = templatesMap.get(template)
      templateDao match {
        case None =>
          CdbChecker.logger.error("Template {} of templated instance input {} not defined for ASCE [{}]",
            template,
            inputId,
            id.getOrElse("?"))
        case _ =>
          if (!checkTemplate(templateDao, Some(instance)))
            CdbChecker.logger.error("Template {} of templated instance input {} error for ASCE [{}] (see previous erro)",
              template,
              inputId, id.getOrElse("?"))
      }
    })

    val output: Option[IasioDao] = Option(asceDao.getOutput)
    if (output.isEmpty || output.get.getId.isEmpty) {
      CdbChecker.logger.error("No output fo ASCE [{}]",id.getOrElse("?"))
      errorsFound = true
    } else {
      if (!iasioIds.contains(output.get.getId)) {
        CdbChecker.logger.error("Output [{}] of ASCE [{}] not defined in CDB",
          if (output.isDefined) output.map(_.getId).getOrElse("?") else "NOT-FOUND",
          id.getOrElse("?"))
        errorsFound = true
      }
    }

    CdbChecker.logger.info("ASCE [{}] with output {} has {} inputs: {}",
      id.getOrElse("?"),
      if (output.isDefined) output.map(_.getId).getOrElse("?") else "NOT-FOUND",
      inputs.size,
      inputs.mkString(","))

    val templateId = Option(asceDao.getTemplateId)
    templateId.foreach(tid => {
      if (!templatesMap.keySet.contains(tid)) {
        CdbChecker.logger.error("Template [{}] not found for ASCE [{}]",tid,id.getOrElse("?"))
        errorsFound = true
      }
    })

    errorsFound
  }

  def checkIasio(iasioDao: IasioDao, templatesMap: Map[String, TemplateDao]): Boolean = {
    require(Option(iasioDao).isDefined)

    var errorsFound = false

    val id = Option(iasioDao.getId)

    // This should never happen because the ID is the identifier
    if (id.isEmpty || id.get.isEmpty) {
      CdbChecker.logger.error("This IASIO has no ID")
      errorsFound = true
    }

    val iType = Option(iasioDao.getIasType)
    if (iType.isEmpty) {
      CdbChecker.logger.error("Undefined type of IASIO {}",id.getOrElse("?"))
      errorsFound = true
    }

    val templateId = Option(iasioDao.getTemplateId)
    templateId.foreach(tid => {
      if (!templatesMap.keySet.contains(tid)) {
        CdbChecker.logger.error("Template [{}] not found for IASIO [{}]",tid,id.getOrElse("?"))
        errorsFound = true
      }
    })

    errorsFound
  }

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

    val templateOk: Boolean = checkTemplate(Option(dtd.getTemplate),Option(dtd.getInstance()))
    if (!templateOk) CdbChecker.logger.error("Error in template definition of DauToDeploy [{}]",dasuId)

    !templateOk || dasu.isEmpty
  }


  /**
    * Check if the IAS has been defined
    *
    * @param iasDaoOpt the IAS to check
    * @return true in case of errors, false otherwise
    */
  def checkIas(iasDaoOpt: Option[IasDao]): Boolean = {
    iasDaoOpt match {
      case None =>
        CdbChecker.logger.error("IAS not found")
        false
      case Some(ias) =>
        var errorFound = false
        if (ias.getRefreshRate<=0) {
          CdbChecker.logger.error("Refresh rate must be >0: {} found",ias.getRefreshRate)
          errorFound = true
        }

        if (ias.getValidityThreshold<=ias.getRefreshRate) {
          CdbChecker.logger.error("Validity threshold must be > refresh rate: {} found",ias.getValidityThreshold)
          errorFound = true
        }

        if (ias.getHbFrequency<=0) {
          CdbChecker.logger.error("HB frequency must be >0: {} found",ias.getHbFrequency)
          errorFound = true
        }

        val bsdb = Option(ias.getBsdbUrl)
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

  /**
    * Check if there are errors in the DASU
    *
    * @param dasuDao The DasuDao to check for error
    * @param iasioIds The IDDs of the IASIOs
    * @param templatesMap The map of templates where the key is the ID of the template
    * @return true in case of errors; false otherwise
    */
  def checkDasu(dasuDao: DasuDao, iasioIds: Set[String], templatesMap: Map[String, TemplateDao]): Boolean = {
    require(Option(dasuDao).isDefined)
    var errorsFound = false

    val id = Option(dasuDao.getId)

    // This should never happen because the ID is the identifier
    if (id.isEmpty || id.get.isEmpty) {
      CdbChecker.logger.error("This DASU has no ID")
      errorsFound = true
    }

    val output = Option(dasuDao.getOutput)
    if (output.isEmpty || output.get.getId.isEmpty) {
      CdbChecker.logger.error("No output fo DASU [{}]",id.getOrElse("?"))
      errorsFound = true
    } else {
      if (!iasioIds.contains(output.get.getId)) {
        CdbChecker.logger.error("Output [{}] of DASU [{}] not defined in CDB",
          output.get.getId,
          id.getOrElse("?"))
        errorsFound = true
      }
    }

    val asces: Set[AsceDao] = CollectionConverters.asScala(dasuDao.getAsces).toSet
    if (asces.isEmpty) {
      CdbChecker.logger.error("No ASCEs for DASU [{}] not defined in CDB", id.getOrElse("?"))
      errorsFound = true
    } else {
      CdbChecker.logger.info("{} ASCEs to deploy in DASU [{}]: {}",
        asces.size,
        id.getOrElse("?"),
        asces.map(_.getId).mkString(","))
    }

    val templateId = Option(dasuDao.getTemplateId)
    templateId.foreach(tid => {
      if (!templatesMap.keySet.contains(tid)) {
        CdbChecker.logger.error("Template [{}] not found for DASU [{}]",tid,id.getOrElse("?"))
        errorsFound = true
      }
    })

    errorsFound
  }



}

object CdbChecker {

  /** Build the usage message */
  val cmdLineSyntax: String = "cdbChecker [-h|--help] [-j|-jCdb JSON|YAML-CDB-PATH] [-c|--cdbClass <class>] [-x|--logLevel log level]"

  /**
    * Parse the command line.
    *
    * If help is requested, prints the message and exits.
    *
    * @param args The params read from the command line
    * @return the path of the cdb and the log level dao
    */
  def parseCommandLine(args: Array[String]): (Option[String], Option[LogLevelDao]) = {
    val options: Options = new Options
    options.addOption("h", "help",false,"Print help and exit")
    options.addOption("j", "jCdb", true, "Use the JSON Cdb at the passed path instead of the RDB")
    options.addOption("c", "cdbClass", true, "Use an external CDB reader with the passed class")
    options.addOption("x", "logLevel", true, "Set the log level (TRACE, DEBUG, INFO, WARN, ERROR)")

    val parser: CommandLineParser = new DefaultParser
    val cmdLineParseAction = Try(parser.parse(options,args))
    if (cmdLineParseAction.isFailure) {
      val e = cmdLineParseAction.asInstanceOf[Failure[Exception]].exception
      println(e.toString + "\n")
      new HelpFormatter().printHelp(cmdLineSyntax, options)
      System.exit(-1)
    }

    val cmdLine = cmdLineParseAction.asInstanceOf[Success[CommandLine]].value
    val help = cmdLine.hasOption('h')
    val jcdb = Option(cmdLine.getOptionValue('j'))

    val logLvl: Option[LogLevelDao] = {
      val t = Try(Option(cmdLine.getOptionValue('x')).map(level => LogLevelDao.valueOf(level)))
      t match {
        case Success(opt) => opt
        case Failure(f) =>
          println("Unrecognized log level")
          new HelpFormatter().printHelp(cmdLineSyntax, options)
          System.exit(-1)
          None
      }
    }

    if (help) {
      new HelpFormatter().printHelp(cmdLineSyntax, options)
      System.exit(0)
    }

    val ret = (jcdb, logLvl)
    CdbChecker.logger.info("Params from command line: jCdb={}, logLevel={}",
      ret._1.getOrElse("Undefined"),
      ret._2.getOrElse("Undefined"))
    ret

  }

  /** The logger */
  val logger: Logger = IASLogger.getLogger(CdbChecker.getClass)

  def main(args: Array[String]): Unit = {
    val parsedArgs = parseCommandLine(args)

    // Set the log level
    parsedArgs._2.foreach( level => IASLogger.setRootLogLevel(level.toLoggerLogLevel))

    // Invoke the cdb checker
    val cdbChecker = new CdbChecker(args)
    if (cdbChecker.check()) {
      logger.error("Errors found in the CDB")
      sys.exit(1)
    } else {
      logger.info("Done")
    }
  }
}
