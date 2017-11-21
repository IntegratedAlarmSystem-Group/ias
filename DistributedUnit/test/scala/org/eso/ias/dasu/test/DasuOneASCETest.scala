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

/**
 * Test the DASU with one ASCE and the MinMaxThreshold TF.
 * 
 * Being a simple case, this test will do some basic tests.
 * 
 * The configurations of DASU, ASCE, TF and IASIOs are all stored 
 * in the CDB folder.
 */
class DasuOneASCETest extends FlatSpec with OutputListener {
  
  /** The logger */
  private val logger = IASLogger.getLogger(this.getClass);
  
  // Build the CDB reader
  val cdbParentPath =  FileSystems.getDefault().getPath(".");
  val cdbFiles = new CdbJsonFiles(cdbParentPath)
  val cdbReader: CdbReader = new JsonReader(cdbFiles)
  
  val dasuId = "DasuWithOneASCE"
  
  val stringSerializer = Option(new IasValueJsonSerializer)
  val outputPublisher: OutputPublisher = new ListenerOutputPublisherImpl(this,stringSerializer)
  
  val dasu = new Dasu(dasuId,outputPublisher,cdbReader)
  
  /** Notifies about a new output produced by the DASU */
  override def outputEvent(output: IASValue[_]) {
    logger.info("Output received [{}]", output.id)
  }
  
  /** Notifies about a new output produced by the DASU 
   *  formatted as String
   */
  override def outputStringifiedEvent(outputStr: String) = {
    logger.info("JSON output received [{}]", outputStr)
  }
  
}