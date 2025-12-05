#!/usr/bin/bash
iasRun -r org.junit.platform.console.ConsoleLauncher execute --select-class org.eso.ias.kafkautils.test.ConsumerProducerTest
iasRun -r org.junit.platform.console.ConsoleLauncher execute --select-class org.eso.ias.kafkautils.test.KafkaIasiosConsumerTest
iasRun -r org.junit.platform.console.ConsoleLauncher execute --select-class org.eso.ias.kafkautils.test.SlowIasiosProcessorTest
iasRun -r org.scalatest.run org.eso.ias.kafkaneo.test.consumer.ListenerTest
iasRun -r org.scalatest.run org.eso.ias.kafkaneo.test.consumer.ConsumerHelperTest
iasRun -r org.scalatest.run org.eso.ias.kafkaneo.test.consumer.ConsumerTest
testValueProdCons
testKafkaHelper
