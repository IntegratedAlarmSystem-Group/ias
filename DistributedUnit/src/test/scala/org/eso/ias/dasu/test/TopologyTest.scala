package org.eso.ias.dasu.test

import org.scalatest.FlatSpec
import org.eso.ias.dasu.topology.AsceTopology
import org.eso.ias.dasu.topology.Topology

/** Test the Topology */
class TopologyTest extends FlatSpec {
  
  // Builds the ASCEs for the topology to test
  val asce1: AsceTopology = new AsceTopology("ASCE1-ID",Set("IN1","IN2","IN3"),                        "ASCE-OUT1")
  val asce2: AsceTopology = new AsceTopology("ASCE2-ID",Set("IN3","IN4"),                              "ASCE-OUT2")
  val asce3: AsceTopology = new AsceTopology("ASCE3-ID",Set("IN6"),                                    "ASCE-OUT3")
  val asce4: AsceTopology = new AsceTopology("ASCE4-ID",Set("ASCE-OUT1","IN3"),                        "ASCE-OUT4")
  val asce5: AsceTopology = new AsceTopology("ASCE5-ID",Set("ASCE-OUT2","ASCE-OUT3","IN5"),            "ASCE-OUT5")
  val asce6: AsceTopology = new AsceTopology("ASCE6-ID",Set("ASCE-OUT4","ASCE-OUT2","ASCE-OUT5","IN3"),"ASCE-OUT6")
  
  // Most test uses the following topology
  //
  // This topology has the following 11 linearized trees generated 
  // from the passed ASCEs (asce1..asce6):
  // IN1 -> 1------------> 4 -----> 6
  // IN2 -> 1------------> 4 -----> 6
  // IN3 -> 1------------> 4 -----> 6
  // IN3 ----------------> 4 -----> 6
  // IN3 -------------------------> 6
  // IN3 ------> 2 ---------------> 6
  // IN3 ------> 2 ----------> 5 -> 6
  // IN4 ------> 2 ---------------> 6
  // IN4 ------> 2 ----------> 5 -> 6
  // IN5 --------------------> 5 -> 6
  // IN6 -----------> 3 -----> 5 -> 6
  val topology = new Topology(List(asce1,asce2,asce3,asce4,asce5,asce6),"DASU-ID","ASCE-OUT6")
  
  behavior of "The DASU topology"
  
  
  it must "catch the inputs of the DASU from all the ASCEs" in {
    // This test uses another topology for testing a specific case
    val asce1: AsceTopology = new AsceTopology("ASCE1-ID",Set("IN1","IN2","IN3"),"ASCE1-OUT")
    val asce2: AsceTopology = new AsceTopology("ASCE2-ID",Set("ASCE1-OUT","IN2","IN4","IN5"),"ASCE2-OUT")
    val asce3: AsceTopology = new AsceTopology("ASCE3-ID",Set("ASCE2-OUT","ASCE1-OUT","IN6","IN7","IN8"),"ASCE3-OUT")
    
    val anotherTopology = new Topology(List(asce1,asce2,asce3),"DASU-ID","ASCE3-OUT")
    
    val allInputs: Set[String] = (asce1.inputs++asce2.inputs++asce3.inputs).filter(s => s.startsWith("IN"))
    
    allInputs.foreach(input => assert(anotherTopology.dasuInputs.contains(input)))
  }
  
  it must "correctly build the graph of connections" in {
    // There must be one three for each possible input
    // i.e. a input not produced by ASCEs running in the DASU itself
    assert(topology.trees.size==6)
    // Check if all the trees starts with an input
    topology.trees.foreach(node => assert(node.id.startsWith("IN")))
  }
  
  it must "correctly build the linearized graphs of connections" in {
    // There must be one three for each possible input
    // i.e. a input not produced by ASCEs running in the DASU itself
    assert(topology.linearizedTrees.size==11)
    // Check if all the trees starts with an input
    topology.linearizedTrees.foreach(node => assert(node.id.startsWith("IN")))
    
    assert(topology.linearizedTrees.count(n => n.id.startsWith("IN1"))==1)
    assert(topology.linearizedTrees.count(n => n.id.startsWith("IN2"))==1)
    assert(topology.linearizedTrees.count(n => n.id.startsWith("IN3"))==5)
    assert(topology.linearizedTrees.count(n => n.id.startsWith("IN4"))==2)
    assert(topology.linearizedTrees.count(n => n.id.startsWith("IN5"))==1)
    assert(topology.linearizedTrees.count(n => n.id.startsWith("IN6"))==1)
  }
  
  it must "correctly calculate the depth" in {
    assert(topology.maxDepth==3)
    assert(topology.minDepth==1)
  }
  
  it must "detect a cycle" in {
    // This test uses another topology for testing a specific case
    val asce1: AsceTopology = new AsceTopology("ASCE1-ID",Set("IN1"),"ASCE1-OUT")
    val asce2: AsceTopology = new AsceTopology("ASCE2-ID",Set("ASCE1-OUT","ASCE3-OUT"),"ASCE2-OUT")
    val asce3: AsceTopology = new AsceTopology("ASCE3-ID",Set("ASCE2-OUT"),"ASCE3-OUT")
    val asce4: AsceTopology = new AsceTopology("ASCE4-ID",Set("ASCE2-OUT"),"ASCE4-OUT")
    val asce5: AsceTopology = new AsceTopology("ASCE5-ID",Set("ASCE3-OUT","ASCE4-OUT"),"ASCE5-OUT")
    
    assertThrows[IllegalArgumentException] {
      val anotherTopology = new Topology(List(asce1,asce2,asce3,asce4,asce5),"DASU-ID","ASCE5-OUT")
    }
  }
  
  it must "correctly build the levels" in {
    assert(topology.levels.size==topology.maxDepth)
    assert(topology.levels(2)==Set[String]("ASCE6-ID"))
    assert(topology.levels(1)==Set[String]("ASCE4-ID","ASCE5-ID"))
    assert(topology.levels(0)==Set[String]("ASCE1-ID","ASCE2-ID","ASCE3-ID"))
  }
  
  it must "correctly return the ID of the ASCE that produces the output" in {
    val asceId3 = topology.asceProducingOutput(asce3.output)
    assert(asceId3.isDefined && asce3.identifier==asceId3.get)
    
    val asceId6 =topology.asceProducingOutput(asce6.output)
    assert(asceId6.isDefined && asceId6.get==asce6.identifier)
  }
  
  it must "properly return the set of inputs of a given ASCEs" in {
    val inputIds = topology.inputsOfAsce("ASCE2-ID")
    assert(inputIds==Set("IN3","IN4"))
    
    val inputIds2 = topology.inputsOfAsce("ASCE6-ID")
    assert(inputIds2==Set("ASCE-OUT4","ASCE-OUT2","ASCE-OUT5","IN3"))
    
    val inputIds3 = topology.inputsOfAsce("ASCE3-ID")
    assert(inputIds3==Set("IN6"))
  }
  
  it must "properly return the Ids of the ASCEs that require an input" in {
    val asceIds = topology.ascesOfInput("ASCE-OUT2")
    assert(asceIds==Set("ASCE5-ID","ASCE6-ID"))
    
    val asceIds2 = topology.ascesOfInput("IN1")
    assert(asceIds2==Set("ASCE1-ID"))
    
    val asceIds3 = topology.ascesOfInput("IN3")
    assert(asceIds3==Set("ASCE1-ID","ASCE2-ID","ASCE4-ID","ASCE6-ID"))
  }
  
}