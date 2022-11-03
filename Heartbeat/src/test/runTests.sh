#!/usr/bin/bash
iasRun org.scalatest.run org.eso.ias.heartbeat.test.TestJsonSerialization
iasRun org.scalatest.run org.eso.ias.heartbeat.test.TestHeartbeat
iasRun org.scalatest.run org.eso.ias.heartbeat.test.TestEngine
iasRun org.scalatest.run org.eso.ias.heartbeat.test.TestKafkaPublisher
iasRun org.scalatest.run org.eso.ias.heartbeat.test.TestHbsCollector
