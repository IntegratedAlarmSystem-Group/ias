#!/usr/bin/bash
set -euo pipefail

iasRun -o -r org.scalatest.tools.Runner \
    -s org.eso.ias.pytfserver.test.TestGrpcPythonExecutorIntegration \
    -s org.eso.ias.pytfserver.test.TestPyTF \
    -s org.eso.ias.pytfserver.test.TestMinMaxPyTF \
    -u "build/integration-test-results/scalatest"

mkdir -p build/integration-test-results/pytest
pytest src/integrationTest/python -rP --timeout=600 --junitxml="build/integration-test-results/pytest/TEST-PyTfServer-pytest.xml"
