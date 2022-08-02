#!/usr/bin/bash
iasRun org.scalatest.run org.eso.ias.supervisor.test.SupervisorTest
iasRun org.scalatest.run org.eso.ias.supervisor.test.SupervisorWithTemplatesTest
iasRun org.scalatest.run org.eso.ias.supervisor.test.TemplatedInputTest
iasRun org.scalatest.run org.eso.ias.supervisor.test.SupervisorWithKafkaTest
iasRun org.scalatest.run org.eso.ias.supervisor.test.TestSupervisorTfChanged
