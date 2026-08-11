#!/usr/bin/bash
set -euo pipefail

iasRun -r org.scalatest.tools.Runner \
	-s org.eso.ias.dasu.test.DasuWithKafkaPubSubTest \
	-u "build/integration-test-results/scalatest"
