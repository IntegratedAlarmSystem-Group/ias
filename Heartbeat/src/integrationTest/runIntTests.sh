#!/usr/bin/bash
set -euo pipefail

iasRun -r org.scalatest.tools.Runner \
	-s org.eso.ias.heartbeat.test.TestKafkaPublisher \
	-s org.eso.ias.heartbeat.test.TestHbsCollector \
	-u "build/integration-test-results/scalatest"
pytest src/integrationTest/python -rP --timeout=600 --junitxml="build/integration-test-results/pytest/TEST-Heartbeat-pytest.xml"
