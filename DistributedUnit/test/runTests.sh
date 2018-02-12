#!/usr/bin/bash
iasRun -l s org.scalatest.run org.eso.ias.dasu.test.TopologyTest
iasRun -l s org.scalatest.run org.eso.ias.dasu.test.DasuOneASCETest
iasRun -l s org.scalatest.run org.eso.ias.dasu.test.CheckDasuOutputTimestamps
iasRun -l s org.scalatest.run org.eso.ias.dasu.test.Dasu7ASCEsTest
iasRun -l s org.scalatest.run org.eso.ias.dasu.test.JsonPublisherTest
iasRun -l s org.scalatest.run org.eso.ias.dasu.test.DasuWithKafkaPubSubTest
