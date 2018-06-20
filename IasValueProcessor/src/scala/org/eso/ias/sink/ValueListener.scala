package org.eso.ias.sink

import java.util.concurrent.atomic.AtomicBoolean

import org.eso.ias.cdb.pojos.IasioDao
import org.eso.ias.logging.IASLogger
import org.eso.ias.types.IASValue

import scala.util.{Failure, Success, Try}

/**
  * The listener of IasValues to be processed
  *
  * @param id The identifier to distinguish between many listeners int he same processor
  *           Mainly used for logging messages
  */
abstract class ValueListener(val id: String) {
  require(Option(id).isDefined && !id.isEmpty,"Invalid listener id")

  /** The logger */
  val logger = IASLogger.getLogger(classOf[ValueListener])

  /** The listener has been initialized  */
  val initialized = new AtomicBoolean(false)

  /** The listener has been closed */
  val closed = new AtomicBoolean(false)

  /** The configuration of the IASIOs from the CDB */
  var iasValuesDaos: Map[String,IasioDao] = Map.empty

  /**
    * If one of the method of the listener threw one execption
    * we falg the event and the listener stop processing values
    */
  private val broken = new AtomicBoolean(false)

  /**
    * @return true if the processor is broke; false otherwise
    */
  def isBroken: Boolean = broken.get()

  /**
    * Initialization
    *
    * @param iasValues The configuration of the IASIOs read from the CDB
    */
  final def setUp(iasValues: Map[String,IasioDao]): Unit = {
    assert(!initialized.get(),"Already initialized")
    assert(Option(iasValues).isDefined && iasValues.nonEmpty,"Invalid IASIOs configuration")
    iasValuesDaos=iasValues
    logger.debug("Initilizing listener {}",id)
    Try(init()) match {
      case Success(_) =>logger.info("Listener {} initialized",id)
      case Failure(e) => logger.error("Listener {} failed to init",id,e)
                         broken.set(true)
    }
  }

  /**
    * Initialization
    */
  protected def init()

  final def tearDown(): Unit = {
    val alreadyClosed = closed.getAndSet(true)
    if (alreadyClosed) {
      Try(logger.warn("{} already closed",id))
    } else {
      logger.debug("Closing {}",id)
      Try(close()) match {
        case Success(_) => logger.info("Listener {} successfully closed",id)
        case Failure(e) => logger.warn("Listener {} failed to close",id,e)
                           broken.set(true)
      }
    }
  }

  /**
    * Free all the allocated resources
    */
  protected def close()

  /**
    * A new set of IasValues has been received from the BSDB and needs to be processed
    *
    * @param iasValues the values read from the BSDB
    */
  final def processIasValues(iasValues: List[IASValue[_]]): Unit = {
    assert(initialized.get(),"Not initialized")
    if (!closed.get() && Option(iasValues).isDefined && iasValues.nonEmpty && !broken.get()) {
      Try(process(iasValues)) match {
        case Failure(e) => logger.error("Listener {} failed to process events: will stop processing events",id,e)
                           broken.set(true)
        case _ =>
      }
    }
  }

  /**
    * Process the IasValues read from the BSDB
    *
    * @param iasValues the values read from the BSDB
    */
  protected def process(iasValues: List[IASValue[_]])
}
