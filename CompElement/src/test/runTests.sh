#!/usr/bin/bash
iasRun -l s org.scalatest.run org.eso.ias.asce.test.TestTransferFunctionSetting
iasRun -l s org.scalatest.run org.eso.ias.asce.test.TestComponent
iasRun -l s org.scalatest.run org.eso.ias.asce.test.TestTransferFunction
iasRun -l s org.scalatest.run org.eso.ias.asce.test.TestMinMaxThreshold
iasRun -l s org.scalatest.run org.eso.ias.asce.test.TestMultiplicityTF
iasRun -l s org.scalatest.run org.eso.ias.asce.test.TestSlowTF
iasRun -l s org.scalatest.run org.eso.ias.asce.test.TestPyTF
iasRun -l s org.scalatest.run org.eso.ias.asce.test.TestMinMaxPyTF

