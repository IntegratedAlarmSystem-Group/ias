#!/usr/bin/bash
iasRun -l j org.junit.platform.console.ConsoleLauncher -c org.eso.ias.command.test.TestCmdReplySerialization
iasRun -l j org.junit.platform.console.ConsoleLauncher -c org.eso.ias.command.test.TestCommandManager

