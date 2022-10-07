#!/usr/bin/bash
iasRun -l s org.scalatest.run org.eso.ias.heartbeat.test.TestJsonSerialization
iasRun -l s org.scalatest.run org.eso.ias.heartbeat.test.TestHeartbeat
iasRun -l s org.scalatest.run org.eso.ias.heartbeat.test.TestEngine
iasRun -l s org.scalatest.run org.eso.ias.heartbeat.test.TestKafkaPublisher
iasRun -l s org.scalatest.run org.eso.ias.heartbeat.test.TestHbsCollector
