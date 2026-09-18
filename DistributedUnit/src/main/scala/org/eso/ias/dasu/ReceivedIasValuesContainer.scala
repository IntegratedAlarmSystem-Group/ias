package org.eso.ias.dasu

import scala.collection.mutable.{HashMap, Map=>MutableMap}

import org.eso.ias.types.IASValue
import org.eso.ias.logging.IASLogger

/**
 * The container of the received IAS values.
 * 
 * Objects of this class, contain thne IasValues received from the BSDB
 * waiting to be processed by the DASU. The container is thread safe.
 * 
 * @param dasuId the identifier of the DASU
 * @param acceptedIds the set of the IDs of the IasValues accepted by the DASU
 */
class ReceivedIasValuesContainer(
    val dasuId: String,
    val acceptedIds: Set[String]) {
    require(Option(dasuId).exists(_.nonEmpty), "Invalid ID of the DASU")
    require(Option(acceptedIds).exists(_.nonEmpty), "Invalid set of accepted IDs of the DASU")

    ReceivedIasValuesContainer.logger.debug("Inputs container for DASU [{}] accepts values with IDs: {}",
        dasuId,
        acceptedIds.mkString(", "))
    

    /**
    * Values that have been received in input from plugins or other DASUs (BSDB)
    * and not yet processed by the ASCEs
    *
    * This map must be taken synchronized because it is accessed by several threads
    */
    val notYetProcessedInputs: MutableMap[String,IASValue[?]] = new HashMap[String,IASValue[?]]()

    def clear(): Unit = synchronized {
        notYetProcessedInputs.clear()
    }

    /**
     * Store the received IAS values in the container
     *
     * @param iasVaslues the IAS values to store
     */
    def storeInputs(iasValues: Iterable[IASValue[?]]): Unit = synchronized {
        
        def acceptIasValue(value: IASValue[?]): Boolean = {
        // Accept the value if
        //  * its ID is the ID of an input
        //  * its timetsamp is newer that that already in the map of inputs to process
        assert(Option(value).isDefined)
        assert(value.productionTStamp.isPresent,"Undefined production timestamp for "+value.toString)
        

        val valueFromMap: Option[IASValue[?]] = notYetProcessedInputs.get(value.id)

        acceptedIds.contains(value.id) && valueFromMap.map (v => {

            val valueTstamp = value.productionTStamp.get
            val tstampOfValueInMap = v.productionTStamp.get()

            valueTstamp>=tstampOfValueInMap
        }).getOrElse(true) // Not in map: accept the value
        }


        // Merge the inputs with the buffered ones to keep only the last updated values
        iasValues.filter( acceptIasValue(_)).foreach(iasio => {
            notYetProcessedInputs.put(iasio.id, iasio)
        })
    }

    /**
     * Check if there are received IAS values not yet processed
     *
     * @return true if there are received IAS values not yet processed
     */
    def nonEmpty: Boolean = synchronized {
        notYetProcessedInputs.nonEmpty
    }

    /**
     * Check if there are received IAS values not yet processed
     *
     * @return true if there are received IAS values not yet processed
     */
    def isEmpty: Boolean = synchronized {
        notYetProcessedInputs.isEmpty
    }

    /**
     * Get the number of received IAS values not yet processed
     *
     * @return the number of received IAS values not yet processed
     */
    def size: Int = synchronized {
        notYetProcessedInputs.size
    }

    def getValuesAndClear(): Set[IASValue[?]] = synchronized {
        val ret = notYetProcessedInputs.values.toSet
        notYetProcessedInputs.clear()
        ret
    }

    /**
      * Get the full running IDs of the received IAS values not yet processed
      *
      * @return a map of id, full running IDs of the received IAS values in the containers
      */
    def getFullRunningIds: Map [String, String] = synchronized {
        notYetProcessedInputs.values.map(v => v.id -> v.fullRunningId).toMap
    }
}

object ReceivedIasValuesContainer {
  /** The logger */
  private val logger = IASLogger.getLogger(this.getClass)
}
