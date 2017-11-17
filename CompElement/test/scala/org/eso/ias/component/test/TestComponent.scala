package org.eso.ias.component.test

import org.scalatest.FlatSpec
import org.eso.ias.prototype.input.Identifier
import org.eso.ias.plugin.OperationalMode
import org.eso.ias.prototype.input.Validity
import scala.collection.mutable.HashMap
import org.eso.ias.prototype.input.java.IASTypes
import org.eso.ias.prototype.input.InOut
import scala.collection.mutable.{Map => MutableMap }
import org.eso.ias.prototype.transfer.TransferFunctionSetting
import org.eso.ias.prototype.transfer.TransferFunctionLanguage
import java.util.Properties
import org.eso.ias.prototype.compele.CompEleThreadFactory
import org.eso.ias.prototype.compele.ComputingElement
import org.eso.ias.prototype.transfer.ScalaTransfer
import org.eso.ias.prototype.transfer.JavaTransfer
import org.eso.ias.prototype.input.java.IdentifierType
import org.eso.ias.plugin.AlarmSample
import org.eso.ias.prototype.input.java.IASValue

/**
 * Test the basic functionalities of the IAS Component,
 * while the functioning of the transfer function
 * is checked elsewhere.
 */
class TestComponent extends FlatSpec {
  
  // The ID of the DASU where the components runs
  val dasId = new Identifier("DAS-ID",IdentifierType.DASU,None)
  
  // The ID of the component to test
  val compId = new Identifier("ComponentId",IdentifierType.ASCE,Option[Identifier](dasId))
  
  // The ID of the output generated by the component
  val outId = new Identifier("OutputId",IdentifierType.IASIO,Option(compId))
  
  
  
  val mpRefreshRate = InOut.MinRefreshRate+50
  
  // The IDs of the monitor points in input 
  // to pass when building a Component
  val requiredInputIDs = List("ID1", "ID2")
  
  // The ID of the first MP
  val mpI1Identifier = new Identifier(requiredInputIDs(0),IdentifierType.IASIO,Option(compId))
  val mp1 = InOut[AlarmSample](
      None,
      mpI1Identifier,
      mpRefreshRate,
      OperationalMode.UNKNOWN,
      Validity.Unreliable,
      IASTypes.ALARM)
  
  // The ID of the second MP
  val mpI2Identifier = new Identifier(requiredInputIDs(1),IdentifierType.IASIO,Option(compId))
  val mp2 = InOut[AlarmSample](
      None,
      mpI2Identifier,
      mpRefreshRate,
      OperationalMode.UNKNOWN,
      Validity.Unreliable,
      IASTypes.ALARM)
  val actualInputs: Set[InOut[_]] = Set(mp1,mp2)
  
  behavior of "A Component"
  
  it must "be correctly initialized" in {
    val output = InOut[AlarmSample](
      None,
      outId,
      mpRefreshRate,
      OperationalMode.UNKNOWN,
      Validity.Unreliable,
      IASTypes.ALARM)
    
    val threadaFactory = new CompEleThreadFactory("Test-runninId")
    val tfSetting =new TransferFunctionSetting(
        "org.eso.ias.prototype.transfer.TransferExecutorImpl",
        TransferFunctionLanguage.java,
        threadaFactory)
    val comp: ComputingElement[AlarmSample] = new ComputingElement[AlarmSample](
       compId,
       output,
       actualInputs,
       tfSetting,
       new Properties()) with JavaTransfer[AlarmSample]
    
    assert(comp.id==compId)
    assert(comp.output.id==outId)
  }
  
}
