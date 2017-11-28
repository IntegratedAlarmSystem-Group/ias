package org.eso.ias.prototype.transfer

import java.util.Properties
import java.util.concurrent.ThreadFactory
import scala.util.Try
import org.ias.prototype.logging.IASLogger
import scala.sys.SystemProperties
import scala.util.Success
import scala.util.Failure

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
      shutdownThread.join(10000)
      if (shutdownThread.isAlive()) {
        logger.warn("User provided shutdown did not terminate in 10 secs.") 
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
      props: Properties): Boolean = {
    require(Option(asceId).isDefined,"Invalid ASCE id")
    require(Option(asceRunningId).isDefined,"Invalid ASCE running id")
    require(Option(props).isDefined)
    assert(!initialized)
    
    logger.info("Initializing the TF {}",className)
    
    // Load the class
    val tfExecutorClass: Try[Class[_]] = Try((this.getClass.getClassLoader().loadClass(className)))
    transferExecutor = tfExecutorClass match {
      case Failure(e) => logger.error("Error loading {}",className,e); None
      case Success(tec) => this.synchronized {
        instantiateExecutor(tec, asceId, asceRunningId, props)
      }
    }
    
    transferExecutor.foreach( te => {
      logger.debug("TF of type {} instantiated", className)
      val thread = threadFactory.newThread(new Runnable() {
          def run() {
            logger.debug("Async initializing the executor of {}", className)
            initialized = initExecutor(te)
          }
        })
        thread.start()
        thread.join(60000)
        if (thread.isAlive()) {
          logger.warn("User provided shutdown did not terminate in 60 secs.") 
        } else {
         logger.info("Async initialization of the executor of {} terminated", className) 
        }
    })
    transferExecutor.isDefined
    
  }

  /**
   * Initialize the passed executor
   * 
   * @param executor: The executor to initialize
   * @return true if the TE has been correctly initialized, false otherwise
   */
  private[this] def initExecutor(executor: TransferExecutor): Boolean = {
    require(Option(executor).isDefined)
    try {
      executor.initialize()
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
   * Instantiate the executor of the passed class
   *
   * @param executorClass: the class of the executor to instantiate
   * @param asceId: the ID of the ASCE
   * @param asceRunningId: the runningID of the ASCE
   * @param props: the user defined properties
   * @return The instantiated executor
   *
   */
  private[this] def instantiateExecutor(
    executorClass: Class[_],
    asceId: String,
    asceRunningId: String,
    props: Properties): Option[TransferExecutor] = {
    assert(Option(executorClass).isDefined)
    require(Option(asceId).isDefined && asceId.size>0)
    require(Option(asceRunningId).isDefined && asceRunningId.size>0)
    require(Option(props).isDefined)

    // Get the constructor with 3 parameters
    val ctors = executorClass.getConstructors().filter(c => c.getParameterTypes().size == 3)
    // Get the constructor
    val ctor = ctors.find(c =>
      c.getParameterTypes()(0) == asceId.getClass &&
        c.getParameterTypes()(1) == asceRunningId.getClass &&
        c.getParameterTypes()(2) == props.getClass)

    val instance = Try(ctor.map(c => {
      // The arguments to pass to the constructor
      val args = Array[AnyRef](asceId, asceRunningId, props)
      // Invoke the constructor to build the TransferExecutor
      c.newInstance(args: _*).asInstanceOf[TransferExecutor]
    }))

    instance match {
      case Success(te) => te
      case Failure(x) => logger.error("Error building the transfer function", x); None
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
