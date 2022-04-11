#!/usr/bin/bash
iasRun org.junit.platform.console.ConsoleLauncher -c org.eso.ias.command.test.TestCmdReplySerialization
iasRun org.junit.platform.console.ConsoleLauncher -c org.eso.ias.command.test.TestCommandManager
iasRun org.junit.platform.console.ConsoleLauncher -c org.eso.ias.command.test.TestCommandSender
testCmdReply

