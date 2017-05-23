#!/usr/bin/bash
iasRun.py -l j org.junit.runner.JUnitCore org.eso.ias.plugin.test.config.JsonConfigReaderTest
iasRun.py -l j org.junit.runner.JUnitCore org.eso.ias.plugin.test.filter.TestFilterBase
iasRun.py -l j org.junit.runner.JUnitCore org.eso.ias.plugin.test.filter.NoneFilterTest
iasRun.py -l j org.junit.runner.JUnitCore org.eso.ias.plugin.test.publisher.PublisherBaseTest
iasRun.py -l j org.junit.runner.JUnitCore org.eso.ias.plugin.test.publisher.PublisherStressTest
iasRun.py -l j org.junit.runner.JUnitCore org.eso.ias.plugin.test.stats.DetailedStatsTest
