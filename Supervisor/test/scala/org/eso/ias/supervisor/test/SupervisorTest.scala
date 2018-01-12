package org.eso.ias.supervisor.test

import org.scalatest.FlatSpec
import org.eso.ias.prototype.input.java.IasValueJsonSerializer
import org.eso.ias.dasu.publisher.OutputPublisher
import org.eso.ias.dasu.publisher.OutputListener
import org.eso.ias.dasu.publisher.ListenerOutputPublisherImpl
import org.ias.prototype.logging.IASLogger
import org.eso.ias.dasu.publisher.DirectInputSubscriber
import org.eso.ias.prototype.input.java.IASValue
import org.eso.ias.supervisor.Supervisor
import org.eso.ias.prototype.input.Identifier
import org.eso.ias.prototype.input.java.IdentifierType
import org.eso.ias.cdb.CdbReader
import org.eso.ias.cdb.json.CdbJsonFiles
import org.eso.ias.cdb.json.JsonReader
import java.nio.file.FileSystems
import org.eso.ias.prototype.input.java.OperationalMode
import org.eso.ias.prototype.input.java.IasValidity
import org.eso.ias.prototype.input.java.IASTypes

class SupervisorTest extends FlatSpec with OutputListener {
  
  /** The logger */
  private val logger = IASLogger.getLogger(this.getClass);
  
  val stringSerializer = Option(new IasValueJsonSerializer)  
  val outputPublisher: OutputPublisher = new ListenerOutputPublisherImpl(this,stringSerializer)
  
  val inputsProvider = new DirectInputSubscriber()
  
  // Build the CDB reader
  val cdbParentPath =  FileSystems.getDefault().getPath(".");
  val cdbFiles = new CdbJsonFiles(cdbParentPath)
  val cdbReader: CdbReader = new JsonReader(cdbFiles)
  
  val supervIdentifier = new Identifier("SupervisorID",IdentifierType.SUPERVISOR,None)
  
  /**
   * @see OutputListener
   */
  def outputStringifiedEvent(outputStr: String) {}
  
  /**
   * @see OutputListener
   */
  def outputEvent(output: IASValue[_]) {}
  
  behavior of "The Supervisor"
  
  it must "instantiate the DASUs defined in the CDB" in {
    val supervisor = new Supervisor(
        supervIdentifier,
        outputPublisher,
        inputsProvider,
        cdbReader,
        DasuMock.apply
        )
    // There are 3 DASUs
    assert(supervisor.dasuIds.size==3)
    assert(supervisor.dasuIds.forall(id => id=="Dasu1" || id=="Dasu2" || id=="Dasu3"))
  }
  
  it must "properly associate inputs to DASUs" in {
    val supervisor = new Supervisor(
        supervIdentifier,
        outputPublisher,
        inputsProvider,
        cdbReader,
        DasuMock.apply
        )
    val inputsToDasu1 = supervisor.iasiosToDasusMap("Dasu1")
    assert(inputsToDasu1.size==5*10) // 5 inputs for each ASCE running in the DASU
    assert(inputsToDasu1.forall(id => id.contains("Dasu1")))
    
    val inputsToDasu2 = supervisor.iasiosToDasusMap("Dasu2")
    assert(inputsToDasu2.size==5*15) // 5 inputs for each ASCE running in the DASU
    assert(inputsToDasu2.forall(id => id.contains("Dasu2")))
    
    val inputsToDasu3 = supervisor.iasiosToDasusMap("Dasu3")
    assert(inputsToDasu3.size==5*8) // 5 inputs for each ASCE running in the DASU
    assert(inputsToDasu3.forall(id => id.contains("Dasu3")))
  }
  
  it must "start each DASU" in {
    val supervisor = new Supervisor(
        supervIdentifier,
        outputPublisher,
        inputsProvider,
        cdbReader,
        DasuMock.apply
        )
    assert(supervisor.start().isSuccess)
    
    val dasus = supervisor.dasus.values
    dasus.foreach(d => assert(d.asInstanceOf[DasuMock].numOfStarts.get()==1) )
    
    supervisor.cleanUp()
  }
  
  it must "clean up each DASU" in {
    val supervisor = new Supervisor(
        supervIdentifier,
        outputPublisher,
        inputsProvider,
        cdbReader,
        DasuMock.apply
        )
    assert(supervisor.start().isSuccess)
    
    supervisor.cleanUp()
    
    val dasus = supervisor.dasus.values
    dasus.foreach(d => assert(d.asInstanceOf[DasuMock].numOfCleanUps.get()==1) )
  }
  
  it must "activate the auto refresh of the output of each DASU" in {
    val supervisor = new Supervisor(
        supervIdentifier,
        outputPublisher,
        inputsProvider,
        cdbReader,
        DasuMock.apply
        )
    assert(supervisor.start().isSuccess)
    
    val dasus = supervisor.dasus.values
    dasus.foreach(d => assert(d.asInstanceOf[DasuMock].numOfEnableAutorefresh.get()==1) )
    
    supervisor.cleanUp()
  }
  
  it must "properly forward IASIOs do DASUs" in {
    val supervisor = new Supervisor(
        supervIdentifier,
        outputPublisher,
        inputsProvider,
        cdbReader,
        DasuMock.apply
        )
    
    // DASU should receive the following IASIOs
    val Sup2Id = new Identifier("AnotherSupervID", IdentifierType.SUPERVISOR, None)
    val DasuId = new Identifier("DASU-Id", IdentifierType.DASU, Sup2Id)
    val AsceId = new Identifier("ASCE-Id", IdentifierType.ASCE, DasuId)
    val iasiosToSend = for (i <-1 to 20) yield {
      val iasValueId = new Identifier("IASIO-Id"+i, IdentifierType.IASIO, AsceId)
      IASValue.buildIasValue(
          10L, System.currentTimeMillis(), 
          OperationalMode.OPERATIONAL, 
          IasValidity.RELIABLE, 
          iasValueId.fullRunningID, 
          IASTypes.LONG)
    }
    inputsProvider.sendInputs(iasiosToSend.toSet)
    val dasus = supervisor.dasus.values
    dasus.foreach(d => assert(d.asInstanceOf[DasuMock].inputsReceivedFromSuperv.isEmpty) )
    
    // Only DASU2 should receive the followings
    //
    // The format of the input iof this DASU is ASCEXOnDasu2-IDY-In
    val iasioForDasu2 = for {
      i <-1 to 15
      x <- 1 to 5
      id = "ASCE"+i+"OnDasu2-ID"+ x+"-In"
      } yield {
        val iasValueId = new Identifier(id, IdentifierType.IASIO, AsceId)
        IASValue.buildIasValue(
          10L, System.currentTimeMillis(), 
          OperationalMode.OPERATIONAL, 
          IasValidity.RELIABLE, 
          iasValueId.fullRunningID, 
          IASTypes.LONG)
    }
    
    val iasioForDasu2Set = iasioForDasu2.toSet
    val ids = iasioForDasu2Set.map(i =>i.id)
     
    logger.info("Sending: {}",ids.mkString(", "))
    inputsProvider.sendInputs(iasioForDasu2Set)
    assert(supervisor.dasus("Dasu1").asInstanceOf[DasuMock].inputsReceivedFromSuperv.isEmpty) 
    assert(supervisor.dasus("Dasu2").asInstanceOf[DasuMock].inputsReceivedFromSuperv.size==15*5)
    assert(supervisor.dasus("Dasu3").asInstanceOf[DasuMock].inputsReceivedFromSuperv.isEmpty)
  }
}