#!/usr/bin/bash
iasRun -l s org.scalatest.run org.eso.ias.component.test.TestTransferFunctionSetting
iasRun -l s org.scalatest.run org.eso.ias.component.test.TestComponent
iasRun -l s org.scalatest.run org.eso.ias.component.test.TestTransferFunction
iasRun -l s org.scalatest.run org.eso.ias.component.test.TestMinMaxThreshold
iasRun -l s org.scalatest.run org.eso.ias.component.test.TestMultiplicityTF
