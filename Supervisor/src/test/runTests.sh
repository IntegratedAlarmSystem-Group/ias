#!/usr/bin/bash
iasRun -l s org.scalatest.run org.eso.ias.supervisor.test.SupervisorTest
iasRun -l s org.scalatest.run org.eso.ias.supervisor.test.SupervisorWithTemplatesTest
iasRun -l s org.scalatest.run org.eso.ias.supervisor.test.TemplatedInputTest
iasRun -l s org.scalatest.run org.eso.ias.supervisor.test.SupervisorWithKafkaTest
iasRun -l s org.scalatest.run org.eso.ias.supervisor.test.SupervisorCmdExecutorTest
