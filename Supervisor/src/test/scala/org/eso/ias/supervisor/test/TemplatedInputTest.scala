package org.eso.ias.supervisor.test

import org.scalatest.FlatSpec
import org.eso.ias.logging.IASLogger
import org.eso.ias.types.IasValueJsonSerializer
import scala.collection.mutable.ArrayBuffer
import org.eso.ias.dasu.publisher.OutputListener
import org.eso.ias.dasu.publisher.ListenerOutputPublisherImpl
import org.eso.ias.types.IASValue
import org.eso.ias.dasu.publisher.OutputPublisher
import org.eso.ias.dasu.publisher.DirectInputSubscriber
import java.nio.file.FileSystems
import org.eso.ias.cdb.json.CdbJsonFiles
import org.eso.ias.cdb.CdbReader
import org.eso.ias.cdb.json.JsonReader
import org.eso.ias.types.Identifier
import org.eso.ias.types.IdentifierType
import org.eso.ias.supervisor.Supervisor
import org.eso.ias.cdb.pojos.DasuDao
import org.eso.ias.dasu.subscriber.InputSubscriber
import org.eso.ias.dasu.DasuImpl
import org.eso.ias.types.OperationalMode
import org.eso.ias.types.IasValidity

// The following import is required by the usage of the fixture
import language.reflectiveCalls
import org.eso.ias.types.IASTypes
import org.eso.ias.heartbeat.serializer.HbJsonSerializer
import org.eso.ias.heartbeat.publisher.HbLogProducer

/**
 * This test checks if the the TF running in a templated ASCE
 * is able to get the inputs by their IDs by calling getValue.
 * 
 * It instantiates a templated DASU with one ASCE that
 * sums 2 inputs of type Long.
 * One input is from a non templated input while the other 
 * one is templated.
 * The TF gets the values of the inputs by calling getEval
 * and not directly accessing the map. 
 * getEval must recognize the templated and not templated parameters. 
 * For templated parameters, getValue must take into account the 
 * instance of the DASU/ASCE.
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
class TemplatedInputTest extends FlatSpec {
  
  /** The logger */
  private val logger = IASLogger.getLogger(this.getClass);
  
  /** Fixture to build same type of objects for the tests */
  def fixture =
    new {

      val stringSerializer = Option(new IasValueJsonSerializer)

      /**
       * Events i.e. outputs received
       */
      val outputEventsreceived = new ArrayBuffer[IASValue[_]]

      /**
       * Stringified outputs received
       */
      val stringEventsreceived = new ArrayBuffer[String]

      val outputListener = new OutputListener {
        /**
         * @see OutputListener
         */
        def outputStringifiedEvent(outputStr: String) {
          logger.info("Output received: {}", outputStr)
          stringEventsreceived.append(outputStr)
        }

        /**
         * @see OutputListener
         */
        def outputEvent(output: IASValue[_]) {
          outputEventsreceived.append(output)
        }
      }

      val outputPublisher: OutputPublisher = new ListenerOutputPublisherImpl(outputListener, stringSerializer)

      val inputsProvider = new DirectInputSubscriber()
      
      val factory = (dd: DasuDao, i: Identifier, op: OutputPublisher, id: InputSubscriber) => 
      DasuImpl(dd,i,op,id,10,10)

      // Build the CDB reader
      val cdbParentPath = FileSystems.getDefault().getPath(".");
      val cdbFiles = new CdbJsonFiles(cdbParentPath)
      val cdbReader: CdbReader = new JsonReader(cdbFiles)

      val supervIdentifier = new Identifier("SupervisorToTestInputs", IdentifierType.SUPERVISOR, None)

      /** The supervisor to test */
      val supervisor = new Supervisor(
        supervIdentifier,
        outputPublisher,
        inputsProvider,
        new HbLogProducer(new HbJsonSerializer),
        cdbReader,
        factory)
      
      supervisor.start()
      supervisor.enableAutoRefreshOfOutput(false)
      
      // The identifier to send the inputs
      val Sup2Id = new Identifier("AnotherSupervID", IdentifierType.SUPERVISOR, None)
      val DasuId = new Identifier("DASU-Id", IdentifierType.DASU, Sup2Id)
      val AsceId = new Identifier("ASCE-Id", IdentifierType.ASCE, DasuId)
      
      val idTemplated = new Identifier(Identifier.buildIdFromTemplate("TemplatedId",4), IdentifierType.IASIO, AsceId)
      val idNonTemplated = new Identifier("NonTemplatedId", IdentifierType.IASIO, AsceId)
      
      val outputId = "TemplatedOutId"

    }

  behavior of "The TF with templates"
  
  it must "get the IASValue by their IDs" in {
    val f = fixture
    
    val t0 = System.currentTimeMillis()-100
    val templatedValue = IASValue.build(
      8L,
		  OperationalMode.OPERATIONAL,
		  IasValidity.RELIABLE,
		  f.idTemplated.fullRunningID,
		  IASTypes.LONG,
		  t0,
		  t0+5,
		  t0+10,
		  t0+15,
		  t0+20,
		  t0+25,
		  null,
		null,null)
		
		val t1 = System.currentTimeMillis()-50
    val nonTemplatedValue = IASValue.build(
      3L,
		  OperationalMode.OPERATIONAL,
		  IasValidity.RELIABLE,
		  f.idNonTemplated.fullRunningID,
		  IASTypes.LONG,
		  t1,
		  t1+5,
		  t1+10,
		  t1+15,
		  t1+20,
		  t1+25,
		  null,
		null,null)
    
    val inputs:  Set[IASValue[_]] = Set(nonTemplatedValue,templatedValue)
    
    // Sends the input to the Supervisor and the DASU
    f.inputsProvider.sendInputs(inputs)
    
    // Did the DASU produce the output?
    assert(f.stringEventsreceived.size==1)
    assert(f.outputEventsreceived.size==1)
    
    val valueOfOutput = f.outputEventsreceived.head.value.asInstanceOf[Long]
    assert(valueOfOutput==11L)
    
    f.supervisor.cleanUp()
  }
  
  
}
