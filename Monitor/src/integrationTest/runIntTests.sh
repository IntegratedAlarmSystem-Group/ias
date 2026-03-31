#!/usr/bin/bash
iasRun -r org.scalatest.tools.Runner \
	-s org.eso.ias.monitor.test.HbAlarmGenerationTest \
	-u "build/integration-test-results/scalatest"
