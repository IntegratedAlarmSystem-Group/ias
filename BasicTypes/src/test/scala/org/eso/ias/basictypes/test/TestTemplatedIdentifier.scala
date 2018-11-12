package org.eso.ias.basictypes.test

import org.scalatest.FlatSpec
import org.eso.ias.types.Identifier
import org.eso.ias.types.IdentifierType

/**
 * Test the identifier generated from a template
 */
class TestTemplatedIdentifier extends FlatSpec {
  behavior of "A Identifier generated form a template"
  
  // Not all identifier types support templates
  it must "avoid building templates from wrong types" in {
    
    assertThrows[IllegalArgumentException] {
      new Identifier("monSysyId",Some(3),IdentifierType.MONITORED_SOFTWARE_SYSTEM,None)
    }
    val msId=new Identifier("monSysyId",None,IdentifierType.MONITORED_SOFTWARE_SYSTEM,None)
    
    assertThrows[IllegalArgumentException] {
      new Identifier("pluginId",Some(5),IdentifierType.PLUGIN,Some(msId))
    }
    val plId=new Identifier("pluginId",IdentifierType.PLUGIN,Some(msId))
    
    assertThrows[IllegalArgumentException] {
      new Identifier("converterId",Some(4),IdentifierType.CONVERTER,Some(plId))
    }
    val convId=new Identifier("converterId",None,IdentifierType.CONVERTER,Some(plId))
    
    assertThrows[IllegalArgumentException] {
      new Identifier("SupervId",Some(0),IdentifierType.SUPERVISOR,None)
    }
    
    val supervId = new Identifier("SupervId",None,IdentifierType.SUPERVISOR,None)
    
    
    val dasuId1 = new Identifier("dasuId1",Some(2),IdentifierType.DASU,Some(supervId))
    val dasuId2 = new Identifier("dasuId2",None,IdentifierType.DASU,Some(supervId))
    
    val asceId1 = new Identifier("asceId1",Some(7),IdentifierType.ASCE,Some(dasuId1))
    val asceId2 = new Identifier("asceId2",None,IdentifierType.ASCE,Some(dasuId2))
    
    val ioId1=new Identifier("iasioId1",Some(9),IdentifierType.IASIO,Some(convId))
    val ioId2=new Identifier("iasioId2",None,IdentifierType.IASIO,Some(asceId1))
  }
  
  it must "flag a templated identifier" in {
    val supervId = new Identifier("SupervId",None,IdentifierType.SUPERVISOR,None)
    assert(!supervId.fromTemplate)
    val dasuId1 = new Identifier("dasuId1",Some(2),IdentifierType.DASU,Some(supervId))
    assert(dasuId1.fromTemplate)
  }
  
  it must "return the number of instance" in {
    val supervId = new Identifier("SupervId",None,IdentifierType.SUPERVISOR,None)
    assert(supervId.templateInstance.isEmpty)
    val dasuId1 = new Identifier("dasuId1",Some(2),IdentifierType.DASU,Some(supervId))
    assert(dasuId1.templateInstance.isDefined)
    assert(dasuId1.templateInstance.get==2)
  }
  
  it must "build a templated ID from java null values" in {
    assertThrows[IllegalArgumentException] {
      new Identifier("monSysyId",IdentifierType.SUPERVISOR,3,null)
    }
    val supervId = new Identifier("SupervId",IdentifierType.SUPERVISOR)
    assert(!supervId.fromTemplate)
    
    val dasuId = new Identifier("dasuId1",IdentifierType.DASU,5,supervId)
     assert(dasuId.fromTemplate)
     assert(dasuId.templateInstance.get==5)
  }

  it must "return the base ID of a templated identifier" in {

    val supervId = new Identifier("SupervId",IdentifierType.SUPERVISOR)
    assert(!supervId.fromTemplate)

    val dasuId = new Identifier("dasuID",IdentifierType.DASU,12,supervId)

    val instance = dasuId.templateInstance
    assert(instance.isDefined)
    val baseId = dasuId.baseId
    assert(baseId=="dasuID")

    val dasuIdNoTemplate = new Identifier("dasuID-NT",IdentifierType.DASU,supervId)
    val baseId2 = dasuIdNoTemplate.baseId
    assert(baseId2=="dasuID-NT")

  }

  it must "get baseid and instance number" in {
    val identifier = Identifier.buildIdFromTemplate("Test",Option(23))
    println(identifier)
    assert(Identifier.getBaseId(identifier)=="Test")
    assert(Identifier.getTemplateInstance(identifier).isDefined)
    assert(Identifier.getTemplateInstance(identifier).get==23)
  }
  
  
}