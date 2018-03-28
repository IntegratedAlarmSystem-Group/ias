#!/usr/bin/bash
iasRun -l j org.junit.runner.JUnitCore org.eso.ias.plugin.test.config.JsonConfigReaderTest
iasRun -l j org.junit.runner.JUnitCore org.eso.ias.plugin.test.filter.TestFilterBase
iasRun -l j org.junit.runner.JUnitCore org.eso.ias.plugin.test.filter.NoneFilterTest
iasRun -l j org.junit.runner.JUnitCore org.eso.ias.plugin.test.publisher.PublisherBaseTest
iasRun -l j org.junit.runner.JUnitCore org.eso.ias.plugin.test.publisher.BufferedPublisherBaseTest
iasRun -l j -Dorg.eso.ias.plugin.buffersize=10 org.junit.runner.JUnitCore org.eso.ias.plugin.test.publisher.PublisherMaxBufferSizeTest
iasRun -l j org.junit.runner.JUnitCore org.eso.ias.plugin.test.publisher.BufferedPublisherStressTest
iasRun -l j org.junit.runner.JUnitCore org.eso.ias.plugin.test.publisher.PublisherStressTest
iasRun -l j org.junit.runner.JUnitCore org.eso.ias.plugin.test.stats.DetailedStatsTest
iasRun -l j org.junit.runner.JUnitCore org.eso.ias.plugin.test.publisher.BufferedMonitoredSystemDataTest
iasRun -l j org.junit.runner.JUnitCore org.eso.ias.plugin.test.publisher.JsonPusblisherTest
iasRun -l j -Dorg.eso.ias.plugin.kafka.partition=0 org.junit.runner.JUnitCore org.eso.ias.plugin.test.publisher.KafkaPublisherTest
iasRun -l j org.junit.runner.JUnitCore org.eso.ias.plugin.test.MonitoredValueTest
iasRun -l j org.junit.runner.JUnitCore org.eso.ias.plugin.test.JsonConversionTest
