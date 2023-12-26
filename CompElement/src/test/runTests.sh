#!/usr/bin/bash
iasRun -r org.scalatest.run org.eso.ias.asce.test.TestTransferFunctionSetting
iasRun -r org.scalatest.run org.eso.ias.asce.test.TestComponent
iasRun -r org.scalatest.run org.eso.ias.asce.test.TestTransferFunction
iasRun -r org.scalatest.run org.eso.ias.asce.test.TestMinMaxThreshold
iasRun -r org.scalatest.run org.eso.ias.asce.test.TestMultiplicityTF
iasRun -r org.scalatest.run org.eso.ias.asce.test.TestSlowTF
iasRun -r org.scalatest.run org.eso.ias.asce.test.TestPyTF
iasRun -r org.scalatest.run org.eso.ias.asce.test.TestMinMaxPyTF
iasRun -r org.scalatest.run org.eso.ias.asce.test.TestAlarmAck

