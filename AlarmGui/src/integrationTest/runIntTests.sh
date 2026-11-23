#!/usr/bin/bash
set -euo pipefail

mkdir -p build/integration-test-results/pytest
pytest src/integrationTest/python -rP --timeout=600 --junitxml="build/integration-test-results/pytest/TEST-AlarmGui-pytest.xml"
