package org.eso.ias.dasu.test

import ch.qos.logback.classic.Level

import org.eso.ias.cdb.CdbReader
import org.eso.ias.cdb.structuredtext.{StructuredTextReader, CdbFiles, CdbTxtFiles, TextFileType}
import org.eso.ias.dasu.DasuImpl
import org.eso.ias.dasu.publisher.{ListenerOutputPublisherImpl, OutputListener, OutputPublisher}
import org.eso.ias.dasu.subscriber.DirectInputSubscriber
import org.eso.ias.logging.IASLogger
import org.eso.ias.types.*
import org.eso.ias.types.IasValidity.*
import org.scalatest.flatspec.AnyFlatSpec

import java.nio.file.FileSystems
import java.util.concurrent.atomic.AtomicInteger
import scala.collection.mutable.ListBuffer

// The following import is required by the usage of the fixture
import org.eso.ias.cdb.pojos.DasuDao
import org.eso.ias.types.IasValidity

import scala.language.reflectiveCalls

/**
 * Test the DASU with 7 ASCEs (in 3 levels).
 * There is no special meaning between connections as this is a test 
 * to check if the flow of inputs and outputs is handled correctly.
 * However this also shows a way to reuse transfer functions (multiplicity
 * and threshold are part of the ComputingElement) plus a user defined one,
 * the AverageTempsTF.
 * In a real case we probably do not want to have so many ASCEs but
 * it is perfectly allowed.
 * 
 * The logs of the test show the topology of the ASCEs of this test.
 * At the bottom level, there is one ASCE, that takes all the for temperatures and returns their
 * average values. This is just to provide an example of a synthetic parameter.
 * At the middle there are 5 ASCEs that get a temperature and 
 * check it against the a threshold to generate alarms. One of such temperatures
 * is the average produced at level 1.
 * All 5 alarms produced by the ASCEs are sent in input to the last ASCE_AlarmsThreshold,
 * in the second and last level: this ASCE applies the multiplicity with a threshold of 3
 * 
 * The DASU takes in input 4 temperatures, calculates their average and produces an alarm
 * if at least three of them are out of the nominal range.
 * 
 * The configurations of DASU, ASCE, TF and IASIOs are all stored 
 * in the CDB folder.
 */
class Dasu7ASCEsTest extends AnyFlatSpec {
  
  /** The logger */
  private val logger = IASLogger.getLogger(this.getClass);
  IASLogger.setLogLevel(Some(Level.DEBUG), None, None)
  
  /** Fixture to build same type of objects for the tests */
  trait Fixture {
    // Build the CDB reader
    val cdbParentPath =  FileSystems.getDefault().getPath("src/test");
    val cdbFiles: CdbFiles = new CdbTxtFiles(cdbParentPath, TextFileType.JSON)
    val cdbReader: CdbReader = StructuredTextReader(cdbFiles)
    cdbReader.init()
  
    val dasuId = "DasuWith7ASCEs"
  
    val stringSerializer = Option(new IasValueJsonSerializer)
  
    /** Notifies about a new output produced by the DASU */
    class TestListener extends OutputListener {
      override def outputEvent(output: IASValue[?]): Unit = {
        logger.info("Event received from DASU: [{}]",output.id)
        eventsReceived.incrementAndGet();
        iasValuesReceived.append(output)
      }

      /** Notifies about a new output produced by the DASU
       *  formatted as String
       */
      override def outputStringifiedEvent(outputStr: String) = {
        logger.info("JSON output received: [{}]", outputStr)
        strsReceived.incrementAndGet();
      }
    }
  
    val outputPublisher: OutputPublisher = new ListenerOutputPublisherImpl(new TestListener(),stringSerializer)

    val inputsProvider = new DirectInputSubscriber()

    // Build the Identifier
    val supervId = new Identifier("SupervId",IdentifierType.SUPERVISOR,None)
    val dasuIdentifier = new Identifier(dasuId,IdentifierType.DASU,supervId)

    val dasuDao: DasuDao = {
      cdbReader.init()
      val dasuDaoOpt = cdbReader.getDasu(dasuId)
      assert(dasuDaoOpt.isPresent())
      dasuDaoOpt.get()
    }
  
    // The DASU to test
    val dasu = new DasuImpl(dasuIdentifier,dasuDao,outputPublisher,inputsProvider,3,4)

    // The identifier of the monitored system
    val monSysId = new Identifier("ConverterID",IdentifierType.MONITORED_SOFTWARE_SYSTEM,None)

    // The identifier of the plugin
    val pluginId = new Identifier("ConverterID",IdentifierType.PLUGIN,Some(monSysId))

    // The identifier of the converter
    val converterId = new Identifier("ConverterID",IdentifierType.CONVERTER,Some(pluginId))

    // The ID of the temperature 1 monitor point in unput to ASCE-Temp1
    val inputTemperature1ID = new Identifier("Temperature1", IdentifierType.IASIO,converterId)
    // The ID of the temperature 1 monitor point in unput to ASCE-Temp1
    val inputTemperature2ID = new Identifier("Temperature2", IdentifierType.IASIO,converterId)
    // The ID of the temperature 1 monitor point in unput to ASCE-Temp1
    val inputTemperature3ID = new Identifier("Temperature3", IdentifierType.IASIO,converterId)
    // The ID of the temperature 1 monitor point in unput to ASCE-Temp1
    val inputTemperature4ID = new Identifier("Temperature4", IdentifierType.IASIO,converterId)

    /** The number of events received
     *  This is the number of output generated by the DASU
     *  after running the TF on all its ASCEs
     */
    val eventsReceived = new AtomicInteger(0)

    /** The number of JSON strings received */
    val strsReceived = new AtomicInteger(0)

    val iasValuesReceived = new ListBuffer[IASValue[?]]
  
    def buildValue(
      id: String, 
      fullRunningID: String, 
      d: Double): IASValue[?] = buildValue(id, fullRunningID, d, RELIABLE)
  
    def buildValue(
      id: String, 
      fullRunningID: String, 
      d: Double,
      validity: IasValidity): IASValue[?] = {
    
        val t0 = System.currentTimeMillis()-100
    
        IASValue.build(
        d,
        OperationalMode.OPERATIONAL,
        validity,
        fullRunningID,
        IASTypes.DOUBLE,
        t0,
        t0+1,
        t0+5,
        t0+10,
        t0+15,
        null,
        null,
        null,
        null)
    }

    /** 
     * A function to wait until the DASU has processed all the inputs or the timeout expires.
     * 
     * This function works if no new inputs are submitted to the DASU while waiting.
     * The DASU does not offer a function for checking the state of the computation 
     * so this function actively wait on the inputs to be processed and the
     * on the presence of scheduled tasks to process the output.
     */
    def waitDasuProcessedAllInputs(timeout: Int): Boolean = {
      val startTime = System.currentTimeMillis()
      val endTime = startTime + timeout
      // wait until there are no more inputs to process
      while (dasu.hasInputsToProcess) {
        Thread.sleep(100)
        if (System.currentTimeMillis() > endTime) {
          logger.warn("Timeout expired while waiting for the DASU to process all inputs")
          return false
        }
      }
      
      // Wait until there are no more scheduled task to calculate the output
      while (dasu.hasScheduledTask) {
        Thread.sleep(100)
        if (System.currentTimeMillis() > endTime) {
          logger.warn("Timeout expired while waiting for the DASU to process all scheduled tasks")
          return false
        }
      }
      logger.info("DASU has processed all inputs and has no scheduled tasks")
      true
    }
  }

  behavior of "The DASU with 7 ASCEs"
  
  it must "return the correct list of input and ASCE IDs" in new Fixture {
    // The inputs of the DASU is not composed by the inputs of all ASCEs
    // but by the only inputs of the ASCEs not produced by other ASCEs 
    // running in the DASU or, to say in another way,
    // by the inputs of the ASCEs that are read from the BSDB
    val inputs = Set(
        "Temperature1", 
        "Temperature2", 
        "Temperature3", 
        "Temperature4")
    assert(dasu.getInputIds().size==inputs.size)
    assert(dasu.getInputIds().forall(inputs.contains(_)))
    
    val asces = Set(
      "ASCE-Temp1",
      "ASCE-Temp2",
      "ASCE-Temp3",
      "ASCE-Temp4",
      "ASCE-AverageTemps",
      "ASCE-AvgTempsAlarm",
      "ASCE-AlarmsThreshold")
    
    assert(dasu.getAsceIds().size==asces.size)
    assert(dasu.getAsceIds().forall(asces.contains(_)))
  }
  
  it must "produce outputs when receives sets of inputs (no throttling)" in new Fixture {
    logger.debug("Start testing outputs generation with no throttling")
    // Start the getting of events in the DASU
    assert(dasu.start().isSuccess)
    iasValuesReceived.clear()
    eventsReceived.set(0)
    logger.info("Submitting a set with only one temp {} in nominal state",inputTemperature1ID.id)
    val inputs: Set[IASValue[?]] = Set(buildValue(inputTemperature1ID.id, inputTemperature1ID.fullRunningID,0))
    // Submit the inputs but we do not expect any output before
    // the DASU receives all the inputs
    inputsProvider.sendInputs(inputs)
    println("Set submitted")
    // We expect no alarm because the DASU has not yet received all the inputs
    assert(iasValuesReceived.size==0)
    
    // wait to avoid the throttling to engage
    Thread.sleep(2*dasu.throttling)
    
    // Submit a set with Temperature 1 in a non nominal state
    logger.info("Submitting a set with only one temp {} in NON nominal state",inputTemperature1ID.id)
    inputsProvider.sendInputs(Set(buildValue(inputTemperature1ID.id, inputTemperature1ID.fullRunningID,100)))
    println("Another set submitted")
    // Give time to process the inputs to the thread
    assert(waitDasuProcessedAllInputs(1000))
    // Still no alarm because the DASU has not yet received all the inputs
    assert(iasValuesReceived.size==0)
    
    
    // Submit the other inputs and then we expect the DASU to
    // produce the output
    val setOfInputs: Set[IASValue[?]] = {
      val v1=buildValue(inputTemperature1ID.id, inputTemperature1ID.fullRunningID,5)
      val v2=buildValue(inputTemperature2ID.id, inputTemperature2ID.fullRunningID,6)
      val v3=buildValue(inputTemperature3ID.id, inputTemperature3ID.fullRunningID,7)
      val v4=buildValue(inputTemperature4ID.id, inputTemperature4ID.fullRunningID,8)
      Set(v1,v2,v3,v4)
    }
    inputsProvider.sendInputs(setOfInputs)
    
    // Give time to process the inputs to the thread
    assert(waitDasuProcessedAllInputs(1000))
    // Now the DASU has all the inputs and must have produced the output
    assert(iasValuesReceived.size==1)
    val outputProducedByDasu = iasValuesReceived.last
    assert(outputProducedByDasu.valueType==IASTypes.ALARM)
    assert(outputProducedByDasu.value.asInstanceOf[Alarm]== Alarm.getInitialAlarmState)
    assert(outputProducedByDasu.productionTStamp.isPresent())
    
    // wait to avoid the throttling
    Thread.sleep(2*dasu.throttling)
    
    //Submit a new set of inputs to trigger the alarm in the output of the DASU
    val setOfInputs2: Set[IASValue[?]] = {
      val v1=buildValue(inputTemperature1ID.id, inputTemperature1ID.fullRunningID,100)
      val v2=buildValue(inputTemperature2ID.id, inputTemperature2ID.fullRunningID,100)
      val v3=buildValue(inputTemperature3ID.id, inputTemperature3ID.fullRunningID,100)
      val v4=buildValue(inputTemperature4ID.id, inputTemperature4ID.fullRunningID,8)
      Set(v1,v2,v3,v4)
    }
    inputsProvider.sendInputs(setOfInputs2)

    // Wait until the DAS has processed all the inputs
    waitDasuProcessedAllInputs(1000)
    assert(iasValuesReceived.size==2)
    val outputProducedByDasu2 = iasValuesReceived.last
    assert(outputProducedByDasu2.valueType==IASTypes.ALARM)
    assert(outputProducedByDasu2.value.asInstanceOf[Alarm].isSet())
    assert(outputProducedByDasu2.productionTStamp.isPresent())
    
    assert(outputProducedByDasu2.dependentsFullRuningIds.isPresent())
    assert(outputProducedByDasu2.dependentsFullRuningIds.get.size()==dasu.getInputIds().size)
    dasu.cleanUp()
  }
  
  it must "produce outputs when receives sets of inputs with throttling)" in new Fixture {
    logger.debug("Start testing outputs generation with throttling")
    // Start the getting of events in the DASU
    assert(dasu.start().isSuccess)
    iasValuesReceived.clear()
    eventsReceived.set(0)
    logger.info("Quickly submits sets of inputs")
    val inputs: Set[IASValue[?]] = Set(buildValue(inputTemperature1ID.id, inputTemperature1ID.fullRunningID,0))
    val setOfInputs1: Set[IASValue[?]] = {
      val v1=buildValue(inputTemperature1ID.id, inputTemperature1ID.fullRunningID,5)
      val v2=buildValue(inputTemperature2ID.id, inputTemperature2ID.fullRunningID,6)
      val v3=buildValue(inputTemperature3ID.id, inputTemperature3ID.fullRunningID,7)
      val v4=buildValue(inputTemperature4ID.id, inputTemperature4ID.fullRunningID,8)
      Set(v1,v2,v3,v4)
    }
    val setOfInputs2: Set[IASValue[?]] = {
      val v1=buildValue(inputTemperature1ID.id, inputTemperature1ID.fullRunningID,100)
      val v2=buildValue(inputTemperature2ID.id, inputTemperature2ID.fullRunningID,100)
      val v3=buildValue(inputTemperature3ID.id, inputTemperature3ID.fullRunningID,100)
      val v4=buildValue(inputTemperature4ID.id, inputTemperature4ID.fullRunningID,8)
      Set(v1,v2,v3,v4)
    }

    // Submit the inputs to trigger the calc. of the output
    //

    // Sending the first set of inputs triggers the generation of the output in the DASU.
    // The generation of the output is performed by  a function that is scheduled with 
    // a delay of 0 milliseconds.
    // So, checking the generation of the output immediately after sending
    // the inputs does not make sense because it is generated by a concurrent method
    logger.debug("Sending {} inputs to the DASU",setOfInputs1.size)
    inputsProvider.sendInputs(setOfInputs1)

    // Give time to schedule the throttling but not enough to run 
    // the task that generates the output so that the next 2 sendings of inpouts
    // will not generate another output
    logger.debug("Give time to schedule the generation of the output")
    Thread.sleep(dasu.throttling/3)
    logger.debug("Sending 1 input to the DASU")
    inputsProvider.sendInputs(Set(buildValue(inputTemperature1ID.id, inputTemperature1ID.fullRunningID,100))) // Delayed
    logger.debug("Sending {} inputs to the DASU",setOfInputs2.size)
    inputsProvider.sendInputs(setOfInputs2)
    logger.debug("Inputs sent to the DASU")

    // And finally the throttling time expires and the output is generated again
    // and now with all the inputs, an output is sent to the BSDB
    
    // Give the throttling task time to submit the inputs and produce the output
    Thread.sleep(2*dasu.throttling)

    // The test sent 3 updates and expect that the DASU delays the generation of the 
    // output because of the throttling
    // In total the DASU is expected to run 1 (instead of 3) update cycles
    assert(dasu.statsCollector.iterationsRun.get.toInt==2)
    assert(iasValuesReceived.size==2) // 2 outputs have been published
    
    val outputProducedByDasu = dasu.lastCalculatedOutput.get
    assert(outputProducedByDasu.isDefined)
    assert(outputProducedByDasu.get.valueType==IASTypes.ALARM)
    assert(outputProducedByDasu.get.value.asInstanceOf[Alarm]== Alarm.getInitialAlarmState.set())
    dasu.cleanUp()
  }
  
  behavior of "the output validity"
  
  it must "be UNRELIABLE when at least one input is UNRELIABLE" in new Fixture {
    // Start the getting of events in the DASU
    assert(dasu.start().isSuccess)
    iasValuesReceived.clear()
    eventsReceived.set(0)
    
    // Avoid auto refresh to engage
    dasu.enableAutoRefreshOfOutput(false)
    
    logger.info("Submits a set of RELIABLE inputs")
    val inputs: Set[IASValue[?]] = Set(buildValue(inputTemperature1ID.id, inputTemperature1ID.fullRunningID,0))
    val setOfInputs1 = Set[IASValue[?]](
      buildValue(inputTemperature1ID.id, inputTemperature1ID.fullRunningID,5),
      buildValue(inputTemperature2ID.id, inputTemperature2ID.fullRunningID,6),
      buildValue(inputTemperature3ID.id, inputTemperature3ID.fullRunningID,7),
      buildValue(inputTemperature4ID.id, inputTemperature4ID.fullRunningID,8)
    )
    inputsProvider.sendInputs(setOfInputs1)
     
    // wait to avoid the throttling to engage
    Thread.sleep(2*dasu.throttling)
    assert(iasValuesReceived.size==1)
    assert(iasValuesReceived.head.iasValidity == RELIABLE)
    
    iasValuesReceived.clear()
    eventsReceived.set(0)
    val setOfInputs2 = Set[IASValue[?]](buildValue(inputTemperature1ID.id, inputTemperature1ID.fullRunningID,5,UNRELIABLE))
    
    inputsProvider.sendInputs(setOfInputs2)
    // wait to avoid the throttling to engage
    Thread.sleep(2*dasu.throttling)
    // The DASU does notproduce any output because the Multiplicity TF takes
    // into account the validity of only the inputs that are relevant
    // @see https://github.com/IntegratedAlarmSystem-Group/ias/issues/201
    assert(iasValuesReceived.size==0)

    // Invalidate more inputs to trigger a change of validity of the output
    val setOfInputs3 = Set[IASValue[?]](
      buildValue(inputTemperature1ID.id, inputTemperature1ID.fullRunningID,5,UNRELIABLE),
      buildValue(inputTemperature2ID.id, inputTemperature2ID.fullRunningID,5,UNRELIABLE),
      buildValue(inputTemperature3ID.id, inputTemperature3ID.fullRunningID,5,UNRELIABLE)
    )
    inputsProvider.sendInputs(setOfInputs3)
    // wait to avoid the throttling to engage
    Thread.sleep(2*dasu.throttling)
    assert(iasValuesReceived.size==1)
    assert(iasValuesReceived.head.iasValidity == UNRELIABLE)
  }
  
  it must "be UNRELIABLE when some of the inputs is not refreshed in time" in new Fixture {
    // Start the getting of events in the DASU
    assert(dasu.start().isSuccess)
    iasValuesReceived.clear()
    eventsReceived.set(0)
    
    // Avoid auto refresh to engage
    dasu.enableAutoRefreshOfOutput(true)
    
    val setOfInputs1: Set[IASValue[?]] = {
      val v1=buildValue(inputTemperature1ID.id, inputTemperature1ID.fullRunningID,5)
      val v2=buildValue(inputTemperature2ID.id, inputTemperature2ID.fullRunningID,6)
      val v3=buildValue(inputTemperature3ID.id, inputTemperature3ID.fullRunningID,7)
      val v4=buildValue(inputTemperature4ID.id, inputTemperature4ID.fullRunningID,8)
      Set(v1,v2,v3,v4)
    }
    inputsProvider.sendInputs(setOfInputs1)
    
    // wait to avoid the throttling to engage
    Thread.sleep(2*dasu.throttling)
    assert(iasValuesReceived.size==1)
    assert(iasValuesReceived.head.iasValidity == RELIABLE)
    
    val setOfInputs2: Set[IASValue[?]] = {
      val v1=buildValue(inputTemperature1ID.id, inputTemperature1ID.fullRunningID,8)
      val v2=buildValue(inputTemperature2ID.id, inputTemperature2ID.fullRunningID,23)
      val v3=buildValue(inputTemperature3ID.id, inputTemperature3ID.fullRunningID,11)
      Set(v1,v2,v3)
    }
    
    iasValuesReceived.clear()
    eventsReceived.set(0)
    
    // Sent 3 inputs of 4 and wait until the DASU sends the new output by auto-sending
    while (eventsReceived.get()!=2) {
      logger.info("Sending some inputs")
      inputsProvider.sendInputs(setOfInputs2)
      Thread.sleep(1000)
    }
    assert(iasValuesReceived.last.iasValidity == UNRELIABLE)
    
  }
}
