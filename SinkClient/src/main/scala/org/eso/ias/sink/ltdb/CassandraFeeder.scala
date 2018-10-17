package org.eso.ias.sink.ltdb
import org.eso.ias.types.IASValue

/**
  * Stores IASIOs in the LTDB implemnted by the Cassandra database
  *
  * @param timeToLeave The time to leave in hours if >0; if 0 no TTL will be set
  */
class CassandraFeeder(timeToLeave: Int) extends DatabaseFeeder {
  /** The time to leave (hours>=0) of the data in the database */
  override val ttl: Integer = timeToLeave
  require(ttl>=0, "Time to leave must be >= 0")

  /** Initialize the database */
  override def init(): Unit = {}

  /** Shutdown and free all the acquired resources */
  override def close(): Unit = {}

  /**
    * Store the passed values in the database.
    *
    * This method is intended to store the passed values in the database without further
    * computation like checking if the value have changed or not.
    * Such kind of computaion must be done by caller of this method.
    *
    * @param iasValues The IASValues to store in the database
    */
  override def store(iasValues: List[IASValue[_]]): Unit = {}
}
