#!/usr/bin/bash
iasRun org.junit.platform.console.ConsoleLauncher -c org.eso.ias.plugin.network.test.UdpPluginTest
TestJsonMsg
TestUdpPlugin
