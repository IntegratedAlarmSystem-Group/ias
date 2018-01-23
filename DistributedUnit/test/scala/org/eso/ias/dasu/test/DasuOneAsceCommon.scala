package org.eso.ias.dasu.test

import org.ias.prototype.logging.IASLogger
import java.nio.file.FileSystems
import org.eso.ias.cdb.json.CdbJsonFiles
import org.eso.ias.cdb.json.JsonReader
import org.eso.ias.cdb.CdbReader
import org.eso.ias.prototype.input.java.IasValueJsonSerializer
import org.eso.ias.dasu.publisher.OutputPublisher
import org.eso.ias.dasu.publisher.DirectInputSubscriber
import org.eso.ias.prototype.input.Identifier
import org.eso.ias.dasu.DasuImpl
import org.eso.ias.prototype.input.java.IdentifierType
import org.eso.ias.prototype.input.java.IASValue
import org.eso.ias.prototype.input.java.IasDouble
import org.eso.ias.prototype.input.java.IasValidity._
import org.eso.ias.prototype.input.java.OperationalMode
import org.eso.ias.dasu.publisher.ListenerOutputPublisherImpl
import org.eso.ias.dasu.publisher.OutputListener
import scala.collection.mutable.ListBuffer

/**
 * Setup the DASU with one ASCE as it is reused by more 
 * than one test 
 * 
 * @param autoRefreshTimeInterval The auto-refresh time (msec) to pass to the DASU 
 */
class DasuOneAsceCommon(autoRefreshTimeInterval: Long) extends OutputListener {
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
  
  // The identifer of the monitor system that produces the temperature in input to teh DASU
  val monSysId = new Identifier("MonitoredSystemID",IdentifierType.MONITORED_SOFTWARE_SYSTEM)
  // The identifier of the plugin
  val pluginId = new Identifier("PluginID",IdentifierType.PLUGIN,monSysId)
  // The identifier of the converter
  val converterId = new Identifier("ConverterID",IdentifierType.CONVERTER,pluginId)
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
  val outputStringsReceived = new ListBuffer[String]()
  
  /**
   * The output values published by the DASU 
   */
  val outputValuesReceived = new ListBuffer[IASValue[_]]()
  
  /** Notifies about a new output produced by the DASU 
   *  formatted as String
   */
  override def outputStringifiedEvent(outputStr: String) = {
    logger.info("JSON output received [{}]", outputStr)
    outputStringsReceived.append(outputStr)
  }
  
  def buildDasu(): Option[DasuImpl] = {
    Some(new DasuImpl(dasuIdentifier,outputPublisher,inputsProvider,cdbReader,autoRefreshTimeInterval))
  }
  
  def buildValue(d: Double): IASValue[_] = {
    new IasDouble(
        d,
        System.currentTimeMillis(),
        OperationalMode.OPERATIONAL,
        UNRELIABLE,
        inputID.fullRunningID)
  }
}