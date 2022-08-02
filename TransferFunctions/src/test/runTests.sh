#!/usr/bin/bash
iasRun org.scalatest.run org.eso.ias.transfer.test.DelayedAlarmTest
iasRun org.scalatest.run org.eso.ias.transfer.test.BackupSelectorTest
iasRun org.scalatest.run org.eso.ias.transfer.test.ThresholdBackupAndDelayTest
iasRun org.scalatest.run org.eso.ias.transfer.test.utilitymodule.StatusWordTest
iasRun org.scalatest.run org.eso.ias.transfer.test.AntPadInhibitorTest
iasRun org.scalatest.run  org.eso.ias.transfer.test.utilitymodule.FireAlarmTest
iasRun org.scalatest.run  org.eso.ias.transfer.test.utilitymodule.AlarmByBitTest
iasRun org.scalatest.run  org.eso.ias.transfer.test.VisualInspectionTest
iasRun org.scalatest.run org.eso.ias.transfer.test.RelocationSelectorTest
iasRun org.scalatest.run org.eso.ias.transfer.test.BoolToAlarmTest
iasRun org.scalatest.run org.eso.ias.transfer.test.RegExpToAlarmTest

