package org.eso.ias.supervisor

import com.typesafe.scalalogging.Logger
import org.eso.ias.cdb.pojos._
import org.eso.ias.logging.IASLogger
import org.eso.ias.types.Identifier

import scala.collection.JavaConverters

/**
 * TemplateHelper helps transforming DASUs, ASCEs and IASIOs 
 * defined by templates into concrete instances.
 * 
 * @param dasusToDeploy The dasus to deploy in the Supervisor
 */
class TemplateHelper(
    val dasusToDeploy :Set[DasuToDeployDao]) {
  require(Option(dasusToDeploy).isDefined)

  // The DASUs with a template that needs to be normalized
  val templatedDasusToDeploy: Set[DasuToDeployDao] = dasusToDeploy.filter(dtd => Option(dtd.getTemplate).isDefined)
  assert(templatedDasusToDeploy.forall(dtd => Option(dtd.getInstance).isDefined))
  
  // The DASUs to deploy in the Supervisor that have no template
  val normalDasusToDeploy: Set[DasuToDeployDao] = dasusToDeploy.filter(dtd => Option(dtd.getTemplate).isEmpty)
  assert(normalDasusToDeploy.forall(dtd => Option(dtd.getInstance).isEmpty))
  
  require(checkTemplateConstraints(templatedDasusToDeploy),"Template constraints violated")
  TemplateHelper.logger.info("Template constraints OK")

  TemplateHelper.logger.info("{} DasuDao to convert from template ({}) and {} standard DasuDao ({})",
      templatedDasusToDeploy.size,
      templatedDasusToDeploy.map(_.getDasu.getId).mkString(","),
      normalDasusToDeploy.size,
      normalDasusToDeploy.map(_.getDasu.getId).mkString(","))
      
  /**
   * Ensure that 
   * - all the ASCEs of a DASU have the same template
   * - all the inputs have the same template or are not templated
   * - all the output have the same template templated
    *
    * Templated inputs are a special case because their template can differ from that
    * of the DASU.
   * 
   * @param dasusToDep the templated DASUs to deploy in the supervisor
   * @return true if all the DASU, ASCEs and IASIOs respect the constraints of the template
   */
  def checkTemplateConstraints(dasusToDep: Set[DasuToDeployDao]): Boolean = {
    TemplateHelper.logger.debug("Checking constraints of {} templated DASUs ({})",
        dasusToDep.size.toString,
        dasusToDep.map(_.getDasu.getId).mkString(", "))
    dasusToDep.forall(dtd => { 
      require(Option(dtd.getTemplate).isDefined,"The DASU to deploy must be templated")
      val templateId = dtd.getTemplate.getId
      val instance = dtd.getInstance
      
      // Is the instance in the range of the allowed instances of the template
      val instanceOk = instance>=dtd.getTemplate.getMin && instance<=dtd.getTemplate.getMax
      
      if (!instanceOk) {
        TemplateHelper.logger.error("Instance {} out of range [{},{}] of template {}",
            instance,
            dtd.getTemplate.getMin.toString,
            dtd.getTemplate.getMin.toString,
            dtd.getTemplate.getId)
      }

      // The ASCEs to deploy in the DASU
      val asces = JavaConverters.asScalaSet(dtd.getDasu.getAsces)
      
      // Do all the ASCEs have the same template of the DASU?
      asces.foreach(a => println(a.getTemplateId))
      val ascesOk = asces.forall(asce => templateId==asce.getTemplateId)
      
      if (!ascesOk) {
        TemplateHelper.logger.error("Template mismatch in the ASCEs {} of the DASU {}: should be {}",
            asces.map(_.getId).mkString(", "),
            dtd.getDasu.getId,
            templateId)
      }
      
      // Do the output of each ASCE (and so the output of the DASU)
      // have the same template of the DASU?
      val outputsOfAscesOk = asces.forall(asce => templateId==asce.getOutput.getTemplateId)
      
      if (!outputsOfAscesOk) {
        TemplateHelper.logger.error("Template mismatch in the outputs {} of the ASCEs ({}) of the DASU {}: template should be {}",
            asces.map(_.getOutput.getId).mkString(", "),
            asces.map(_.getId).mkString(", "),
            dtd.getDasu.getId,
            templateId)
      }
      
      // Does the output have the same template of the DASU?
      //
      // This is redundand, actually because the output of the DASU is the outputt
      // of one of its ASCEs already checked in outputsOfAscesOk
      val outputOk = templateId==dtd.getDasu.getOutput.getTemplateId
      
      if (!outputOk) {
        TemplateHelper.logger.error("Template mismatch in the output {} of the DASU {}",
            dtd.getDasu.getOutput.getId,
            dtd.getDasu.getId)
      }
      
      // Inputs can have the same template of the DASU or being not templated
      val inputs = asces.foldLeft(Set.empty[IasioDao])( (set, asce) => set++JavaConverters.collectionAsScalaIterable(asce.getInputs))
      val inputsOk = inputs.isEmpty || inputs.forall(iasio => Option(iasio.getTemplateId).isEmpty || templateId==iasio.getTemplateId)
      
      if (!inputsOk) {
        TemplateHelper.logger.error("Inputs ({}) of DASU {} must have no template or {}",
            inputs.map(_.getId).mkString(", "),
            dtd.getDasu.getId,
            templateId)
      }

      // Templated inputs can have a different template but and have an instance defined
      val templatedInputs = asces.foldLeft(Set.empty[TemplateInstanceIasioDao])((set, asce) => set++JavaConverters.collectionAsScalaIterable(asce.getTemplatedInstanceInputs))
      val templatedInputsOk = templatedInputs.isEmpty || templatedInputs.forall( ti => Option(ti.getTemplateId).isDefined)
      if (!templatedInputsOk) {
        TemplateHelper.logger.error("Templated inputs ({}) of DASU {} must have a defined template",
          templatedInputs.map(_.getIasio.getId).mkString(", "),
          dtd.getDasu.getId)
      }

      instanceOk && ascesOk && outputsOfAscesOk && outputOk && inputsOk && templatedInputsOk
    })
  }
  
  /**
   * Normalize the passed IasioDao to a IasioDao where the identifier
   * has been replaced with the version generated from the instance of the template 
   * 
   * @param iasio The IasioDao to whose identifier needs to be converted 
   * @return the converted IasioDao if the parameter was templated, 
   *         the same iasio otherwise
   */
  private def normalizeIasio(iasio: IasioDao, instance: Int): IasioDao = {
    if (Option(iasio.getTemplateId).isDefined) {
      TemplateHelper.logger.debug("Normalizing templated IASIO [{}] with instance []", iasio.getId, instance.toString)
      val idFromTemplate = Identifier.buildIdFromTemplate(iasio.getId, Some(instance))
      iasio.setId(idFromTemplate)
      TemplateHelper.logger.debug("IASIO normalized with new ID [{}] ", iasio.getId)
    } else {
      TemplateHelper.logger.debug("IASIO [{}] is not templated: no need to nromalize", iasio.getId)
    }
    iasio
  }

  /**
    * Normalize the passed ASCE by adding the templated instance inputs
    * to the list of the standard inputs, after renaming their IDs.
    *
    * Note that these kind of inputs can be set also for non templated DASU
    *
    * @param asce The ASCE to normalize the templated input instances
    * @return The ASCE normalized for templated inputs instances
    */
  def normalizeAsceWithTemplatedInstanceInputs(asce: AsceDao) {
    require(Option(asce).isDefined)
    TemplateHelper.logger.debug("Normalizing templated input instances of ASCE [{}] with inputs {}",
      asce.getId,
      JavaConverters.collectionAsScalaIterable(asce.getIasiosIDs).mkString(","))

    val numOfInputsBefore = asce.getInputs.size()
    val numOfTemplatedInstanceInputs = asce.getTemplatedInstanceInputs.size()


    if (!asce.getTemplatedInstanceInputs.isEmpty) {
      val templatedInstanceInputs = JavaConverters.collectionAsScalaIterable(asce.getTemplatedInstanceInputs)
      templatedInstanceInputs.foreach( templatedInstance => {
        asce.addInput(normalizeIasio(templatedInstance.getIasio,templatedInstance.getInstance()), false)
      })
      asce.getTemplatedInstanceInputs.clear()
      TemplateHelper.logger.debug("New inputs of ASCE [{}] after normalizing tempated input instances: {}",
        asce.getId,
        JavaConverters.collectionAsScalaIterable(asce.getIasiosIDs).mkString(","))
    }

    assert(numOfInputsBefore+numOfTemplatedInstanceInputs==asce.getInputs.size(), "Wrong number of templated input instances converted")
  }
  
  /**
    * Normalize the passed ASCE:
    * - the output is always templated
    * - the ID of the ASCE is always templated
    * - the inputs can or cannot be templated
    *
    * Apart of the inputs with the same template of the ASCE,
    * there might also be inputs generated by other templates:
    * these needs to be converted and added to the inputs of the ASCE.
    *
    *
    * @param asce the templated AscDao to normalize
    * @return the normalized AsceDaso
    */
  private def normalizeAsce(asce: AsceDao, instance: Int): AsceDao = {
    require(Option(asce).isDefined)
    require(Option(asce.getTemplateId).isDefined,"Template ASCE required")

    TemplateHelper.logger.debug("Normalizing templated ASCE [{}] with instance {}", asce.getId, instance.toString)
    
    // Set the identifier
    asce.setId(Identifier.buildIdFromTemplate(asce.getId, Some(instance)))
    
    // The output is templated (it is a constraint)
    val newOutput = normalizeIasio(asce.getOutput,instance)
    asce.setOutput(newOutput)
    
    // Normalize the inputs that have the same teplate of the ASCE
    val inputs = JavaConverters.collectionAsScalaIterable(asce.getInputs).toSet
    asce.getInputs.clear()
    inputs.foreach( input => {
      if (Option(input.getTemplateId).isDefined) {
        asce.addInput(normalizeIasio(input,instance), false)
      } else {
        asce.addInput(input,false)
      }
    })
    assert(inputs.size==asce.getInputs.size(), "Wrong number of inputs of ASCE!")


    TemplateHelper.logger.debug("ASCE normalized with new ID [{}] and inputs=[{}]",
        asce.getId,
        JavaConverters.collectionAsScalaIterable(asce.getInputs).map(_.getId).mkString(","))
    asce
  }
  
  /**
   * Normalize the passed  DASU
   * 
   * @param dasu the DasuDao to normalize
   * @return the normalized DasuDao
   */
  private def normalizeDasu(dasu: DasuDao, instance: Int): DasuDao = {
    require(Option(dasu).isDefined)
    require(Option(dasu.getTemplateId).isDefined,"Template DASU required")

    TemplateHelper.logger.debug("Normalizing templated DASU [{}] with instance []", dasu.getId, instance.toString)
    
    // Set the identifier
    dasu.setId(Identifier.buildIdFromTemplate(dasu.getId, Some(instance)))
    
    // The output is templated (it is a constraint)
    val newOutput = normalizeIasio(dasu.getOutput,instance)
    dasu.setOutput(newOutput)
    
    val asces = JavaConverters.collectionAsScalaIterable(dasu.getAsces).toSet
    TemplateHelper.logger.debug("{} ASCEs to check",asces.size)

    asces.foreach(a => TemplateHelper.logger.debug("DASU [{}] will convert ASCE [{}]",
        dasu.getId,
        a.getId))

    dasu.getAsces.clear()
    asces.foreach(asce => dasu.addAsce(normalizeAsce(asce,instance)))
    assert(asces.size==dasu.getAsces.size(),"Wrong number of ASCEs of DASU!")

    TemplateHelper.logger.debug("DASU normalized with new ID [{}], ASCES =[{}]",
        dasu.getId,
        JavaConverters.collectionAsScalaIterable(dasu.getAsces).map(_.getId).mkString(", "))
    
    dasu
  }
    
  /**
   * Build the list of DasuDao, converting the templated ones
   * into normal DASUs
   * 
   * @return The normalized DASU generated out of their templates and instances
   */
  def normalize(): Set[DasuDao] = {
    
    // Nothing to do for the normal DASUs
    val normalDasus = normalDasusToDeploy.map(_.getDasu)

    // Normalize templated DASUs
    TemplateHelper.logger.info("Normalizing templated DASUs, ASCEs and IASIOs")
    val templatedDasus = templatedDasusToDeploy.map(dtd => normalizeDasu(dtd.getDasu, dtd.getInstance))


    val allDasus = normalDasus++templatedDasus

    // Are there templated instance inputs in the ASCE?
    // They need to be normalized as well
    val allAsces: Set[AsceDao] =  allDasus.foldLeft(Set.empty[AsceDao])((z, dasu) => {
      val ascesOfDasu = JavaConverters.asScalaSet(dasu.getAsces).toSet
      TemplateHelper.logger.debug("Adding ASCEs of DASU {}: {}",
        dasu.getId,
        ascesOfDasu.map(_.getId).mkString(","))

      z++ascesOfDasu
    })
    TemplateHelper.logger.debug("Normalizing templated input instances of all ASCEs ({})",
      allAsces.map(_.getId).mkString(","))
    allAsces.foreach(normalizeAsceWithTemplatedInstanceInputs(_))
    TemplateHelper.logger.info("Normalization completed")
    
    allDasus
  }
  
  
}

/** Companion object */
object TemplateHelper {
  /** The logger */
  val logger: Logger = IASLogger.getLogger(this.getClass)
}