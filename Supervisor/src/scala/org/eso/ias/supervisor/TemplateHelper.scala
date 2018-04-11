package org.eso.ias.supervisor

import org.eso.ias.cdb.pojos.DasuToDeployDao
import org.eso.ias.cdb.pojos.DasuDao
import org.ias.logging.IASLogger
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
    dasusToDep.forall(dtd => {
      require(Option(dtd.getTemplate).isDefined,"The DASU to deploy must be templated")
      val templateId = dtd.getTemplate.getId
      val instance = dtd.getInstance
      
      // Is the instance in the range of the allowed instances of the template
      val instanceOk = instance>=dtd.getTemplate.getMin && instance<=dtd.getTemplate.getMax
      
      val asces = JavaConverters.asScalaSet(dtd.getDasu.getAsces)
      // Do all the ASCEs have the same template of the DASU?
      val ascesOk = asces.forall(asce => templateId==asce.getTemplateId)
      
      // Does the output have the same template of the DASU?
      val outputOk = templateId==dtd.getDasu.getOutput.getTemplateId
      
      // Inputs can have the same template of the DASU or being not templated
      val inputs = asces.foldLeft(Set.empty[IasioDao])( (set, asce) => set++JavaConverters.collectionAsScalaIterable(asce.getInputs))
      val inputsOk = inputs.forall(iasio => iasio.getTemplateId==null || templateId==iasio.getTemplateId)
      
      instanceOk && ascesOk && outputOk && inputsOk
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
      val idFromTemplate = Identifier.buildIdFromTemplate(iasio.getId, Some(instance))
      iasio.setId(idFromTemplate)
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
    
    // Set the identifier
    asce.setId(Identifier.buildIdFromTemplate(asce.getId, Some(instance)))
    
    // The output is templated (it is a constraint)
    val newOutput = normalizeIasio(asce.getOutput,instance)
    asce.setOutput(newOutput)
    
    // Only templated input must be normalized
    val inputs = JavaConverters.collectionAsScalaIterable(asce.getInputs)
    asce.getInputs.clear()
    inputs.foreach( input => {
      if (Option(input.getTemplateId).isDefined) {
        asce.addInput(normalizeIasio(input,instance), false)
      } else {
        asce.addInput(input,false)
      }
    })
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
    
    // Set the identifier
    dasu.setId(Identifier.buildIdFromTemplate(dasu.getId, Some(instance)))
    
    // The output is templated (it is a constraint)
    val newOutput = normalizeIasio(dasu.getOutput,instance)
    dasu.setOutput(newOutput)
    
    val asces = JavaConverters.collectionAsScalaIterable(dasu.getAsces)
    dasu.getAsces.clear()
    asces.foreach(asce => dasu.addAsce(normalizeAsce(asce,instance)))
    
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
    
    val templatedDasus = templatedDasusToDeploy.map(dtd => normalizeDasu(dtd.getDasu, dtd.getInstance))
    
    normalDasu++templatedDasus
  }
  
  
}