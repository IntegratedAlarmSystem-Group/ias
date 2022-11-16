#!/usr/bin/bash
iasRun org.junit.platform.console.ConsoleLauncher -c org.eso.ias.cdb.test.json.CdbFoldersTest
iasRun org.junit.platform.console.ConsoleLauncher -c org.eso.ias.cdb.test.TestTextFileType
iasRun org.junit.platform.console.ConsoleLauncher -c org.eso.ias.cdb.test.TestStructTextReader
iasRun org.junit.platform.console.ConsoleLauncher -c org.eso.ias.cdb.test.json.TestJsonCdb
iasRun org.junit.platform.console.ConsoleLauncher -c org.eso.ias.cdb.test.json.TestYamlCdb
#iasRun org.junit.platform.console.ConsoleLauncher -c org.eso.ias.cdb.test.rdb.TestRdbCdb
#export IAS_EXTERNAL_JARS="src/test/ExtJARS/" && mkdir -p $IAS_EXTERNAL_JARS && iasRun org.junit.platform.console.ConsoleLauncher -c org.eso.ias.cdb.test.CdbReaderFactoryTest
#alchemyTest
