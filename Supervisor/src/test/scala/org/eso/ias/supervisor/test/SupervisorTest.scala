package org.eso.ias.supervisor.test

import org.eso.ias.cdb.CdbReader
import org.eso.ias.cdb.structuredtext.StructuredTextReader
import org.eso.ias.dasu.publisher.{ListenerOutputPublisherImpl, OutputListener, OutputPublisher}
import org.eso.ias.dasu.subscriber.DirectInputSubscriber
import org.eso.ias.logging.IASLogger
import org.eso.ias.supervisor.Supervisor
import org.eso.ias.types.*
import org.scalatest.flatspec.AnyFlatSpec

import java.nio.file.FileSystems
import scala.collection.mutable.ArrayBuffer

// The following import is required by the usage of the fixture
import org.eso.ias.heartbeat.publisher.HbLogProducer
import org.eso.ias.heartbeat.serializer.HbJsonSerializer

import scala.language.reflectiveCalls

class SupervisorTest extends AnyFlatSpec {

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

    // Build the CDB reader
    val cdbParentPath = FileSystems.getDefault().getPath("src/test");
    val cdbReader: CdbReader = new StructuredTextReader(cdbParentPath.toFile)
    cdbReader.init()

    val supervIdentifier = new Identifier("SupervisorID", IdentifierType.SUPERVISOR, None)

    /** The supervisor to test */
    val supervisor = new Supervisor(
      supervIdentifier,
      outputPublisher,
      inputsProvider,
      new HbLogProducer(new HbJsonSerializer),
      new CommandManagerMock(supervIdentifier),
      cdbReader,
      DasuMock.apply,None)

    cdbReader.shutdown()
  }

  behavior of "The Supervisor"

  it must "instantiate the DASUs defined in the CDB" in new Fixture {
    // There are 3 DASUs
    assert(supervisor.dasuIds.size == 3)
    assert(supervisor.dasuIds.forall(id => id == "Dasu1" || id == "Dasu2" || id == "Dasu3"))
  }

  it must "properly associate inputs to DASUs" in new Fixture {
    val inputsToDasu1 = supervisor.iasiosToDasusMap("Dasu1")
    assert(inputsToDasu1.size == 5 * 10) // 5 inputs for each ASCE running in the DASU
    assert(inputsToDasu1.forall(id => id.contains("Dasu1")))

    val inputsToDasu2 = supervisor.iasiosToDasusMap("Dasu2")
    assert(inputsToDasu2.size == 5 * 15) // 5 inputs for each ASCE running in the DASU
    assert(inputsToDasu2.forall(id => id.contains("Dasu2")))

    val inputsToDasu3 = supervisor.iasiosToDasusMap("Dasu3")
    assert(inputsToDasu3.size == 5 * 8) // 5 inputs for each ASCE running in the DASU
    assert(inputsToDasu3.forall(id => id.contains("Dasu3")))
  }

  it must "start each DASU" in new Fixture {
    assert(supervisor.start().isSuccess)

    val dasus = supervisor.dasus.values
    dasus.foreach(d => assert(d.asInstanceOf[DasuMock].numOfStarts.get() == 1))

    supervisor.close()
  }

  it must "clean up each DASU" in new Fixture {
    assert(supervisor.start().isSuccess)

    supervisor.close()

    val dasus = supervisor.dasus.values
    dasus.foreach(d => assert(d.asInstanceOf[DasuMock].numOfCleanUps.get() == 1))
  }

  it must "activate the auto refresh of the output of each DASU" in new Fixture {
    assert(supervisor.start().isSuccess)

    val dasus = supervisor.dasus.values
    dasus.foreach(d => assert(d.asInstanceOf[DasuMock].numOfEnableAutorefresh.get() == 1))

    supervisor.close()
  }

  it must "properly forward IASIOs do DASUs" in new Fixture {
    assert(supervisor.start().isSuccess)

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
        t0+1,
        t0+5,
        t0+10,
        t0+15,
        t0+20,
        null,
        null,
        null)
    }
    
    assert(iasiosToSend.toSet.size == 20)

    logger.info(
      "Sending {} inputs to the supervisor: {}",
      iasiosToSend.size.toString(),
      iasiosToSend.map(i => i.id).mkString(", "))

    inputsProvider.sendInputs(iasiosToSend.toSet)
    val dasus = supervisor.dasus.values
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
        t0+1,
        t0+5,
        t0+10,
        t0+15,
        t0+20,
        null,
        null,null)
    }

    val iasioForDasu2Set = iasioForDasu2.toSet
    val ids = iasioForDasu2Set.map(i => i.id)

    logger.info("Sending {} iasios {}", ids.size.toString, ids.mkString(", "))
    inputsProvider.sendInputs(iasioForDasu2Set)
    assert(supervisor.dasus("Dasu1").asInstanceOf[DasuMock].inputsReceivedFromSuperv.isEmpty)
    assert(supervisor.dasus("Dasu2").asInstanceOf[DasuMock].inputsReceivedFromSuperv.size == 15 * 5)
    assert(supervisor.dasus("Dasu3").asInstanceOf[DasuMock].inputsReceivedFromSuperv.isEmpty)
  }
  
  // Sends some input to DASU1 and DASU3 and check if the
  // supervisor published their output (bit no output must be
  // produced by DASU2)
  it must "publish the output" in new Fixture {
    assert(supervisor.start().isSuccess)
    
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
        t0+1,
        t0+5,
        t0+10,
        t0+15,
        t0+20,
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
        t0+1,
        t0+5,
        t0+10,
        t0+15,
        t0+20,
        null, null,null)
      
    }

    val iasioForDasusSet = iasioForDasu1.toSet ++ iasioForDasu3.toSet
    val ids = iasioForDasusSet.map(i => i.id)

    logger.info("Sending {} iasios {}", ids.size.toString, ids.mkString(", "))
    inputsProvider.sendInputs(iasioForDasusSet)
    
    assert(outputEventsreceived.size==2)
    assert(stringEventsreceived.size==2)
    assert(stringEventsreceived.forall(s => s.contains("Dasu1")||s.contains("Dasu3")))
  }
}
