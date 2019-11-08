package org.eso.ias.asce.transfer

import java.util.Properties
import java.util.concurrent.{ThreadFactory, TimeUnit}

import org.eso.ias.logging.IASLogger

import scala.sys.SystemProperties
import scala.util.{Failure, Success, Try}

/**
 * Implemented types of transfer functions
 */
object TransferFunctionLanguage extends Enumeration {
  val java, scala, python = Value
}

/**
 * The settings for the transfer function are retrieved
 * from the configuration (DB, XML, text file) and passed
 * to the ComputingElement.
 * 
 * Objects of this class contain all the information 
 * to run the transfer function independently from the 
 * supported programming language (@see TransferFunctionLanguage).
 * 
 * At the present there 3 possible implementations
 * of the transfer function in scala, java or python.
 *
 * Java implementation requires to provide a java-style interface to 
 * developers for which we need to convert scala data structure to/from
 * java. Scala implementations of the TF are therefore more performant (in principle).
 *
 * For python TFs, we use jep (https://github.com/ninia/jep) that provides
 * interoperability between java and python.
 * For the python TFs, a java class is always loaded, PythonExecutorTF, that delegates
 * to the python class whose name is in the className property
 * 
 * @param className: The name of the java/scala/python class to run
 * @param language: the programming language used to implement the TF
 * @param templateInstance: the instance of the template if defined;
 *                          if empty the ASCE is not generated out of a template
 * @param threadFactory: The thread factory to async. run init and
 *                       shutdown on the user provided TF object
 * @see ComputingElement
 */
class TransferFunctionSetting(
    val className: String, 
    val language: TransferFunctionLanguage.Value,
    templateInstance: Option[Int],
    private[this] val threadFactory: ThreadFactory) {
  require(Option[String](className).isDefined && !className.isEmpty)
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
   * For java and scala the class to load is in className but for python
   * the name of the java class to load is fixed and className contains the name
   * of the python class to run the TF
   */
  val javaTFClassForPython = "org.eso.ias.asce.transfer.PythonExecutorTF"
  
  /**
   * The java or scala transfer executor i.e. the java or scala 
   * object that implements the transfer function
   */
  var transferExecutor: Option[TransferExecutor] = None
  
  /** The logger */
  private val logger = IASLogger.getLogger(this.getClass)
  
  override def toString: String = {
    "Transfer function implemented in "+language+" by "+className
  }
  
  /** Shuts the TF down */
  def shutdown() {
    assert(!isShutDown)
    // Init the executor if it has been correctly instantiated 
    if (transferExecutor.isDefined) {
      val shutdownThread = threadFactory.newThread(new Runnable() {
        override def run() {
          shutdownExecutor(transferExecutor)
       }
      })
      shutdownThread.start()
      // Wait for the termination
      shutdownThread.join(10000)
      if (shutdownThread.isAlive) {
        logger.warn("User provided shutdown did not terminate in 10 secs.") 
      }
    }
    isShutDown=true
  }
  
  /**
   * Load and build the executor of the  transfer function.
   * 
   * This can be slow and access remote resources
   *  so it must be run in a worker thread
   * 
   * @param asceId: the ID of the ASCE
   * @param asceRunningId: the runningID of the ASCE
   * @param validityTimeFrame: The time frame (msec) to invalidate monitor points
   * @param props: the user defined properties
   * @return true if the initialization went fine
   *         false otherwise
   */
  def initialize(
      asceId: String,
      asceRunningId: String,
      validityTimeFrame: Long,
      props: Properties): Boolean = {
    require(Option(asceId).isDefined,"Invalid ASCE id")
    require(Option(asceRunningId).isDefined,"Invalid ASCE running id")
    require(Option(props).isDefined)
    assert(!initialized)
    
    logger.info("Initializing the TF {} with language",className,language)
    
    // Load the class
    val tfExecutorClass: Try[Class[_]] =
      if (language==TransferFunctionLanguage.python) {
        logger.debug("Loading the java class to run python TF {}",className)
        Try(this.getClass.getClassLoader.loadClass(javaTFClassForPython))
      } else {
        logger.debug("Loading java class {}",className)
        Try(this.getClass.getClassLoader.loadClass(className))
      }
    transferExecutor = tfExecutorClass match {
      case Failure(e) => logger.error("Error loading {}",className,e); None
      case Success(tec) => this.synchronized {
        logger.info("Class {} loaded",className)
        instantiateExecutor(tec, asceId, asceRunningId, validityTimeFrame, props)
      }
    }
    
    if (transferExecutor.isEmpty) {
      logger.error("Error instantiating the TF {}",className)
    }

    transferExecutor.isDefined
    
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
   * @param validityTimeFrame: The time frame (msec) to invalidate monitor points
   * @param props: the user defined properties
   * @return The instantiated executor
   *
   */
  private[this] def instantiateExecutor(
    executorClass: Class[_],
    asceId: String,
    asceRunningId: String,
    validityTimeFrame: Long,
    props: Properties): Option[TransferExecutor] = {
    assert(Option(executorClass).isDefined)
    require(Option(asceId).isDefined && asceId.nonEmpty)
    require(Option(asceRunningId).isDefined && asceRunningId.nonEmpty)
    require(validityTimeFrame>0)
    require(Option(props).isDefined)

    // Get the constructor with 3 parameters
    val ctors = executorClass.getConstructors.filter(c => c.getParameterTypes.length == 4)
    logger.debug("Found {} constructors with 4 parameters for class {}",ctors.length.toString,className)
    
    // Get the constructor
    val ctor = ctors.find(c =>
      c.getParameterTypes()(0) == asceId.getClass &&
      c.getParameterTypes()(1) == asceRunningId.getClass &&
      c.getParameterTypes()(2) == validityTimeFrame.getClass &&
      c.getParameterTypes()(3) == props.getClass)
    
    val instance = if (ctor.isEmpty) {
      Failure(new Exception("Constructor for "+className+" NOT found"))
    } else { 
      Try(ctor.map(c => {
        logger.debug("Constructor found for {}",className)
        // The arguments to pass to the constructor
        val args = Array[AnyRef](asceId, asceRunningId, new java.lang.Long(validityTimeFrame),props)
        // Invoke the constructor to build the TransferExecutor
        c.newInstance(args: _*).asInstanceOf[TransferExecutor]
      }))
    }

    val name = if (language==TransferFunctionLanguage.python) javaTFClassForPython else className
    instance match {
      case Success(te) => logger.info("Instance of {} built",name); te
      case Failure(x) => logger.error("Error building the transfer function {}", name,x); None
    }
  }
}

object TransferFunctionSetting {
  
  /**
   * The name of the property to set MaxTolerableTFTime
   */
  val MaxTFTimePropName="org.eso.ias.asce.transfer.maxexectime"
  
  /**
    * If the execution time of the TF is higher than this value (msec)
    * then the state of the ASCE changes to Slow.
    *
    * When the TF is too slow, the ASCE logs a message. It the slowness persists,
    * the ASCE will eventually stop running the TF
   */
  lazy val MaxTolerableTFTime : Int=new SystemProperties().getOrElse(MaxTFTimePropName,"500").toInt // msec
  
  /**
   * The name of the property to set MaxAcceptableSlowDuration
   */
  val MaxAcceptableSlowDurationPropName = "org.eso.ias.asce.transfer.maxtimeinslow"

  /**
   * If the TF is slow responding for more then the amount of seconds
   * of  MaxAcceptableSlowDuration then the TF is marked as broken and 
   * will not be executed anymore
   */
  lazy val MaxAcceptableSlowDuration: Long = new SystemProperties().getOrElse(MaxAcceptableSlowDurationPropName,"5").toInt

  /** The MaxAcceptableSlowDuration in milliseconds */
  lazy val MaxAcceptableSlowDurationMillis:Long = TimeUnit.MILLISECONDS.convert(MaxAcceptableSlowDuration, TimeUnit.SECONDS)

}
