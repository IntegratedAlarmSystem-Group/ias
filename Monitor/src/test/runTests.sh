#!/usr/bin/bash
iasRun -l s org.scalatest.run org.eso.ias.monitor.test.ConfigTest
iasRun -l s org.scalatest.run org.eso.ias.monitor.test.MonitorAlarmTest
iasRun -l s org.scalatest.run org.eso.ias.monitor.test.HbAlarmGenerationTest

