#!/usr/bin/bash
iasRun -l j org.junit.platform.console.ConsoleLauncher -c org.eso.ias.plugin.network.test.UdpPluginTest
TestJsonMsg
TestUdpPlugin
