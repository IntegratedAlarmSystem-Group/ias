package org.eso.ias.sink

import java.util.concurrent.atomic.AtomicBoolean

import com.typesafe.scalalogging.Logger
import org.eso.ias.cdb.pojos.{IasDao, IasioDao}
import org.eso.ias.logging.IASLogger
import org.eso.ias.types.IASValue

import scala.util.{Failure, Success, Try}
import scala.compiletime.uninitialized

/**
  * The listener of IasValues to be processed
  *
  * @param id The identifier to distinguish between many listeners in the same processor
  *           Mainly used for logging messages
  */
abstract class ValueListener(val id: String) {
  require(Option(id).isDefined && id.nonEmpty,"Invalid listener id")

  /** The listener has been initialized  */
  val initialized = new AtomicBoolean(false)

  /** The listener has been closed */
  val closed = new AtomicBoolean(false)

  /** The configuration of the IASIOs from the CDB */
  protected var iasValuesDaos: Map[String,IasioDao] = Map.empty

  /** The configuration of the IAS read from the CDB */
  protected var iasDao: IasDao = uninitialized

  /**
    * If one of the method of the listener threw one execption
    * we falg the event and the listener stop processing values
    */
  private val broken = new AtomicBoolean(false)

  /**
    * @return true if the processor is broken; false otherwise
    */
  def isBroken: Boolean = broken.get()

  /**
    * Mark the listener as broken so the it is not run anymore
    *
    * One possibe use case is when the processor detects that the thread is too slow
    * and decide not to run it again
    */
  def markAsBroken(): Unit = broken.set(true)

  /**
    * Initialization
    *
    * @param iasDao The configuration of the IAS read from the CDB
    * @param iasValues The configuration of the IASIOs read from the CDB
    */
  final def setUp(iasDao: IasDao, iasValues: Map[String,IasioDao]): String = {
    require(Option(iasDao).isDefined)
    require(Option(iasValues).isDefined && iasValues.nonEmpty,"Invalid IASIOs configuration")
    val alreadyInited = initialized.getAndSet(true)
    if (alreadyInited) {
      ValueListener.logger.warn("{} already initialized",id)
      id
    } else if (iasValues.isEmpty) {
      ValueListener.logger.warn("{} empty set of IASIO configurations from CDB: inhibited",id)
      broken.set(true)
      throw new Exception("Invalid empty set of IASIO from CDB")
    } else{
      iasValuesDaos=iasValues
      ValueListener.logger.debug("Initializing listener {}",id)
      Try(init()) match {
        case Success(_) =>ValueListener.logger.info("Listener {} initialized",id)
                          id
        case Failure(e) => ValueListener.logger.error("Listener {} failed to init",id,e)
                            broken.set(true)
                            throw e

      }
    }
  }

  /**
    * Initialization
    */
  protected def init(): Unit

  final def tearDown(): String = {
    val alreadyClosed = closed.getAndSet(true)
    if (alreadyClosed) {
      Try(ValueListener.logger.warn("{} already closed",id))
      id
    } else {
      ValueListener.logger.debug("Closing {}",id)
      Try(close()) match {
        case Success(_) => ValueListener.logger.info("Listener {} successfully closed",id)
                           id
        case Failure(e) => ValueListener.logger.warn("Listener {} failed to close",id,e)
                           broken.set(true)
                           throw e
      }
    }
  }

  /**
    * Free all the allocated resources
    */
  protected def close(): Unit

  /**
    * A new set of IasValues has been received from the BSDB and needs to be processed
    *
    * @param iasValues the values read from the BSDB
    */
  final def processIasValues(iasValues: List[IASValue[?]]): String = {
    assert(Option(iasValues).isDefined,"Invalid empty list of values to process")
    if (initialized.get && !closed.get() && iasValues.nonEmpty && !broken.get()) {
      Try(process(iasValues)) match {
        case Failure(e) => ValueListener.logger.error("Listener {} failed to process events: will stop processing events",id,e)
                           broken.set(true)
                           throw e
        case _ => id
      }
    } else {
      id
    }
  }

  /**
    * Process the IasValues read from the BSDB
    *
    * @param iasValues the values read from the BSDB
    */
  protected def process(iasValues: List[IASValue[?]]): Unit
}

object ValueListener {
  /** The logger */
  val logger: Logger = IASLogger.getLogger(ValueListener.getClass)
}