#!/usr/bin/bash
iasRun -l s org.scalatest.run org.eso.ias.heartbeat.test.JsonSerialization
iasRun -l s org.scalatest.run org.eso.ias.heartbeat.test.TestEngine
iasRun -l s org.scalatest.run org.eso.ias.heartbeat.test.TestKafkaPublisher
