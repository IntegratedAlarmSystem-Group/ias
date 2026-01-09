#!/usr/bin/bash
iasRun -r org.junit.platform.console.ConsoleLauncher execute \
	--select-class org.eso.ias.plugin.test.publisher.KafkaPublisherTest \
	--reports-dir build/integration-test-results/junit
