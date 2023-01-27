#!/usr/bin/bash
testCreateModule
export IAS_EXTERNAL_JARS="$PWD/externalJars" && testCommonDefs
iasRun -r org.scalatest.run org.eso.ias.utils.test.ISO8601Test
iasRun -r org.scalatest.run org.eso.ias.utils.test.H2NVCacheTest
iasRun -r org.scalatest.run org.eso.ias.utils.test.InMemoryCacheTest
iasRun -r org.scalatest.run org.eso.ias.utils.test.PCacheTest
