#!/usr/bin/bash
iasRun -r org.junit.platform.console.ConsoleLauncher -c org.eso.ias.command.test.TestCmdReplySerialization
iasRun -r org.junit.platform.console.ConsoleLauncher -c org.eso.ias.command.test.TestCommandManager
iasRun -r org.junit.platform.console.ConsoleLauncher -c org.eso.ias.command.test.TestCommandSender
testCmdReply

