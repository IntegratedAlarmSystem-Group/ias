package org.eso.ias.supervisor.test.tf

import java.util.Properties

import org.eso.ias.asce.transfer.{IasIO, IasioInfo, ScalaTransferExecutor}

/**
 * A TF to test the getting of values with getValue
 * in case of a templated ASCE.
 * 
 * The test aims to check if getValue works as expected when dealing
 * with templated and non templated TF i.e. when getting a 
 * templated value passing its ID only.
 * 
 * @param cEleId: the ID of the ASCE
 * @param cEleRunningId: the runningID of the ASCE
 * @param validityTimeFrame: The time frame (msec) to invalidate monitor points
 * @param props: the user defined properties    
 * @author acaproni
 */
class TemplatedTF(cEleId: String, cEleRunningId: String, validityTimeFrame: Long,props: Properties) 
extends ScalaTransferExecutor[Long](cEleId,cEleRunningId,validityTimeFrame,props) {
  
  /**
   * The ID of a non templated param
   */
  val nontTemplatedId = "NonTemplatedId"
  
  /**
   * The ID of the templated param
   */
  val templatedId = "TemplatedId"

  /**
    * Initialize the TF
    *
    * @param inputsInfo The IDs and types of the inputs
    * @param outputInfo The Id and type of thr output
    **/
  override def initialize(inputsInfo: Set[IasioInfo], outputInfo: IasioInfo): Unit = {
    println("Initialized "+getTemplateInstance().orElse(null));
  }
  
  override def shutdown(): Unit = {
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

    // Get the templated instance inputs
    val tempInst1 = getValue(compInputs, "TemplatedInput", Some(3))
    val valOfTempInst1 = if (tempInst1.isDefined) 1 else 0;
    val tempInst2 = getValue(compInputs, "TemplatedInput", Some(4))
    val valOfTempInst2 = if (tempInst1.isDefined) 2 else 0;

    val nonTempVal = nonTempInOut.get.value.get.asInstanceOf[Long]
    print("Non templated value ="+nonTempVal)
    val tempVal = tempInOut.get.value.get.asInstanceOf[Long]
    print("Templated value ="+tempVal)

    actualOutput.updateValue(nonTempVal+tempVal+valOfTempInst1+valOfTempInst2)
  }
  
}