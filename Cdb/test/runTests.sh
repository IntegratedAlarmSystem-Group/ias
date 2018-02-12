#!/usr/bin/bash
iasRun -l j org.junit.runner.JUnitCore org.eso.ias.cdb.test.json.CdbFoldersTest
iasRun -l j org.junit.runner.JUnitCore org.eso.ias.cdb.test.json.TestJsonCdb
iasRun -l j org.junit.runner.JUnitCore org.eso.ias.cdb.test.rdb.TestRdbCdb
