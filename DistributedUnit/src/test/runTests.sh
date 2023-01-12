#!/usr/bin/bash
iasRun org.scalatest.run org.eso.ias.dasu.test.DasuOneASCETest
iasRun org.scalatest.run org.eso.ias.dasu.test.CheckDasuOutputTimestamps
iasRun org.scalatest.run org.eso.ias.dasu.test.Dasu7ASCEsTest
iasRun org.scalatest.run org.eso.ias.dasu.test.JsonPublisherTest
iasRun org.scalatest.run org.eso.ias.dasu.test.DasuWithKafkaPubSubTest
iasRun org.scalatest.run org.eso.ias.dasu.test.AckTest
