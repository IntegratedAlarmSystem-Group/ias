#!/usr/bin/bash
iasRun -r org.scalatest.run org.eso.ias.sink.test.ValueProcessorTest
iasRun -r org.scalatest.run org.eso.ias.sink.test.AlarmStateTrackerTest
iasRun -r org.scalatest.run org.eso.ias.sink.test.NotificationSenderTest

