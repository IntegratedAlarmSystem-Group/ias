package org.eso.ias.utils.test

import org.eso.ias.utils.pcache.PCache
import org.scalatest.funsuite.AnyFunSuite
import com.typesafe.scalalogging.Logger
import org.eso.ias.logging.IASLogger

import scala.util.Random

/** Test [[PCache]] */
class PCacheTest extends AnyFunSuite {
  val logger: Logger = IASLogger.getLogger(this.getClass)

  /**
   * Put items in cache
   * @param itemsToAdd The number of items to add
   * @param cache The cache to put items into
   * @param idPrefix The prefix of the IDs of each element
   * @param strLen The length of the strings to add
   */
  def loadCache(itemsToAdd: Int, cache: PCache, idPrefix:String, strLen: Int=15): Unit = {
    require(itemsToAdd>=0)
    // Put random strings in the cache (should all go in memory
    for (i <- 1 to itemsToAdd) {
      val rndStr =  Random.alphanumeric take strLen mkString("")
      assert(cache.put(s"$idPrefix$i", rndStr))
    }
  }

  test("Store elements in the volatile and non-volatile caches") {
    val cache = PCache(10,0)

    // Put random strings in the cache (should all go in volatile cache)
    loadCache(10,cache,"IDV")
    assertResult(10) {cache.size}
    assert(cache.inMemorySize==10)
    assert(cache.nonVolatileSize==0)
    assert(cache.size==10)

    // Put random strings in the cache (should all go in non-volatile cache)
    loadCache(10,cache,"IDNV")
    assertResult(20) { cache.size }
    assert(cache.inMemorySize == 10)
    assert(cache.nonVolatileSize == 10)
    assert(cache.size==20)
  }

  test("Get elements in the volatile and non-volatile caches") {
    val cache = PCache(15, 0)
    loadCache(25,cache,"ID")
    assert(cache.inMemorySize == 15)
    assert(cache.nonVolatileSize == 10)

    assert(cache.get("ID5").nonEmpty)
    assert(cache.get("ID19").nonEmpty)
  }

  test("Remove elements in the volatile and non-volatile caches") {
    val cache = PCache(55, 0)
    loadCache(100, cache, "ID")
    assert(cache.inMemorySize == 55)
    assert(cache.nonVolatileSize == 45)
    assert(cache.size==100)

    assert(cache.del("ID29"))
    assert(cache.del("ID71"))
    assert(cache.inMemorySize == 54)
    assert(cache.nonVolatileSize == 44)
    assert(cache.size==98)

    assert(cache.get("ID29").isEmpty)
    assert(cache.get("ID71").isEmpty)
  }

  test("Retrieval of all keys") {
    val cache = PCache(55, 0)
    loadCache(100, cache, "ID")
    assert(cache.inMemorySize == 55)
    assert(cache.nonVolatileSize == 45)
    assert(cache.size==100)


    val keys = cache.keySet
    assert(keys.size==100)
    val inMemKeys = cache.inMemoryKeySet
    assert(inMemKeys.size==55)
    val nvKeys = cache.nonVolatileKeySet
    assert(nvKeys.size==45)

    for (x <- 1 to 100) assert(keys.contains(s"ID$x"))
    for (x <- 1 to 55) assert(inMemKeys.contains(s"ID$x"))
    for (x <- 56 to 100) assert(nvKeys.contains(s"ID$x"))
  }

  test("Put many items at once") {
    val cache = PCache(30, 0)
    val items: Seq[(String, String)] = for {
      id <- 1 to 50
      key = s"ID$id"
      value = Random.alphanumeric take 128 mkString("")
    } yield (key, value)
    cache.putAll(items.toList)

    assert(cache.size==50)
    assert(cache.inMemoryCache.size==30)
    assert(cache.nonVolatileCache.size==20)

    for (id <- 1 to 50) assert(cache.get(s"ID$id").nonEmpty)
  }

  test("Clear the cache") {
    val cache = PCache(55, 0)
    loadCache(100, cache, "ID")
     assert(cache.inMemorySize == 55)
    assert(cache.nonVolatileSize == 45)
    assert(cache.size==100)

    cache.clear()
    assert(cache.size==0)

    loadCache(100, cache, "ID")
    assert(cache.inMemorySize == 55)
    assert(cache.nonVolatileSize == 45)
    assert(cache.size==100)
  }
}
