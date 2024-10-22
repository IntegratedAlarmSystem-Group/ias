#!/usr/bin/bash
iasRun -r org.scalatest.run org.eso.ias.monitor.test.ConfigTest
iasRun -r org.scalatest.run org.eso.ias.monitor.test.MonitorAlarmTest
iasRun -r org.scalatest.run org.eso.ias.monitor.test.HbAlarmPublisherTest
