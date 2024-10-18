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
   * The name of the java/scala class to load
   *
   * This is needed because the name of the java class for running python TF is not passed in the constructor
   * but is needed at least for meaningful log messages
   */
  val nameOfClassToLoad =
    if (language==TransferFunctionLanguage.python) classOf[PythonExecutorTF[?]].getName
    else className

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

  override def toString: String = {
    "Transfer function implemented in "+language+" by "+className
  }

  /** Shuts the TF down */
  def shutdown(): Unit = {
    assert(!isShutDown)
    // Init the executor if it has been correctly instantiated
    if (transferExecutor.isDefined) {
      val shutdownThread = threadFactory.newThread(new Runnable() {
        override def run(): Unit = {
          shutdownExecutor(transferExecutor)
       }
      })
      shutdownThread.start()
      // Wait for the termination
      shutdownThread.join(10000)
      if (shutdownThread.isAlive) {
        TransferFunctionSetting.logger.warn("User provided shutdown did not terminate in 10 secs.")
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

    TransferFunctionSetting.logger.info("Initializing the TF {} with language {}",nameOfClassToLoad,language)

    // Load the class
    val tfExecutorClass: Try[Class[?]] =
      if (language==TransferFunctionLanguage.python) {
        TransferFunctionSetting.logger.debug("Loading the java class to run python TF {}",nameOfClassToLoad)
        // In case of python the java class implementing the TF is known
        // so we do not load the class with a forName because in this way
        // the compiler wil actually ensure that this class exists
        Try(classOf[PythonExecutorTF[?]])
      } else {
        TransferFunctionSetting.logger.debug("Loading java class {}",nameOfClassToLoad)
        Try(this.getClass.getClassLoader.loadClass(className))
      }
    transferExecutor = tfExecutorClass match {
      case Failure(e) => TransferFunctionSetting.logger.error("Error loading {}", nameOfClassToLoad,e); None
      case Success(tec) => this.synchronized {
        TransferFunctionSetting.logger.info("Class {} loaded", nameOfClassToLoad)
        instantiateExecutor(tec, asceId, asceRunningId, validityTimeFrame, props)
      }
    }

    if (transferExecutor.isEmpty) {
      TransferFunctionSetting.logger.error("Error instantiating the TF {}",nameOfClassToLoad)
    }

    transferExecutor.isDefined

  }

  /**
   * Shutdown the passed executor
   *
   * @param executor: The executor to shutdown
   */
  private[this] def shutdownExecutor(executor: Option[TransferExecutor]): Unit = {
    require(executor.isDefined)
    try {
      executor.get.shutdown()
    } catch {
        case e: Exception =>
          TransferFunctionSetting.logger.warn("Exception caught shutting down {}",nameOfClassToLoad,e)
      }
  }

  /**
   * Instantiate the executor of the passed class.
   *
   * This method needs to distinguish between python and th eother languages
   * because the java class that delegates to python has one String parameter more to tell
   * which python class to load
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
    executorClass: Class[?],
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
    val ctors =
      if (language==TransferFunctionLanguage.python) executorClass.getConstructors.filter(c => c.getParameterTypes.length == 5)
      else executorClass.getConstructors.filter(c => c.getParameterTypes.length == 4)
    TransferFunctionSetting.logger.debug("Found {} constructors with proper number of args for class {}",ctors.length.toString,nameOfClassToLoad)


    // Get the constructor
    val ctor = if (language==TransferFunctionLanguage.python) {
      ctors.find(c =>
        c.getParameterTypes()(0) == asceId.getClass &&
          c.getParameterTypes()(1) == asceRunningId.getClass &&
          c.getParameterTypes()(2) == validityTimeFrame.getClass &&
          c.getParameterTypes()(3) == props.getClass &&
          c.getParameterTypes()(4)==classOf[String])
    } else {
      ctors.find(c =>
        c.getParameterTypes()(0) == asceId.getClass &&
          c.getParameterTypes()(1) == asceRunningId.getClass &&
          c.getParameterTypes()(2) == validityTimeFrame.getClass &&
          c.getParameterTypes()(3) == props.getClass)
    }

    val instance = if (ctor.isEmpty) {
      Failure(new Exception("Constructor for "+nameOfClassToLoad+" NOT found"))
    } else {
      Try(ctor.map(c => {
        TransferFunctionSetting.logger.debug("Constructor found for {}",nameOfClassToLoad)
        // The arguments to pass to the constructor
        val args: Array[Any] =
          if (language==TransferFunctionLanguage.python)
            Array(asceId, asceRunningId, validityTimeFrame.toLong, props, className)
          else Array(asceId, asceRunningId, validityTimeFrame, props)
        // Invoke the constructor to build the TransferExecutor
        c.newInstance(args*).asInstanceOf[TransferExecutor]
      }))
    }

    instance match {
      case Success(te) => TransferFunctionSetting.logger.info("Instance of {} built",nameOfClassToLoad); te
      case Failure(x) => TransferFunctionSetting.logger.error("Error building the transfer function {}", nameOfClassToLoad,x); None
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

  /** The logger */
  private val logger = IASLogger.getLogger(this.getClass)

}
