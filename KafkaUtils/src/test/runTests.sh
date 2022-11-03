#!/usr/bin/bash
iasRun org.junit.platform.console.ConsoleLauncher -c org.eso.ias.kafkautils.test.ConsumerProducerTest
iasRun org.junit.platform.console.ConsoleLauncher -c org.eso.ias.kafkautils.test.KafkaIasiosConsumerTest
iasRun org.junit.platform.console.ConsoleLauncher -c org.eso.ias.kafkautils.test.SlowIasiosProcessorTest
testValueProdCons
