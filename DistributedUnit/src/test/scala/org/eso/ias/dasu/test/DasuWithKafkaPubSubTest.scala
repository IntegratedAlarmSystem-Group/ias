package org.eso.ias.dasu.test

import org.eso.ias.cdb.CdbReader
import org.eso.ias.cdb.pojos.DasuDao
import org.eso.ias.cdb.structuredtext.{StructuredTextReader, CdbTxtFiles, TextFileType}
import org.eso.ias.dasu.DasuImpl
import org.eso.ias.dasu.publisher.KafkaPublisher
import org.eso.ias.dasu.subscriber.KafkaSubscriber
import org.eso.ias.kafkautils.KafkaStringsConsumer.StreamPosition
import org.eso.ias.kafkautils.SimpleStringConsumer.KafkaConsumerListener
import org.eso.ias.kafkautils.{KafkaHelper, SimpleStringConsumer, SimpleStringProducer}
import org.eso.ias.logging.IASLogger
import org.eso.ias.types.*
import org.eso.ias.types.IasValidity.*
import org.scalatest.BeforeAndAfterAll
import org.scalatest.flatspec.AnyFlatSpec

import java.nio.file.{FileSystems, Path}
import java.util.Properties
import scala.collection.mutable.ListBuffer
import scala.util.Try

/**
 * Test if the DASU is capable to get events from
 * the IASIO kafka queue and publish the result 
 * in the same queue.
 * 
 * The test re-use the simple dasu with only one ASCE.
 *
 * The DASU has a kafka consumer to get events and a kafka producer to publish the generated output.
 * To check if it works, the test instantiates one kafka producer to submit
 * inputs to the DASU and one consumer to catch if the output produced by the DASU has been pushed in the kafka queue
 */
class DasuWithKafkaPubSubTest extends AnyFlatSpec with KafkaConsumerListener with BeforeAndAfterAll {
  /** The logger */
  private val logger = IASLogger.getLogger(this.getClass)
  
  // Build the CDB reader
  val cdbParentPath: Path =  FileSystems.getDefault.getPath("src/test")
  val cdbFiles = new CdbTxtFiles(cdbParentPath,TextFileType.JSON)
  val cdbReader: CdbReader = new StructuredTextReader(cdbFiles)
  cdbReader.init()
  
  val dasuId = "DasuWithOneASCE"

  val stringPublisher = new SimpleStringProducer(KafkaHelper.DEFAULT_BOOTSTRAP_BROKERS,dasuId)

  /** The kafka publisher used by the DASU to send the output */
  val outputPublisher: KafkaPublisher = KafkaPublisher(dasuId, Option.empty,stringPublisher, Option.empty)
  
  val inputsProvider: KafkaSubscriber = KafkaSubscriber(dasuId, None, None, new Properties())
  
  // Build the Identifier
  val supervId = new Identifier("SupervId",IdentifierType.SUPERVISOR,None)
  val dasuIdentifier = new Identifier(dasuId,IdentifierType.DASU,supervId)
  
  val dasuDao: DasuDao = {
    cdbReader.init()
    val dasuDaoOpt = cdbReader.getDasu(dasuId)
    assert(dasuDaoOpt.isPresent)
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
  
  /** The publisher to send IASValues to kafka topic */
  val eventsListener = new SimpleStringConsumer(
      KafkaHelper.DEFAULT_BOOTSTRAP_BROKERS,
      KafkaHelper.IASIOs_TOPIC_NAME,
      "DasuWithKafka-TestSub")

  val jsonSerializer = new IasValueJsonSerializer()
  
  /** The list with the events received */ 
  val iasValuesReceived: ListBuffer[IASValue[?]] = ListBuffer()
  
  logger.info("Giving kakfka stuff time to be ready...")
  Try(Thread.sleep(5000))
  
  logger.info("Ready to start the test...")

  override def beforeAll(): Unit = {
    super.beforeAll()
    logger.debug("initializing the event listener")
    val props = new Properties()
    props.setProperty("group.id", "DasuTest-groupID")
    eventsListener.setUp(props)
    eventsListener.startGettingStrings(StreamPosition.END,this)
    logger.debug("Initializing the IASIO publisher")
    stringPublisher.setUp()
  }

  override def afterAll(): Unit = {
    logger.info("Cleaning up the event listener")
    eventsListener.tearDown()
    logger.debug("Closing the IASIO publisher")
    stringPublisher.tearDown()
    super.afterAll()
  }
  
  def buildValue(d: Double): IASValue[?] = {
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
        t0+20,

      null,
      null,
      null)
  }
  
  def stringEventReceived(event: String): Unit = {
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
    stringPublisher.push(jSonStr, KafkaHelper.IASIOs_TOPIC_NAME,null, input.id)
    
    // Give some time...
    Try(Thread.sleep(5000))
    
    // We sent one IASIO but the listener must have received 2:
    // - the one we sent
    // the one produced by the DASU
    assert(iasValuesReceived.size==2)

    logger.info("Done")
  } 
}
