#!/usr/bin/bash
iasRun -r org.scalatest.tools.Runner \
	-s org.eso.ias.dasu.test.DasuWithKafkaPubSubTest \
	-u "build/integration-test-results/scalatest"
