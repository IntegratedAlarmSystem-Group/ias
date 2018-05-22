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
import org.eso.ias.types.IdentifierType
import org.eso.ias.types.Identifier
import org.eso.ias.cdb.pojos.DasuDao
import org.eso.ias.dasu.DasuImpl
import org.eso.ias.supervisor.Supervisor
import scala.util.Success
import scala.util.Failure
import java.util.concurrent.CountDownLatch
import org.eso.ias.logging.IASLogger
import org.scalatest.FlatSpec
import org.eso.ias.kafkautils.KafkaIasiosProducer
import org.eso.ias.kafkautils.KafkaHelper
import org.eso.ias.types.IasValueStringSerializer
import org.eso.ias.types.IasValueJsonSerializer
import org.eso.ias.kafkautils.KafkaIasiosConsumer
import org.eso.ias.kafkautils.SimpleStringConsumer.StartPosition
import org.eso.ias.kafkautils.KafkaIasiosConsumer.IasioListener
import org.eso.ias.types.IASValue
import org.eso.ias.types.OperationalMode
import org.eso.ias.types.IasValidity
import org.eso.ias.types.IASTypes
import scala.collection.mutable.ListBuffer

import java.util.concurrent.TimeUnit
import org.scalatest.BeforeAndAfter
import org.eso.ias.kafkautils.KafkaIasiosConsumer.IasioListener
import org.scalatest.BeforeAndAfterAll
import java.util.concurrent.atomic.AtomicReference
import java.util.HashSet
import org.eso.ias.heartbeat.publisher.HbLogProducer
import org.eso.ias.heartbeat.serializer.HbJsonSerializer

/**
 * Test the Supervisor connected to the kafka BSDB.
 * 
 * Note that supervisor, IASIOs producer and consumer are created once
 * at the beginning of the test and reused along the different tests
 *
 * The configuration of this Supervisor is in the JSON CDB.
 *
 * The supervisor runs 2 DASU with one ASCE each and
 * checks the generation of the outputs by its DASUs and their ASCEs
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
class SupervisorWithKafkaTest extends FlatSpec with BeforeAndAfterAll with BeforeAndAfter {

  /** The logger */
  private val logger = IASLogger.getLogger(this.getClass)

  val monSysId = new Identifier("MonitoredSystemID", IdentifierType.MONITORED_SOFTWARE_SYSTEM, None)
  val pluginId = new Identifier("SimulatedPluginID", IdentifierType.PLUGIN, monSysId)
  val converterId = new Identifier("ConverterID", IdentifierType.CONVERTER, pluginId)

  /** The ID of the temeprature processed by a DASU of the Supervisor */
  val temperatureID = new Identifier("Temperature", IdentifierType.IASIO, converterId)

  /** The ID of the strenght processed by a DASU of the Supervisor */
  val strenghtID = new Identifier("Strenght", IdentifierType.IASIO, converterId)

  /** The identifier of the supervisor */
  val supervisorId = new Identifier("SupervisorWithKafka", IdentifierType.SUPERVISOR, None)

  // The JSON CDB reader
  val cdbParentPath = FileSystems.getDefault().getPath(".");
  val cdbFiles = new CdbJsonFiles(cdbParentPath)
  val cdbReader: CdbReader = new JsonReader(cdbFiles)

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

  /** All the values read from the BSDB */
  val receivedIasValues = new ListBuffer[IASValue[_]]()

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

  /** The kafka publisher used by the supervisor to send the output of the DASUs to the BSDB*/
  val outputPublisher: OutputPublisher = KafkaPublisher(supervisorId.id, None, None, new Properties())

  /** The kafka consumer gets AISValues from the BSDB */
  val inputConsumer: InputSubscriber = KafkaSubscriber(supervisorId.id, None, None, new Properties())
  
  /** The test uses real DASu i.e. the factory instantiates a DasuImpl */
  val factory = (dd: DasuDao, i: Identifier, op: OutputPublisher, id: InputSubscriber) => 
    DasuImpl(dd, i, op, id, 1,1)

  /** The supervisor to test */
  val supervisor = new Supervisor(
      supervisorId, 
      outputPublisher, 
      inputConsumer, 
      new HbLogProducer(new HbJsonSerializer),
      cdbReader,
      factory)
  
  val latchRef: AtomicReference[CountDownLatch] = new AtomicReference
  
  before {
    receivedIasValues.clear()
  }
  
  after {}

  override def beforeAll() {
    logger.info("BeforeAll...")
    /**
     * The listener of IASValues published in the BSDB
     * It receives the values sent by the test as well as the outputs
     * of the DASUs running in the supervisor
     */
    val iasioListener = new IasioListener {
      /**
       * A value has been read from the BSDB
       * i.e. it has been received by iasiosConsumer
       */
      def iasioReceived(value: IASValue[_]) {
        logger.info("IASValue received {}: [{}]", value.id, value.toString())
        receivedIasValues.append(value)
        val latch = latchRef.get
        if (latch!=null) {
          latch.countDown()
        }
        logger.info("{} events in the queue", receivedIasValues.size.toString())
      }
    }

    iasiosConsumer.startGettingEvents(StartPosition.END, iasioListener)
    
    // .get returns the value from this Success or throws the exception if this is a Failure.
    assert(supervisor.start().isSuccess)
    
    supervisor.enableAutoRefreshOfOutput(false)
    
    logger.info("BeforeAll done.")
  }

  override def afterAll() {
    logger.info("AfterAll...")
    iasiosProducer.tearDown()
    iasiosConsumer.tearDown()
    supervisor.cleanUp()
    logger.info("AfterAll done")
  }

  /**
   * Build a IASVlaue to submit to the BSDB
   */
  def buildIasioToSubmit(identifier: Identifier, value: Double) = {
    val t0 = System.currentTimeMillis()-100
      IASValue.build(
        value,
			  OperationalMode.OPERATIONAL,
			  IasValidity.RELIABLE,
			  identifier.fullRunningID,
			  IASTypes.DOUBLE,
			  t0,
			  t0+5,
			  t0+10,
			  t0+15,
			  t0+20,
			  t0+25,
			null,
			null,null)
    
  }

  behavior of "The Supervisor with Kafka input/ouput"

  it must "activate 2 DASUs with one ASCE each" in {
    val latch = new CountDownLatch(1)
    assert(supervisor.dasus.values.size == 2)

    assert(supervisor.dasus.values.forall(d => d.getAsceIds().size == 1))
  }

  it must "produce the output when a value is submitted to the BSDB" in {
    val latch = new CountDownLatch(2)
    latchRef.set(latch)
    
    val iasio = buildIasioToSubmit(temperatureID, 5);

    // Disable the auto-refresh to avoid replication
    logger.info("Disabling auto-refresh of the output by the DASU")
    supervisor.enableAutoRefreshOfOutput(false)

    // We expect to see in the BSDB the iasio (iasio) we just sent
    // plus the output produced by the DASU
    logger.info("Publishing IASValue [{}]", iasio.id)
    iasiosProducer.push(iasio)
    
    // Wait until the output is produced
    logger.info("Waiting for the output from the DASU...")
    assert(latch.await(1, TimeUnit.MINUTES))
    
    val receivedIds=receivedIasValues.map(_.id)
    
    assert(receivedIds.forall(id => id==temperatureID.id || id=="TemperatureAlarm"))

    
  }
  
  it must "produce the output of both DASUs when values are submitted to the BSDB" in {
    val latch = new CountDownLatch(4)
    latchRef.set(latch)
    
    val iasioTemp = buildIasioToSubmit(temperatureID, 40);
    val iasioStrenght = buildIasioToSubmit(strenghtID, 10);

    // Desable the autorefresh to avoid replication
    logger.info("Disabling auto-refresh of the output by the DASU")
    supervisor.enableAutoRefreshOfOutput(false)

    // We expect to see in the BSDB the iasio (iasio) we just sent
    // plus the output produced by the DASU
    logger.info("Publishing IASValues {} and {}", iasioTemp.id, iasioStrenght.id)
    iasiosProducer.push(iasioTemp)
    iasiosProducer.push(iasioStrenght)
    
    // Wait until the output is produced
    logger.info("Waiting for the output from the DASU...")
    assert(latch.await(1, TimeUnit.MINUTES))
    
    val receivedIds=receivedIasValues.map(_.id)
    
    assert(receivedIds.forall(id =>
        id==temperatureID.id || 
        id=="TemperatureAlarm" ||
        id==strenghtID.id ||
        id=="StrenghtAlarm"))
  }

}
