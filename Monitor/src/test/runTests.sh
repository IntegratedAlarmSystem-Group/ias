#!/usr/bin/bash
iasRun org.scalatest.run org.eso.ias.monitor.test.ConfigTest
iasRun org.scalatest.run org.eso.ias.monitor.test.MonitorAlarmTest
iasRun org.scalatest.run org.eso.ias.monitor.test.HbAlarmGenerationTest
iasRun org.scalatest.run org.eso.ias.monitor.test.HbAlarmPublisherTest

