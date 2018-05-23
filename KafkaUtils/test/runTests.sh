#!/usr/bin/bash
iasRun -l j org.junit.platform.console.ConsoleLauncher -c org.eso.ias.kafkautils.test.ConsumerProducerTest
iasRun -l j org.junit.platform.console.ConsoleLauncher -c org.eso.ias.kafkautils.test.FilteredConsumerTest
