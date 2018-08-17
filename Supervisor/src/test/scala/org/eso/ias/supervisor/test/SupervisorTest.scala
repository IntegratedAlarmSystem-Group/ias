package org.eso.ias.supervisor.test

import org.scalatest.FlatSpec
import org.eso.ias.types.IasValueJsonSerializer
import org.eso.ias.dasu.publisher.OutputPublisher
import org.eso.ias.dasu.publisher.OutputListener
import org.eso.ias.dasu.publisher.ListenerOutputPublisherImpl
import org.eso.ias.logging.IASLogger
import org.eso.ias.dasu.publisher.DirectInputSubscriber
import org.eso.ias.types.IASValue
import org.eso.ias.supervisor.Supervisor
import org.eso.ias.types.Identifier
import org.eso.ias.types.IdentifierType
import org.eso.ias.cdb.CdbReader
import org.eso.ias.cdb.json.CdbJsonFiles
import org.eso.ias.cdb.json.JsonReader
import java.nio.file.FileSystems
import org.eso.ias.types.OperationalMode
import org.eso.ias.types.IasValidity
import org.eso.ias.types.IASTypes
import scala.collection.mutable.ArrayBuffer

// The following import is required by the usage of the fixture
import language.reflectiveCalls
import java.util.HashSet
import org.eso.ias.heartbeat.HbProducer
import org.eso.ias.heartbeat.publisher.HbLogProducer
import org.eso.ias.heartbeat.serializer.HbJsonSerializer

class SupervisorTest extends FlatSpec {

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

      // Build the CDB reader
      val cdbParentPath = FileSystems.getDefault().getPath(".");
      val cdbFiles = new CdbJsonFiles(cdbParentPath)
      val cdbReader: CdbReader = new JsonReader(cdbFiles)

      val supervIdentifier = new Identifier("SupervisorID", IdentifierType.SUPERVISOR, None)
      
      /** The supervisor to test */
      val supervisor = new Supervisor(
        supervIdentifier,
        outputPublisher,
        inputsProvider,
        new HbLogProducer(new HbJsonSerializer),
        cdbReader,
        DasuMock.apply)

    }

  behavior of "The Supervisor"

  it must "instantiate the DASUs defined in the CDB" in {
    val f = fixture
    // There are 3 DASUs
    assert(f.supervisor.dasuIds.size == 3)
    assert(f.supervisor.dasuIds.forall(id => id == "Dasu1" || id == "Dasu2" || id == "Dasu3"))
  }

  it must "properly associate inputs to DASUs" in {
    val f = fixture
    val inputsToDasu1 = f.supervisor.iasiosToDasusMap("Dasu1")
    assert(inputsToDasu1.size == 5 * 10) // 5 inputs for each ASCE running in the DASU
    assert(inputsToDasu1.forall(id => id.contains("Dasu1")))

    val inputsToDasu2 = f.supervisor.iasiosToDasusMap("Dasu2")
    assert(inputsToDasu2.size == 5 * 15) // 5 inputs for each ASCE running in the DASU
    assert(inputsToDasu2.forall(id => id.contains("Dasu2")))

    val inputsToDasu3 = f.supervisor.iasiosToDasusMap("Dasu3")
    assert(inputsToDasu3.size == 5 * 8) // 5 inputs for each ASCE running in the DASU
    assert(inputsToDasu3.forall(id => id.contains("Dasu3")))
  }

  it must "start each DASU" in {
    val f = fixture
    assert(f.supervisor.start().isSuccess)

    val dasus = f.supervisor.dasus.values
    dasus.foreach(d => assert(d.asInstanceOf[DasuMock].numOfStarts.get() == 1))

    f.supervisor.cleanUp()
  }

  it must "clean up each DASU" in {
    val f = fixture
    assert(f.supervisor.start().isSuccess)

    f.supervisor.cleanUp()

    val dasus = f.supervisor.dasus.values
    dasus.foreach(d => assert(d.asInstanceOf[DasuMock].numOfCleanUps.get() == 1))
  }

  it must "activate the auto refresh of the output of each DASU" in {
    val f = fixture
    assert(f.supervisor.start().isSuccess)

    val dasus = f.supervisor.dasus.values
    dasus.foreach(d => assert(d.asInstanceOf[DasuMock].numOfEnableAutorefresh.get() == 1))

    f.supervisor.cleanUp()
  }

  it must "properly forward IASIOs do DASUs" in {
    val f = fixture
    assert(f.supervisor.start().isSuccess)

    // NO DASU should receive the following IASIOs
    val Sup2Id = new Identifier("AnotherSupervID", IdentifierType.SUPERVISOR, None)
    val DasuId = new Identifier("DASU-Id", IdentifierType.DASU, Sup2Id)
    val AsceId = new Identifier("ASCE-Id", IdentifierType.ASCE, DasuId)
    val iasiosToSend = for (i <- 1 to 20) yield {
    val iasValueId = new Identifier("IASIO-Id" + i, IdentifierType.IASIO, AsceId)
    
    val t0 = System.currentTimeMillis()-100
      IASValue.build(
        10L,
			  OperationalMode.OPERATIONAL,
			  IasValidity.RELIABLE,
			  iasValueId.fullRunningID,
			  IASTypes.LONG,
			  t0,
			  t0+5,
			  t0+10,
			  t0+15,
			  t0+20,
			  t0+25,
			  null,
			null,null)
    }
    
    assert(iasiosToSend.toSet.size == 20)

    logger.info(
      "Sending {} inputs to the supervisor: {}",
      iasiosToSend.size.toString(),
      iasiosToSend.map(i => i.id).mkString(", "))

    f.inputsProvider.sendInputs(iasiosToSend.toSet)
    val dasus = f.supervisor.dasus.values
    dasus.foreach(d => assert(d.asInstanceOf[DasuMock].inputsReceivedFromSuperv.isEmpty))

    // Only DASU2 should receive the followings
    //
    // The format of the input of this DASU is ASCEXOnDasu2-IDY-In
    val iasioForDasu2 = for {
      i <- 1 to 15
      x <- 1 to 5
      id = "ASCE" + i + "OnDasu2-ID" + x + "-In"
    } yield {
      val iasValueId = new Identifier(id, IdentifierType.IASIO, AsceId)
      val t0 = System.currentTimeMillis()-100
      IASValue.build(
        10L,
			  OperationalMode.OPERATIONAL,
			  IasValidity.RELIABLE,
			  iasValueId.fullRunningID,
			  IASTypes.LONG,
			  t0,
			  t0+5,
			  t0+10,
			  t0+15,
			  t0+20,
			  t0+25,
			null,
			null,null)
			
      
    }

    val iasioForDasu2Set = iasioForDasu2.toSet
    val ids = iasioForDasu2Set.map(i => i.id)

    logger.info("Sending {} iasios {}", ids.size.toString, ids.mkString(", "))
    f.inputsProvider.sendInputs(iasioForDasu2Set)
    assert(f.supervisor.dasus("Dasu1").asInstanceOf[DasuMock].inputsReceivedFromSuperv.isEmpty)
    assert(f.supervisor.dasus("Dasu2").asInstanceOf[DasuMock].inputsReceivedFromSuperv.size == 15 * 5)
    assert(f.supervisor.dasus("Dasu3").asInstanceOf[DasuMock].inputsReceivedFromSuperv.isEmpty)
  }
  
  // Sends some input to DASU1 and DASU3 and check if the
  // supervisor published their output (bit no output must be
  // produced by DASU2)
  it must "publish the output" in {
    
    val f = fixture
    assert(f.supervisor.start().isSuccess)
    
    val Sup2Id = new Identifier("AnotherSupervID", IdentifierType.SUPERVISOR, None)
    val DasuId = new Identifier("DASU-Id", IdentifierType.DASU, Sup2Id)
    val AsceId = new Identifier("ASCE-Id", IdentifierType.ASCE, DasuId)
    
    // Few IASIOs for DASU1
    //
    // The format of the input of this DASU is ASCEXOnDasu1-IDY-In
    val iasioForDasu1 = for {
      i <- 1 to 8 // ASCEs
      x <- 1 to 3
      id = "ASCE" + i + "OnDasu1-ID" + x + "-In"
    } yield {
      val iasValueId = new Identifier(id, IdentifierType.IASIO, AsceId)
      val t0 = System.currentTimeMillis()-100
      IASValue.build(
        10L,
			  OperationalMode.OPERATIONAL,
			  IasValidity.RELIABLE,
			  iasValueId.fullRunningID,
			  IASTypes.LONG,
			  t0,
			  t0+5,
			  t0+10,
			  t0+15,
			  t0+20,
			  t0+25,
			  null,
			  null,null)
    }
    
    // And for DASU3
    val iasioForDasu3 = for {
      i <- 1 to 5 // ASCEs
      x <- 1 to 4
      id = "ASCE" + i + "OnDasu3-ID" + x + "-In"
    } yield {
      val iasValueId = new Identifier(id, IdentifierType.IASIO, AsceId)
      val t0 = System.currentTimeMillis()-100
      IASValue.build(
        10L,
			  OperationalMode.OPERATIONAL,
			  IasValidity.RELIABLE,
			  iasValueId.fullRunningID,
			  IASTypes.LONG,
			  t0,
			  t0+5,
			  t0+10,
			  t0+15,
			  t0+20,
			  t0+25,
			  null,
			  null,null)
      
    }

    val iasioForDasusSet = iasioForDasu1.toSet ++ iasioForDasu3.toSet
    val ids = iasioForDasusSet.map(i => i.id)

    logger.info("Sending {} iasios {}", ids.size.toString, ids.mkString(", "))
    f.inputsProvider.sendInputs(iasioForDasusSet)
    
    assert(f.outputEventsreceived.size==2)
    assert(f.stringEventsreceived.size==2)
    assert(f.stringEventsreceived.forall(s => s.contains("Dasu1")||s.contains("Dasu3")))
  }
}
