package org.eso.ias.supervisor.test

import org.eso.ias.cdb.CdbReader
import org.eso.ias.cdb.pojos.DasuDao
import org.eso.ias.cdb.structuredtext.StructuredTextReader
import org.eso.ias.dasu.DasuImpl
import org.eso.ias.dasu.publisher.{ListenerOutputPublisherImpl, OutputListener, OutputPublisher}
import org.eso.ias.dasu.subscriber.{DirectInputSubscriber, InputSubscriber}
import org.eso.ias.logging.IASLogger
import org.eso.ias.supervisor.Supervisor
import org.eso.ias.types.*
import org.scalatest.flatspec.AnyFlatSpec

import java.nio.file.FileSystems
import scala.collection.mutable.ArrayBuffer

// The following import is required by the usage of the fixture
import org.eso.ias.heartbeat.publisher.HbLogProducer
import org.eso.ias.heartbeat.serializer.HbJsonSerializer
import org.eso.ias.types.IASTypes

import scala.language.reflectiveCalls

/**
 * This test checks if the the TF running in a templated ASCE
 * is able to get the inputs by their IDs by calling getValue.
 * 
 * It instantiates a templated DASU with one ASCE that
 * has 2 inputs of type Long plus 2 inputs of type alarm.
  * The TF simes the values of the longs plus 2 and 1 for the alarms.
 * One input is from a non templated input, other one
 * one is templated annd the alarms are templated input instances.
 * The TF gets the values of the inputs by calling getEval
 * and not directly accessing the map. 
 * getEval must recognize the templated and not templated parameters. 
 * For templated parameters, getValue must take into account the 
 * instance of the DASU/ASCE and the instance of templated inputs instances.
 * 
 * The test instantiates the DASU with the ASCE and send the inputs.
 * Finally, it checks the output.
 * 
 * The test, instantiates a real DASU and ASCE i.e. no mock used here.
 * 
 * The configuration is read from test/CDB:
 * Supervisor: SupervisorToTestInputs
 * Template: TemplToTestInputs
 * DASU: DasuToTestInputs
 * ASCE: AsceToTestInputs
 * Inputs: 
 *   Templated: TemplatedId (Long)
 *   Non Templated: NonTemplatedId (Long)
 * Output: TemplatedOutId (Long)
 */
class TemplatedInputTest extends AnyFlatSpec {
  
  /** The logger */
  private val logger = IASLogger.getLogger(this.getClass);
  
  /** Fixture to build same type of objects for the tests */
  trait Fixture {

    val stringSerializer = Option(new IasValueJsonSerializer)

    /**
     * Events i.e. outputs received
     */
    val outputEventsreceived = new ArrayBuffer[IASValue[?]]

    /**
     * Stringified outputs received
     */
    val stringEventsreceived = new ArrayBuffer[String]

    val outputListener = new OutputListener {
      /**
       * @see OutputListener
       */
      def outputStringifiedEvent(outputStr: String): Unit = {
        logger.info("Output received: {}", outputStr)
        stringEventsreceived.append(outputStr)
      }

      /**
       * @see OutputListener
       */
      def outputEvent(output: IASValue[?]): Unit = {
        outputEventsreceived.append(output)
      }
    }

    val outputPublisher: OutputPublisher = new ListenerOutputPublisherImpl(outputListener, stringSerializer)

    val inputsProvider = new DirectInputSubscriber()

    val factory = (dd: DasuDao, i: Identifier, op: OutputPublisher, id: InputSubscriber) =>
    DasuImpl(dd,i,op,id,10,10)

    // Build the CDB reader
    val cdbParentPath = FileSystems.getDefault().getPath("src/test");
    val cdbReader: CdbReader = new StructuredTextReader(cdbParentPath.toFile)
    cdbReader.init()

    val supervIdentifier = new Identifier("SupervisorToTestInputs", IdentifierType.SUPERVISOR, None)

    /** The supervisor to test */
    val supervisor = new Supervisor(
      supervIdentifier,
      outputPublisher,
      inputsProvider,
      new HbLogProducer(new HbJsonSerializer),
      new CommandManagerMock(supervIdentifier),
      cdbReader,
      factory,
      None)

    cdbReader.shutdown()

    supervisor.start()
    supervisor.enableAutoRefreshOfOutput(false)

    // The identifier to send the inputs
    val Sup2Id = new Identifier("AnotherSupervID", IdentifierType.SUPERVISOR, None)
    val DasuId = new Identifier("DASU-Id", IdentifierType.DASU, Sup2Id)
    val AsceId = new Identifier("ASCE-Id", IdentifierType.ASCE, DasuId)

    val idTemplated = new Identifier(Identifier.buildIdFromTemplate("TemplatedId",4), IdentifierType.IASIO, AsceId)
    val idNonTemplated = new Identifier("NonTemplatedId", IdentifierType.IASIO, AsceId)
    val idTemplInstanceInput1 = new Identifier(Identifier.buildIdFromTemplate("TemplatedInput",3), IdentifierType.IASIO, AsceId)
    val idTemplInstanceInput2 = new Identifier(Identifier.buildIdFromTemplate("TemplatedInput",4), IdentifierType.IASIO, AsceId)

    val outputId = "TemplatedOutId"

  }

  behavior of "The TF with templates"
  
  it must "get the IASValue by their IDs" in new Fixture {
    val t0 = System.currentTimeMillis()-100
    val templatedValue = IASValue.build(
      8L,
      OperationalMode.OPERATIONAL,
      IasValidity.RELIABLE,
      idTemplated.fullRunningID,
      IASTypes.LONG,
      t0,
      t0+1,
      t0+5,
      t0+10,
      t0+15,
      t0+20,
      null, null,null)

    val t1 = System.currentTimeMillis()-50
    val nonTemplatedValue = IASValue.build(
      3L,
      OperationalMode.OPERATIONAL,
      IasValidity.RELIABLE,
      idNonTemplated.fullRunningID,
      IASTypes.LONG,
      t1,
      t1+1,
      t1+5,
      t1+10,
      t1+15,
      t1+20,
      null,null,null)

    val t2 = System.currentTimeMillis()-25
    val templInstValue1 = IASValue.build(
      Alarm.getInitialAlarmState.set(),
      OperationalMode.OPERATIONAL,
      IasValidity.RELIABLE,
      idTemplInstanceInput1.fullRunningID,
      IASTypes.ALARM,
      t2,
      t2+1,
      t2+5,
      t2+10,
      t2+15,
      t2+20,
      null,
      null,null)

    val t3 = System.currentTimeMillis()-10
    val templInstValue2 = IASValue.build(
      Alarm.getInitialAlarmState(Priority.CRITICAL).set(),
      OperationalMode.OPERATIONAL,
      IasValidity.RELIABLE,
      idTemplInstanceInput2.fullRunningID,
      IASTypes.ALARM,
      t3,
      t3+1,
      t3+5,
      t3+10,
      t3+15,
      t3+20,
      null,
      null,null)
    
    val inputs:  Set[IASValue[?]] = Set(nonTemplatedValue,templatedValue,templInstValue1,templInstValue2)
    
    // Sends the input to the Supervisor and the DASU
    inputsProvider.sendInputs(inputs)
    
    // A thread of the DASU produces the output: give time to the thread
    val now = System.currentTimeMillis()
    while (outputEventsreceived.isEmpty && System.currentTimeMillis() < now + 5000) {
      Thread.sleep(100)
    }
    // The output shall be ready now
    assert(stringEventsreceived.size==1, "Expected one output event, but got: " + stringEventsreceived.size)	
    assert(outputEventsreceived.size==1, "Expected one output event, but got: " + outputEventsreceived.size)
    
    val valueOfOutput = outputEventsreceived.head.value.asInstanceOf[Long]
    assert(valueOfOutput==14L)
    
    supervisor.close()
  }
}
