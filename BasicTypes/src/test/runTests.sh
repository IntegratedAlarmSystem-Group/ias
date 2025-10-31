#!/usr/bin/bash
iasRun -r org.scalatest.run org.eso.ias.basictypes.test.TestInOut
iasRun -r org.scalatest.run org.eso.ias.basictypes.test.TestValidity
iasRun -r org.scalatest.run org.eso.ias.basictypes.test.TestIdentifier
iasRun -r org.scalatest.run org.eso.ias.basictypes.test.TestTemplatedIdentifier
iasRun -r org.scalatest.run org.eso.ias.basictypes.test.TestJavaConversion
iasRun -r org.junit.platform.console.ConsoleLauncher execute --select-class org.eso.ias.basictypes.test.IasValueJsonSerializerTest
iasRun -r org.junit.platform.console.ConsoleLauncher execute --select-class org.eso.ias.basictypes.test.IASValueTest
iasRun -r org.scalatest.run org.eso.ias.basictypes.test.TestAlarm
iasRun -r org.scalatest.run org.eso.ias.basictypes.test.TestOperationalMode
testIasValue
testTimestamp
testAlarm

