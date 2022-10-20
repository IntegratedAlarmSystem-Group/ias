package org.eso.ias.utils.pcache.impl

import com.typesafe.scalalogging.Logger
import org.eso.ias.logging.IASLogger
import org.eso.ias.utils.pcache.NonVolatileCache
import org.h2.tools.DeleteDbFiles

import java.sql.{Connection, DriverManager}
import java.util.Objects
import scala.util.Random

/**
 * A non volatile cache of objects persisted with H2 (https://www.h2database.com/html/main.html)
 *
 * If an URL is not provided, H2NVCache builds a database in $IAS_TPM.
 * If an URL is provided, H2NVCache connects to an existing database: this allows to use
 * in-memory databases or share the same DB on disk between different applications
 */
class H2NVCache(dbUrl: String, userName: String="ias-user", password: String="") extends NonVolatileCache {
  require(Objects.nonNull(dbUrl) && dbUrl.trim.nonEmpty,"Invalid H2 URL")
  H2NVCache.logger.info("H2 database URL: {}",dbUrl)

  /** The connection to the RDBMS */
  private var conn: Connection = _

  init
  val h2FileName = dbUrl.split(":")(2)
  H2NVCache.logger.info("H2 file: {}",h2FileName)

  /**
   * Auxiliary constructor, builds the DB in $IAS_TMP or in the
   * system temporary folder
   */
  def this() = {
    this(H2NVCache.generateRndFileName)
  }

  /** Initialize the database */
  private def init: Unit = {
    conn = DriverManager.getConnection(dbUrl, userName, password)
    val stmt = conn.createStatement()
    stmt.execute(H2NVCache.buildDbSqlStatement)
    stmt.close()
  }

  private def shutdown(): Unit = {
    //DeleteDbFiles.execute()
  }

  /**
   * Puts/update a value in the cache
   *
   * @param key   The unique key to identify the object
   * @param value : The string to store in the cache
   * @return true if the object has been stored in the cache,
   *         false otherwise
   */
  override def put(key: String, value: String): Boolean = {
    val sqlStmtStr = s"INSERT INTO ${H2NVCache.tableName} VALUES (key, ${System.currentTimeMillis()}, value);"
    try {
      val stmt = conn.createStatement()
      stmt.execute(sqlStmtStr)
      stmt.close()
      true
    } catch {
      case t: Throwable =>
        H2NVCache.logger.error("Error writing {} in cache",key,t)
        false
    }
  }

  /**
   * Get an object from the cache
   *
   * @param key
   * @return The Object in the cache if it exists, empty otherwise
   */
  override def get(key: String): Option[String] = ???

  /**
   * Remove an object from the cache
   *
   * @param key The key of the object to remove
   * @return True if the object has been removed,
   *         False otherwise (for example the object was not in the cache)
   */
  override def del(key: String): Boolean = ???

  /** @return the number of objects in the cache (both in memory and persisted) */
  override def size: Int = ???
}

object H2NVCache {
  /** The logger */
  val logger: Logger = IASLogger.getLogger(H2NVCache.getClass)

  /** The name of the table in the DB */
  val tableName = "CACHE"

  /** The statement to build a DB for the cache */
  val buildDbSqlStatement = s"CREATE TABLE $tableName (ID VARCHAR(255), TSTAMP BIGINT, JSONSTR VARCHAR(2048), CONSTRAINT PK_ID PRIMARY KEY (ID));"

  /** @return a random file name for the H2 database */
  def generateRndFileName: String = {
    val tmpFolder = Option(System.getProperty("ias.tmp.folder")).getOrElse(System.getProperty("java.io.tmpdir"))
    val rndStr =  Random.alphanumeric take 10 mkString("")
    s"jdbc:h2:$tmpFolder/$rndStr-${System.currentTimeMillis()}"
  }
}
