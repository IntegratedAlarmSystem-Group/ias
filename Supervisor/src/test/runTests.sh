#!/usr/bin/bash
iasRun -r org.scalatest.run org.eso.ias.supervisor.test.SupervisorTest
iasRun -r org.scalatest.run org.eso.ias.supervisor.test.SupervisorWithTemplatesTest
iasRun -r org.scalatest.run org.eso.ias.supervisor.test.TemplatedInputTest
iasRun -r org.scalatest.run org.eso.ias.supervisor.test.SupervisorWithKafkaTest
iasRun -r org.scalatest.run org.eso.ias.supervisor.test.TestSupervisorTfChanged
iasRun -r org.scalatest.run org.eso.ias.supervisor.test.TestAck
