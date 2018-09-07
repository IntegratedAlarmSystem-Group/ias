package org.eso.ias.transfer.test

import org.eso.ias.logging.IASLogger
import org.eso.ias.types.Identifier
import org.eso.ias.asce.transfer.IasIO
import org.eso.ias.types.Alarm
import org.scalatest.FlatSpec
import org.eso.ias.types.IdentifierType
import org.eso.ias.types.InOut
import org.eso.ias.types.IASTypes
import java.util.Properties
import org.eso.ias.tranfer.BackupSelector
import org.eso.ias.tranfer.BackupSelectorException
import org.eso.ias.types.OperationalMode
import org.eso.ias.types.IasValidity
import org.eso.ias.types.Validity

/**
 * Test the BackupSelector with 4 DOUBLE inputs
 */
class BackupSelectorTest extends FlatSpec {
  
  /** The logger */
  val logger = IASLogger.getLogger(this.getClass)
  
  /**  The ID of the SUPERVISOR where the components runs */
  val supervId = new Identifier("SupervId",IdentifierType.SUPERVISOR,None)
  
  /**  The ID of the DASU where the components runs */
  val dasuId = new Identifier("DasuId",IdentifierType.DASU,supervId)

  /** The ID of the component running into the DASU */
  val compID = new Identifier("ASCE-ID-ForTest",IdentifierType.ASCE,Option(dasuId))
  
  /** The ID of the output generated by the component */
  val outId = new Identifier("OutputId",IdentifierType.IASIO, Some(compID))
  
  /** the IASIO in output */
  val out: IasIO[Double] = new IasIO(InOut.asOutput(outId,IASTypes.DOUBLE))
  
  /** The ID of the main AISIO */
  val mainId = new Identifier("MAIN",IdentifierType.IASIO, Some(compID))
  
  /** The ID of the MAIN input */
  val main: IasIO[Double] = buildInput(mainId,1.1d)
  
  /** The ID of the main AISIO */
  val aId = new Identifier("A",IdentifierType.IASIO, Some(compID))
  
  /** The ID of the A input */
  val a: IasIO[Double] = buildInput(aId,2.2d)
  
  /** The ID of the main AISIO */
  val bId = new Identifier("B",IdentifierType.IASIO, Some(compID))
  
  /** The ID of the B input */
  val b: IasIO[Double] = buildInput(bId,3.3d)
  
  /** The ID of the main AISIO */
  val cId = new Identifier("C",IdentifierType.IASIO, Some(compID))
  
  /** The ID of the C input */
  val c: IasIO[Double] = buildInput(cId,4.4d)
  
  /** The ID of the main AISIO */
  val wrongId = new Identifier("Wrong",IdentifierType.IASIO, Some(compID))
  
  /** The ID of the C input */
  val wrong: IasIO[Double] = buildInput(wrongId,5.5d)
  
  /** The time frame for the validity */
  val validityTimeFrame = 2000
  
  /** 
   *  Build the IASIO in input with the passed id, type and timestamp
   *  
   *  @param id The identifier of the input
   *  @param value initial value
   *  @param tStamp the daus production timestamp
   *  @param mode the operational mode
   */
  def buildInput(
      id: Identifier,
      value: Double,
      tStamp: Long = System.currentTimeMillis(), 
      mode: OperationalMode = OperationalMode.OPERATIONAL,
      validity: IasValidity = IasValidity.RELIABLE): IasIO[Double] = {
    
    val inout: InOut[Double] = InOut.asInput(id,IASTypes.DOUBLE)
      .updateDasuProdTStamp(tStamp)
      .updateValueValidity(Some(value),Some(Validity(validity))) 
    (new IasIO(inout)).updateMode(mode)
  }
  
  behavior of "The BackupSelector transfer function"
  
  it must "get the list from the property" in {
    val props = new Properties
    props.put(BackupSelector.PrioritizedIdsPropName, "MAIN, A,B, C")
    val tf = new BackupSelector(compID.id,compID.fullRunningID,validityTimeFrame,props)
    assert(tf.prioritizedIDs.size==4)
    assert(tf.prioritizedIDs.contains("MAIN"))
    assert(tf.prioritizedIDs.contains("A"))
    assert(tf.prioritizedIDs.contains("B"))
    assert(tf.prioritizedIDs.contains("C"))
  }
  
  it must "throw an exception for IDs mismatch" in {
    val props = new Properties
    props.put(BackupSelector.PrioritizedIdsPropName, "MAIN, A,B, C")
    val tf = new BackupSelector[Double](compID.id,compID.fullRunningID,validityTimeFrame,props)
    
    val inputs = Map(main.id-> main, a.id -> a, b.id -> b, wrong.id -> wrong)
    assertThrows[AssertionError] {
      tf.eval(inputs, out)
    }
    
    val inputs2 = Map(main.id-> main, a.id -> a, b.id -> b, c.id -> c, wrong.id -> wrong)
    assertThrows[AssertionError] {
      tf.eval(inputs2, out)
    }
    
    val inputsOk = Map(main.id-> main, a.id -> a, b.id -> b, c.id -> c)
    tf.eval(inputsOk, out)
  }
  
  it must "produce the first output when operational and valid" in {
    val props = new Properties
    props.put(BackupSelector.PrioritizedIdsPropName, "MAIN, A,B, C")
    val tf = new BackupSelector[Double](compID.id,compID.fullRunningID,validityTimeFrame,props)
    val inputs = Map(main.id-> main, a.id -> a, b.id -> b, c.id -> c)
    val res = tf.eval(inputs, out)
    
    assert(res.value.isDefined)
    assert(res.value.get==1.1d)
    assert(res.id==outId.id)
    assert(res.mode==OperationalMode.OPERATIONAL)
    assert(res.validityConstraints.isDefined)
    assert(res.validityConstraints.get.size==1)
    assert(res.validityConstraints.get.contains(main.id))
  }
  
  it must "produce the second output when the first one is not operational" in {
    val props = new Properties
    props.put(BackupSelector.PrioritizedIdsPropName, "MAIN, A,B, C")
    val tf = new BackupSelector[Double](compID.id,compID.fullRunningID,validityTimeFrame,props)
    val inputs = Map(main.id-> main.updateMode(OperationalMode.DEGRADED), a.id -> a, b.id -> b, c.id -> c)
    val res = tf.eval(inputs, out)
    
    assert(res.value.isDefined)
    assert(res.value.get==2.2d)
    assert(res.id==outId.id)
    assert(res.mode==OperationalMode.OPERATIONAL)
    assert(res.validityConstraints.isDefined)
    assert(res.validityConstraints.get.size==1)
    assert(res.validityConstraints.get.contains(a.id))
    
    val inputs2 = Map(main.id-> main.updateMode(OperationalMode.DEGRADED), a.id -> a.updateMode(OperationalMode.UNKNOWN), b.id -> b, c.id -> c)
    val res2 = tf.eval(inputs2, out)
    assert(res2.value.isDefined)
    assert(res2.value.get==3.3d)
    assert(res2.id==outId.id)
    assert(res2.mode==OperationalMode.OPERATIONAL)
    assert(res2.validityConstraints.isDefined)
    assert(res2.validityConstraints.get.size==1)
    assert(res2.validityConstraints.get.contains(b.id))
  }
  
  it must "produce the second output when the first one is not valid" in {
    val props = new Properties
    props.put(BackupSelector.PrioritizedIdsPropName, "MAIN, A,B, C")
    val tf = new BackupSelector[Double](compID.id,compID.fullRunningID,validityTimeFrame,props)
    
    val mainInvalid = buildInput(mainId,1.1d,validity=IasValidity.UNRELIABLE)
    
    val inputs = Map(main.id-> mainInvalid, a.id -> a, b.id -> b, c.id -> c)
    val res = tf.eval(inputs, out)
    
    assert(res.value.isDefined)
    assert(res.value.get==2.2d)
    assert(res.id==outId.id)
    assert(res.mode==OperationalMode.OPERATIONAL)
    assert(res.validityConstraints.isDefined)
    assert(res.validityConstraints.get.size==1)
    assert(res.validityConstraints.get.contains(a.id))
    
    val inputs2 = Map(main.id-> main.updateMode(OperationalMode.DEGRADED), a.id -> a.updateMode(OperationalMode.UNKNOWN), b.id -> b, c.id -> c)
    val res2 = tf.eval(inputs2, out)
    assert(res2.value.isDefined)
    assert(res2.value.get==3.3d)
    assert(res2.id==outId.id)
    assert(res2.mode==OperationalMode.OPERATIONAL)
    assert(res2.validityConstraints.isDefined)
    assert(res2.validityConstraints.get.size==1)
    assert(res2.validityConstraints.get.contains(b.id))
  }
  
  it must "produce the first output when all are invaild/not operational" in {
    val props = new Properties
    props.put(BackupSelector.PrioritizedIdsPropName, "MAIN, A,B, C")
    val tf = new BackupSelector[Double](compID.id,compID.fullRunningID,validityTimeFrame,props)
    
    val mainInvalid = buildInput(mainId,1.1d,validity=IasValidity.UNRELIABLE)
    val aInvalid = buildInput(aId,1.1d,validity=IasValidity.UNRELIABLE)
    
    val inputs = Map(
        main.id-> mainInvalid, 
        a.id -> aInvalid, 
        b.id -> b.updateMode(OperationalMode.DEGRADED), 
        c.id -> c.updateMode(OperationalMode.DEGRADED))
    val res = tf.eval(inputs, out)
    
    assert(res.value.isDefined)
    assert(res.value.get==1.1d)
    assert(res.mode==OperationalMode.OPERATIONAL)
    assert(res.validityConstraints.isEmpty)
  }
}