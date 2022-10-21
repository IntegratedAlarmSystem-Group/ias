package org.eso.ias.utils.test

import com.typesafe.scalalogging.Logger
import org.eso.ias.logging.IASLogger
import org.eso.ias.utils.pcache.impl.H2NVCache
import org.scalatest.flatspec.AnyFlatSpec

import java.nio.file.{Files, Paths}

import scala.util.Random

class H2NVCacheTest extends AnyFlatSpec  {

  val logger: Logger = IASLogger.getLogger(this.getClass)

  behavior of "The H2 cache"

  it must "Initialize the DB" in {
    logger.info("H2 intialization test started")
    val cache = new H2NVCache()
    val fName = cache.h2FileName+".mv.db"
    assert(Files.exists(Paths.get(fName)), s"H2 file $fName NOT found")
    logger.info("H2 intialization test done")
  }

  it must "store and retrieves items" in {
    logger.info("H2 store and retrieve items test started")
    val cache = new H2NVCache()
    val fName = cache.h2FileName+".mv.db"
    // Put random strings in the cache
    for (i <- 1 to 10) {
      val rndStr =  Random.alphanumeric take 10 mkString("")
      assert(cache.put(s"ID$i", rndStr))
    }
    assertResult(10) {cache.size}

    val idToRetrieve = "ImportantID"
    val stringToRetrieve = "The string to be saved for the important ID"
    assert(cache.put(idToRetrieve, stringToRetrieve))
    assertResult(11) {cache.size}

    // MOre strings in cache
    for (i <- 1 to 150) {
      val rndStr =  Random.alphanumeric take 10 mkString("")
      assert(cache.put(s"ID2-$i", rndStr))
    }
    assertResult(161) {cache.size}

    val retrievedStrOpt = cache.get(idToRetrieve)
    assert(retrievedStrOpt.nonEmpty)
    assert(retrievedStrOpt.get == stringToRetrieve, s"Retrieved string ${retrievedStrOpt.get} differs from submitted string $stringToRetrieve")

    logger.info("H2 store and retrieve items test done")
  }

  it must "Return None if an item is not in cache" in {
    logger.info("H2 returns None for missing ID test started")
    val cache = new H2NVCache()
    val fName = cache.h2FileName+".mv.db"
    // Put random strings in the cache
    for (i <- 1 to 10) {
      val rndStr =  Random.alphanumeric take 10 mkString("")
      assert(cache.put(s"ID$i", rndStr))
    }
    assertResult(10) {cache.size}

    val foundOpt = cache.get("UnknownID")
    assert(foundOpt.isEmpty)

    logger.info("H2 returns None for missing ID test done")
  }

  it must "Delete an entry from the cache" in {
    logger.info("H2 must delete entries test started")
    val cache = new H2NVCache()
    val fName = cache.h2FileName+".mv.db"
    // Put random strings in the cache
    for (i <- 1 to 10) {
      val rndStr =  Random.alphanumeric take 10 mkString("")
      assert(cache.put(s"ID$i", rndStr))
    }
    assertResult(10) {cache.size}

    val idToRemove = "GhostID"
    val strToWrite = "The value of an object to delete"
    assert(cache.put(idToRemove, strToWrite))

    // Put some more items in cache
    for (i <- 11 to 25) {
      val rndStr =  Random.alphanumeric take 10 mkString("")
      assert(cache.put(s"ID$i", rndStr))
    }
    assertResult(26) {cache.size}

    assert(cache.del(idToRemove))
    assertResult(25) {cache.size}
    assert(cache.get(idToRemove).isEmpty)

    // Try to delete something that does not exist
    assert(!cache.del("Unknown key"))
    assertResult(25) {cache.size}

    logger.info("H2 must delete entries test done")
  }

}
