package org.eso.ias.asce.test

import org.eso.ias.asce.transfer.{TransferFunctionLanguage, TransferFunctionSetting}
import org.scalatest.flatspec.AnyFlatSpec

import java.util.Properties
import java.util.concurrent.ThreadFactory

/**
 * Test the TransferFunctionSetting
 * 
 * @see TransferFunctionSetting
 */
class TestTransferFunctionSetting extends AnyFlatSpec {
  
  
  
  trait TFBuilder {
    // The thread factory used by the setting to async
    // intialize and shutdown the TF objects
    val threadFactory = new TestThreadFactory()
    
    val javaTF = new TransferFunctionSetting(
        "org.eso.ias.asce.test.transfer.TransferExecutorImpl",
        TransferFunctionLanguage.java,
        None,
        threadFactory)
    
    val scalaTF = new TransferFunctionSetting(
        "org.eso.ias.asce.test.transfer.TransferExample",
        TransferFunctionLanguage.scala,
        None,
        threadFactory)
  }

  behavior of "TransferFunctionSetting"
  
  it must "load and shutdown a java TF" in new TFBuilder {
    assert(!javaTF.initialized)
    assert(!javaTF.isShutDown)
    javaTF.initialize("ASCE-ID", "ASCE-running-ID", 1000, new Properties())
    assert(!javaTF.isShutDown)
    javaTF.shutdown()
    Thread.sleep(1000)
    assert(javaTF.isShutDown)
    
    assert(threadFactory.numberOfAliveThreads()==0)
    assert(threadFactory.instantiatedThreads==1)
  }
  
  it must "load and shutdown a scala TF" in new TFBuilder {
    assert(!scalaTF.initialized)
    assert(!scalaTF.isShutDown)
    scalaTF.initialize("ASCE-ID", "ASCE-running-ID", 1000, new Properties())
    assert(!scalaTF.isShutDown)
    scalaTF.shutdown()
    Thread.sleep(500)
    assert(scalaTF.isShutDown)
    
    assert(threadFactory.numberOfAliveThreads()==0)
    assert(threadFactory.instantiatedThreads==1)
  }
}