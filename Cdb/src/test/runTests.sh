#!/usr/bin/bash
iasRun -r org.junit.platform.console.ConsoleLauncher execute --select-class org.eso.ias.cdb.test.TestTextFileType
iasRun -r org.junit.platform.console.ConsoleLauncher execute --select-class org.eso.ias.cdb.test.TestStructTextReader
iasRun -r org.junit.platform.console.ConsoleLauncher execute --select-class org.eso.ias.cdb.test.TestJsonCdb
iasRun -r org.junit.platform.console.ConsoleLauncher execute --select-class org.eso.ias.cdb.test.TestYamlCdb
iasRun -r org.junit.platform.console.ConsoleLauncher execute --select-class org.eso.ias.cdb.test.TestUnreadableCdb
#iasRun org.junit.platform.console.ConsoleLauncher -c org.eso.ias.cdb.test.rdb.TestRdbCdb
export IAS_EXTERNAL_JARS="src/test/ExtJARS/" && mkdir -p $IAS_EXTERNAL_JARS && iasRun -r org.junit.platform.console.ConsoleLauncher execute --select-class org.eso.ias.cdb.test.CdbReaderFactoryTest
#alchemyTest
testCdbFolders
testCdbTxtFiles
