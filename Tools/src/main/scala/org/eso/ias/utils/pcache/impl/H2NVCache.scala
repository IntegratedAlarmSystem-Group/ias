package org.eso.ias.utils.pcache.impl

import com.typesafe.scalalogging.Logger
import org.eso.ias.logging.IASLogger
import org.eso.ias.utils.pcache.NonVolatileCache
import org.h2.tools.DeleteDbFiles
import org.h2.jdbc.JdbcSQLIntegrityConstraintViolationException

import java.sql.{Connection, DriverManager}
import java.util.Objects
import scala.util.Random
import scala.compiletime.uninitialized

/**
 * A non volatile cache of objects persisted with H2 (https://www.h2database.com/html/main).
 *
 * The URl allows to create the DB in,memory, on a file on disk, or to connect to a remote DB:
 * see [[https://www.h2database.com/html/features.html#database_url H2 URL]] for a detailed discussion.
 *
 * Objects of this class cannot be instantiated directly but must be instantiate with one of the
 * case classes provided in the companion object.
 *
 * This class is not thread safe.
 *
 * @param dbUrl The URL of the DB (see H2 for the format)
 * @param userName The user name to log into the RDMS
 * @param password The password for the login
 */
abstract sealed class H2NVCache(val dbUrl: String, val userName: String, val password: String) extends NonVolatileCache {
  require(Objects.nonNull(dbUrl) && dbUrl.trim.nonEmpty,"Invalid H2 URL")
  H2NVCache.logger.info("H2 database URL: {}",dbUrl)

  /** The connection to the RDBMS */
  private var conn: Connection = uninitialized

  init()

  /** The name of the file(s) used by H2 */
  val h2FileName: String = dbUrl.split(":")(2)
  H2NVCache.logger.info("H2 file: {}",h2FileName)

  /** Initialize the database */
  private def init(): Unit = {
    H2NVCache.logger.debug("Initializing H2 DB")
    conn = DriverManager.getConnection(dbUrl, userName, password)
    H2NVCache.logger.debug("Creating the table in the H2 DB")
    if (isTableCreated()) {
      H2NVCache.logger.info("Table already present in the DB")
    } else {
      H2NVCache.logger.info("Creating table {} the DB", H2NVCache.tableName)
      val stmt = conn.createStatement()
      val ret = stmt.executeUpdate(H2NVCache.buildDbSqlStatement)
      stmt.close()
    }
    
    H2NVCache.logger.info("H2 DB initialized")
  }

  def shutdown(): Unit

  /**
    * Check if the table already exists in the DB
    *
    * @return true if the table exists, false otherwise
    */
  def isTableCreated(): Boolean = {
    val sqlStr = s"SELECT * FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_NAME = '${H2NVCache.tableName}'"
    val stmt = conn.createStatement()
    val ret = stmt.execute(sqlStr)
    val rs = stmt.getResultSet
    rs.first() // If the table returned one row then the table exists
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
    require(key.nonEmpty && value.nonEmpty)
    val sqlStmtStr = s"INSERT INTO ${H2NVCache.tableName} VALUES('$key', ${System.currentTimeMillis()}, '$value');"
    try {
      val stmt = conn.createStatement()
      val ret = try {
        stmt.executeUpdate(sqlStmtStr)
      } catch {
        JdbcSQLIntegrityConstraintViolationException => { // Object already in cache
          // SQL update instead
          val sqlStmtStr = s"UPDATE ${H2NVCache.tableName} SET ${H2NVCache.timeStampColName}=${System.currentTimeMillis()}, ${H2NVCache.jsonStrColName}='$value' WHERE ${H2NVCache.idColName}='$key';"
          stmt.executeUpdate(sqlStmtStr)
        }
      }
      stmt.close()
      H2NVCache.logger.debug("INSERT returned {}",ret)
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
   * @param key The key of the object
   * @return The Object in the cache if it exists, empty otherwise
   */
  override def get(key: String): Option[String] = {
    require(key.nonEmpty)
    val stmt = conn.createStatement()
    val sqlStmtStr = s"SELECT ${H2NVCache.jsonStrColName} FROM ${H2NVCache.tableName} WHERE ${H2NVCache.idColName}='$key';"
    stmt.execute(sqlStmtStr)
    val rs = stmt.getResultSet
    if (rs.first()) {
      Some(rs.getString(1))
    } else {
      None
    }
  }

  /**
   * Remove an object from the cache
   *
   * @param key The key of the object to remove
   * @return True if the object has been removed,
   *         False otherwise (for example the object was not in the cache)
   */
  override def del(key: String): Boolean = {
    require(key.nonEmpty)
    val stmt = conn.createStatement()
    val sqlStmtStr = s"DELETE FROM ${H2NVCache.tableName} WHERE ${H2NVCache.idColName}='$key';"
    val ret = stmt.executeUpdate(sqlStmtStr)
    stmt.close()
    ret==1
  }

  /** @return the number of objects in the cache (both in memory and persisted) */
  override def size: Int = {
    val stmt = conn.createStatement()
    stmt.execute(s"SELECT COUNT(*) as COUNT_ROW FROM ${H2NVCache.tableName};")
    val rs = stmt.getResultSet
    rs.first()
    val res = rs.getInt(1)
    stmt.close()
    res
  }

  /** Collects all keys of this map in a set */
  override def keySet: Set[String] = {
    val stmt = conn.createStatement()
    stmt.execute(s"SELECT ${H2NVCache.idColName} FROM ${H2NVCache.tableName};")
    val rs = stmt.getResultSet

    val ret =new Iterator[String] {
      def hasNext = rs.next()
      def next() = rs.getString(1)
    }.toSet
    stmt.close()
    ret
  }

  /** Empty the cache */
  override def clear(): Unit = {
    val stmt = conn.createStatement()
    stmt.execute(s"DELETE FROM ${H2NVCache.tableName};")
    stmt.close()
  }
}

object H2NVCache {
  /** The logger */
  val logger: Logger = IASLogger.getLogger(H2NVCache.getClass)

  /** The name of the table in the DB */
  val tableName = "CACHE"

  /** The name of the column with the ID */
  val idColName = "ID"

  /** The name of the column for the timestamp */
  val timeStampColName = "TSTAMP"

  /** The name of teh column with the JSPON string */
  val jsonStrColName = "JSONTSTR"

  /** The user name for default args */
  val defaultUserName = "ias-user"

  /** The password for default args */
  val defaultPassword = ""

  /** The statement to build a DB for the cache */
  val buildDbSqlStatement: String = s"CREATE TABLE $tableName (" +
    s"$idColName VARCHAR(255) PRIMARY KEY, " +
    s"$timeStampColName BIGINT NOT NULL, " +
    s"$jsonStrColName VARCHAR(2048) NOT NULL);"

  /**
    * Connection data for the H2 DB on file
    *
    * @param folderName The folder where the files of the DB will be stored
    * @param fileName The file name used by H2
    * @param h2DbUrl the H2 UR
    */
  case class H2FileConnectionData(val folderName: String, val fileName: String, val h2DbUrl: String)

  /** 
   * Build a random Url for a H2 file DB in a temporary folder
   * 
   * @return a random Url for the H2 database on file 
   */
  def generateRndFileH2Url(): H2FileConnectionData = {
    val tmpFolder = Option(System.getProperty("ias.tmp.folder")).getOrElse(System.getProperty("java.io.tmpdir"))
    val rndStr =  Random.alphanumeric take 10 mkString("")
    buildH2Url(tmpFolder,s"$rndStr-${System.currentTimeMillis()}")
  }

  /**
    * Build the H2 URL for a file
    *
    * @param folder The folder where the files of the DB will be stored
    * @param fileName The file name used by H2
    * @return a tuple composed of the folder name, the file name and the H2 URL
    */  
  def buildH2Url(folder: String, fileName: String): H2FileConnectionData = 
    H2FileConnectionData(folder, fileName, s"jdbc:h2:$folder/$fileName")

  /** 
   * A cache on file in the local file system. It deletes the DB in the file on exit. 
   *
   * The file will be created in a temporary folder (IAS_TMP or, if not defined
   * the temporary folder as returned by the java.io.tmpdir property)
   */
  case class H2FileCache(val connectionData: H2FileConnectionData = generateRndFileH2Url()) 
  extends H2NVCache(dbUrl = connectionData.h2DbUrl, userName = defaultUserName , password = defaultPassword ) {
    sys.addShutdownHook(shutdown())
    

    override def shutdown(): Unit = {
      DeleteDbFiles.execute(connectionData.folderName, connectionData.fileName, false)
    }
  }

  /** 
   * A cache on file in the local file system that does not delete the DB on exit
   *
   * It connects to the DB on file if it already exists so the same DB on file
   * can be shared by different apps.
   * 
   * @param folder The folder where the DB files are
   * @param fileName The name of the H@ file
   * @param userName User name (optional)
   * @param password Password (optional)
   */
  case class H2SharedFileCache(
    val folder: String, 
    val fileName: String,
    override val userName: String = defaultUserName, 
    override val password: String = defaultPassword)
  extends H2NVCache(buildH2Url(folder, fileName).h2DbUrl, userName, password){
    override def shutdown(): Unit = {}
  }
}
