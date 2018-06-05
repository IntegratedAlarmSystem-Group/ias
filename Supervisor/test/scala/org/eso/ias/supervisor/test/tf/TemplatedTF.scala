package org.eso.ias.supervisor.test.tf

import java.util.Properties
import org.eso.ias.asce.transfer.ScalaTransferExecutor
import org.eso.ias.types.Alarm
import org.eso.ias.asce.transfer.IasIO

/**
 * A TF to test the getting of values with getValue
 * in case of a templated ASCE.
 * 
 * The test aims to check if getValue works as expected when dealing
 * with templated and non templated TF i.e. when getting a 
 * templated value passing its ID only.
 */
class TemplatedTF(cEleId: String, cEleRunningId: String, props: Properties) 
extends ScalaTransferExecutor[Long](cEleId,cEleRunningId,props) {
  
  /**
   * The ID of a non templated param
   */
  val nontTemplatedId = "NonTemplatedId"
  
  /**
   * The ID of the templated param
   */
  val templatedId = "TemplatedId"
  
  override def initialize() {
    println("Initialized "+getTemplateInstance().orElse(null));
  }
  
  override def shutdown() {
    println("Initialized")
  }
  
  /**
   * eval sums the values of all the IasIO in the map, getting them 
   * with their IDs
   */
  override def eval(compInputs: Map[String, IasIO[_]], actualOutput: IasIO[Long]): IasIO[Long] = {
    
    val nonTempInOut = getValue(compInputs, nontTemplatedId)
    assert(nonTempInOut.isDefined)
    val tempInOut = getValue(compInputs, templatedId)
    assert(tempInOut.isDefined)
    
    val nonTempVal = nonTempInOut.get.value.get.asInstanceOf[Long]
    print("Non templated value ="+nonTempVal)
    val tempVal = tempInOut.get.value.get.asInstanceOf[Long]
    print("Templated value ="+tempVal)
    
    actualOutput.updateValue(nonTempVal+tempVal)
  }
  
}