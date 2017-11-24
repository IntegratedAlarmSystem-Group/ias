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
import org.eso.ias.plugin.OperationalMode
import org.eso.ias.prototype.input.InOut
import org.eso.ias.prototype.input.JavaConverter
import org.eso.ias.dasu.InputsListener

/**
 * Test the DASU with 5 ASCEs (in 2 levels).
 * 
 * At th ebottom level, there are 4 ASCEs that get a temperature and 
 * check it against the MinMahThreshold.
 * All 4 alarms poroduced by those ASCEs are the input of the ASCE_AlarmsThreshold,
 * in the next and last level, that applies the multiplicity with a threshold of 3
 * 
 * The DASU takes in punt 4 temperatures and produces an alarm if at least
 * three of them are out of the nominal range.
 * 
 * The configurations of DASU, ASCE, TF and IASIOs are all stored 
 * in the CDB folder.
 */
class Dasu5ASCEsTest extends FlatSpec with OutputListener {
  
  /** The logger */
  private val logger = IASLogger.getLogger(this.getClass);
  
  // Build the CDB reader
  val cdbParentPath =  FileSystems.getDefault().getPath(".");
  val cdbFiles = new CdbJsonFiles(cdbParentPath)
  val cdbReader: CdbReader = new JsonReader(cdbFiles)
  
  val dasuId = "DasuWith5ASCEs"
  
  val stringSerializer = Option(new IasValueJsonSerializer)
  val outputPublisher: OutputPublisher = new ListenerOutputPublisherImpl(this,stringSerializer)
  
  // The DASU to test
  val dasu = new Dasu(dasuId,outputPublisher,cdbReader)
  // The DASU is also the inputs listener
  val inputsListener = dasu.asInstanceOf[InputsListener]
  
  // The identifer of the monitor system that produces the temperature in input to teh DASU
  val monSysId = new Identifier("MonitoredSystemID",IdentifierType.MONITORED_SOFTWARE_SYSTEM)
  // The identifier of the plugin
  val pluginId = new Identifier("PluginID",IdentifierType.PLUGIN,monSysId)
  // The identifier of the converter
  val converterId = new Identifier("ConverterID",IdentifierType.CONVERTER,pluginId)
  
  // The ID of the temperature 1 monitor point in unput to ASCE-Temp1
  val inputTemperature1ID = new Identifier("Temperature1", IdentifierType.IASIO,converterId)
  // The ID of the temperature 1 monitor point in unput to ASCE-Temp1
  val inputTemperature2ID = new Identifier("Temperature2", IdentifierType.IASIO,converterId)
  // The ID of the temperature 1 monitor point in unput to ASCE-Temp1
  val inputTemperature3ID = new Identifier("Temperature3", IdentifierType.IASIO,converterId)
  // The ID of the temperature 1 monitor point in unput to ASCE-Temp1
  val inputTemperature4ID = new Identifier("Temperature4", IdentifierType.IASIO,converterId)
  
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
  
  def buildValue(id: String, fullRunningID: String, d: Double): IASValue[_] = {
    new IasDouble(
        d,
        System.currentTimeMillis(),
        OperationalMode.OPERATIONAL,
        id,
        fullRunningID)
  }
  
  behavior of "The DASU"
  
  it must "produce the output when a new set inputs is notified" in {
    val inputs: Set[IASValue[_]] = Set(buildValue(inputTemperature1ID.id, inputTemperature1ID.fullRunningID,0))
    // Sumbit the inputs
    inputsListener.inputsReceived(inputs)
  }
  
}