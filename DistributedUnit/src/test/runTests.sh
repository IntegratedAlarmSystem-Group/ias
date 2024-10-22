#!/usr/bin/bash
iasRun -r org.scalatest.run org.eso.ias.dasu.test.DasuOneASCETest
iasRun -r org.scalatest.run org.eso.ias.dasu.test.CheckDasuOutputTimestamps
iasRun -r org.scalatest.run org.eso.ias.dasu.test.Dasu7ASCEsTest
iasRun -r org.scalatest.run org.eso.ias.dasu.test.JsonPublisherTest
iasRun -r org.scalatest.run org.eso.ias.dasu.test.AckTest
