#!/usr/bin/bash
iasRun -r org.junit.platform.console.ConsoleLauncher execute --select-class org.eso.ias.command.test.TestCommandManager
iasRun -r org.junit.platform.console.ConsoleLauncher execute --select-class org.eso.ias.command.test.TestCommandSender
testCommandSender
testCommandManager
