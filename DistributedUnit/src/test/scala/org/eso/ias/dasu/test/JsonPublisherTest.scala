package org.eso.ias.dasu.test

import org.scalatest.FlatSpec
import org.eso.ias.types.IasValueJsonSerializer
import org.eso.ias.dasu.Dasu
import org.eso.ias.types.Identifier
import org.eso.ias.types.IASValue
import org.eso.ias.cdb.json.CdbJsonFiles
import org.eso.ias.cdb.CdbReader
import java.io.FileWriter
import org.eso.ias.dasu.publisher.JsonWriterPublisher
import org.eso.ias.cdb.json.JsonReader
import org.eso.ias.logging.IASLogger
import java.nio.file.FileSystems
import org.eso.ias.types.IdentifierType
import java.io.File
import scala.io.Source
import org.eso.ias.types.IASTypes
import org.eso.ias.types.OperationalMode._
import org.eso.ias.types.Alarm
import org.eso.ias.types.IasValidity._
import org.eso.ias.dasu.DasuImpl
import org.eso.ias.dasu.publisher.DirectInputSubscriber
import java.util.HashSet
import org.eso.ias.cdb.pojos.DasuDao

/** 
 *  Test the writing of the output of the DASU
 *  in a JSON file. 
 */
class JsonPublisherTest extends FlatSpec {
/** The logger */
  private val logger = IASLogger.getLogger(this.getClass);
  
  // Build the CDB reader
  val cdbParentPath =  FileSystems.getDefault().getPath(".");
  val cdbFiles = new CdbJsonFiles(cdbParentPath)
  val cdbReader: CdbReader = new JsonReader(cdbFiles)
  
  val dasuId = "DasuWithOneASCE"
  
  val outputFile = new File("./JsonPublisherTest.json")
  val writer = new FileWriter(outputFile)
  val outputPublisher = new JsonWriterPublisher(writer)
  
  val inputsProvider = new DirectInputSubscriber()
  
  // Build the Identifier
  val supervId = new Identifier("SupervId",IdentifierType.SUPERVISOR,None)
  val dasuIdentifier = new Identifier(dasuId,IdentifierType.DASU,supervId)
  
  val dasuDao: DasuDao = {
    val dasuDaoOpt = cdbReader.getDasu(dasuId)
    assert(dasuDaoOpt.isPresent())
    dasuDaoOpt.get()
  }
  
  // The DASU
  val dasu = new DasuImpl(dasuIdentifier,dasuDao,outputPublisher,inputsProvider,3,1)

  // The identifier of the monitored system
  val monSysId = new Identifier("ConverterID",IdentifierType.MONITORED_SOFTWARE_SYSTEM,None)

  // The identifier of the plugin
  val pluginId = new Identifier("ConverterID",IdentifierType.PLUGIN,Some(monSysId))
  
  // The identifier of the converter
  val converterId = new Identifier("ConverterID",IdentifierType.CONVERTER,Some(pluginId))

  // The ID of the monitor point in unput (it matched the ID in theJSON file)
  val inputID = new Identifier("Temperature", IdentifierType.IASIO,converterId)
  
  def buildValue(d: Double): IASValue[_] = {
    
    val t0 = System.currentTimeMillis()-100
    
    IASValue.build(
      d,
			OPERATIONAL,
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
  
  behavior of "The DASU"
  
  it must "produce the output when a new set inputs is notified" in {
    // Start the getting of events in the DASU
    dasu.start()
    val inputs: Set[IASValue[_]] = Set(buildValue(0))
    // Sumbit the inputs
    inputsProvider.sendInputs(inputs)
    
    // Read the produced JSON file
    assert(outputFile.exists())
    assert(outputFile.canRead())
    val source = Source.fromFile(outputFile)
    val strBuilder = new StringBuilder()
    source.getLines().foreach(line => strBuilder.append(line))
    source.close()
    val strReadFromFile =  strBuilder.toString().trim()
    val jsonSerializer = new IasValueJsonSerializer()
    val iasValue = jsonSerializer.valueOf(strReadFromFile)
    assert(iasValue.id=="ThresholdAlarm")
    assert(iasValue.valueType==IASTypes.ALARM)
    assert(iasValue.value==Alarm.CLEARED)
    outputFile.deleteOnExit()
  }  
}
