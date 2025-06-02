package org.eso.ias.dasu.test

import org.eso.ias.cdb.CdbReader
import org.eso.ias.cdb.pojos.DasuDao
import org.eso.ias.cdb.structuredtext.{StructuredTextReader, CdbTxtFiles, TextFileType, CdbFiles}
import org.eso.ias.dasu.DasuImpl
import org.eso.ias.dasu.publisher.JsonWriterPublisher
import org.eso.ias.dasu.subscriber.DirectInputSubscriber
import org.eso.ias.logging.IASLogger
import org.eso.ias.types.*
import org.eso.ias.types.IasValidity.*
import org.eso.ias.types.OperationalMode.*
import org.scalatest.flatspec.AnyFlatSpec

import java.io.{File, FileWriter}
import java.nio.file.FileSystems
import scala.io.Source

/** 
 *  Test the writing of the output of the DASU
 *  in a JSON file. 
 */
class JsonPublisherTest extends AnyFlatSpec {
  /** The logger */
  private val logger = IASLogger.getLogger(this.getClass);
  
  // Build the CDB reader
  val cdbParentPath =  FileSystems.getDefault().getPath("src/test")
  val cdbFiles = new CdbTxtFiles(cdbParentPath, TextFileType.JSON)
  val cdbReader: CdbReader = new StructuredTextReader(cdbFiles)
  cdbReader.init()
  
  val dasuId = "DasuWithOneASCE"
  
  val outputFile = new File("./JsonPublisherTest.json")
  val writer = new FileWriter(outputFile)
  val outputPublisher = new JsonWriterPublisher(writer)
  
  val inputsProvider = new DirectInputSubscriber()
  
  // Build the Identifier
  val supervId = new Identifier("SupervId",IdentifierType.SUPERVISOR,None)
  val dasuIdentifier = new Identifier(dasuId,IdentifierType.DASU,supervId)
  
  val dasuDao: DasuDao = {
    cdbReader.init()
    val dasuDaoOpt = cdbReader.getDasu(dasuId)
    assert(dasuDaoOpt.isPresent())
    dasuDaoOpt.get()
  }
  
  // The DASU
  val dasu = new DasuImpl(dasuIdentifier,dasuDao,outputPublisher,inputsProvider,3,4)

  // The identifier of the monitored system
  val monSysId = new Identifier("ConverterID",IdentifierType.MONITORED_SOFTWARE_SYSTEM,None)

  // The identifier of the plugin
  val pluginId = new Identifier("ConverterID",IdentifierType.PLUGIN,Some(monSysId))
  
  // The identifier of the converter
  val converterId = new Identifier("ConverterID",IdentifierType.CONVERTER,Some(pluginId))

  // The ID of the monitor point in unput (it matched the ID in theJSON file)
  val inputID = new Identifier("Temperature", IdentifierType.IASIO,converterId)
  
  def buildValue(d: Double): IASValue[?] = {
    
    val t0 = System.currentTimeMillis()-100
    
    IASValue.build(
      d,
      OPERATIONAL,
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

  /** 
     * A function to wait until the DASU has processed all the inputs or the timeout expires.
     * 
     * This function works if no new inputs are submitted to the DASU while waiting.
     * The DASU does not offer a function for checking the state of the computation 
     * so this function actively wait on the inputs to be processed and the
     * on the presence of scheduled tasks to process the output.
     */
    def waitDasuProcessedAllInputs(timeout: Int): Boolean = {
      val startTime = System.currentTimeMillis()
      val endTime = startTime + timeout
      // wait until there are no more inputs to process
      while (dasu.hasInputsToProcess) {
        Thread.sleep(100)
        if (System.currentTimeMillis() > endTime) {
          logger.warn("Timeout expired while waiting for the DASU to process all inputs")
          return false
        }
      }
      
      // Wait until there are no more scheduled task to calculate the output
      while (dasu.hasScheduledTask) {
        Thread.sleep(100)
        if (System.currentTimeMillis() > endTime) {
          logger.warn("Timeout expired while waiting for the DASU to process all scheduled tasks")
          return false
        }
      }
      logger.info("DASU has processed all inputs and has no scheduled tasks")
      true
    }
  
  
  behavior of "The DASU"
  
  it must "produce the output when a new set of inputs is notified" in {
    // Start the getting of events in the DASU
    dasu.start()
    val inputs: Set[IASValue[?]] = Set(buildValue(0))
    // Submit the inputs
    inputsProvider.sendInputs(inputs)
    // Wait until the thread of the DASU has processed all inputs
    assert(waitDasuProcessedAllInputs(1000), "DASU did not process all inputs in time")
    
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
    assert(iasValue.value==Alarm.getInitialAlarmState)
    outputFile.deleteOnExit()
  }  
}
