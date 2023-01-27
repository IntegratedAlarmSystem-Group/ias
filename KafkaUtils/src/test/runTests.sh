#!/usr/bin/bash
iasRun -r org.junit.platform.console.ConsoleLauncher -c org.eso.ias.kafkautils.test.ConsumerProducerTest
iasRun -r org.junit.platform.console.ConsoleLauncher -c org.eso.ias.kafkautils.test.KafkaIasiosConsumerTest
iasRun -r org.junit.platform.console.ConsoleLauncher -c org.eso.ias.kafkautils.test.SlowIasiosProcessorTest
testValueProdCons
