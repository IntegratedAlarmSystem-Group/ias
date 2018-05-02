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
import org.eso.ias.heartbeat.serializer.HbJsonSerializer
import org.eso.ias.heartbeat.publisher.HbLogProducer

class SupervisorWithTemplatesTest extends FlatSpec {

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

      val supervIdentifier = new Identifier("SupervisorWithTemplates", IdentifierType.SUPERVISOR, None)

      /** The supervisor to test */
      val supervisor = new Supervisor(
        supervIdentifier,
        outputPublisher,
        inputsProvider,
        new HbLogProducer(new HbJsonSerializer),
        cdbReader,
        DasuMock.apply)

    }

  behavior of "The Supervisor with templates"

  it must "instantiate the templated DASUs defined in the CDB" in {
    val f = fixture
    // There are 2 templated DASUs and one non templated DASU
    assert(f.supervisor.dasuIds.size == 3)
    logger.info("DASUs of Supervisor {}: {}",f.supervIdentifier.fullRunningID,f.supervisor.dasuIds.mkString(", "))
    
    assert(f.supervisor.dasus.values.size==3)
    
    val templatedDasus=f.supervisor.dasus.values.filter(d => d.id.startsWith("DasuTemplateID"))
    assert(templatedDasus.size==2)
    assert(templatedDasus.forall(_.dasuIdentifier.fromTemplate))
    
    val nonTempdasu = f.supervisor.dasus.get("Dasu1")
    assert(nonTempdasu.isDefined)
    assert(!nonTempdasu.get.dasuIdentifier.fromTemplate)
    
  }

  it must "properly associate inputs to DASUs" in {
    val f = fixture
    
    // Check the number of inputs of the non templated DASU, Dasu1
    val inputsToNonTempDasu = f.supervisor.iasiosToDasusMap(Identifier.buildIdFromTemplate("Dasu1", None))
    assert(inputsToNonTempDasu.size == 50)
    
    val inputsToDasu1 = f.supervisor.iasiosToDasusMap(Identifier.buildIdFromTemplate("DasuTemplateID1", Some(3)))
    assert(inputsToDasu1.size == 3)
    assert(inputsToDasu1.forall(id => 
      id.startsWith("AsceTemp1-ID1-In") || id.startsWith("AsceTemp1-ID2-In") || id=="Temperature"))

    val inputsToDasu2 = f.supervisor.iasiosToDasusMap(Identifier.buildIdFromTemplate("DasuTemplateID2", Some(5)))
    assert(inputsToDasu2.size == 1)
    assert(inputsToDasu2.forall(id => id.startsWith("AsceTemp2-ID1-In")))
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

  it must "properly forward IASIOs to the non-templated DASU" in {
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
			  t0+5,
			  t0+10,
			  t0+15,
			  t0+20,
			  t0+25,
			null,
			null,null)
    }

    val iasioForDasu1Set = iasioForDasu1.toSet
    val ids = iasioForDasu1Set.map(i => i.id)

    logger.info("Sending {} iasios {}", ids.size.toString, ids.mkString(","))
    f.inputsProvider.sendInputs(iasioForDasu1Set)
    assert(f.supervisor.dasus("Dasu1").asInstanceOf[DasuMock].inputsReceivedFromSuperv.size==50)
    
    val templatedDasus = f.supervisor.dasus.values.filter(_.id!="Dasu1")
    assert(templatedDasus.forall(d => d.asInstanceOf[DasuMock].inputsReceivedFromSuperv.isEmpty))
  }
  
  it must "properly forward IASIOs to the templated DASU" in {
    // It sends inputs to DasuTemplateID1 because its ASCE, ASCE-Templated-ID1,
    // accepts a mix of templated (AsceTemp1-ID1-In, AsceTemp1-ID2-In)
    // and non-templated (Temperature) inputs.
    
    val f = fixture
    assert(f.supervisor.start().isSuccess)
    
    val Sup2Id = new Identifier("AnotherSupervID", IdentifierType.SUPERVISOR, None)
    val DasuId = new Identifier("DASU-Id", IdentifierType.DASU, Sup2Id)
    val AsceId = new Identifier("ASCE-Id", IdentifierType.ASCE, DasuId)
    
    // Build the inputs for DasuTemplateID1:
    // the other DASUS shall not receive IASValues
    val t0 = System.currentTimeMillis()-100
    val tempIasio: IASValue[_] = IASValue.build(
        5.5,
			  OperationalMode.OPERATIONAL,
			  IasValidity.RELIABLE,
			  (new Identifier("Temperature", IdentifierType.IASIO, AsceId)).fullRunningID,
			  IASTypes.DOUBLE,
			  t0,
			  t0+5,
			  t0+10,
			  t0+15,
			  t0+20,
			  t0+25,
			null,
			null,null)
		val t1 = System.currentTimeMillis()-100
    val templ1: IASValue[_] = IASValue.build(
        5.5,
			  OperationalMode.OPERATIONAL,
			  IasValidity.RELIABLE,
			  (new Identifier("AsceTemp1-ID1-In[!#3!]", IdentifierType.IASIO, AsceId)).fullRunningID,
			  IASTypes.DOUBLE,
			  t1,
			  t1+5,
			  t1+10,
			  t1+15,
			  t1+20,
			  t1+25,
			null,
			null,null)
		val t2 = System.currentTimeMillis()-100
    val templ2: IASValue[_] = IASValue.build(
        5.5,
			  OperationalMode.OPERATIONAL,
			  IasValidity.RELIABLE,
			  (new Identifier("AsceTemp1-ID2-In[!#3!]", IdentifierType.IASIO, AsceId)).fullRunningID,
			  IASTypes.DOUBLE,
			  t2,
			  t2+5,
			  t2+10,
			  t2+15,
			  t2+20,
			  t2+25,
			null,
			null,null)
			
		// Sends the IASValues to the DASUs
		val iasiosToSend: Set[IASValue[_]] = Set(templ1,tempIasio,templ2)
		logger.info("Sending inputs: {}",iasiosToSend.map(_.id).mkString)
    f.inputsProvider.sendInputs(iasiosToSend)
    
    logger.info("Instantiated DASUs= {}",f.supervisor.dasus.values.map(_.id).mkString,(", "))
    
    // Check which DASU got the inputs
    assert(f.supervisor.dasus("Dasu1").asInstanceOf[DasuMock].inputsReceivedFromSuperv.isEmpty)
    assert(f.supervisor.dasus("DasuTemplateID1[!#3!]").asInstanceOf[DasuMock].inputsReceivedFromSuperv.size==3)
    assert(f.supervisor.dasus("DasuTemplateID2[!#5!]").asInstanceOf[DasuMock].inputsReceivedFromSuperv.isEmpty)    
  }
  
  // Sends some input to Dasu1 and DasuTemplateID2 and check if the
  // supervisor published their outputs (but no output must be
  // produced by DasuTemplateID1)
  it must "publish the output" in {
    
    val f = fixture
    assert(f.supervisor.start().isSuccess)
    
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
			  t0+5,
			  t0+10,
			  t0+15,
			  t0+20,
			  t0+25,
			  null,
			  null,null)
    }
    
    // And for DasuTemplateID2
    val t1 = System.currentTimeMillis()-100
    val templ1: IASValue[_] = IASValue.build(
        7.5,
			  OperationalMode.OPERATIONAL,
			  IasValidity.RELIABLE,
			  (new Identifier("AsceTemp2-ID1-In[!#5!]", IdentifierType.IASIO, AsceId)).fullRunningID,
			  IASTypes.DOUBLE,
			  t1,
			  t1+5,
			  t1+10,
			  t1+15,
			  t1+20,
			  t1+25,
			null,
			null,null)

    val iasioForDasusSet: Set[IASValue[_]] = iasioForDasu1.toSet + templ1
    val ids = iasioForDasusSet.map(i => i.id)

    logger.info("Sending {} iasios {}", ids.size.toString, ids.mkString(", "))
    f.inputsProvider.sendInputs(iasioForDasusSet)
    
    assert(f.outputEventsreceived.size==2)
    assert(f.stringEventsreceived.size==2)
    assert(f.stringEventsreceived.forall(s => s.contains("Dasu1")||s.contains("DasuTemplateID2[!#5!]")))
  }
}
