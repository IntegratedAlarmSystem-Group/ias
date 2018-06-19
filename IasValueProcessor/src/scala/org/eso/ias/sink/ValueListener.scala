package org.eso.ias.sink

import java.util.concurrent.atomic.AtomicBoolean

import org.eso.ias.cdb.CdbReader
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
    * Initialization
    *
    * @param iasValues The configuration of the IASIOs read from the CDB
    */
  final def setUp(iasValues: Map[String,IasioDao]): Try[Unit] = {
    assert(!initialized.get(),"Already initialized")
    assert(Option(iasValues).isDefined && iasValues.nonEmpty,"Invalid IASIOs configuration")
    iasValuesDaos=iasValues
    Try({
      logger.debug("Initilizing listener {}",id)
      init()
      logger.info("Listener {} initialized",id)
    })
  }

  /**
    * Initialization
    */
  def init()

  final def tearDown(): Try[Unit] = {
    val alreadyClosed = closed.getAndSet(true)
    if (alreadyClosed) {
      Try(logger.warn("{} already closed",id))
    } else {
      Try( {
        logger.debug("Closing {}",id)
        close()
        logger.info("Listener {} closed",id)
      })
    }
  }

  /**
    * Free all the allocated resources
    */
  def close()

  /**
    * A new set of IasValues has been received from the BSDB and needs to be processed
    *
    * @param iasValues the values read from the BSDB
    */
  final def processIasValues(iasValues: Set[IASValue[_]]): Try[Unit] = {
    assert(initialized.get(),"Not initialized")
    Try({
      if (!closed.get() && Option(iasValues).isDefined && iasValues.nonEmpty) process(iasValues)
    })
  }

  /**
    * Process the IasValues read from the BSDB
    *
    * @param iasValues the values read from the BSDB
    */
  def process(iasValues: Set[IASValue[_]])
}
