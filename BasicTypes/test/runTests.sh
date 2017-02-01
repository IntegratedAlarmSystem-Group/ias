#!/usr/bin/bash
iasRun.py -l s org.scalatest.run org.eso.ias.basictypes.test.TestAlarmValue
iasRun.py -l s org.scalatest.run org.eso.ias.basictypes.test.TestHeteroIO
iasRun.py -l s org.scalatest.run org.eso.ias.basictypes.test.TestValidity
iasRun.py -l s org.scalatest.run org.eso.ias.basictypes.test.TestIdentifier
iasRun.py -l s org.scalatest.run org.eso.ias.basictypes.test.TestJavaConversion
iasRun.py -l s org.scalatest.run org.eso.ias.basictypes.test.TestHIOEquality