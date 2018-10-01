#!/usr/bin/bash
iasRun -l s org.scalatest.run org.eso.ias.asce.test.TestTransferFunctionSetting
iasRun -l s org.scalatest.run org.eso.ias.asce.test.TestComponent
iasRun -l s org.scalatest.run org.eso.ias.asce.test.TestTransferFunction
iasRun -l s org.scalatest.run org.eso.ias.asce.test.TestMinMaxThreshold
iasRun -l s org.scalatest.run org.eso.ias.asce.test.TestMultiplicityTF
