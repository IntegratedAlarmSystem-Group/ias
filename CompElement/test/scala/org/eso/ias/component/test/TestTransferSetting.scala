package org.eso.ias.component.test

import org.scalatest.FlatSpec
import java.util.concurrent.ThreadFactory
import org.eso.ias.prototype.transfer.TransferFunctionSetting
import org.eso.ias.prototype.transfer.TransferFunctionLanguage

/**
 * Test the TransferFunctionSetting
 * 
 * @see TransferFunctionSetting
 */
class TestTransferFunctionSetting extends FlatSpec {
  
  
  
  trait TFBuilder {
    // The thread factory used by the setting to async
    // intialize and shutdown the TF objects
    val threadFactory = new TestThreadFactory()
    
    val javaTF = new TransferFunctionSetting(
        "org.eso.ias.component.test.transfer.TransferExecutorImpl",
        TransferFunctionLanguage.java,
        threadFactory)
    
    val scalaTF = new TransferFunctionSetting(
        "org.eso.ias.component.test.transfer.TransferExample",
        TransferFunctionLanguage.scala,
        threadFactory)
  }
  
  
  // The thread factory used by the setting to async
  // intialize and shutdown the TF objects
  val threadFactory = new TestThreadFactory() 
  
  behavior of "TransferFunctionSetting"
  
  it must "load, initialize and shutdown a java TF" in new TFBuilder {
    assert(!javaTF.initialized)
    assert(!javaTF.isShutDown)
    javaTF.initialize("ASCE-ID", "ASCE-running-ID", System.getProperties)
    Thread.sleep(500)
    assert(javaTF.initialized)
    assert(!javaTF.isShutDown)
    javaTF.shutdown()
    Thread.sleep(500)
    assert(javaTF.initialized)
    assert(javaTF.isShutDown)
    
    assert(threadFactory.numberOfAliveThreads()==0)
    assert(threadFactory.instantiatedThreads==2)
  }
  
  it must "load, initialize and shutdown a scala TF" in new TFBuilder {
    assert(!scalaTF.initialized)
    assert(!scalaTF.isShutDown)
    scalaTF.initialize("ASCE-ID", "ASCE-running-ID", System.getProperties)
    Thread.sleep(500)
    assert(scalaTF.initialized)
    assert(!scalaTF.isShutDown)
    scalaTF.shutdown()
    Thread.sleep(500)
    assert(scalaTF.initialized)
    assert(scalaTF.isShutDown)
    
    assert(threadFactory.numberOfAliveThreads()==0)
    assert(threadFactory.instantiatedThreads==2)
  }
}