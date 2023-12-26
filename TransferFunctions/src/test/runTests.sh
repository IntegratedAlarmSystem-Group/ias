#!/usr/bin/bash
iasRun -r org.scalatest.run org.eso.ias.transfer.test.DelayedAlarmTest
iasRun -r org.scalatest.run org.eso.ias.transfer.test.BackupSelectorTest
iasRun -r org.scalatest.run org.eso.ias.transfer.test.ThresholdBackupAndDelayTest
iasRun -r org.scalatest.run org.eso.ias.transfer.test.utilitymodule.StatusWordTest
iasRun -r org.scalatest.run org.eso.ias.transfer.test.AntPadInhibitorTest
iasRun -r org.scalatest.run  org.eso.ias.transfer.test.utilitymodule.FireAlarmTest
iasRun -r org.scalatest.run  org.eso.ias.transfer.test.utilitymodule.AlarmByBitTest
iasRun -r org.scalatest.run  org.eso.ias.transfer.test.VisualInspectionTest
iasRun -r org.scalatest.run org.eso.ias.transfer.test.RelocationSelectorTest
iasRun -r org.scalatest.run org.eso.ias.transfer.test.BoolToAlarmTest
iasRun -r org.scalatest.run org.eso.ias.transfer.test.RegExpToAlarmTest

