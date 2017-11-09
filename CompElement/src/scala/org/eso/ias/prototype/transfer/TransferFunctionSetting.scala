package org.eso.ias.prototype.transfer

import java.util.Properties
import java.util.concurrent.ThreadFactory
import scala.util.Try
import org.ias.prototype.logging.IASLogger
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
  
  /** The logger */
  private val logger = IASLogger.getLogger(this.getClass)
  
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
        logger.warn("User provided shutdown did not terminate in 1.5 sec.") 
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
      props: Option[Map[String,String]]) = {
    require(Option(asceId).isDefined,"Invalid ASCE id")
    require(Option(asceRunningId).isDefined,"Invalid ASCE running id")
    require(Option(props).isDefined)
    assert(!initialized)
    
    logger.info("Initializing the TF {}",className)
    
    // Load the class
    val tfExecutorClass: Option[Class[_]] = loadClass(className)
    logger.debug("Class {} loaded",className)
     
    // Go through the constructors and instantiate the executor
    //
    // We can suppose that the executor has more the one c'tor
    // as it is a common practice for testing
    if (tfExecutorClass.isDefined) {
      this.synchronized{
        transferExecutor = instantiateExecutor(tfExecutorClass,asceId,asceRunningId,props)
      }
    }
    
    // Init the executor if it has been correctly instantiated 
    if (transferExecutor.isDefined) {
      logger.debug("Constructor of {} instantiated",className)
      threadFactory.newThread(new Runnable() {
        def run() {
          logger.debug("Async initializing the executor of {}",className)
          initialized=initExecutor(transferExecutor)
          logger.info("Async initialization of the executor of {} terminated",className)
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
        case e: Exception => 
          logger.error("Exception caught initializing {}",className,e)
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
        case e: Exception => 
          logger.warn("Exception caught shutting down {}",className,e)
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
          logger.error("Exception caught loading {}",e)
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
      props: Option[Map[String,String]]): Option[TransferExecutor] = {
    assert(executorClass.isDefined)
    require(Option(asceId).isDefined)
    require(Option(asceRunningId).isDefined)
    require(Option(props).isDefined)
    
    val javaProps: Properties = new Properties()
    props.get.keySet.foreach(k => javaProps.setProperty(k, props.get.get(k).get))
    
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
            ctorFound=(paramTypes(0)==asceId.getClass && paramTypes(1)==asceRunningId.getClass && paramTypes(2)==javaProps.getClass)
            if (ctorFound) {
              val args = Array[AnyRef](asceId,asceRunningId,javaProps)
              ret = Some(ctor.newInstance(args:_*).asInstanceOf[TransferExecutor])
            }
          }
      ret
    } catch {
        case e: Throwable => 
          logger.error("Exception caught instantiating the executor",e)
          None
      }
  }
}

object TransferFunctionSetting {
  
  /**
   * The name of the property to set MaxTolerableTFTime
   */
  val MaxTFTimePropName="org.eso.ias.asce.transfer.maxexectime"
  
  /**
   * If the execution time of the TF is higher the this value
   * the the state of the ASCE changes. At the present we do 
   * not block the execution but in future we could prefer to block
   * slow TFs unless it is a transient problem.
   */
  lazy val MaxTolerableTFTime : Int=new SystemProperties().getOrElse(MaxTFTimePropName,"1000").toInt // msec
  
  /**
   * The name of the property to set MaxAcceptableSlowDuration
   */
  val MaxAcceptableSlowDurationPropName = "org.eso.ias.asce.transfer.maxtimeinslow"
  
  /**
   * If the TF is slow responding for more then the amount of seconds
   * of  MaxAcceptableSlowDuration then the TF is marked as broken and 
   * will not be executed anymore
   */
  lazy val MaxAcceptableSlowDuration = new SystemProperties().getOrElse(MaxAcceptableSlowDurationPropName,"30").toInt
}
