#!/usr/bin/bash
testCreateModule
export IAS_EXTERNAL_JARS="$PWD/externalJars" && testCommonDefs
iasRun org.scalatest.run org.eso.ias.utils.test.ISO8601Test
