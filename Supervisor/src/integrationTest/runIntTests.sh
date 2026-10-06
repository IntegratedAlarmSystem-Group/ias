#!/usr/bin/bash
set -euo pipefail

iasRun -o -r org.scalatest.tools.Runner \
    -s org.eso.ias.supervisor.inttest.SupervisorWithBooleanTFs \
    -s org.eso.ias.supervisor.inttest.SupervisorWithKafkaTest \
    -s org.eso.ias.supervisor.inttest.TestSupervisorTfChanged \
    -s org.eso.ias.supervisor.inttest.TestAck \
        -u "build/integration-test-results/scalatest"

pytest src/integrationTest/python -rP --timeout=600 --junitxml="build/integration-test-results/pytest/TEST-Supervisor-pytest.xml"
