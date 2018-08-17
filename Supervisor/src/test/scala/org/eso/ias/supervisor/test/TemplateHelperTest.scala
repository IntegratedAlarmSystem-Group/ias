package org.eso.ias.supervisor.test

import org.scalatest.FlatSpec
import org.eso.ias.cdb.CdbReader
import org.eso.ias.cdb.json.CdbJsonFiles
import java.nio.file.FileSystems
import org.eso.ias.cdb.json.JsonReader
import org.eso.ias.supervisor.TemplateHelper
import scala.collection.JavaConverters

// The following import is required by the usage of the fixture
import language.reflectiveCalls
import org.eso.ias.types.Identifier
import org.eso.ias.cdb.pojos.AsceDao

/**
 * Test the TemplateHelper class
 * 
 * The test uses the CDB in test/CDB and the DasusToDeploy defined in 
 * the SupervisorWithTemplates Supervisor.
 */
class TemplateHelperTest extends FlatSpec {
  
   /** Fixture to build same type of objects for the tests */
  def fixture = new {
    
    /**
     * The ID of the supervisor
     */
    val supervisorId = "SupervisorWithTemplates"
    
    // Build the CDB reader
      val cdbParentPath = FileSystems.getDefault().getPath(".");
      val cdbFiles = new CdbJsonFiles(cdbParentPath)
      val cdbReader: CdbReader = new JsonReader(cdbFiles)
      
      val superv = {
        val supervOpt=cdbReader.getSupervisor(supervisorId)
        assert(supervOpt.isPresent())
        supervOpt.get
      }
      
      val dasusToDeploy = JavaConverters.asScalaSet(superv.getDasusToDeploy).toSet
      
      val templateHelper = new TemplateHelper(dasusToDeploy)
  }
  
  behavior of "The TemplateHelper"
  
  it must "distinguish between templated and non templated DASUs" in {
    val f = fixture
    assert(f.dasusToDeploy.size==3)
    assert(f.templateHelper.normalDasusToDeploy.size==1)
    assert(f.templateHelper.normalDasusToDeploy.filter(d => d.getDasu.getId=="Dasu1").size==1)
    
    assert(f.templateHelper.templatedDasusToDeploy.size==2)
    val tempIds = f.templateHelper.templatedDasusToDeploy.map(dtd => dtd.getDasu.getId)
    assert(tempIds.contains("DasuTemplateID1"))
    assert(tempIds.contains("DasuTemplateID2"))
  }
  
  it must "return all the DASUs when normalizing" in {
    val f = fixture
    val dasus = f.templateHelper.normalize()
    assert(dasus.size==f.dasusToDeploy.size)
  }
  
  it must "not normalize a non templated DASU" in {
    val f = fixture
    val dasus = f.templateHelper.normalize()
    val dasuIds = dasus.map(_.getId)
    assert(dasuIds.contains("Dasu1"))
    assert(!dasuIds.contains("DasuTemplateID1"))
    assert(!dasuIds.contains("DasuTemplateID2"))
  }
  
  it must "normalize the templated DASUs" in {
    val f = fixture
    val dasus = f.templateHelper.normalize()
    val dasuIds = dasus.map(_.getId).filter(s => "Dasu1"!=s)
    assert(dasuIds.size==f.templateHelper.templatedDasusToDeploy.size)
    assert(dasuIds.forall( _.matches(Identifier.templatedIdRegExp.regex)))
  }
  
  it must "not normalize the ASCEs of non templated DASUs" in {
    val f = fixture
    val dasus = f.templateHelper.normalize()
    val nonTemplatedDasus = dasus.filter(!_.getId.matches(Identifier.templatedIdRegExp.regex))
    val nonTemplatedAsces: Set[AsceDao] = nonTemplatedDasus.foldLeft(Set.empty[AsceDao])( (s, d) => s++JavaConverters.collectionAsScalaIterable(d.getAsces))
    assert(nonTemplatedAsces.forall(!_.getId.matches(Identifier.templatedIdRegExp.regex)))
  }
  
  it must "normalize the ASCEs of the templated DASUs" in {
    val f = fixture
    val dasus = f.templateHelper.normalize()
    val templatedDasus = dasus.filter(_.getId.matches(Identifier.templatedIdRegExp.regex))
    val templatedAsces: Set[AsceDao] = templatedDasus.foldLeft(Set.empty[AsceDao])( (s, d) => s++JavaConverters.collectionAsScalaIterable(d.getAsces))
    assert(templatedAsces.forall(_.getId.matches(Identifier.templatedIdRegExp.regex)))
  }
  
  it must "normalize the output of the templated DASUs" in {
    val f = fixture
    val dasus = f.templateHelper.normalize()
    val templatedDasus = dasus.filter(_.getId.matches(Identifier.templatedIdRegExp.regex))
    val outputs = templatedDasus.map(_.getOutput)
    assert(outputs.forall(_.getId.matches(Identifier.templatedIdRegExp.regex)))
  }
  
  it must "not normalize the output of non templated DASUs" in {
    val f = fixture
    val dasus = f.templateHelper.normalize()
    val nonTemplatedDasus = dasus.filter(!_.getId.matches(Identifier.templatedIdRegExp.regex))
    val nonTemplateoutputs = nonTemplatedDasus.map(_.getOutput)
    assert(!nonTemplateoutputs.forall(_.getId.matches(Identifier.templatedIdRegExp.regex)))
  }
  
  it must "normalize the output of the templated ASCEs" in {
    val f = fixture
    val dasus = f.templateHelper.normalize()
    val templatedDasus = dasus.filter(_.getId.matches(Identifier.templatedIdRegExp.regex))
    val templatedAsces: Set[AsceDao] = templatedDasus.foldLeft(Set.empty[AsceDao])( (s, d) => s++JavaConverters.collectionAsScalaIterable(d.getAsces))
    assert(templatedAsces.forall(_.getOutput.getId.matches(Identifier.templatedIdRegExp.regex)))
  }
  
  it must "not normalize the output of non templated ASCEs" in {
    val f = fixture
    val dasus = f.templateHelper.normalize()
    val nonTemplatedDasus = dasus.filter(!_.getId.matches(Identifier.templatedIdRegExp.regex))
    val nonTemplatedAsces: Set[AsceDao] = nonTemplatedDasus.foldLeft(Set.empty[AsceDao])( (s, d) => s++JavaConverters.collectionAsScalaIterable(d.getAsces))
    assert(!nonTemplatedAsces.forall(_.getOutput.getId.matches(Identifier.templatedIdRegExp.regex)))
  }
  
}