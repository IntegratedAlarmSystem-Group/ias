package org.eso.ias.dasu.test

import org.scalatest.FlatSpec
import java.nio.file.FileSystems
import org.eso.ias.cdb.CdbReader
import org.eso.ias.cdb.json.CdbJsonFiles
import org.eso.ias.cdb.json.JsonReader
import org.eso.ias.dasu.Dasu
import org.eso.ias.dasu.publisher.OutputListener
import org.eso.ias.dasu.publisher.ListenerOutputPublisherImpl
import org.eso.ias.dasu.publisher.OutputPublisher
import org.eso.ias.prototype.input.java.IasValueJsonSerializer
import org.ias.prototype.logging.IASLogger
import org.eso.ias.prototype.input.java.IASValue
import org.eso.ias.prototype.input.java.IasDouble
import org.eso.ias.prototype.input.Identifier
import org.eso.ias.prototype.input.java.IdentifierType
import org.eso.ias.prototype.input.java.OperationalMode
import org.eso.ias.prototype.input.InOut
import org.eso.ias.prototype.input.JavaConverter
import org.eso.ias.dasu.subscriber.InputsListener
import org.eso.ias.dasu.subscriber.InputSubscriber
import scala.util.Success
import scala.util.Try
import scala.collection.mutable.{HashSet => MutableSet}
import org.eso.ias.prototype.input.java.IasValidity._
import org.eso.ias.dasu.DasuImpl

/**
 * Test the DASU with one ASCE and the MinMaxThreshold TF.
 * 
 * Being a simple case, this test will do some basic tests.
 * 
 * The configurations of DASU, ASCE, TF and IASIOs are all stored 
 * in the CDB folder.
 */
class DasuOneASCETest extends FlatSpec with OutputListener {
  
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
  val dasu = new DasuImpl(dasuIdentifier,outputPublisher,inputsProvider,cdbReader)
  
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
  }
  
  /** Notifies about a new output produced by the DASU 
   *  formatted as String
   */
  override def outputStringifiedEvent(outputStr: String) = {
    logger.info("JSON output received [{}]", outputStr)
  }
  
  def buildValue(d: Double): IASValue[_] = {
    new IasDouble(
        d,
        System.currentTimeMillis(),
        OperationalMode.OPERATIONAL,
        UNRELIABLE,
        inputID.fullRunningID)
  }
  
  behavior of "The DASU"
  
  it must "produce the output when a new set inputs is notified" in {
    // Start the getting of events in the DASU
    dasu.start()
    val inputs: Set[IASValue[_]] = Set(buildValue(0))
    // Sumbit the inputs
    inputsProvider.sendInputs(inputs)
  }
  
}