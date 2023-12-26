#!/usr/bin/bash
iasRun -r org.junit.platform.console.ConsoleLauncher -c org.eso.ias.plugin.network.test.UdpPluginTest
TestJsonMsg
TestUdpPlugin
