package org.eso.ias.cdbchecker.test

import org.eso.ias.cdb.cdbchecker.CdbChecker
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import java.nio.file.{Files, Paths}

/**
  * Tests for the duplicate display tag and duplicate ID checks in CdbChecker.
  *
  * Verifies that when two or more IASIOs share the same getTagToDisplay()
  * value or the same ID, the checker reports an error.
  */
class CdbCheckerDuplicatedTagTest extends AnyFlatSpec with Matchers {

  /** Path to the test CDB parent folder (the folder containing the `CDB` subdirectory) */
  private val testCdbPath = {
    val base = Paths.get(System.getProperty("user.dir"))
    val path = base.resolve("src/test")
    if (Files.exists(path)) path.toString
    else {
      val altPath = base.resolve("src/test/resources")
      if (Files.exists(altPath)) altPath.toString
      else {
        val resource = getClass.getClassLoader.getResource(".")
        if (resource != null) {
          val resourcePath = Paths.get(resource.getPath)
          val parent = resourcePath.getParent()
          if (Files.exists(parent)) parent.toString
          else throw new RuntimeException(s"Test CDB not found. Looked at ${path.toAbsolutePath} and ${altPath.toAbsolutePath}")
        } else throw new RuntimeException(s"Test CDB not found. Looked at ${path.toAbsolutePath} and ${altPath.toAbsolutePath}")
      }
    }
  }

  behavior of "CdbChecker"

  it should "detect duplicated IASIO display tags" in {
    val args = Array("-j", testCdbPath)
    val checker = new CdbChecker(args)
    val hasErrors = checker.check()
    hasErrors shouldBe true
  }

  it should "detect duplicated IASIO IDs" in {
    val args = Array("-j", testCdbPath)
    val checker = new CdbChecker(args)
    val hasErrors = checker.check()
    hasErrors shouldBe true
  }
}
