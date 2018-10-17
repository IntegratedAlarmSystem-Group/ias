package org.eso.ias.sink.test

import java.nio.file.FileSystems

import com.typesafe.scalalogging.Logger
import org.eso.ias.cdb.CdbReader
import org.eso.ias.cdb.json.{CdbJsonFiles, JsonReader}
import org.eso.ias.cdb.pojos.{IasDao, IasioDao}
import org.eso.ias.logging.IASLogger
import org.eso.ias.sink.IasValueProcessor
import org.eso.ias.sink.ltdb.{DatabaseFeeder, LtdbFeeder}
import org.eso.ias.types._
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

  /**
    * Build a IASValue to be stored in the LTDB
    *
    * @param id The identifier of the IASIO
    * @param alarm the value
    * @param mode the mode
    * @param validity the validity
    * @return the IASValue
    */
  def buildValue(id: String, alarm: Alarm, mode: OperationalMode, validity: IasValidity): IASValue[_] = {

    // The identifier of the monitored system
    val monSysId = new Identifier("ConverterID",IdentifierType.MONITORED_SOFTWARE_SYSTEM,None)

    // The identifier of the plugin
    val pluginId = new Identifier("ConverterID",IdentifierType.PLUGIN,Some(monSysId))

    // The identifier of the converter
    val converterId = new Identifier("ConverterID",IdentifierType.CONVERTER,Some(pluginId))

    // The ID of the monitor point
    val inputId = new Identifier(id, IdentifierType.IASIO,converterId)

    // Timestamp
    val tStamp = System.currentTimeMillis()

    IASValue.build(
      alarm,
      mode,
      validity,
      inputId.fullRunningID,
      IASTypes.ALARM,
      tStamp,
      tStamp+1,
      tStamp+5,
      tStamp+10,
      tStamp+15,
      tStamp+20,
      null,
      null,
      null,
      null)
  }

  behavior of "The LtdbFeeder"

  it must "ïnit the databse feeder" in {
    val dbFeeder = new DatabaseFeederForTesting
    val ltdbFeeder: LtdbFeeder = new LtdbFeeder("FeederId",true,dbFeeder)
    ltdbFeeder.setUp(iasDao,iasioDaosMap)
    assert(ltdbFeeder.initialized.get(),"Feeder not initialized")
    assert(dbFeeder.inited,"Database feeder not initialized")
  }

  it must "close the database feeder" in {
    val dbFeeder = new DatabaseFeederForTesting
    val ltdbFeeder: LtdbFeeder = new LtdbFeeder("FeederId",true,dbFeeder)
    ltdbFeeder.setUp(iasDao,iasioDaosMap)
    ltdbFeeder.tearDown()
    assert(ltdbFeeder.closed.get(),"Feeder not closed")
    assert(dbFeeder.closed,"Database feeder not closed")
  }

  it must "not store values before being inited" in {
    val dbFeeder = new DatabaseFeederForTesting
    val ltdbFeeder: LtdbFeeder = new LtdbFeeder("FeederId",true,dbFeeder)

    val values: List[IASValue[_]] = List(
      buildValue("A",Alarm.CLEARED,OperationalMode.OPERATIONAL, IasValidity.RELIABLE),
      buildValue("B",Alarm.CLEARED,OperationalMode.CLOSING, IasValidity.UNRELIABLE),
      buildValue("C",Alarm.CLEARED,OperationalMode.MALFUNCTIONING, IasValidity.RELIABLE))

    ltdbFeeder.processIasValues(values)
    Thread.sleep(IasValueProcessor.defaultPeriodicSendingTimeInterval+100)
    assert(dbFeeder.storedIasios.isEmpty)
    ltdbFeeder.tearDown()
  }

  it must "store values after being inited" in {
    val dbFeeder = new DatabaseFeederForTesting
    val ltdbFeeder: LtdbFeeder = new LtdbFeeder("FeederId",true,dbFeeder)
    ltdbFeeder.setUp(iasDao,iasioDaosMap)
    val values: List[IASValue[_]] = List(
      buildValue("A",Alarm.CLEARED,OperationalMode.OPERATIONAL, IasValidity.RELIABLE),
      buildValue("B",Alarm.CLEARED,OperationalMode.CLOSING, IasValidity.UNRELIABLE),
      buildValue("C",Alarm.CLEARED,OperationalMode.MALFUNCTIONING, IasValidity.RELIABLE))

    ltdbFeeder.processIasValues(values)
    Thread.sleep(IasValueProcessor.defaultPeriodicSendingTimeInterval+100)
    assert(dbFeeder.storedIasios.nonEmpty)
    ltdbFeeder.tearDown()
  }

  it must "not store values after being closed" in {
    val dbFeeder = new DatabaseFeederForTesting
    val ltdbFeeder: LtdbFeeder = new LtdbFeeder("FeederId",true,dbFeeder)
    ltdbFeeder.setUp(iasDao,iasioDaosMap)
    ltdbFeeder.tearDown()
    val values: List[IASValue[_]] = List(
      buildValue("A",Alarm.CLEARED,OperationalMode.OPERATIONAL, IasValidity.RELIABLE),
      buildValue("B",Alarm.SET_HIGH,OperationalMode.CLOSING, IasValidity.UNRELIABLE),
      buildValue("C",Alarm.SET_MEDIUM,OperationalMode.MALFUNCTIONING, IasValidity.RELIABLE))

    ltdbFeeder.processIasValues(values)
    Thread.sleep(IasValueProcessor.defaultPeriodicSendingTimeInterval+100)
    assert(dbFeeder.storedIasios.isEmpty)


  }

  it must "store all IASIOs if on change is disabled" in {
    val dbFeeder = new DatabaseFeederForTesting
    val ltdbFeeder: LtdbFeeder = new LtdbFeeder("FeederId",false,dbFeeder)
    ltdbFeeder.setUp(iasDao,iasioDaosMap)

    // Send the same 3 values, 2 times
    val values: List[IASValue[_]] = List(
      buildValue("A",Alarm.CLEARED,OperationalMode.OPERATIONAL, IasValidity.RELIABLE),
      buildValue("B",Alarm.SET_HIGH,OperationalMode.CLOSING, IasValidity.UNRELIABLE),
      buildValue("C",Alarm.SET_MEDIUM,OperationalMode.MALFUNCTIONING, IasValidity.RELIABLE),
      buildValue("A",Alarm.CLEARED,OperationalMode.OPERATIONAL, IasValidity.RELIABLE),
      buildValue("B",Alarm.SET_HIGH,OperationalMode.CLOSING, IasValidity.UNRELIABLE),
      buildValue("C",Alarm.SET_MEDIUM,OperationalMode.MALFUNCTIONING, IasValidity.RELIABLE))

    ltdbFeeder.processIasValues(values)
    Thread.sleep(IasValueProcessor.defaultPeriodicSendingTimeInterval+100)
    assert(dbFeeder.storedIasios.length==values.length)

    ltdbFeeder.tearDown()
  }

  it must "store changed IASIOs only, if on change is enabled" in {
    val dbFeeder = new DatabaseFeederForTesting
    val ltdbFeeder: LtdbFeeder = new LtdbFeeder("FeederId",true,dbFeeder)
    ltdbFeeder.setUp(iasDao,iasioDaosMap)

    // Send the same 3 values, 2 times
    val values: List[IASValue[_]] = List(
      buildValue("A",Alarm.CLEARED,OperationalMode.OPERATIONAL, IasValidity.RELIABLE),
      buildValue("B",Alarm.SET_HIGH,OperationalMode.CLOSING, IasValidity.UNRELIABLE),
      buildValue("C",Alarm.SET_MEDIUM,OperationalMode.MALFUNCTIONING, IasValidity.RELIABLE),
      buildValue("A",Alarm.CLEARED,OperationalMode.OPERATIONAL, IasValidity.RELIABLE),
      buildValue("B",Alarm.SET_HIGH,OperationalMode.CLOSING, IasValidity.UNRELIABLE),
      buildValue("C",Alarm.SET_MEDIUM,OperationalMode.MALFUNCTIONING, IasValidity.RELIABLE))

    ltdbFeeder.processIasValues(values)
    Thread.sleep(IasValueProcessor.defaultPeriodicSendingTimeInterval+100)
    assert(dbFeeder.storedIasios.length==values.length/2)

    ltdbFeeder.tearDown()
  }



}

object LtdbFeederTest {
  /** The logger */
  val logger: Logger = IASLogger.getLogger(LtdbFeederTest.getClass)
}
