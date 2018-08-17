#!/usr/bin/bash
iasRun -l s org.scalatest.run org.eso.ias.basictypes.test.TestInOut
iasRun -l s org.scalatest.run org.eso.ias.basictypes.test.TestValidity
iasRun -l s org.scalatest.run org.eso.ias.basictypes.test.TestIdentifier
iasRun -l s org.scalatest.run org.eso.ias.basictypes.test.TestTemplatedIdentifier
iasRun -l s org.scalatest.run org.eso.ias.basictypes.test.TestJavaConversion
iasRun -l j org.junit.platform.console.ConsoleLauncher -c org.eso.ias.basictypes.test.IasValueJsonSerializerTest
iasRun -l j org.junit.platform.console.ConsoleLauncher -c org.eso.ias.basictypes.test.IASValueTest
iasRun -l s org.scalatest.run org.eso.ias.basictypes.test.TestAlarm
testIasValue
testTimestamp
