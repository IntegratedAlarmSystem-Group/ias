package org.eso.ias.supervisor.test

import org.eso.ias.cdb.CdbReader
import org.eso.ias.cdb.structuredtext.StructuredTextReader
import org.eso.ias.dasu.publisher.{ListenerOutputPublisherImpl, OutputListener, OutputPublisher}
import org.eso.ias.dasu.subscriber.DirectInputSubscriber
import org.eso.ias.logging.IASLogger
import org.eso.ias.supervisor.Supervisor
import org.eso.ias.types.*
import org.scalatest.flatspec.AnyFlatSpec

import java.nio.file.{FileSystems, Path}
import scala.collection.mutable.ArrayBuffer

// The following import is required by the usage of the fixture
import org.eso.ias.heartbeat.publisher.HbLogProducer
import org.eso.ias.heartbeat.serializer.HbJsonSerializer

import scala.language.reflectiveCalls

class SupervisorWithTemplatesTest extends AnyFlatSpec {

  /** The logger */
  private val logger = IASLogger.getLogger(this.getClass)

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

    val outputListener: OutputListener = new OutputListener {
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
    val cdbParentPath: Path = FileSystems.getDefault.getPath("src/test")
    val cdbReader: CdbReader = new StructuredTextReader(cdbParentPath.toFile)
    cdbReader.init()

    val supervIdentifier = new Identifier("SupervisorWithTemplates", IdentifierType.SUPERVISOR, None)

    /** The supervisor to test */
    val supervisor = new Supervisor(
      supervIdentifier,
      outputPublisher,
      inputsProvider,
      new HbLogProducer(new HbJsonSerializer),
      new CommandManagerMock(supervIdentifier),
      cdbReader,
      DasuMock.apply,
      None)

    cdbReader.shutdown()
  }

  behavior of "The Supervisor with templates"

  it must "instantiate the templated DASUs defined in the CDB" in new Fixture {
    // There are 2 templated DASUs and one non templated DASU
    assert(supervisor.dasuIds.size == 4)
    logger.info("DASUs of Supervisor {}: {}",supervIdentifier.fullRunningID,supervisor.dasuIds.mkString(", "))
    
    assert(supervisor.dasus.values.size==4)
    
    val templatedDasus=supervisor.dasus.values.filter(d => d.id.startsWith("DasuTemplateID"))
    assert(templatedDasus.size==2)
    assert(templatedDasus.forall(_.dasuIdentifier.fromTemplate))
    
    val nonTempdasu = supervisor.dasus.get("Dasu1")
    assert(nonTempdasu.isDefined)
    assert(!nonTempdasu.get.dasuIdentifier.fromTemplate)
    
  }

  it must "properly associate inputs to DASUs" in new Fixture {
    // Check the number of inputs of the non templated DASU, Dasu1
    val inputsToNonTempDasu = supervisor.iasiosToDasusMap(Identifier.buildIdFromTemplate("Dasu1", None))
    assert(inputsToNonTempDasu.size == 50)
    
    val inputsToDasu1 = supervisor.iasiosToDasusMap(Identifier.buildIdFromTemplate("DasuTemplateID1", Some(3)))
    assert(inputsToDasu1.size == 3)
    assert(inputsToDasu1.forall(id => 
      id.startsWith("AsceTemp1-ID1-In") || id.startsWith("AsceTemp1-ID2-In") || id=="Temperature"))

    val inputsToDasu2 = supervisor.iasiosToDasusMap(Identifier.buildIdFromTemplate("DasuTemplateID2", Some(5)))
    assert(inputsToDasu2.size == 1)
    assert(inputsToDasu2.forall(id => id.startsWith("AsceTemp2-ID1-In")))

    // Chelk the inputs of the DASU with templated instance inputs
    val inputsToDasu3 = supervisor.iasiosToDasusMap(Identifier.buildIdFromTemplate("DasuToTestInputs", Some(4)))
    assert(inputsToDasu3.size == 4) // 2 template + 2 templated instance inputs
    assert(inputsToDasu3.forall(id =>
      id.startsWith("TemplatedIput") || id.startsWith("TemplatedI") || id.startsWith("NonTemplatedId")))
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

  it must "properly forward IASIOs to the non-templated DASU" in new Fixture {
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
        null,null)
    }
    
    assert(iasiosToSend.toSet.size == 20)

    logger.info(
      "Sending {} inputs to the supervisor: {}",
      iasiosToSend.size.toString,
      iasiosToSend.map(i => i.id).mkString(", "))

    inputsProvider.sendInputs(iasiosToSend.toSet)
    val dasus = supervisor.dasus.values
    dasus.foreach(d => assert(d.asInstanceOf[DasuMock].inputsReceivedFromSuperv.isEmpty))

    // Only Dasu1 should receive the followings
    //
    // The format of the input of this DASU is ASCEXOnDasu2-IDY-In
    val iasioForDasu1 = for {
      i <- 1 to 10
      x <- 1 to 5
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

    val iasioForDasu1Set = iasioForDasu1.toSet
    val ids = iasioForDasu1Set.map(i => i.id)

    logger.info("Sending {} iasios {}", ids.size.toString, ids.mkString(","))
    inputsProvider.sendInputs(iasioForDasu1Set)
    assert(supervisor.dasus("Dasu1").asInstanceOf[DasuMock].inputsReceivedFromSuperv.size==50)
    
    val templatedDasus = supervisor.dasus.values.filter(_.id!="Dasu1")
    assert(templatedDasus.forall(d => d.asInstanceOf[DasuMock].inputsReceivedFromSuperv.isEmpty))
  }
  
  it must "properly forward IASIOs to the templated DASU" in new Fixture {
    // It sends inputs to DasuTemplateID1 because its ASCE, ASCE-Templated-ID1,
    // accepts a mix of templated (AsceTemp1-ID1-In, AsceTemp1-ID2-In)
    // and non-templated (Temperature) inputs.
    assert(supervisor.start().isSuccess)
    
    val Sup2Id = new Identifier("AnotherSupervID", IdentifierType.SUPERVISOR, None)
    val DasuId = new Identifier("DASU-Id", IdentifierType.DASU, Sup2Id)
    val AsceId = new Identifier("ASCE-Id", IdentifierType.ASCE, DasuId)
    
    // Build the inputs for DasuTemplateID1:
    // the other DASUS shall not receive IASValues
    val t0 = System.currentTimeMillis()-100
    val tempIasio: IASValue[?] = IASValue.build(
      5.5,
      OperationalMode.OPERATIONAL,
      IasValidity.RELIABLE,
      new Identifier("Temperature", IdentifierType.IASIO, AsceId).fullRunningID,
      IASTypes.DOUBLE,
      t0,
      t0+1,
      t0+5,
      t0+10,
      t0+15,
      t0+20,
      null, null,null)

    val t1 = System.currentTimeMillis()-100
    val templ1: IASValue[?] = IASValue.build(
        5.5,
      OperationalMode.OPERATIONAL,
      IasValidity.RELIABLE,
      new Identifier("AsceTemp1-ID1-In[!#3!]", IdentifierType.IASIO, AsceId).fullRunningID,
      IASTypes.DOUBLE,
      t1,
      t1+1,
      t1+5,
      t1+10,
      t1+15,
      t1+20,
      null,	null,null)
    val t2 = System.currentTimeMillis()-100
    val templ2: IASValue[?] = IASValue.build(
        5.5,
      OperationalMode.OPERATIONAL, IasValidity.RELIABLE,
      new Identifier("AsceTemp1-ID2-In[!#3!]", IdentifierType.IASIO, AsceId).fullRunningID,
      IASTypes.DOUBLE,
      t2,
      t2+1,
      t2+5,
      t2+10,
      t2+15,
      t2+20,
      null, null,null)
			
		// Sends the IASValues to the DASUs
    val iasiosToSend: Set[IASValue[?]] = Set(templ1,tempIasio,templ2)
    logger.info("Sending inputs: {}",iasiosToSend.map(_.id).mkString)
    inputsProvider.sendInputs(iasiosToSend)
    
    logger.info("Instantiated DASUs= {}",supervisor.dasus.values.map(_.id).mkString(", "))
    
    // Check which DASU got the inputs
    assert(supervisor.dasus("Dasu1").asInstanceOf[DasuMock].inputsReceivedFromSuperv.isEmpty)
    assert(supervisor.dasus("DasuTemplateID1[!#3!]").asInstanceOf[DasuMock].inputsReceivedFromSuperv.size==3)
    assert(supervisor.dasus("DasuTemplateID2[!#5!]").asInstanceOf[DasuMock].inputsReceivedFromSuperv.isEmpty)
  }
  
  // Sends some input to Dasu1 and DasuTemplateID2 and check if the
  // supervisor published their outputs (but no output must be
  // produced by DasuTemplateID1)
  it must "publish the output" in new Fixture {
    assert(supervisor.start().isSuccess)
    
    val Sup2Id = new Identifier("AnotherSupervID", IdentifierType.SUPERVISOR, None)
    val DasuId = new Identifier("DASU-Id", IdentifierType.DASU, Sup2Id)
    val AsceId = new Identifier("ASCE-Id", IdentifierType.ASCE, DasuId)
    
    // Few IASIOs for Dasu1
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
        null,null,null)
    }
    
    // And for DasuTemplateID2
    val t1 = System.currentTimeMillis()-100
    val templ1: IASValue[?] = IASValue.build(
        7.5,
      OperationalMode.OPERATIONAL,
      IasValidity.RELIABLE,
      new Identifier("AsceTemp2-ID1-In[!#5!]", IdentifierType.IASIO, AsceId).fullRunningID,
      IASTypes.DOUBLE,
      t1,
      t1+1,
      t1+5,
      t1+10,
      t1+15,
      t1+20,
      null, null,null)

    val iasioForDasusSet: Set[IASValue[?]] = iasioForDasu1.toSet + templ1
    val ids = iasioForDasusSet.map(i => i.id)

    logger.info("Sending {} iasios {}", ids.size.toString, ids.mkString(", "))
    inputsProvider.sendInputs(iasioForDasusSet)
    
    assert(outputEventsreceived.size==2)
    assert(stringEventsreceived.size==2)
    assert(stringEventsreceived.forall(s => s.contains("Dasu1")||s.contains("DasuTemplateID2[!#5!]")))
  }
}
