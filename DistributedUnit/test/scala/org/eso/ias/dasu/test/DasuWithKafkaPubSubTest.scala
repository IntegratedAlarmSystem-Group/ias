package org.eso.ias.dasu.test

import org.scalatest.FlatSpec
import org.eso.ias.logging.IASLogger
import java.nio.file.FileSystems
import org.eso.ias.cdb.json.CdbJsonFiles
import org.eso.ias.cdb.CdbReader
import org.eso.ias.cdb.json.JsonReader
import org.eso.ias.dasu.publisher.KafkaPublisher
import java.util.Properties
import org.eso.ias.dasu.subscriber.KafkaSubscriber
import org.eso.ias.dasu.Dasu
import org.eso.ias.types.Identifier
import org.eso.ias.types.IASValue
import org.eso.ias.types.IdentifierType
import org.eso.ias.types.OperationalMode
import org.eso.ias.kafkautils.SimpleStringConsumer
import org.eso.ias.kafkautils.KafkaHelper
import org.eso.ias.kafkautils.SimpleStringProducer
import org.eso.ias.kafkautils.SimpleStringConsumer.KafkaConsumerListener
import org.eso.ias.types.IasValueJsonSerializer
import scala.util.Try
import scala.collection.mutable.ListBuffer
import org.eso.ias.kafkautils.SimpleStringConsumer.StartPosition
import org.eso.ias.types.IasValidity._
import org.eso.ias.dasu.DasuImpl
import org.eso.ias.types.IASTypes
import java.util.HashSet
import org.eso.ias.cdb.pojos.DasuDao

/**
 * Test if the DASU is capable to get events from
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
class DasuWithKafkaPubSubTest extends FlatSpec with KafkaConsumerListener {
  /** The logger */
  private val logger = IASLogger.getLogger(this.getClass);
  
  // Build the CDB reader
  val cdbParentPath =  FileSystems.getDefault().getPath(".");
  val cdbFiles = new CdbJsonFiles(cdbParentPath)
  val cdbReader: CdbReader = new JsonReader(cdbFiles)
  
  val dasuId = "DasuWithOneASCE"
  
  /** The kafka publisher used by the DASU to send the output */
  val outputPublisher = KafkaPublisher(dasuId, None, None, new Properties())
  
  val inputsProvider = KafkaSubscriber(dasuId, None, None, new Properties())
  
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
      "DasuWithKafka-TestSub")
  logger.debug("initializing the event listener")
  val props = new Properties()
  props.setProperty("group.id", "DasuTest-groupID")
  eventsListener.setUp(props)
  eventsListener.startGettingEvents(StartPosition.END,this)
  
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
			  t0+25,
			  null,
			  null,
			  null)
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
    
    // We sent one IASIO but the listener must have received 2:
    // - the one we sent
    // the one produced by the DASU
    assert(iasValuesReceived.size==2)
    
    logger.info("Cleaning up the event listener")
    eventsListener.tearDown()
    logger.info("Cleaning up the IASIO publisher")
    stringPublisher.tearDown()
    logger.info("Done")
  } 
}
