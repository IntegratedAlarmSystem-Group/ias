#!/usr/bin/bash
iasRun org.junit.platform.console.ConsoleLauncher -c org.eso.ias.cdb.test.CdbFoldersTest
iasRun org.junit.platform.console.ConsoleLauncher -c org.eso.ias.cdb.test.TestTextFileType
iasRun org.junit.platform.console.ConsoleLauncher -c org.eso.ias.cdb.test.TestStructTextReader
iasRun org.junit.platform.console.ConsoleLauncher -c org.eso.ias.cdb.test.TestJsonCdb
iasRun org.junit.platform.console.ConsoleLauncher -c org.eso.ias.cdb.test.TestYamlCdb
#iasRun org.junit.platform.console.ConsoleLauncher -c org.eso.ias.cdb.test.rdb.TestRdbCdb
export IAS_EXTERNAL_JARS="src/test/ExtJARS/" && mkdir -p $IAS_EXTERNAL_JARS && iasRun org.junit.platform.console.ConsoleLauncher -c org.eso.ias.cdb.test.CdbReaderFactoryTest
#alchemyTest
