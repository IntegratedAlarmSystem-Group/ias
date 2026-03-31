#!/usr/bin/bash
iasRun -i UdpPluginTest -r org.junit.platform.console.ConsoleLauncher execute \
	--select-class org.eso.ias.plugin.network.test.UdpPluginTest \
	--reports-dir build/integration-test-results/junit
pytest src/integrationTest/python --junitxml="build/integration-test-results/pytest/TEST-PythonPluginFeeder-pytest.xml"
