#!/usr/bin/bash
iasRun -l j org.junit.platform.console.ConsoleLauncher -c org.eso.ias.plugin.test.config.JsonConfigReaderTest
iasRun -l j org.junit.platform.console.ConsoleLauncher -c org.eso.ias.plugin.test.filter.TestFilterBase
iasRun -l j org.junit.platform.console.ConsoleLauncher -c org.eso.ias.plugin.test.filter.NoneFilterTest
iasRun -l j org.junit.platform.console.ConsoleLauncher -c org.eso.ias.plugin.test.filter.AvgBySamplesFilterTest
iasRun -l j org.junit.platform.console.ConsoleLauncher -c org.eso.ias.plugin.test.filter.AvgByTimeFilterTest
iasRun -l j org.junit.platform.console.ConsoleLauncher -c org.eso.ias.plugin.test.filter.TestFilterFactory
iasRun -l j org.junit.platform.console.ConsoleLauncher -c org.eso.ias.plugin.test.publisher.PublisherBaseTest
iasRun -l j org.junit.platform.console.ConsoleLauncher -c org.eso.ias.plugin.test.publisher.BufferedPublisherBaseTest
iasRun -l j -Dorg.eso.ias.plugin.buffersize=10 org.junit.platform.console.ConsoleLauncher -c org.eso.ias.plugin.test.publisher.PublisherMaxBufferSizeTest
iasRun -l j org.junit.platform.console.ConsoleLauncher -c org.eso.ias.plugin.test.publisher.BufferedPublisherStressTest
iasRun -l j org.junit.platform.console.ConsoleLauncher -c org.eso.ias.plugin.test.publisher.PublisherStressTest
iasRun -l j org.junit.platform.console.ConsoleLauncher -c org.eso.ias.plugin.test.stats.DetailedStatsTest
iasRun -l j org.junit.platform.console.ConsoleLauncher -c org.eso.ias.plugin.test.publisher.BufferedMonitoredSystemDataTest
iasRun -l j org.junit.platform.console.ConsoleLauncher -c org.eso.ias.plugin.test.publisher.JsonPusblisherTest
iasRun -l j org.junit.platform.console.ConsoleLauncher -c org.eso.ias.plugin.test.publisher.KafkaPublisherTest
iasRun -l j org.junit.platform.console.ConsoleLauncher -c org.eso.ias.plugin.test.MonitoredValueTest
iasRun -l j org.junit.platform.console.ConsoleLauncher -c org.eso.ias.plugin.test.JsonConversionTest
iasRun -l j org.junit.platform.console.ConsoleLauncher -c org.eso.ias.plugin.test.ReplicatedPluginTest
