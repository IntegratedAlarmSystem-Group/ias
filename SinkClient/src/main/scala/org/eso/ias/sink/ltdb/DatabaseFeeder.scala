package org.eso.ias.sink.ltdb

import org.eso.ias.types.IASValue

/**
  * DatabaseFeeder decouples the [[LtdbFeeder]] from the actual database.
  */
trait DatabaseFeeder {

  /** The time to leave (hours>=0) of the data in the database */
  val ttl: Integer

  /** Initialize the database */
  def init(): Unit

  /** Shutdown and free all the acquired resources */
  def close(): Unit

  /**
    * Store the passed values in the database.
    *
    * This method is intended to store the passed values in the database without further
    * computation like checking if the value have changed or not.
    * Such kind of computaion must be done by caller of this method.
    *
    * @param iasValues The IASValues to store in the database
    */
  def store(iasValues: List[IASValue[_]])

}
