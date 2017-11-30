package org.eso.ias.dasu.test

import org.scalatest.FlatSpec
import org.ias.prototype.logging.IASLogger
import java.nio.file.FileSystems
import org.eso.ias.cdb.json.CdbJsonFiles
import org.eso.ias.cdb.CdbReader
import org.eso.ias.cdb.json.JsonReader
import org.eso.ias.dasu.publisher.KafkaPublisher
import java.util.Properties
import org.eso.ias.dasu.subscriber.KafkaSubscriber
import org.eso.ias.dasu.Dasu
import org.eso.ias.prototype.input.Identifier
import org.eso.ias.prototype.input.java.IASValue
import org.eso.ias.prototype.input.java.IasDouble
import org.eso.ias.prototype.input.java.IdentifierType
import org.eso.ias.plugin.OperationalMode
import org.eso.ias.kafkautils.SimpleStringConsumer
import org.eso.ias.kafkautils.KafkaHelper
import org.eso.ias.kafkautils.SimpleStringProducer
import org.eso.ias.kafkautils.SimpleStringConsumer.KafkaConsumerListener
import org.eso.ias.prototype.input.java.IasValueJsonSerializer
import scala.util.Try
import scala.collection.mutable.ListBuffer

/**
 * test if the DASU is capable to get events from
 * the IASIO kafka queue and publish the result 
 * in the same queue.
 * 
 * The test re-use the simple dasu with only one ASCE.
 * The DASU has a kafka publisher to get events and a kafka subscriber
 * to ppublish the generated output.
 * To check if it works, the test instantiates one kafka publisher to submit
 * inputs to the DASU and one subscriber to catch if the output
 * produced by the DASU has been pushed in the kafka queue 
 */
class KafkaDasuWithKafka extends FlatSpec with KafkaConsumerListener {
  /** The logger */
  private val logger = IASLogger.getLogger(this.getClass);
  
  // Build the CDB reader
  val cdbParentPath =  FileSystems.getDefault().getPath(".");
  val cdbFiles = new CdbJsonFiles(cdbParentPath)
  val cdbReader: CdbReader = new JsonReader(cdbFiles)
  
  val dasuId = "DasuWithOneASCE"
  
  /** The kafka publisher used by the DASU to send the output */
  val outputPublisher = KafkaPublisher(dasuId, new Properties())
  
  val inputsProvider = new KafkaSubscriber(dasuId, new Properties())
  
  // The DASU
  val dasu = new Dasu(dasuId,outputPublisher,inputsProvider,cdbReader)
  
  // The identifer of the monitor system that produces the temperature in input to teh DASU
  val monSysId = new Identifier("MonitoredSystemID",IdentifierType.MONITORED_SOFTWARE_SYSTEM)
  // The identifier of the plugin
  val pluginId = new Identifier("PluginID",IdentifierType.PLUGIN,monSysId)
  // The identifier of the converter
  val converterId = new Identifier("ConverterID",IdentifierType.CONVERTER,pluginId)
  // The ID of the monitor point in unput (it matched the ID in theJSON file)
  val inputID = new Identifier("Temperature", IdentifierType.IASIO,converterId)
  
  /** The publisher to send IASValues to kafka topic */
  val eventsListener = new SimpleStringConsumer(
      KafkaHelper.DEFAULT_BOOTSTRAP_BROKERS,
      KafkaHelper.IASIOs_TOPIC_NAME,
      this)
  logger.debug("initializing the event listener")
  val props = new Properties()
  props.setProperty("group.id", "DasuTest-groupID")
  eventsListener.setUp(props)
  
  val stringPublisher = new SimpleStringProducer(
      KafkaHelper.DEFAULT_BOOTSTRAP_BROKERS,
      KafkaHelper.IASIOs_TOPIC_NAME,
      "KafkaDasuTestID-Producer")
  logger.debug("initializing the IASIO publisher")
  stringPublisher.setUp()
  
  val jsonSerializer = new IasValueJsonSerializer()
  
  /** The list with the events received */ 
  val iasValuesReceived: ListBuffer[IASValue[_]] = ListBuffer()
  
  logger.info("Giving kakfka stuff time to be ready...")
  Try(Thread.sleep(5000))
  
  logger.info("Ready to start the test...")
  
  def buildValue(d: Double): IASValue[_] = {
    new IasDouble(
        d,
        System.currentTimeMillis(),
        OperationalMode.OPERATIONAL,
        inputID.id,
        inputID.fullRunningID)
  }
  
  def stringEventReceived(event: String) = {
    logger.info("String received", event)
    val iasValue = jsonSerializer.valueOf(event)
    iasValuesReceived+=iasValue
  }
  
  behavior of "The DASU"
  
  it must "produce the output when a new set inputs is notified" in {
    // Start the getting of events in the DASU
    dasu.start()
    
    // Send the input to the kafka queue
    val input = buildValue(0)
    val jSonStr = jsonSerializer.iasValueToString(input)
    stringPublisher.push(jSonStr, null, input.id)
    
    // Give some time...
    Try(Thread.sleep(5000))
    
    assert(iasValuesReceived.size==1)
    
    
    logger.info("Cleaning up the event listener")
    eventsListener.tearDown()
    logger.info("Cleaning up the IASIO publisher")
    stringPublisher.tearDown()
    logger.info("Done")
  } 
}