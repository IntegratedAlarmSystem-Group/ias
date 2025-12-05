#!/usr/bin/bash
iasRun -r org.scalatest.run org.eso.ias.supervisor.test.SupervisorWithKafkaTest
iasRun -r org.scalatest.run org.eso.ias.supervisor.test.TestSupervisorTfChanged
iasRun -r org.scalatest.run org.eso.ias.supervisor.test.TestAck
testPyAck
