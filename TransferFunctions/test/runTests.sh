#!/usr/bin/bash
iasRun -l s org.scalatest.run org.eso.ias.transfer.test.DelayedAlarmTest
iasRun -l s org.scalatest.run org.eso.ias.transfer.test.BackupSelectorTest
iasRun -l s org.scalatest.run org.eso.ias.transfer.test.ThresholdBackupAndDelayTest
iasRun -l s org.scalatest.run org.eso.ias.transfer.test.utilitymodule.StatusWordTest