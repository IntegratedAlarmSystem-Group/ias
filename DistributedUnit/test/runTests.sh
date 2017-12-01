#!/usr/bin/bash
iasRun.py -l s org.scalatest.run org.eso.ias.dasu.test.TopologyTest
iasRun.py -l s org.scalatest.run org.eso.ias.dasu.test.DasuOneASCETest
iasRun.py -l s org.scalatest.run org.eso.ias.dasu.test.Dasu7ASCEsTest
iasRun.py -l s org.scalatest.run org.eso.ias.dasu.test.JsonPublisherTest
iasRun.py -l s org.scalatest.run org.eso.ias.dasu.test.DasuWithKafkaPubSubTest
