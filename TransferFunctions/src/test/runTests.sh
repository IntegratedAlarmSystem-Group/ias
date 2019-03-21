#!/usr/bin/bash
iasRun -l s org.scalatest.run org.eso.ias.transfer.test.DelayedAlarmTest
iasRun -l s org.scalatest.run org.eso.ias.transfer.test.BackupSelectorTest
iasRun -l s org.scalatest.run org.eso.ias.transfer.test.ThresholdBackupAndDelayTest
iasRun -l s org.scalatest.run org.eso.ias.transfer.test.utilitymodule.StatusWordTest
iasRun -l s org.scalatest.run org.eso.ias.transfer.test.AntPadInhibitorTest
iasRun -l s  org.scalatest.run  org.eso.ias.transfer.test.utilitymodule.FireAlarmTest
iasRun -l s  org.scalatest.run  org.eso.ias.transfer.test.utilitymodule.AlarmByBitTest
iasRun -l s  org.scalatest.run  org.eso.ias.transfer.test.VisualInspectionTest
iasRun -l s org.scalatest.run org.eso.ias.transfer.test.RelocationSelectorTest
iasRun -l s org.scalatest.run org.eso.ias.transfer.test.BoolToAlarmTest

