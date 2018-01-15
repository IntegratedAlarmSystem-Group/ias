package org.eso.ias.supervisor.test

import java.nio.file.FileSystems
import org.eso.ias.cdb.json.CdbJsonFiles
import org.eso.ias.cdb.json.JsonReader
import org.eso.ias.cdb.CdbReader
import org.eso.ias.dasu.publisher.KafkaPublisher
import java.util.Properties
import org.eso.ias.dasu.publisher.OutputPublisher
import org.eso.ias.dasu.subscriber.InputSubscriber
import org.eso.ias.dasu.subscriber.KafkaSubscriber
import org.eso.ias.prototype.input.java.IdentifierType
import org.eso.ias.prototype.input.Identifier
import org.eso.ias.cdb.pojos.DasuDao
import org.eso.ias.dasu.DasuImpl
import org.eso.ias.supervisor.Supervisor
import scala.util.Success
import scala.util.Failure
import java.util.concurrent.CountDownLatch
import org.ias.prototype.logging.IASLogger
import org.scalatest.FlatSpec
import org.eso.ias.kafkautils.KafkaIasiosProducer
import org.eso.ias.kafkautils.KafkaHelper
import org.eso.ias.prototype.input.java.IasValueStringSerializer
import org.eso.ias.prototype.input.java.IasValueJsonSerializer
import org.eso.ias.kafkautils.KafkaIasiosConsumer
import org.eso.ias.kafkautils.SimpleStringConsumer.StartPosition
import org.eso.ias.kafkautils.KafkaIasiosConsumer.IasioListener
import org.eso.ias.prototype.input.java.IASValue
import org.eso.ias.prototype.input.java.OperationalMode
import org.eso.ias.prototype.input.java.IasValidity
import org.eso.ias.prototype.input.java.IASTypes
import scala.collection.mutable.ListBuffer

/**
 * Test the Supervisor connected to the BSDB.
 * 
 * The configuration of this Supervisor is in the JSON CDB.
 * 
 * The supervisor runs 2 DASU with one ASCE each and
 * checks the generation of the output by its DASUs and their ASCEs 
 * when some input is read from the kafka topic.
 * 
 * This test is meant to check the loop only so each DASU has only one ASCE
 * that run the MinMaxTF against one input only. This way the CDB is simple.
 * 
 * The test 
 * - runs the supervisor
 * - run a producer of IASIOs in the BSDB (iasiosProducer): this is to
 *   submit IASIOs to the Supervisor as if they have been produced by DASU
 *   or plugins
 * - run a IASIOs consumer (iasiosConsumer) that listen to all the IASIOs
 *   written in the BDSB i.e. those published by the producer and 
 *   those published by the DASUs running in the Supervisor
 *   
 * The test submit some IASIOs and checks what is published in the BSDB.
 */
class SupervisorWithKafkaTest extends IasioListener {
  
  /** The logger */
  private val logger = IASLogger.getLogger(this.getClass);
  
  /** All the values read from the BASD */
  val receivedIasValues = new ListBuffer[IASValue[_]]()
  
  /** The serializer to JSON strings */
  val serializer: IasValueStringSerializer = new IasValueJsonSerializer()
  
  /** 
   *  The IASIOs producer submits IASIOs to the kafka topic i.e. to the BSDB:
   *  these IASIOs will be received and processed by the DASU running in the Supervisor
   *   
   */
  val iasiosProducer = new KafkaIasiosProducer(
      KafkaHelper.DEFAULT_BOOTSTRAP_BROKERS,
      KafkaHelper.IASIOs_TOPIC_NAME,
      "SupervisorWithKafka-Producer",
      serializer)
  iasiosProducer.setUp()
  logger.info("Testing producer started")
  
  /**
   * The kafka consumer gets all the IASIOs written in the IASIO kafka topic
   * 
   * It means that it will get the IASIOs submitted by iasiosProducer and those
   * produced by the DASUs running in the Supervisor.
   * 
   * To have finer control over the test, we do not setup filters
   * to this consumer
   */
  val iasiosConsumer = new KafkaIasiosConsumer(
      KafkaHelper.DEFAULT_BOOTSTRAP_BROKERS,
      KafkaHelper.IASIOs_TOPIC_NAME,
      "SupervisorWithKafka-Consumer")
  iasiosConsumer.setUp()
  logger.info("Testing consumer started")
  
  iasiosConsumer.startGettingEvents(StartPosition.END, this)
  
  val monSysId = new Identifier("MonitoredSystemID",IdentifierType.MONITORED_SOFTWARE_SYSTEM,None)
  val pluginId = new Identifier("SimulatedPluginID",IdentifierType.PLUGIN,monSysId)
  val converterId = new Identifier("ConverterID",IdentifierType.CONVERTER,pluginId)
  
  /** The ID of the temeprature processed by a DASU of the Supervisor */
  val temperatureID = new Identifier("Temperature",IdentifierType.IASIO,converterId)
  
  /** The ID of the strenght processed by a DASU of the Supervisor */
  val strenghtID = new Identifier("Strenght",IdentifierType.IASIO,converterId)
  
  
  /** The identifier of the supervisor */
  val supervisorId = new Identifier("SupervisorWithKafka", IdentifierType.SUPERVISOR, None)
  
  // The JSON CDB reader
  val cdbParentPath = FileSystems.getDefault().getPath(".");
  val cdbFiles = new CdbJsonFiles(cdbParentPath)
  val cdbReader: CdbReader = new JsonReader(cdbFiles)
  
  /** The kafka publisher used by the supervisor to send the output of the DASUs to the BSDB*/
  val outputPublisher: OutputPublisher = KafkaPublisher(supervisorId.id, new Properties())
  
  /** The kafka consumer gets AISValues from the BSDB */
  val inputConsumer: InputSubscriber = new KafkaSubscriber(supervisorId.id, new Properties())
  
  /** The test uses real DASu i.e. the factory instantiates a DasuImpl */
  val factory = (dd: DasuDao, i: Identifier, op: OutputPublisher, id: InputSubscriber, cr: CdbReader) => DasuImpl(dd,i,op,id,cr)
  
  /** Signal the supervisor to terminate */
  val latch = new CountDownLatch(1)
  
  // Build the supervisor
  val supervisor = new Supervisor(supervisorId,outputPublisher,inputConsumer,cdbReader,factory)
  
  val started = supervisor.start()
  assert(started.isSuccess)

  runTest()
  
  /**
   * A value has been read from the BSDB
   * i.e. it has been received by iasiosConsumer
   */
  def iasioReceived(value: IASValue[_]) {
    logger.info("IASValue received {}",value.id)
    receivedIasValues.append(value)
  }
  
  /**
   * Run the test
   */
  def runTest() {
    
  }
  
  /**
   * Build a IASVlaue to submit to the BSDB
   */
  def buildIasioToSubmit(identifier: Identifier, value: Double) = {
    IASValue.buildIasValue(
        value, 
        System.currentTimeMillis(), 
        OperationalMode.OPERATIONAL, 
        IasValidity.RELIABLE, 
        identifier.fullRunningID, 
        IASTypes.DOUBLE)
  }
  
  /**
   * Signal the supervisor to terminate
   */
  def terminate() {
    logger.info("Request to terminate notified")
    latch.countDown()
  }
}