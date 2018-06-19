package org.eso.ias.sink

import java.util.concurrent.{Callable, ExecutorCompletionService, Executors}

import org.eso.ias.logging.IASLogger
import org.eso.ias.types.Identifier
import org.eso.ias.types.IdentifierType
import org.eso.ias.cdb.CdbReader
import org.eso.ias.cdb.json.CdbFiles
import org.eso.ias.cdb.json.CdbJsonFiles
import org.eso.ias.cdb.json.JsonReader
import org.eso.ias.cdb.pojos.IasioDao
import org.eso.ias.cdb.rdb.RdbReader
import org.eso.ias.kafkautils.KafkaHelper

import scala.collection.mutable.{Map => MutableMap}

/**
 * The IasValueProcessor gets all the IasValues published in the BSDB
 * and sends them to the listener for further processing.
  *
  * @param processorIdentifier the idenmtifier of the value processor
  * @param listeners the processors of the IasValues read from the BSDB
 */
class IasValueProcessor(
                         val processorIdentifier: Identifier,
                         val listeners: List[ValueListener],
                         val iasioDaos: Set[IasioDao]) {
  require(Option(processorIdentifier).isDefined,"Invalid identifier")
  require(Option(listeners).isDefined && listeners.nonEmpty,"Mo listener defined")
  require(listeners.map(_.id).toSet.size==listeners.size,"Duplicated IDs of listeners")
  require(Option(iasioDaos).isDefined && iasioDaos.nonEmpty,"No IASIOs from CDB")

  /** The executor service to async process the IasValues in the listeners */
  val executorService = new ExecutorCompletionService[Unit](
    Executors.newFixedThreadPool(2 * listeners.size, new ProcessorThreadFactory(processorIdentifier.id)))

  // The listeners in the map are the ones to run process the IasValues:
  // when a listener throws an exception, it is removed from the Map
  val validListeners: MutableMap[String, ValueListener] = MutableMap.empty
  listeners.foreach(listener => validListeners.put(listener.id,listener))

  def initListeners(): Unit = {
    IasValueProcessor.logger.debug("Initializing the listeners")
    val callables: List[Callable[Unit]] = listeners.map(listener => {
      new Callable[Unit] {
        override def call(): Unit = listener.init()
      }
    })
    val futures = callables.map(task => executorService.submit(task))

  }

  def closeListeners(): Unit = {
    IasValueProcessor.logger.debug("Closing the listeners")
  }
}

object IasValueProcessor {
  
  /** The logger */
  val logger = IASLogger.getLogger(classOf[IasValueProcessor])
  
  /** Build the usage message */
  def printUsage() = {
		"""Usage: IasValueProcessor Processor-ID [-jcdb JSON-CDB-PATH]
		-jcdb force the usage of the JSON CDB
		   * Processor-ID: the identifier of the IasValueProcessor
		   * JSON-CDB-PATH: the path of the JSON CDB"""
	}
  
  def main(args: Array[String]) = {
    require(!args.isEmpty, "Missing identifier in command line")
    require(args.size == 1 || args.size == 3, "Invalid command line params\n" + printUsage())
    require(if(args.size == 3) args(1)=="-jcdb" else true, "Invalid command line params\n" + printUsage())
    val processorId = args(0)
    // The identifier of the supervisor
    val identifier = new Identifier(processorId, IdentifierType.SINK, None)
    
    val reader: CdbReader = {
      if (args.size == 3) {
        logger.info("Using JSON CDB at {}",args(2))
        val cdbFiles: CdbFiles = new CdbJsonFiles(args(2))
        new JsonReader(cdbFiles)

      } else {
        logger.info("Using CDB RDB")
        new RdbReader()
      }
    }
    
    val kafkaBrokers = {
      // The brokers from the java property
      val fromPropsOpt=Option(System.getProperties().getProperty(KafkaHelper.BROKERS_PROPNAME))
      val iasDaoJOptional = reader.getIas
      val fromCdbOpt = if (iasDaoJOptional.isPresent) {
        Option(iasDaoJOptional.get().getBsdbUrl)
      } else {
        None
      }

      fromPropsOpt.getOrElse(fromCdbOpt.getOrElse(KafkaHelper.DEFAULT_BOOTSTRAP_BROKERS))
    }
  }
}