#!/usr/bin/bash
iasRun -r org.junit.platform.console.ConsoleLauncher execute \
	--select-class org.eso.ias.kafkautils.test.ConsumerProducerTest \
	--select-class org.eso.ias.kafkautils.test.KafkaIasiosConsumerTest \
	--select-class org.eso.ias.kafkautils.test.SlowIasiosProcessorTest \
	--reports-dir build/integration-test-results/junit
iasRun -r org.scalatest.tools.Runner \
	-s org.eso.ias.kafkaneo.test.consumer.ListenerTest \
	-s org.eso.ias.kafkaneo.test.consumer.ConsumerHelperTest \
	-s org.eso.ias.kafkaneo.test.consumer.ConsumerTest \
	-u "build/integration-test-results/scalatest"
pytest src/integrationTest/python --junitxml="build/integration-test-results/pytest/TEST-KafkaUtils-pytest.xml"
