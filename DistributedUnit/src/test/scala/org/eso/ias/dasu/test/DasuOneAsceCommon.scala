package org.eso.ias.dasu.test

import org.eso.ias.cdb.CdbReader
import org.eso.ias.cdb.pojos.DasuDao
import org.eso.ias.cdb.structuredtext.StructuredTextReader
import org.eso.ias.dasu.DasuImpl
import org.eso.ias.dasu.publisher.{ListenerOutputPublisherImpl, OutputListener, OutputPublisher}
import org.eso.ias.dasu.subscriber.DirectInputSubscriber
import org.eso.ias.logging.IASLogger
import org.eso.ias.types.*
import org.eso.ias.types.IasValidity.*

import java.nio.file.FileSystems
import scala.collection.mutable.ArrayBuffer

/**
 * Setup the DASU with one ASCE as it is reused by more 
 * than one test 
 * 
 * @param autoRefreshTimeInterval The auto-refresh time (msec) to pass to the DASU 
 */
class DasuOneAsceCommon(autoRefreshTimeInterval: Integer, validityThreshold: Integer) extends OutputListener {
  /** The logger */
  private val logger = IASLogger.getLogger(this.getClass);
  
  // Build the CDB reader
  val cdbParentPath =  FileSystems.getDefault().getPath("src/test");
  val cdbReader: CdbReader = new StructuredTextReader(cdbParentPath.toFile)
  cdbReader.init()
  
  val dasuId = "DasuWithOneASCE"
  
  val stringSerializer = Option(new IasValueJsonSerializer)
  val outputPublisher: OutputPublisher = new ListenerOutputPublisherImpl(this,stringSerializer)
  
  val inputsProvider = new DirectInputSubscriber()
  
  // Build the Identifier
  val supervId = new Identifier("SupervId",IdentifierType.SUPERVISOR,None)
  val dasuIdentifier = new Identifier(dasuId,IdentifierType.DASU,supervId)
  
  // The DASU to test
  var dasu: Option[DasuImpl] = None

  // The identifier of the monitored system
  val monSysId = new Identifier("ConverterID",IdentifierType.MONITORED_SOFTWARE_SYSTEM,None)

  // The identifier of the plugin
  val pluginId = new Identifier("ConverterID",IdentifierType.PLUGIN,Some(monSysId))
  
  // The identifier of the converter
  val converterId = new Identifier("ConverterID",IdentifierType.CONVERTER,Some(pluginId))

  // The ID of the monitor point in unput (it matched the ID in theJSON file)
  val inputID = new Identifier("Temperature", IdentifierType.IASIO,converterId)
  
  /** Notifies about a new output produced by the DASU */
  override def outputEvent(output: IASValue[_]): Unit = {
    logger.info("Output received [{}]", output.id)
    outputValuesReceived.append(output)
  }
  
  /**
   * The output, formatted as strings, published by the DASU 
   */
  val outputStringsReceived = new ArrayBuffer[String]()
  
  /**
   * The output values published by the DASU 
   */
  val outputValuesReceived = new ArrayBuffer[IASValue[_]]()
  
  /** Notifies about a new output produced by the DASU 
   *  formatted as String
   */
  override def outputStringifiedEvent(outputStr: String) = {
    logger.info("JSON output received [{}]", outputStr)
    outputStringsReceived.append(outputStr)
  }
  
  val dasuDao: DasuDao = {
    cdbReader.init()
    val dasuDaoOpt = cdbReader.getDasu(dasuId)
    assert(dasuDaoOpt.isPresent())
    dasuDaoOpt.get()
  }
  
  def buildDasu(): Option[DasuImpl] = {
    Some(new DasuImpl(dasuIdentifier,dasuDao,outputPublisher,inputsProvider,autoRefreshTimeInterval,validityThreshold))
  }
  
  
  def buildValue(d: Double): IASValue[_] = {
    
    val t0 = System.currentTimeMillis()-100
    
    IASValue.build(
      d,
      OperationalMode.OPERATIONAL,
      UNRELIABLE,
      inputID.fullRunningID,
      IASTypes.DOUBLE,
      t0,
      t0+1,
      t0+5,
      t0+10,
      t0+15,
      null,
      null,
      null,
      null)
  }
    
}
