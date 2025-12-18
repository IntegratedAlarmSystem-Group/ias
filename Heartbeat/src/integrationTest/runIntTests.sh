#!/usr/bin/bash
iasRun -r org.scalatest.tools.Runner \
	-s org.eso.ias.heartbeat.test.TestKafkaPublisher \
	-s org.eso.ias.heartbeat.test.TestHbsCollector \
	-u "build/integration-test-results/scalatest"
pytest src/integrationTest/python --junitxml="build/integration-test-results/pytest/TEST-Heartbeat-pytest.xml"
