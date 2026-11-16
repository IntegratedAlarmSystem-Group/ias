#!/usr/bin/bash
set -euo pipefail

iasRun -o -r org.scalatest.tools.Runner \
    -s org.eso.ias.pytfserver.test.TestGrpcPythonExecutorIntegration \
    -s org.eso.ias.pytfserver.test.TestPyTF \
    -s org.eso.ias.pytfserver.test.TestMinMaxPyTF \
    -u "build/integration-test-results/scalatest"
