package org.eso.ias.utils.test

import com.typesafe.scalalogging.Logger
import org.eso.ias.logging.IASLogger
import org.eso.ias.utils.pcache.impl.InMemoryCacheImpl
import org.scalatest.flatspec.AnyFlatSpec

import java.nio.file.{Files, Paths}
import scala.util.Random

class InMemoryCacheTest extends AnyFlatSpec {

  val logger: Logger = IASLogger.getLogger(this.getClass)

   behavior of "The the in-memory cache"

  it must "store and retrieves items" in {
    logger.info("Store and retrieve items test started")
    val cache = new InMemoryCacheImpl()

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

    logger.info("Store and retrieve items test done")
  }

  it must "Return None if an item is not in cache" in {
    logger.info("Returns None for missing ID test started")
    val cache = new InMemoryCacheImpl()

    // Put random strings in the cache
    for (i <- 1 to 10) {
      val rndStr =  Random.alphanumeric take 10 mkString("")
      assert(cache.put(s"ID$i", rndStr))
    }
    assertResult(10) {cache.size}

    val foundOpt = cache.get("UnknownID")
    assert(foundOpt.isEmpty)

    logger.info("Returns None for missing ID test done")
  }

  it must "Delete an entry from the cache" in {
    logger.info("Delete entries test started")
    val cache = new InMemoryCacheImpl()

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

    logger.info("Delete entries test done")
  }

  it must "Reject to store elements when the limit (num. objects) has been reached" in {
    logger.info("Test memory limits (#objects) started")

    val cache = new InMemoryCacheImpl(10)
    assert(cache.maxSize==10)
    assert(cache.maxMemSizeBytes==0)
    // Put random strings in the cache
    for (i <- 1 to 10) {
      val rndStr =  Random.alphanumeric take 10 mkString("")
      assert(cache.put(s"ID$i", rndStr))
    }
    assertResult(10) {cache.size}

    val idToRejected = "RejectedID"
    val strToWrite = "Item ti be rejected (limit reached)"
    assert(!cache.put(idToRejected, strToWrite))
    assertResult(10) {cache.size}

    assert(cache.get(idToRejected).isEmpty)

    logger.info("Test memory limits (#objects) done")

  }

  it must "Reject to store elements when the limit (memory size) has been reached" in {
    logger.info("Test memory limits (mem size) started")

    val cache = new InMemoryCacheImpl(0, 1)
    assert(cache.maxSize==0)
    assert(cache.maxMemSizeBytes>0)
    assert(cache.memSize==0)

    // Put random strings in the cache without passing the memory limit
    val increment = 250
    var idn=0
    while (cache.memSize < cache.maxMemSizeBytes) {
      val rndStr =  Random.alphanumeric take increment mkString("")
      assert(cache.put(s"ID$idn", rndStr))
      idn+=1
    }

    // Adding the next item shall pass the allowed memory limit and be rejected
    val sz = cache.size
    val idToRejected = "RejectedID"
    val strToWrite = Random.alphanumeric take increment mkString("")
    assert(!cache.put(idToRejected, strToWrite))
    assertResult(sz) {cache.size}

    assert(cache.get(idToRejected).isEmpty)

    logger.info("Test memory limits (mem size) done")
  }

}
