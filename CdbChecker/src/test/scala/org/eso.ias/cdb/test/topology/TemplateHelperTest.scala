package org.eso.ias.cdb.test.topology

import org.eso.ias.cdb.CdbReader
import org.eso.ias.cdb.pojos.{DasuToDeployDao, IasioDao}
import org.eso.ias.cdb.structuredtext.json.{CdbTxtFiles, JsonReader}
import org.eso.ias.cdb.topology.TemplateHelper
import org.scalatest.flatspec.AnyFlatSpec

import java.nio.file.FileSystems
import scala.jdk.javaapi.CollectionConverters

// The following import is required by the usage of the fixture
import org.eso.ias.cdb.pojos.AsceDao
import org.eso.ias.types.Identifier

import scala.language.reflectiveCalls

/**
 * Test the TemplateHelper class
 * 
 * The test uses the CDB in test/CDB and the DasusToDeploy defined in 
 * the SupervisorWithTemplates Supervisor.
 */
class TemplateHelperTest extends AnyFlatSpec {
  
   /** Fixture to build same type of objects for the tests */
  trait Fixture {
    
    /**
     * The ID of the supervisor
     */
    val supervisorId = "SupervisorWithTemplates"
    
    // Build the CDB reader
    val cdbParentPath = FileSystems.getDefault().getPath("src/test");
    val cdbFiles = new CdbTxtFiles(cdbParentPath)
    val cdbReader: CdbReader = new JsonReader(cdbFiles)
    cdbReader.init()

    val superv = {
      val supervOpt=cdbReader.getSupervisor(supervisorId)
      assert(supervOpt.isPresent())
      supervOpt.get
    }

    val dasusToDeploy: Set[DasuToDeployDao] = CollectionConverters.asScala(superv.getDasusToDeploy).toSet
      
  }
  
  behavior of "The TemplateHelper"
  
  it must "distinguish between templated and non templated DASUs" in new Fixture {
    assert(dasusToDeploy.size==4)
    val normalDasus=TemplateHelper.getNormalDasusToDeploy(dasusToDeploy)
    assert(normalDasus.size==1)
    assert(normalDasus.filter(d => d.getDasu.getId=="Dasu1").size==1)

    val templatedDasus = TemplateHelper.getTemplatedDasusToDeploy(dasusToDeploy)
    assert(templatedDasus.size==3)
    val tempIds = templatedDasus.map(dtd => dtd.getDasu.getId)
    assert(tempIds.contains("DasuTemplateID1"))
    assert(tempIds.contains("DasuTemplateID2"))
  }
  
  it must "return all the DASUs when normalizing" in new Fixture {
    val dasus = TemplateHelper.normalizeDasusToDeploy(dasusToDeploy)
    assert(dasus.size==dasusToDeploy.size)
  }
  
  it must "not normalize a non templated DASU" in new Fixture {
    val dasus = TemplateHelper.normalizeDasusToDeploy(dasusToDeploy)
    val dasuIds = dasus.map(_.getId)
    assert(dasuIds.contains("Dasu1"))
    assert(!dasuIds.contains("DasuTemplateID1"))
    assert(!dasuIds.contains("DasuTemplateID2"))
  }
  
  it must "normalize the templated DASUs" in new Fixture {
    val dasus = TemplateHelper.normalizeDasusToDeploy(dasusToDeploy)
    val dasuIds = dasus.map(_.getId).filter(s => "Dasu1"!=s)
    assert(dasuIds.size==TemplateHelper.getTemplatedDasusToDeploy(dasusToDeploy).size)
    assert(dasuIds.forall( _.matches(Identifier.templatedIdRegExp.regex)))
  }
  
  it must "not normalize the ASCEs of non templated DASUs" in new Fixture {
    val dasus = TemplateHelper.normalizeDasusToDeploy(dasusToDeploy)
    val nonTemplatedDasus = dasus.filter(!_.getId.matches(Identifier.templatedIdRegExp.regex))
    val nonTemplatedAsces: Set[AsceDao] = nonTemplatedDasus.foldLeft(Set.empty[AsceDao])( (s, d) => s++CollectionConverters.asScala(d.getAsces))
    assert(nonTemplatedAsces.forall(!_.getId.matches(Identifier.templatedIdRegExp.regex)))
  }
  
  it must "normalize the ASCEs of the templated DASUs" in new Fixture {
    val dasus = TemplateHelper.normalizeDasusToDeploy(dasusToDeploy)
    val templatedDasus = dasus.filter(_.getId.matches(Identifier.templatedIdRegExp.regex))
    val templatedAsces: Set[AsceDao] = templatedDasus.foldLeft(Set.empty[AsceDao])( (s, d) => s++CollectionConverters.asScala(d.getAsces))
    assert(templatedAsces.forall(_.getId.matches(Identifier.templatedIdRegExp.regex)))
  }
  
  it must "normalize the output of the templated DASUs" in new Fixture {
    val dasus = TemplateHelper.normalizeDasusToDeploy(dasusToDeploy)
    val templatedDasus = dasus.filter(_.getId.matches(Identifier.templatedIdRegExp.regex))
    val outputs = templatedDasus.map(_.getOutput)
    assert(outputs.forall(_.getId.matches(Identifier.templatedIdRegExp.regex)))
  }
  
  it must "not normalize the output of non templated DASUs" in new Fixture {
    val dasus = TemplateHelper.normalizeDasusToDeploy(dasusToDeploy)
    val nonTemplatedDasus = dasus.filter(!_.getId.matches(Identifier.templatedIdRegExp.regex))
    val nonTemplateoutputs = nonTemplatedDasus.map(_.getOutput)
    assert(!nonTemplateoutputs.forall(_.getId.matches(Identifier.templatedIdRegExp.regex)))
  }
  
  it must "normalize the output of the templated ASCEs" in new Fixture {
    val dasus = TemplateHelper.normalizeDasusToDeploy(dasusToDeploy)
    val templatedDasus = dasus.filter(_.getId.matches(Identifier.templatedIdRegExp.regex))
    val templatedAsces: Set[AsceDao] = templatedDasus.foldLeft(Set.empty[AsceDao])( (s, d) => s++CollectionConverters.asScala(d.getAsces))
    assert(templatedAsces.forall(_.getOutput.getId.matches(Identifier.templatedIdRegExp.regex)))
  }
  
  it must "not normalize the output of non templated ASCEs" in new Fixture {
    val dasus = TemplateHelper.normalizeDasusToDeploy(dasusToDeploy)
    val nonTemplatedDasus = dasus.filter(!_.getId.matches(Identifier.templatedIdRegExp.regex))
    val nonTemplatedAsces: Set[AsceDao] = nonTemplatedDasus.foldLeft(Set.empty[AsceDao])( (s, d) => s++CollectionConverters.asScala(d.getAsces))
    assert(!nonTemplatedAsces.forall(_.getOutput.getId.matches(Identifier.templatedIdRegExp.regex)))
  }

  it must "normalize templated instance inputs" in new Fixture {
    // Templated instance inputs are converted and added to the inputs of the ASCE
    //
    // This test checks if the templated input instances are contained in the inputs of the ASCE

    // Get the templated instance inputs: this must be done before normalizing
    // because th list of templated instances is emptied during nromalization
    // and the ASCE contains the templated instances in its inputs
    val allDasus= dasusToDeploy.map(_.getDasu)
    // For each ASCE, the ascesWithTemplatedInstanceInputs map save the
    // ID and instance of the templated input instances
    val ascesWithTemplatedInstanceInputs: Map[AsceDao, Set[(String, Int)]] = {
      var map = Map.empty[AsceDao, Set[(String, Int)]]
      val allAsces = allDasus.foldLeft(Set.empty[AsceDao]) ( (z,dasu) => z++CollectionConverters.asScala(dasu.getAsces).toSet)
      val ascesWithTemplatedInstanceInputs = allAsces.filter(!_.getTemplatedInstanceInputs.isEmpty)

      ascesWithTemplatedInstanceInputs.foreach( asce => {
        val tempInstInputs = CollectionConverters.asScala(asce.getTemplatedInstanceInputs).toSet
        // We save the id before conversion and the instance number
        val dataToSave = tempInstInputs.map(tii => (tii.getIasio.getId, tii.getInstance()))
        if (!tempInstInputs.isEmpty) map=map+(asce -> dataToSave)
      })
      map
    }
    assert(ascesWithTemplatedInstanceInputs.nonEmpty, "No templated instance inputs found")

    val normalizedDasus = TemplateHelper.normalizeDasusToDeploy(dasusToDeploy)
    val allAsces = normalizedDasus.foldLeft(Set.empty[AsceDao]) ( (z,dasu) => z++CollectionConverters.asScala(dasu.getAsces).toSet)
    // After normalizing all the templated inputs must have been cleared
    assert(allAsces.filter(asce => !asce.getTemplatedInstanceInputs.isEmpty).isEmpty)

    ascesWithTemplatedInstanceInputs.keySet.forall( asce => {
      val templatedInstanceInputs: Set[(String, Int)] = ascesWithTemplatedInstanceInputs(asce)
      val asceInputs: Set[IasioDao] = CollectionConverters.asScala(asce.getInputs).toSet
      val asceInputIds = asceInputs.map( i => i.getId)

      templatedInstanceInputs.forall( tii => {
        val tiiId = tii._1 // Identifier
        val tiiInstance= tii._2 // Instance
        val expectedId = Identifier.buildIdFromTemplate(tiiId,tiiInstance)

        asceInputIds.contains(expectedId)
      })
    })
  }
  
}