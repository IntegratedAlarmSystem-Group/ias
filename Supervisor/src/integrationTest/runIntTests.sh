#!/usr/bin/bash
iasRun -r org.scalatest.tools.Runner \
    -s org.eso.ias.supervisor.test.SupervisorWithKafkaTest \
    -s org.eso.ias.supervisor.test.TestSupervisorTfChanged \
    -s org.eso.ias.supervisor.test.TestAck \
    -u "build/integration-test-results/scalatest"
pytest src/integrationTest/python --junitxml="build/integration-test-results/pytest/TEST-Supervisor-pytest.xml"
