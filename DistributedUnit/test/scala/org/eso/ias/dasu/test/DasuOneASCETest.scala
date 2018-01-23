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
import org.eso.ias.prototype.input.java.IasValueJsonSerializer
import org.ias.prototype.logging.IASLogger
import org.eso.ias.prototype.input.java.IASValue
import org.eso.ias.prototype.input.java.IasDouble
import org.eso.ias.prototype.input.Identifier
import org.eso.ias.prototype.input.java.IdentifierType
import org.eso.ias.prototype.input.java.OperationalMode
import org.eso.ias.prototype.input.InOut
import org.eso.ias.prototype.input.JavaConverter
import org.eso.ias.dasu.subscriber.InputsListener
import org.eso.ias.dasu.subscriber.InputSubscriber
import scala.util.Success
import scala.util.Try
import scala.collection.mutable.{HashSet => MutableSet}
import org.eso.ias.prototype.input.java.IasValidity._
import org.eso.ias.dasu.DasuImpl
import org.eso.ias.dasu.publisher.DirectInputSubscriber
import org.scalatest.BeforeAndAfter

/**
 * Test the DASU with one ASCE and the MinMaxThreshold TF.
 * 
 * Being a simple case, this test will do some basic tests.
 * 
 * The configurations of DASU, ASCE, TF and IASIOs are all stored 
 * in the CDB folder.
 */
class DasuOneASCETest extends FlatSpec  with BeforeAndAfter {
  
  val f = new DausOneAsceCommon
  
  before {
    f.dasu = Some(new DasuImpl(f.dasuIdentifier,f.outputPublisher,f.inputsProvider,f.cdbReader,1000))
    f.dasu.get.start()
  }
  
  after {
    f.dasu.get.cleanUp()
    f.dasu = None
  }
  
  behavior of "The DASU"
  
  it must "return the correct list of input and ASCE IDs" in {
    assert(f.dasu.get.getInputIds().size==1)
    assert(f.dasu.get.getInputIds().forall(s => s=="Temperature"))
    
    assert(f.dasu.get.getAsceIds().size==1)
    assert(f.dasu.get.getAsceIds().forall(s => s=="ASCE-ID1"))
  }
  
  it must "produce the output when a new set inputs is notified" in {
    // Start the getting of events in the DASU
    val inputs: Set[IASValue[_]] = Set(f.buildValue(0))
    // Sumbit the inputs
    f.inputsProvider.sendInputs(inputs)
  }
  
}