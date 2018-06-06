package org.eso.ias.dasu.test

import org.scalatest.FlatSpec
import java.nio.file.FileSystems
import org.eso.ias.cdb.CdbReader
import org.eso.ias.cdb.json.CdbJsonFiles
import org.eso.ias.cdb.json.JsonReader
import org.eso.ias.dasu.Dasu
import org.eso.ias.dasu.publisher.OutputListener
import org.eso.ias.dasu.publisher.ListenerOutputPublisherImpl
import org.eso.ias.dasu.publisher.OutputPublisher
import org.eso.ias.types.IasValueJsonSerializer
import org.eso.ias.logging.IASLogger
import org.eso.ias.types.IASValue
import org.eso.ias.types.Identifier
import org.eso.ias.types.IdentifierType
import org.eso.ias.types.OperationalMode
import org.eso.ias.types.InOut
import org.eso.ias.dasu.subscriber.InputsListener
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.locks.ReentrantLock
import java.util.concurrent.TimeUnit
import scala.collection.mutable.ListBuffer
import org.eso.ias.types.IASTypes
import org.eso.ias.types.Alarm
import org.eso.ias.types.IasValidity._
import org.eso.ias.dasu.DasuImpl
import org.eso.ias.dasu.publisher.DirectInputSubscriber
import org.eso.ias.dasu.publisher.OutputListener

// The following import is required by the usage of the fixture
import language.reflectiveCalls
import org.eso.ias.types.IasValidity
import java.util.HashSet
import org.eso.ias.cdb.pojos.DasuDao

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
class Dasu7ASCEsTest extends FlatSpec {
  
  /** The logger */
  private val logger = IASLogger.getLogger(this.getClass);
  
  /** Fixture to build same type of objects for the tests */
  def fixture = new {
    // Build the CDB reader
    val cdbParentPath =  FileSystems.getDefault().getPath(".");
    val cdbFiles = new CdbJsonFiles(cdbParentPath)
    val cdbReader: CdbReader = new JsonReader(cdbFiles)
  
    val dasuId = "DasuWith7ASCEs"
  
    val stringSerializer = Option(new IasValueJsonSerializer)
  
  /** Notifies about a new output produced by the DASU */
  class TestListener extends OutputListener { 
    override def outputEvent(output: IASValue[_]) {
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
    val dasuDaoOpt = cdbReader.getDasu(dasuId)
    assert(dasuDaoOpt.isPresent())
    dasuDaoOpt.get()
  }
  
  // The DASU to test
  val dasu = new DasuImpl(dasuIdentifier,dasuDao,outputPublisher,inputsProvider,3,1)
  
  // The identifier of the monitored system that produces the temperature in input to the DASU
  val monSysId = new Identifier("MonitoredSystemID",IdentifierType.MONITORED_SOFTWARE_SYSTEM)
  // The identifier of the plugin
  val pluginId = new Identifier("PluginID",IdentifierType.PLUGIN,monSysId)
  // The identifier of the converter
  val converterId = new Identifier("ConverterID",IdentifierType.CONVERTER,pluginId)
  
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
  
  val iasValuesReceived = new ListBuffer[IASValue[_]]
  
  def buildValue(
      id: String, 
      fullRunningID: String, 
      d: Double): IASValue[_] = buildValue(id, fullRunningID, d, RELIABLE)
  
  def buildValue(
      id: String, 
      fullRunningID: String, 
      d: Double,
      validity: IasValidity): IASValue[_] = {
    
    val t0 = System.currentTimeMillis()-100
    
    IASValue.build(
      d,
			OperationalMode.OPERATIONAL,
			validity,
			fullRunningID,
			IASTypes.DOUBLE,
			t0,
			t0+5,
			t0+10,
			t0+15,
			t0+20,
			null,
			null,
			null,
			null)
    }
    
  }
  
  
  
  behavior of "The DASU with 7 ASCEs"
  
  it must "return the correct list of input and ASCE IDs" in {
    
    val f = fixture
    
    // The inputs of the DASU is not composed by the inputs of all ASCEs
    // but by the only inputs of the ASCEs not produced by other ASCEs 
    // running in the DASU or, to say in another way,
    // by the inputs of the ASCEs that are read from the BSDB
    val inputs = Set(
        "Temperature1", 
        "Temperature2", 
        "Temperature3", 
        "Temperature4")
    assert(f.dasu.getInputIds().size==inputs.size)
    assert(f.dasu.getInputIds().forall(inputs.contains(_)))
    
    val asces = Set(
        "ASCE-Temp1",
  		"ASCE-Temp2",
  		"ASCE-Temp3",
  		"ASCE-Temp4",
  		"ASCE-AverageTemps",
  		"ASCE-AvgTempsAlarm",
  		"ASCE-AlarmsThreshold")
    
    assert(f.dasu.getAsceIds().size==asces.size)
    assert(f.dasu.getAsceIds().forall(asces.contains(_)))
  }
  
  it must "produce outputs when receives sets of inputs (no throttling)" in {
    
    val f = fixture
    // Start the getting of events in the DASU
    assert(f.dasu.start().isSuccess)
    f.iasValuesReceived.clear()
    f.eventsReceived.set(0)
    logger.info("Submitting a set with only one temp {} in nominal state",f.inputTemperature1ID.id)
    val inputs: Set[IASValue[_]] = Set(f.buildValue(f.inputTemperature1ID.id, f.inputTemperature1ID.fullRunningID,0))
    // Submit the inputs but we do not expect any output before
    // the DASU receives all the inputs
    f.inputsProvider.sendInputs(inputs)
    println("Set submitted")
    // We expect no alarm because teh DASU has not yet received all the inputs
    assert(f.iasValuesReceived.size==0)
    
    // wait to avoid the throttling to engage
    Thread.sleep(2*f.dasu.throttling)
    
    // Submit a set with Temperature 1 in a non nominal state
    logger.info("Submitting a set with only one temp {} in NON nominal state",f.inputTemperature1ID.id)
    f.inputsProvider.sendInputs(Set(f.buildValue(f.inputTemperature1ID.id, f.inputTemperature1ID.fullRunningID,100)))
    println("Another empty set submitted")
    // Stil no alarm because teh DASU have not yet received all the inputs
    assert(f.iasValuesReceived.size==0)
    
    // wait to avoid the throttling
    Thread.sleep(2*f.dasu.throttling)
    
    // Submit the other inputs and then we expect the DASU to
    // produce the output
    val setOfInputs: Set[IASValue[_]] = {
      val v1=f.buildValue(f.inputTemperature1ID.id, f.inputTemperature1ID.fullRunningID,5)
      val v2=f.buildValue(f.inputTemperature2ID.id, f.inputTemperature2ID.fullRunningID,6)
      val v3=f.buildValue(f.inputTemperature3ID.id, f.inputTemperature3ID.fullRunningID,7)
      val v4=f.buildValue(f.inputTemperature4ID.id, f.inputTemperature4ID.fullRunningID,8)
      Set(v1,v2,v3,v4)
    }
    f.inputsProvider.sendInputs(setOfInputs)
    // No the DASU has all the inputs and must produce the output
    assert(f.iasValuesReceived.size==1)
    val outputProducedByDasu = f.iasValuesReceived.last
    assert(outputProducedByDasu.valueType==IASTypes.ALARM)
    assert(outputProducedByDasu.value.asInstanceOf[Alarm]== Alarm.CLEARED)
    assert(outputProducedByDasu.dasuProductionTStamp.isPresent())
    
    // wait to avoid the throttling
    Thread.sleep(2*f.dasu.throttling)
    
    //Submit a new set of inputs to trigger the alarm in the output of the DASU
    val setOfInputs2: Set[IASValue[_]] = {
      val v1=f.buildValue(f.inputTemperature1ID.id, f.inputTemperature1ID.fullRunningID,100)
      val v2=f.buildValue(f.inputTemperature2ID.id, f.inputTemperature2ID.fullRunningID,100)
      val v3=f.buildValue(f.inputTemperature3ID.id, f.inputTemperature3ID.fullRunningID,100)
      val v4=f.buildValue(f.inputTemperature4ID.id, f.inputTemperature4ID.fullRunningID,8)
      Set(v1,v2,v3,v4)
    }
    f.inputsProvider.sendInputs(setOfInputs2)
    assert(f.iasValuesReceived.size==2)
    val outputProducedByDasu2 = f.iasValuesReceived.last
    assert(outputProducedByDasu2.valueType==IASTypes.ALARM)
    assert(outputProducedByDasu2.value.asInstanceOf[Alarm]== Alarm.getSetDefault)
    assert(outputProducedByDasu2.dasuProductionTStamp.isPresent())
    
    assert(outputProducedByDasu2.dependentsFullRuningIds.isPresent())
    assert(outputProducedByDasu2.dependentsFullRuningIds.get.size()==f.dasu.getInputIds().size)
    f.dasu.cleanUp()
  }
  
  it must "produce outputs when receives sets of inputs with throttling)" in {
    val f = fixture
    
    // Start the getting of events in the DASU
    assert(f.dasu.start().isSuccess)
    f.iasValuesReceived.clear()
    f.eventsReceived.set(0)
    logger.info("Quickly submits sets of inputs")
    val inputs: Set[IASValue[_]] = Set(f.buildValue(f.inputTemperature1ID.id, f.inputTemperature1ID.fullRunningID,0))
    val setOfInputs1: Set[IASValue[_]] = {
      val v1=f.buildValue(f.inputTemperature1ID.id, f.inputTemperature1ID.fullRunningID,5)
      val v2=f.buildValue(f.inputTemperature2ID.id, f.inputTemperature2ID.fullRunningID,6)
      val v3=f.buildValue(f.inputTemperature3ID.id, f.inputTemperature3ID.fullRunningID,7)
      val v4=f.buildValue(f.inputTemperature4ID.id, f.inputTemperature4ID.fullRunningID,8)
      Set(v1,v2,v3,v4)
    }
    val setOfInputs2: Set[IASValue[_]] = {
      val v1=f.buildValue(f.inputTemperature1ID.id, f.inputTemperature1ID.fullRunningID,100)
      val v2=f.buildValue(f.inputTemperature2ID.id, f.inputTemperature2ID.fullRunningID,100)
      val v3=f.buildValue(f.inputTemperature3ID.id, f.inputTemperature3ID.fullRunningID,100)
      val v4=f.buildValue(f.inputTemperature4ID.id, f.inputTemperature4ID.fullRunningID,8)
      Set(v1,v2,v3,v4)
    }
    f.inputsProvider.sendInputs(setOfInputs1) // // Calculate output immediately
    f.inputsProvider.sendInputs(Set(f.buildValue(f.inputTemperature1ID.id, f.inputTemperature1ID.fullRunningID,100))) // Delayed
    f.inputsProvider.sendInputs(setOfInputs2) // Delayed
    // And finally the throttling time expires and the output is generated again
    // and now with all the inputs, an output is sent to the BSDB
    
    // Give the throttling task time to submit the inputs and produce the output
    Thread.sleep(2*f.dasu.throttling)

    // We sent 4 updates and expect that the DASU
    // * updated immediately the output when the first inputs have been submitted
    // * with the second submit of inputs, the DASU activate the throttling and delayed the execution
    // * finally the throttling time expires and the output is calculated
    // 
    // In total the DASU is expected to run 2 (instead of 4) update cycles
    assert(f.dasu.statsCollector.iterationsRun.get.toInt==2)
    assert(f.iasValuesReceived.size==2) // Only one output has been published
    
    val outputProducedByDasu = f.dasu.lastCalculatedOutput.get
    assert(outputProducedByDasu.isDefined)
    assert(outputProducedByDasu.get.valueType==IASTypes.ALARM)
    assert(outputProducedByDasu.get.value.asInstanceOf[Alarm]== Alarm.getSetDefault)
    f.dasu.cleanUp()
  }
  
  behavior of "the output validity"
  
  it must "be UNRELIABLE when at least one input is UNRELIABLE" in {
    val f = fixture
    
    // Start the getting of events in the DASU
    assert(f.dasu.start().isSuccess)
    f.iasValuesReceived.clear()
    f.eventsReceived.set(0)
    
    // Avoid auto refresh to engage
    f.dasu.enableAutoRefreshOfOutput(false)
    
    logger.info("Submits a set of RELIABLE inputs")
    val inputs: Set[IASValue[_]] = Set(f.buildValue(f.inputTemperature1ID.id, f.inputTemperature1ID.fullRunningID,0))
    val setOfInputs1: Set[IASValue[_]] = {
      val v1=f.buildValue(f.inputTemperature1ID.id, f.inputTemperature1ID.fullRunningID,5)
      val v2=f.buildValue(f.inputTemperature2ID.id, f.inputTemperature2ID.fullRunningID,6)
      val v3=f.buildValue(f.inputTemperature3ID.id, f.inputTemperature3ID.fullRunningID,7)
      val v4=f.buildValue(f.inputTemperature4ID.id, f.inputTemperature4ID.fullRunningID,8)
      Set(v1,v2,v3,v4)
    }
    f.inputsProvider.sendInputs(setOfInputs1)
     
    // wait to avoid the throttling to engage
    Thread.sleep(2*f.dasu.throttling)
    assert(f.iasValuesReceived.size==1)
    assert(f.iasValuesReceived.head.iasValidity == RELIABLE)
    
    f.iasValuesReceived.clear()
    f.eventsReceived.set(0)
    val setOfInputs2 = Set[IASValue[_]](f.buildValue(f.inputTemperature1ID.id, f.inputTemperature1ID.fullRunningID,5,UNRELIABLE))
    
     f.inputsProvider.sendInputs(setOfInputs2)
    // wait to avoid the throttling to engage
    Thread.sleep(2*f.dasu.throttling)
    assert(f.iasValuesReceived.size==1)
    assert(f.iasValuesReceived.head.iasValidity == UNRELIABLE)
  }
  
  it must "be UNRELIABLE when some of the inputs is not refreshed in time" in {
    val f = fixture
    
    // Start the getting of events in the DASU
    assert(f.dasu.start().isSuccess)
    f.iasValuesReceived.clear()
    f.eventsReceived.set(0)
    
    // Avoid auto refresh to engage
    f.dasu.enableAutoRefreshOfOutput(true)
    
    val setOfInputs1: Set[IASValue[_]] = {
      val v1=f.buildValue(f.inputTemperature1ID.id, f.inputTemperature1ID.fullRunningID,5)
      val v2=f.buildValue(f.inputTemperature2ID.id, f.inputTemperature2ID.fullRunningID,6)
      val v3=f.buildValue(f.inputTemperature3ID.id, f.inputTemperature3ID.fullRunningID,7)
      val v4=f.buildValue(f.inputTemperature4ID.id, f.inputTemperature4ID.fullRunningID,8)
      Set(v1,v2,v3,v4)
    }
    f.inputsProvider.sendInputs(setOfInputs1)
    
    // wait to avoid the throttling to engage
    Thread.sleep(2*f.dasu.throttling)
    assert(f.iasValuesReceived.size==1)
    assert(f.iasValuesReceived.head.iasValidity == RELIABLE)
    
    val setOfInputs2: Set[IASValue[_]] = {
      val v1=f.buildValue(f.inputTemperature1ID.id, f.inputTemperature1ID.fullRunningID,8)
      val v2=f.buildValue(f.inputTemperature2ID.id, f.inputTemperature2ID.fullRunningID,23)
      val v3=f.buildValue(f.inputTemperature3ID.id, f.inputTemperature3ID.fullRunningID,11)
      Set(v1,v2,v3)
    }
    
    f.iasValuesReceived.clear()
    f.eventsReceived.set(0)
    
    // Sent 3 inputs of 4 and wait until the DASU sends the new output by auto-sending
    while (f.eventsReceived.get()!=2) {
      logger.info("Sending some inputs")
      f.inputsProvider.sendInputs(setOfInputs2)
      Thread.sleep(1000)
    }
    assert(f.iasValuesReceived.last.iasValidity == UNRELIABLE)
    
  }
  
  
  
}
