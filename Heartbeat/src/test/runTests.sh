#!/usr/bin/bash
iasRun -r org.scalatest.run org.eso.ias.heartbeat.test.TestJsonSerialization
iasRun -r org.scalatest.run org.eso.ias.heartbeat.test.TestHeartbeat
iasRun -r org.scalatest.run org.eso.ias.heartbeat.test.TestEngine
hbStatusTest
hbProdTypeTest
heartbeatTest
hbMessageTest
