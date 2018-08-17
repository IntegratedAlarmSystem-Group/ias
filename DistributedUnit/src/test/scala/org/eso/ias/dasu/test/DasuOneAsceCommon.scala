package org.eso.ias.dasu.test

import org.eso.ias.logging.IASLogger
import java.nio.file.FileSystems
import org.eso.ias.cdb.json.CdbJsonFiles
import org.eso.ias.cdb.json.JsonReader
import org.eso.ias.cdb.CdbReader
import org.eso.ias.types.IasValueJsonSerializer
import org.eso.ias.dasu.publisher.OutputPublisher
import org.eso.ias.dasu.publisher.DirectInputSubscriber
import org.eso.ias.types.Identifier
import org.eso.ias.dasu.DasuImpl
import org.eso.ias.types.IdentifierType
import org.eso.ias.types.IASValue
import org.eso.ias.types.IasValidity._
import org.eso.ias.types.OperationalMode
import org.eso.ias.dasu.publisher.ListenerOutputPublisherImpl
import org.eso.ias.dasu.publisher.OutputListener
import scala.collection.mutable.ArrayBuffer
import org.eso.ias.types.IASTypes
import java.util.HashSet
import org.eso.ias.cdb.pojos.DasuDao

/**
 * Setup the DASU with one ASCE as it is reused by more 
 * than one test 
 * 
 * @param autoRefreshTimeInterval The auto-refresh time (msec) to pass to the DASU 
 */
class DasuOneAsceCommon(autoRefreshTimeInterval: Integer, tolerance: Integer) extends OutputListener {
  /** The logger */
  private val logger = IASLogger.getLogger(this.getClass);
  
  // Build the CDB reader
  val cdbParentPath =  FileSystems.getDefault().getPath(".");
  val cdbFiles = new CdbJsonFiles(cdbParentPath)
  val cdbReader: CdbReader = new JsonReader(cdbFiles)
  
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
  override def outputEvent(output: IASValue[_]) {
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
    val dasuDaoOpt = cdbReader.getDasu(dasuId)
    assert(dasuDaoOpt.isPresent())
    dasuDaoOpt.get()
  }
  
  def buildDasu(): Option[DasuImpl] = {
    Some(new DasuImpl(dasuIdentifier,dasuDao,outputPublisher,inputsProvider,autoRefreshTimeInterval,tolerance))
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
			t0+5,
			t0+10,
			t0+15,
			t0+20,
			null,
			null,
			null,
			null)
  }
    
}
