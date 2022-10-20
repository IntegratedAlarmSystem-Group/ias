package org.eso.ias.utils.test

import com.typesafe.scalalogging.Logger
import org.eso.ias.logging.IASLogger
import org.eso.ias.utils.pcache.impl.H2NVCache
import org.scalatest.flatspec.AnyFlatSpec

import java.nio.file.{Files, Paths}

import scala.util.Random

class H2NVCacheTest extends AnyFlatSpec  {

  val logger: Logger = IASLogger.getLogger(H2NVCache.getClass)

  behavior of "The H2 cache"

  it must "Initialize the DB" in {
    logger.info("H2 intialization test started")
    val cache = new H2NVCache()
    val fName = cache.h2FileName+".mv.db"
    assert(Files.exists(Paths.get(fName)), s"H2 file $fName NOT found")
    logger.info("H2 intialization test done")
  }

  it must "store items" in {
    logger.info("H2 store items test started")
    val cache = new H2NVCache()
    val fName = cache.h2FileName+".mv.db"
    // Put random strings in the cache
    for (i <- 1 to 10) {
      val rndStr =  Random.alphanumeric take 10 mkString("")
      assert(cache.put(s"ID$i", rndStr))
    }
    assertResult(10) {cache.size}

    logger.info("H2 store items test done")
  }

}
