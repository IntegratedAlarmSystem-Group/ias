#!/usr/bin/bash
iasRun -r org.junit.platform.console.ConsoleLauncher execute \
	--select-class org.eso.ias.converter.test.TestKafkaStreaming \
	--reports-dir build/integration-test-results/junit
