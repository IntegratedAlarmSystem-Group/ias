package org.eso.ias.supervisor.test

import org.eso.ias.cdb.CdbReader
import org.eso.ias.cdb.pojos.DasuDao
import org.eso.ias.cdb.structuredtext.StructuredTextReader
import org.eso.ias.dasu.DasuImpl
import org.eso.ias.dasu.publisher.OutputPublisher
import org.eso.ias.dasu.subscriber.{InputSubscriber, KafkaSubscriber}
import org.eso.ias.kafkautils.KafkaStringsConsumer.StreamPosition
import org.eso.ias.kafkautils.SimpleKafkaIasiosConsumer.IasioListener
import org.eso.ias.kafkautils.{KafkaHelper, KafkaIasiosProducer, SimpleKafkaIasiosConsumer, SimpleStringProducer}
import org.eso.ias.logging.IASLogger
import org.eso.ias.supervisor.Supervisor
import org.eso.ias.types.*
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.{BeforeAndAfter, BeforeAndAfterAll}

import java.nio.file.{FileSystems, Path}
import java.util
import java.util.Properties
import java.util.concurrent.atomic.AtomicReference
import java.util.concurrent.{CountDownLatch, TimeUnit}
import scala.collection.mutable.ListBuffer
import scala.jdk.javaapi.CollectionConverters

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
class SupervisorWithKafkaTest extends AnyFlatSpec with BeforeAndAfterAll with BeforeAndAfter {

  /** The logger */
  private val logger = IASLogger.getLogger(this.getClass)

  val monSysId = new Identifier("MonitoredSystemID", IdentifierType.MONITORED_SOFTWARE_SYSTEM, None)
  val pluginId = new Identifier("SimulatedPluginID", IdentifierType.PLUGIN, monSysId)
  val converterId = new Identifier("ConverterID", IdentifierType.CONVERTER, pluginId)

  /** The ID of the temperature processed by a DASU of the Supervisor */
  val temperatureID = new Identifier("Temperature", IdentifierType.IASIO, converterId)

  /** The ID of the strength processed by a DASU of the Supervisor */
  val strenghtID = new Identifier("Strenght", IdentifierType.IASIO, converterId)

  /** The identifier of the supervisor */
  val supervisorId = new Identifier("SupervisorWithKafka", IdentifierType.SUPERVISOR, None)

  // The JSON CDB reader
  val cdbParentPath: Path = FileSystems.getDefault.getPath("src/test")
  val cdbReader: CdbReader = new StructuredTextReader(cdbParentPath.toFile)
  cdbReader.init()

  /** The serializer to JSON strings */
  val serializer: IasValueStringSerializer = new IasValueJsonSerializer()

  /**
   * The kafka producer used by the test (the supervisor builds its own shared consumer
   */
  val stringProducer = new SimpleStringProducer(KafkaHelper.DEFAULT_BOOTSTRAP_BROKERS,"SupervisorWithKafka-Producer")

  /**
   *  The IASIOs producer submits IASIOs to the kafka topic i.e. to the BSDB:
   *  these IASIOs will be received and processed by the DASU running in the Supervisor
   *
   */
  val iasiosProducer = new KafkaIasiosProducer(
    stringProducer,
    KafkaHelper.IASIOs_TOPIC_NAME,
    serializer)
  iasiosProducer.setUp()
  logger.info("Testing producer started")

  /** All the values read from the BSDB */
  val receivedIasValues = new ListBuffer[IASValue[?]]()

  /**
   * The kafka consumer gets all the IASIOs written in the IASIO kafka topic
   *
   * It means that it will get the IASIOs submitted by iasiosProducer and those
   * produced by the DASUs running in the Supervisor.
   *
   * To have finer control over the test, we do not setup filters
   * to this consumer
   */
  val iasiosConsumer = new SimpleKafkaIasiosConsumer(
    KafkaHelper.DEFAULT_BOOTSTRAP_BROKERS,
    KafkaHelper.IASIOs_TOPIC_NAME,
    "SupervisorWithKafka-Consumer",
    )
  iasiosConsumer.setUp()
  logger.info("Testing consumer started")

  /** The kafka consumer gets AISValues from the BSDB */
  val inputConsumer: InputSubscriber = KafkaSubscriber(supervisorId.id, None, None, new Properties())
  
  /** The test uses real DASU i.e. the factory instantiates a DasuImpl */
  val factory: (DasuDao, Identifier, OutputPublisher, InputSubscriber) => DasuImpl = (dd: DasuDao, i: Identifier, op: OutputPublisher, id: InputSubscriber) => DasuImpl(dd, i, op, id, 1,1)

  /** The supervisor to test */
  val supervisor = new Supervisor(
    supervisorId,
    KafkaHelper.DEFAULT_BOOTSTRAP_BROKERS,
    inputConsumer,
    cdbReader,
    factory,
    None)

  cdbReader.shutdown()
  
  val latchRef: AtomicReference[CountDownLatch] = new AtomicReference
  
  before {
    receivedIasValues.clear()
  }
  
  after {}

  override def beforeAll(): Unit = {
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
      override def iasiosReceived(events: util.Collection[IASValue[?]]): Unit = {
        val iasValuesReceived = CollectionConverters.asScala(events)

        logger.info("{} IASValues received", iasValuesReceived.size)

        iasValuesReceived.foreach(iasio => {
          logger.info("IASValue received {}: [{}]", iasio.id, iasio.toString)
          receivedIasValues.append(iasio)
          val latch = latchRef.get
          if (latch != null) {
            latch.countDown()
          }
          logger.info("{} events in the queue", receivedIasValues.size.toString)
        })
      }
    }

    iasiosConsumer.startGettingEvents(StreamPosition.END, iasioListener)
    
    assert(supervisor.start().isSuccess)
    
    supervisor.enableAutoRefreshOfOutput(false)
    
    logger.info("BeforeAll done.")
  }

  override def afterAll(): Unit = {
    logger.info("AfterAll...")
    iasiosProducer.tearDown()
    iasiosConsumer.tearDown()
    supervisor.close()
    logger.info("AfterAll done")
  }

  /**
   * Build a [[IASValue]] to submit to the BSDB
   */
  def buildIasioToSubmit(identifier: Identifier, value: Double): IASValue[?] = {
    val t0 = System.currentTimeMillis()-100
      IASValue.build(
        value,
        OperationalMode.OPERATIONAL,
        IasValidity.RELIABLE,
        identifier.fullRunningID,
        IASTypes.DOUBLE,
        t0,
        t0+1,
        t0+5,
        t0+10,
        t0+15,
        t0+20,
        null,null,null)
  }

  behavior of "The Supervisor with Kafka input/output"

  it must "activate 2 DASUs with one ASCE each" in {
    val latch = new CountDownLatch(1)
    assert(supervisor.dasus.values.size == 2)

    assert(supervisor.dasus.values.forall(d => d.getAsceIds().size == 1))
  }

  it must "produce the output when a value is submitted to the BSDB" in {
    val latch = new CountDownLatch(2)
    latchRef.set(latch)
    
    val iasio = buildIasioToSubmit(temperatureID, 5)

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
    
    val iasioTemp = buildIasioToSubmit(temperatureID, 40)
    val iasioStrenght = buildIasioToSubmit(strenghtID, 10)

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
