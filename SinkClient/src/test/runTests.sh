#!/usr/bin/bash
iasRun -l s org.scalatest.run org.eso.ias.sink.test.ValueProcessorTest
iasRun -l s org.scalatest.run org.eso.ias.sink.test.AlarmStateTrackerTest
iasRun -l s org.scalatest.run org.eso.ias.sink.test.NotificationSenderTest

