#!/usr/bin/bash
iasRun.py -l s org.scalatest.run org.eso.ias.component.test.TestTransferFunctionSetting
iasRun.py -l s org.scalatest.run org.eso.ias.component.test.TestComponent
iasRun.py -l s org.scalatest.run org.eso.ias.component.test.TestTransferFunction
iasRun.py -l s org.scalatest.run org.eso.ias.component.test.TestMinMaxThreshold

