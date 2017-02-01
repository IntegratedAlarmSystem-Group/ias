package org.eso.ias.prototype.transfer

import java.util.Properties
import java.util.concurrent.ThreadFactory
import scala.util.Try
import org.eso.ias.prototype.input.AlarmValue
import scala.sys.SystemProperties

/**
 * Implemented types of transfer functions
 */
object TransferFunctionLanguage extends Enumeration {
  val java, scala = Value
}

/**
 * The settings for the transfer function are retrieved
 * from the configuration (DB, XML, text file) and passed
 * to the {@link ComputingElement}.
 * 
 * Objects of this class contain all the information 
 * to run the transfer function independently from the 
 * supported programming language (@see TransferFunctionLanguage).
 * 
 * At the present in the prototype we foresee 2 possible implementations
 * of the transfer function in scala or java.
 * Other possibilities is to write the TF in python with jithon
 * that ultimately compiles the script into a java class, or use a DSL
 * for which scala offers some advantages compared to java.
 * 
 * Java implementation requires to provide a java-style interface to 
 * developers for which we need to convert scala data structure to/from
 * java. Scala implementations of the TF are therefore more performant (in principle).
 * 
 * @param className: The name of the java/scala class to run
 * @param language: the programming language used to implement the TF
 * @param threadFactory: The thread factory to async. run init and
 *                       shutdown on the user provided TF object
 * @see {@link ComputingElement}
 */
class TransferFunctionSetting(
    val className: String, 
    val language: TransferFunctionLanguage.Value,
    private[this] val threadFactory: ThreadFactory) {
  require(Option[String](className).isDefined && !className.isEmpty())
  require(Option[TransferFunctionLanguage.Value](language).isDefined)
      
  /**
   * Initialized is true when the object to run the TF has been
   * loaded and initialized
   */
  @volatile var initialized = false
  
  /**
   * isShutDown is true when the object has been shutdown
   */
  @volatile var isShutDown = false
  
  /**
   * The java or scala transfer executor i.e. the java or scala 
   * object that implements the transfer function
   */
  var transferExecutor: Option[TransferExecutor] = None
  
  override def toString(): String = {
    "Transfer function implemented in "+language+" by "+className
  }
  
  /**
   * Shutsdown the TF
   */
  def shutdown() {
    assert(!isShutDown)
    // Init the executor if it has been correctly instantiated 
    if (transferExecutor.isDefined) {
      val shutdownThread = threadFactory.newThread(new Runnable() {
        def run() {
          shutdownExecutor(transferExecutor)
       }
      })
      shutdownThread.start()
      // Wait for the termination
      shutdownThread.join(1500)
      if (shutdownThread.isAlive()) {
        println("User provided shutdown did not terminate in 1.5 sec.") 
      }
    }
    isShutDown=true
  }
  
  /**
   * Load, build and initialize the object to run
   * the transfer function.
   * 
   * This can be slow and access remote resources
   *  so it must be run in a worker thread
   * 
   * @param asceId: the ID of the ASCE
   * @param asceRunningId: the runningID of the ASCE
   * @param props: the user defined properties
   * @return true if the initialization went fine
   *         false otherwise
   */
  def initialize(
      asceId: String,
      asceRunningId: String,
      props: Properties) = {
    require(Option[String](asceId).isDefined)
    require(Option[String](asceRunningId).isDefined)
    require(Option[Properties](props).isDefined)
    assert(!initialized)
    
    // Load the class
    val tfExecutorClass: Option[Class[_]] = loadClass(className)
     
    // Go through the constructors and instantiate the executor
    //
    // We can suppose that the executor has more the one c'tor
    // as it is a common practice for testing
    if (tfExecutorClass.isDefined) {
      this.synchronized{
        transferExecutor = instantiateExecutor(tfExecutorClass,asceId,asceRunningId,props: Properties)
      }
    }
    
    // Init the executor if it has been correctly instantiated 
    if (transferExecutor.isDefined) {
      threadFactory.newThread(new Runnable() {
        def run() {
          initialized=initExecutor(transferExecutor)
          println("Initialized="+initialized)
        }
      }).start()
    }
  }

  /**
   * Initialize the passed executor
   * 
   * @param executor: The executor to initialize
   */
  private[this] def initExecutor(executor: Option[TransferExecutor]): Boolean = {
    require(executor.isDefined)
    try {
      executor.get.initialize()
      true
    } catch {
        case e: Throwable => 
          println("Exception caught initializing "+className+": "+e.getMessage)
          e.printStackTrace()
          false
      }
  }
  
  /**
   * Shutdown the passed executor
   * 
   * @param executor: The executor to shutdown
   */
  private[this] def shutdownExecutor(executor: Option[TransferExecutor]) {
    require(executor.isDefined)
    try {
      executor.get.shutdown()
    } catch {
        case e: Throwable => 
          println("Exception caught shutting down "+className+": "+e.getMessage)
          e.printStackTrace()
      }
  }
  
  /**
   * Dynamically load the class
   * 
   * @param name: the name of the class to load
   */
  private[this] def loadClass(name: String): Option[Class[_]] = {
    try {
        Option[Class[_]](this.getClass.getClassLoader().loadClass(name))
      } catch {
        case e: Throwable => 
          println("Exception caught loading "+name+": "+e.getMessage)
          e.printStackTrace()
          None
      }
  }
  
  /**
   * Instantiate the executor of the passed class
   * 
   * @param executorClass: the class of the executor to instantiate
   * @param asceId: the ID of the ASCE
   * @param asceRunningId: the runningID of the ASCE
   * @param props: the user defined properties
   * @return The instantiated object
   * 
   */
  private[this] def instantiateExecutor(
      executorClass: Option[Class[_]],
      asceId: String,
      asceRunningId: String,
      props: Properties): Option[TransferExecutor] = {
    assert(executorClass.isDefined)
    require(Option[String](asceId).isDefined)
    require(Option[String](asceRunningId).isDefined)
    require(Option[Properties](props).isDefined)
    // Go through the constructors and instantiate the executor
    //
    // We can suppose that the executor has more the one c'tor
    // as it is a common practice for testing
    try {
      val ctors = executorClass.get.getConstructors()
      var ctorFound=false
      var ret: Option[TransferExecutor] = None
      for {ctor <-ctors 
          paramTypes = ctor.getParameterTypes()
          if paramTypes.size==3} {        
            ctorFound=(paramTypes(0)==asceId.getClass && paramTypes(1)==asceRunningId.getClass && paramTypes(2)==props.getClass)
            if (ctorFound) {
              val args = Array[AnyRef](asceId,asceRunningId,props)
              ret = Some(ctor.newInstance(args:_*).asInstanceOf[TransferExecutor])
            }
          }
      ret
    } catch {
        case e: Throwable => 
          println("Exception caught instantiating the executor: "+e.getMessage)
          e.printStackTrace()
          None
      }
  }
}

object TransferFunctionSetting {
  
  /**
   * The name of the property to set maxTolerableTFTime
   */
  val MaxTFTimePropName="ias.prototype.asce.transfer.maxexectime"
  
  /**
   * If the execution time of the TF is higher the this value
   * the the state of the ASCE changes. At the present we do 
   * not block the execution but in future we could prefer to block
   * slow TFs unless it is a transient problem.
   * 
   * For the prototype it is enough to monitor.
   */
  lazy val MaxTolerableTFTime : Int=new SystemProperties().getOrElse(MaxTFTimePropName,"1000").toInt // msec
}
