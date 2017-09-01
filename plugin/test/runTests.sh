#!/usr/bin/bash
iasRun.py -l j org.junit.runner.JUnitCore org.eso.ias.plugin.test.config.JsonConfigReaderTest
iasRun.py -l j org.junit.runner.JUnitCore org.eso.ias.plugin.test.filter.TestFilterBase
iasRun.py -l j org.junit.runner.JUnitCore org.eso.ias.plugin.test.filter.NoneFilterTest
iasRun.py -l j org.junit.runner.JUnitCore org.eso.ias.plugin.test.publisher.PublisherBaseTest
iasRun.py -l j org.junit.runner.JUnitCore org.eso.ias.plugin.test.publisher.BufferedPublisherBaseTest
iasRun.py -l j -Dorg.eso.ias.plugin.buffersize=10 org.junit.runner.JUnitCore org.eso.ias.plugin.test.publisher.PublisherMaxBufferSizeTest
iasRun.py -l j org.junit.runner.JUnitCore org.eso.ias.plugin.test.publisher.BufferedPublisherStressTest
iasRun.py -l j org.junit.runner.JUnitCore org.eso.ias.plugin.test.publisher.PublisherStressTest
iasRun.py -l j org.junit.runner.JUnitCore org.eso.ias.plugin.test.stats.DetailedStatsTest
iasRun.py -l j org.junit.runner.JUnitCore org.eso.ias.plugin.test.publisher.BufferedMonitoredSystemDataTest
iasRun.py -l j org.junit.runner.JUnitCore org.eso.ias.plugin.test.publisher.JsonPusblisherTest
iasRun.py -l j -Dorg.eso.ias.plugin.kafka.partition=0 org.junit.runner.JUnitCore org.eso.ias.plugin.test.publisher.KafkaPublisherTest
iasRun.py -l j org.junit.runner.JUnitCore org.eso.ias.plugin.test.MonitoredValueTest
iasRun.py -l j org.junit.runner.JUnitCore org.eso.ias.plugin.test.JsonConversionTest
