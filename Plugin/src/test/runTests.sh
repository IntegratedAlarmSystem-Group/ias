#!/usr/bin/bash
iasRun -r org.junit.platform.console.ConsoleLauncher execute --select-class org.eso.ias.plugin.test.config.JsonConfigReaderTest
iasRun -r org.junit.platform.console.ConsoleLauncher execute --select-class org.eso.ias.plugin.test.filter.TestFilterBase
iasRun -r org.junit.platform.console.ConsoleLauncher execute --select-class org.eso.ias.plugin.test.filter.NoneFilterTest
iasRun -r org.junit.platform.console.ConsoleLauncher execute --select-class org.eso.ias.plugin.test.filter.AvgBySamplesFilterTest
iasRun -r org.junit.platform.console.ConsoleLauncher execute --select-class org.eso.ias.plugin.test.filter.AvgByTimeFilterTest
iasRun -r org.junit.platform.console.ConsoleLauncher execute --select-class org.eso.ias.plugin.test.filter.TestFilterFactory
iasRun -r org.junit.platform.console.ConsoleLauncher execute --select-class org.eso.ias.plugin.test.publisher.PublisherBaseTest
iasRun -r org.junit.platform.console.ConsoleLauncher execute --select-class org.eso.ias.plugin.test.publisher.BufferedPublisherBaseTest
iasRun -Dorg.eso.ias.plugin.buffersize=10 -r org.junit.platform.console.ConsoleLauncher execute --select-class org.eso.ias.plugin.test.publisher.PublisherMaxBufferSizeTest
iasRun -r org.junit.platform.console.ConsoleLauncher execute --select-class org.eso.ias.plugin.test.publisher.BufferedPublisherStressTest
iasRun -r org.junit.platform.console.ConsoleLauncher execute --select-class org.eso.ias.plugin.test.publisher.PublisherStressTest
iasRun -r org.junit.platform.console.ConsoleLauncher execute --select-class org.eso.ias.plugin.test.stats.DetailedStatsTest
iasRun -r org.junit.platform.console.ConsoleLauncher execute --select-class org.eso.ias.plugin.test.publisher.BufferedMonitoredSystemDataTest
iasRun -r org.junit.platform.console.ConsoleLauncher execute --select-class org.eso.ias.plugin.test.publisher.JsonPusblisherTest
iasRun -r org.junit.platform.console.ConsoleLauncher execute --select-class org.eso.ias.plugin.test.MonitoredValueTest
iasRun -r org.junit.platform.console.ConsoleLauncher execute --select-class org.eso.ias.plugin.test.JsonConversionTest
iasRun -r org.junit.platform.console.ConsoleLauncher execute --select-class org.eso.ias.plugin.test.ReplicatedPluginTest
