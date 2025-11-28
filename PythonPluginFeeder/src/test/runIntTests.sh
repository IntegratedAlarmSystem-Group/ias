#!/usr/bin/bash
iasRun -r org.junit.platform.console.ConsoleLauncher execute --select-class org.eso.ias.plugin.network.test.UdpPluginTest
TestJsonMsg
TestUdpPlugin
