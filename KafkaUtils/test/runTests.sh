#!/usr/bin/bash
iasRun -l j org.junit.runner.JUnitCore org.eso.ias.kafkautils.test.ConsumerProducerTest
iasRun -l j org.junit.runner.JUnitCore org.eso.ias.kafkautils.test.FilteredConsumerTest
