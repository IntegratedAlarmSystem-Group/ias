#!/usr/bin/bash
iasRun.py -l j org.junit.runner.JUnitCore org.eso.ias.plugin.test.config.JsonConfigReaderTest
iasRun.py -l j org.junit.runner.JUnitCore org.eso.ias.plugin.test.filter.TestFilterBase
iasRun.py -l j org.junit.runner.JUnitCore org.eso.ias.plugin.test.filter.NoneFilterTest

