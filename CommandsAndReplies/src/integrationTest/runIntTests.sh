#!/usr/bin/bash
iasRun -r org.junit.platform.console.ConsoleLauncher execute \
	--select-class org.eso.ias.command.test.TestCommandSender \
	--reports-dir build/integration-test-results/junit
pytest src/integrationTest/python --junitxml="build/integration-test-results/pytest/TEST-CommandsAndReplies-pytest.xml"
