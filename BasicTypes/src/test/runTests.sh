#!/usr/bin/bash
iasRun org.scalatest.run org.eso.ias.basictypes.test.TestInOut
iasRun org.scalatest.run org.eso.ias.basictypes.test.TestValidity
iasRun org.scalatest.run org.eso.ias.basictypes.test.TestIdentifier
iasRun org.scalatest.run org.eso.ias.basictypes.test.TestTemplatedIdentifier
iasRun org.scalatest.run org.eso.ias.basictypes.test.TestJavaConversion
iasRun org.junit.platform.console.ConsoleLauncher -c org.eso.ias.basictypes.test.IasValueJsonSerializerTest
iasRun org.junit.platform.console.ConsoleLauncher -c org.eso.ias.basictypes.test.IASValueTest
iasRun org.scalatest.run org.eso.ias.basictypes.test.TestAlarm
iasRun org.scalatest.run org.eso.ias.basictypes.test.TestOperationalMode
testIasValue
testTimestamp
