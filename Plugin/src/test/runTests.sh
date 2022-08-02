#!/usr/bin/bash
iasRun org.junit.platform.console.ConsoleLauncher -c org.eso.ias.plugin.test.config.JsonConfigReaderTest
iasRun org.junit.platform.console.ConsoleLauncher -c org.eso.ias.plugin.test.filter.TestFilterBase
iasRun org.junit.platform.console.ConsoleLauncher -c org.eso.ias.plugin.test.filter.NoneFilterTest
iasRun org.junit.platform.console.ConsoleLauncher -c org.eso.ias.plugin.test.filter.AvgBySamplesFilterTest
iasRun org.junit.platform.console.ConsoleLauncher -c org.eso.ias.plugin.test.filter.AvgByTimeFilterTest
iasRun org.junit.platform.console.ConsoleLauncher -c org.eso.ias.plugin.test.filter.TestFilterFactory
iasRun org.junit.platform.console.ConsoleLauncher -c org.eso.ias.plugin.test.publisher.PublisherBaseTest
iasRun org.junit.platform.console.ConsoleLauncher -c org.eso.ias.plugin.test.publisher.BufferedPublisherBaseTest
iasRun -Dorg.eso.ias.plugin.buffersize=10 org.junit.platform.console.ConsoleLauncher -c org.eso.ias.plugin.test.publisher.PublisherMaxBufferSizeTest
iasRun org.junit.platform.console.ConsoleLauncher -c org.eso.ias.plugin.test.publisher.BufferedPublisherStressTest
iasRun org.junit.platform.console.ConsoleLauncher -c org.eso.ias.plugin.test.publisher.PublisherStressTest
iasRun org.junit.platform.console.ConsoleLauncher -c org.eso.ias.plugin.test.stats.DetailedStatsTest
iasRun org.junit.platform.console.ConsoleLauncher -c org.eso.ias.plugin.test.publisher.BufferedMonitoredSystemDataTest
iasRun org.junit.platform.console.ConsoleLauncher -c org.eso.ias.plugin.test.publisher.JsonPusblisherTest
iasRun org.junit.platform.console.ConsoleLauncher -c org.eso.ias.plugin.test.publisher.KafkaPublisherTest
iasRun org.junit.platform.console.ConsoleLauncher -c org.eso.ias.plugin.test.MonitoredValueTest
iasRun org.junit.platform.console.ConsoleLauncher -c org.eso.ias.plugin.test.JsonConversionTest
iasRun org.junit.platform.console.ConsoleLauncher -c org.eso.ias.plugin.test.ReplicatedPluginTest
