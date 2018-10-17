package org.eso.ias.sink.test

import java.nio.file.FileSystems

import com.typesafe.scalalogging.Logger
import org.eso.ias.cdb.CdbReader
import org.eso.ias.cdb.json.{CdbJsonFiles, JsonReader}
import org.eso.ias.cdb.pojos.{IasDao, IasioDao}
import org.eso.ias.logging.IASLogger
import org.eso.ias.sink.ltdb.{DatabaseFeeder, LtdbFeeder}
import org.eso.ias.types.IASValue
import org.scalatest.{BeforeAndAfterAll, FlatSpec}

import scala.collection.{JavaConverters, mutable}

/**
  * Test the [[org.eso.ias.sink.ltdb.LtdbFeeder]]
  */
class LtdbFeederTest extends FlatSpec with BeforeAndAfterAll {

  /**
    * Database feeder used for testing.
    *
    * All the values to store in the davabase will be put in storedIasios
    */
  class DatabaseFeederForTesting extends DatabaseFeeder {

    /** All the IASIOs stored by the feeder */
    var storedIasios: List[IASValue[_]]= List.empty

    /** Signal if the init has been invoked */
    @volatile var inited: Boolean = false

    /** Signal if the close has been invoked */
    @volatile var closed: Boolean = false

    /** The time to leave (hours>=0) of the data in the database */
    override val ttl: Integer = 1

    /** Initialize the database */
    override def init(): Unit = {
      inited=true
      storedIasios= List.empty
    }

    /** Shutdown and free all the acquired resources */
    override def close(): Unit = {
      closed=true
      storedIasios= List.empty
    }

    /**
      * Store the passed values in the database.
      *
      * This method is intended to store the passed values in the database without further
      * computation like checking if the value have changed or not.
      * Such kind of computaion must be done by caller of this method.
      *
      * @param iasValues The IASValues to store in the database
      */
    override def store(iasValues: List[IASValue[_]]): Unit = synchronized {
      storedIasios = storedIasios++iasValues
    }
  }

  /** The configuration of the IAS */
  var iasDao: IasDao = _

  /** Configuration of IASIOs from CDB */
  var iasiosDaos: Set[IasioDao] = _

  /** Map of IASIOs */
  var iasioDaosMap: Map[String, IasioDao] = _

  override def beforeAll(): Unit = {
    // Build the CDB reader
    val cdbParentPath = FileSystems.getDefault.getPath(".");
    val cdbFiles = new CdbJsonFiles(cdbParentPath)
    val cdbReader: CdbReader = new JsonReader(cdbFiles)

    iasiosDaos = {
      val iasiosDaoJOpt = cdbReader.getIasios
      assert(iasiosDaoJOpt.isPresent, "Error getting the IASIOs from the CDB")
      JavaConverters.asScalaSet(iasiosDaoJOpt.get()).toSet
    }

    iasDao = {
      val iasDaoJOpt=cdbReader.getIas
      assert(iasDaoJOpt.isPresent,"IAS config not found in CDB")
      iasDaoJOpt.get()
    }

    iasioDaosMap = {
      val mutableMap: mutable.Map[String, IasioDao] = mutable.Map.empty
      iasiosDaos.foreach(iDao => mutableMap(iDao.getId)=iDao)
      mutableMap.toMap
    }
  }

  behavior of "The LtdbFeeder"

  it must "ïnit the databse feeder" in {
    val dbFeeder = new DatabaseFeederForTesting
    val ltdbFeeder: LtdbFeeder = new LtdbFeeder("FeederId",true,dbFeeder)
    ltdbFeeder.setUp(iasDao,iasioDaosMap)
    assert(ltdbFeeder.initialized.get(),"Feeder not initialized")
    assert(dbFeeder.inited,"Database feeder not initialized")
  }

  it must "close the databse feeder" in {
    val dbFeeder = new DatabaseFeederForTesting
    val ltdbFeeder: LtdbFeeder = new LtdbFeeder("FeederId",true,dbFeeder)
    ltdbFeeder.setUp(iasDao,iasioDaosMap)
    ltdbFeeder.tearDown()
    assert(ltdbFeeder.closed.get(),"Feeder not closed")
    assert(dbFeeder.closed,"Database feeder not closed")
  }



}

object LtdbFeederTest {
  /** The logger */
  val logger: Logger = IASLogger.getLogger(LtdbFeederTest.getClass)
}
