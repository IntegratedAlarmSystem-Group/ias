#!/usr/bin/bash
pytest src/integrationTest/python -rP --timeout=600 --junitxml="build/integration-test-results/pytest/TEST-Py-Ack-pytest.xml"
