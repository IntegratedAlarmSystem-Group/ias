#!/usr/bin/bash
set -euo pipefail

iasRun -r org.junit.platform.console.ConsoleLauncher execute \
	--select-class org.eso.ias.command.test.TestCommandManager \
	--select-class org.eso.ias.command.test.TestCommandSender \
	--reports-dir build/integration-test-results/junit
pytest src/integrationTest/python -rP --timeout=600 --junitxml="build/integration-test-results/pytest/TEST-CommandsAndReplies-pytest.xml"
