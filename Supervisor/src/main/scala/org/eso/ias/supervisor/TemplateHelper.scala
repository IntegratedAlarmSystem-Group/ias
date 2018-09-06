package org.eso.ias.supervisor

import org.eso.ias.cdb.pojos.DasuToDeployDao
import org.eso.ias.cdb.pojos.DasuDao
import org.eso.ias.logging.IASLogger
import scala.collection.JavaConverters
import org.eso.ias.cdb.pojos.IasioDao
import org.eso.ias.types.Identifier
import org.eso.ias.cdb.pojos.AsceDao

/**
 * TemplateHelper helps transforming DASUs, ASCEs and IASIOs 
 * defined by templates into concrete instances.
 * 
 * @param dasusToDeploy The dasus to deploy in the Supervisor
 */
class TemplateHelper(
    val dasusToDeploy :Set[DasuToDeployDao]) {
  require(Option(dasusToDeploy).isDefined)
  
  /** The logger */
  val logger = IASLogger.getLogger(this.getClass)
  
  // The DASU with a template that needs to be normalized
  val templatedDasusToDeploy = dasusToDeploy.filter(dtd => Option(dtd.getTemplate).isDefined)
  assert(templatedDasusToDeploy.forall(dtd => Option(dtd.getInstance).isDefined))
  
  // The DASUS to deploy in the Supervisor that have no template
  val normalDasusToDeploy = dasusToDeploy.filter(dtd => Option(dtd.getTemplate).isEmpty)
  assert(normalDasusToDeploy.forall(dtd => Option(dtd.getInstance).isEmpty))
  
  require(checkTemplateConstraints(templatedDasusToDeploy),"Template constraints violated")
  logger.info("Template constraints OK")
  
  logger.info("{} DasuDao to convert from template and {} standard DasuDao",
      templatedDasusToDeploy.size.toString(),
      normalDasusToDeploy.size.toString())
      
  /**
   * Ensure that 
   * * all the ASCEs of a DASU have the same template
   * * all the inputs have the same template or are not templated
   * * all the output have the same template templated
   * 
   * @param dasusToDep the templated DASUs to deploy in the supervisor
   * @return true if all the DASU, ASCEs and IASIOs respect the constraints of the template
   */
  def checkTemplateConstraints(dasusToDep: Set[DasuToDeployDao]): Boolean = {
    logger.debug("Checking constraints of {} templated DASUs: {}", 
        dasusToDep.size.toString(),
        dasusToDep.map(_.getDasu.getId).mkString(", "))
    dasusToDep.forall(dtd => { 
      require(Option(dtd.getTemplate).isDefined,"The DASU to deploy must be templated")
      val templateId = dtd.getTemplate.getId
      val instance = dtd.getInstance
      
      // Is the instance in the range of the allowed instances of the template
      val instanceOk = instance>=dtd.getTemplate.getMin && instance<=dtd.getTemplate.getMax
      
      if (!instanceOk) {
        logger.error("Instance {} out of range [{},{}] of template {}", 
            instance,
            dtd.getTemplate.getMin.toString(),
            dtd.getTemplate.getMin.toString(),
            dtd.getTemplate.getId)
      }
      
      val asces = JavaConverters.asScalaSet(dtd.getDasu.getAsces)
      
      // Do all the ASCEs have the same template of the DASU?
      asces.foreach(a => println(a.getTemplateId))
      val ascesOk = asces.forall(asce => templateId==asce.getTemplateId)
      
      if (!ascesOk) {
        logger.error("Template mismatch in the ASCEs {} of the DASU {}: should be {}", 
            asces.map(_.getId).mkString(", "),
            dtd.getDasu.getId,
            templateId)
      }
      
      // Do the output of each ASCE (and so the output of the DASU)
      // have the same template of the DASU?
      val outputsOfAscesOk = asces.forall(asce => templateId==asce.getOutput.getTemplateId)
      
      if (!outputsOfAscesOk) {
        logger.error("Template mismatch in the outputs {} of the ASCEs ({}) of the DASU {}", 
            asces.map(_.getOutput.getId).mkString(", "),
            asces.map(_.getId).mkString(", "),
            dtd.getDasu.getId)
      }
      
      // Does the output have the same template of the DASU?
      //
      // This is redundand, actually because the output of the DASU is the outputt
      // of one of its ASCEs already checked in outputsOfAscesOk
      val outputOk = templateId==dtd.getDasu.getOutput.getTemplateId
      
      if (!outputOk) {
        logger.error("Template mismatch in the output {} of the DASU {}", 
            dtd.getDasu.getOutput.getId,
            dtd.getDasu.getId)
      }
      
      // Inputs can have the same template of the DASU or being not templated
      val inputs = asces.foldLeft(Set.empty[IasioDao])( (set, asce) => set++JavaConverters.collectionAsScalaIterable(asce.getInputs))
      val inputsOk = inputs.forall(iasio => iasio.getTemplateId==null || templateId==iasio.getTemplateId)
      
      if (!inputsOk) {
        logger.error("Inputs ({}) of DASU {} must have a template null or {}", 
            inputs.map(_.getId).mkString(", "),
            dtd.getDasu.getId,
            templateId)
      }
      
      instanceOk && ascesOk && outputsOfAscesOk && outputOk && inputsOk
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
      logger.debug("Normalizing templated IASIO [{}] with instance []", iasio.getId, instance.toString())
      val idFromTemplate = Identifier.buildIdFromTemplate(iasio.getId, Some(instance))
      iasio.setId(idFromTemplate)
      logger.debug("IASIO normalized with new ID [{}] ", iasio.getId)
    } else {
      logger.debug("IASIO [{}] is not templated: no need to nromalize", iasio.getId)
    }
    iasio
  }
  
  /**
   * Normalize the passed ASCE:   * 
   * - the output is always templated
   * - the ID of the ASCE is always templated
   * - the inputs can or cannot be templated
   * 
   * 
   * @param the templated AscDao to normalize
   * @return the normalized AsceDaso
   */
  private def normalizeAsce(asce: AsceDao, instance: Int): AsceDao = {
    require(Option(asce).isDefined)
    require(Option(asce.getTemplateId).isDefined,"Template ASCE required")
    
    logger.debug("Normalizing templated ASCE [{}] with instance {}", asce.getId, instance.toString())
    
    // Set the identifier
    asce.setId(Identifier.buildIdFromTemplate(asce.getId, Some(instance)))
    
    // The output is templated (it is a constraint)
    val newOutput = normalizeIasio(asce.getOutput,instance)
    asce.setOutput(newOutput)
    
    // Only templated input must be normalized
    val inputs = JavaConverters.collectionAsScalaIterable(asce.getInputs).toList
    asce.getInputs.clear()
    inputs.foreach( input => {
      if (Option(input.getTemplateId).isDefined) {
        asce.addInput(normalizeIasio(input,instance), false)
      } else {
        asce.addInput(input,false)
      }
    })
    
    logger.debug("ASCE normalized with new ID [{}], inputs=[{}]", 
        asce.getId,
        JavaConverters.collectionAsScalaIterable(asce.getInputs).map(_.getId).mkString(","))
    asce
  }
  
  /**
   * Normalize the passed  DASU
   * 
   * @param the DasuDao to normalize
   * @return the normalized DasuDao
   */
  private def normalizeDasu(dasu: DasuDao, instance: Int): DasuDao = {
    require(Option(dasu).isDefined)
    require(Option(dasu.getTemplateId).isDefined,"Template DASU required")
    
    logger.debug("Normalizing templated DASU [{}] with instance []", dasu.getId, instance.toString())
    
    // Set the identifier
    dasu.setId(Identifier.buildIdFromTemplate(dasu.getId, Some(instance)))
    
    // The output is templated (it is a constraint)
    val newOutput = normalizeIasio(dasu.getOutput,instance)
    dasu.setOutput(newOutput)
    
    val asces = JavaConverters.collectionAsScalaIterable(dasu.getAsces).toList
   
    asces.foreach(a => logger.debug("DASU [{}] will convert ASCE [{}]",
        dasu.getId,
        a.getId))
    dasu.getAsces.clear()
    asces.foreach(asce => dasu.addAsce(normalizeAsce(asce,instance)))
    
    
    logger.debug("DASU normalized with new ID [{}], ASCES =[{}]", 
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
    val normalDasu = normalDasusToDeploy.map(_.getDasu)
    
    logger.info("Normalizing DASUs, ASCEs and IASIOs")
    val templatedDasus = templatedDasusToDeploy.map(dtd => normalizeDasu(dtd.getDasu, dtd.getInstance))
    logger.info("Normalization completed")
    
    normalDasu++templatedDasus
  }
  
  
}
