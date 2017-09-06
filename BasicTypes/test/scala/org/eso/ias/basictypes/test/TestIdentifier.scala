package org.eso.ias.basictypes.test

import org.scalatest.FlatSpec
import org.eso.ias.prototype.input.Identifier
import org.eso.ias.prototype.input.java.IdentifierType

class TestIdentifier extends FlatSpec {
  behavior of "A Identifier"
  
  it must "forbid to declare IDs with null or empty strings" in {
    assertThrows[NullPointerException] {
      val id1: Identifier = new Identifier(null,null,None)
    }
    assertThrows[IllegalArgumentException] {
      val id2: Identifier = new Identifier(Some[String](""),Some(IdentifierType.MONITORED_SOFTWARE_SYSTEM),None)
    }
  }
  
  it must "forbid to declare IDs containing the separator char '"+Identifier.separator+"'" in {
    val wrongID = "Prefix"+Identifier.separator+"-suffix"
    assertThrows[IllegalArgumentException] {
      val id1: Identifier = new Identifier(Some[String](wrongID),Some(IdentifierType.ASCE),None)
    }
  }
  
  it must "forbid to instantiate a ID with a parent of the wrong type" in {
    val msId=new Identifier(Some("monSysyId"),Some(IdentifierType.MONITORED_SOFTWARE_SYSTEM),None)
    
    
    val supId=new Identifier(Some("supId"),Some(IdentifierType.SUPERVISOR),None)
    val dasuId = new Identifier(Some("dasuId"),Some(IdentifierType.DASU),Some(supId))
    
    val plId=new Identifier(Some("pluginId"),Some(IdentifierType.PLUGIN),Some(msId))
    val convId=new Identifier(Some("converterId"),Some(IdentifierType.CONVERTER),Some(plId))
    val ioId=new Identifier(Some("iasioId"),Some(IdentifierType.IASIO),Some(convId))
    
    assertThrows[IllegalArgumentException] {
      val ioId2=new Identifier(Some("iasioId"),Some(IdentifierType.IASIO),Some(plId))
    }
    
    assertThrows[IllegalArgumentException] {
      val ioId2=new Identifier(Some("iasioId"),Some(IdentifierType.IASIO),Some(supId))
    }
    
  }
  
  /**
   * Check the construction of the runningID.
   */
  it must "provide a non-empty runningID string" in {
    val id1: Identifier = new Identifier(Some("monSysyId"),Some(IdentifierType.MONITORED_SOFTWARE_SYSTEM),None)
    assert(!id1.runningID.isEmpty())
    val id2: Identifier = new Identifier(Some("pluginId"),Some(IdentifierType.PLUGIN),Some(id1))
    assert(!id2.runningID.isEmpty())
    val id3: Identifier = new Identifier(Some[String]("converterId"),Some(IdentifierType.CONVERTER),Some(id2))
    assert(!id3.runningID.isEmpty())
    
    assert(id3.runningID.contains(id3.id.get))
    assert(id3.runningID.contains(id2.id.get))
    assert(id3.runningID.contains(id1.id.get))
  }
  
  /**
   * Check the construction of the fullRunningID.
   */
  it must "provide a non-empty fullRunningID string" in {
    val id1: Identifier = new Identifier(Some("monSysyId"),Some(IdentifierType.MONITORED_SOFTWARE_SYSTEM),None)
    assert(!id1.fullRunningID.isEmpty())
    val id2: Identifier = new Identifier(Some("pluginId"),Some(IdentifierType.PLUGIN),Some(id1))
    assert(!id2.fullRunningID.isEmpty())
    val id3: Identifier = new Identifier(Some[String]("converterId"),Some(IdentifierType.CONVERTER),Some(id2))
    assert(!id3.fullRunningID.isEmpty())
    
    assert(id3.fullRunningID.contains(id3.id.get))
    assert(id3.fullRunningID.contains(id2.id.get))
    assert(id3.fullRunningID.contains(id1.id.get))
  }
  
  /**
   * Check the construction of the runningID.
   */
  it must "must properly order the runnigID" in {
    val id1: Identifier = new Identifier(Some("monSysyId"),Some(IdentifierType.MONITORED_SOFTWARE_SYSTEM),None)
    val id2: Identifier = new Identifier(Some("pluginId"),Some(IdentifierType.PLUGIN),Some(id1))
    val id3: Identifier = new Identifier(Some[String]("converterId"),Some(IdentifierType.CONVERTER),Some(id2))
    val id4: Identifier = new Identifier(Some[String]("iasioId"),Some(IdentifierType.IASIO),Some(id3))
    
    assert(!id4.runningID.isEmpty())
    assert(id4.runningID.endsWith(id4.id.get))
    assert(id4.runningID.startsWith(id1.id.get))
    
  }
  
  behavior of "The object factory (apply)"
  
  /**
   * Check the factory method with a list of tuples (IDs,types)
   */
  it must "" in {
    
  }
}