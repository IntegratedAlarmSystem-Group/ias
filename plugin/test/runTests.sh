#!/usr/bin/bash
iasRun.py -l j org.junit.runner.JUnitCore org.eso.ias.plugin.config.test.JsonConfigReaderTest
iasRun.py -l j org.junit.runner.JUnitCore org.eso.ias.plugin.filter.test.TestFilterBase
iasRun.py -l j org.junit.runner.JUnitCore org.eso.ias.plugin.filter.test.NoneFilterTest

