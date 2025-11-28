#!/usr/bin/bash
test_create_module
export IAS_EXTERNAL_JARS="$PWD/externalJars1:$PWD/externalJars2" && test_common_defs
iasRun -r org.scalatest.run org.eso.ias.utils.test.ISO8601Test
iasRun -r org.scalatest.run org.eso.ias.utils.test.H2NVCacheTest
iasRun -r org.scalatest.run org.eso.ias.utils.test.InMemoryCacheTest
iasRun -r org.scalatest.run org.eso.ias.utils.test.PCacheTest
iasRun -r org.scalatest.run org.eso.ias.utils.test.CircularBufferTest
