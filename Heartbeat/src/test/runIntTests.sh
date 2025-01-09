#!/usr/bin/bash
iasRun -r org.scalatest.run org.eso.ias.heartbeat.test.TestKafkaPublisher
iasRun -r org.scalatest.run org.eso.ias.heartbeat.test.TestHbsCollector
hbKafkaConsumerTest
