package org.eso.ias.dasu.test

import org.scalatest.FlatSpec
import org.scalatest.BeforeAndAfter
import org.eso.ias.dasu.DasuImpl

/**
 * Checks the timestamps of the output produced by a DASU
 * when inputs change and when the auto-refresh is in place.
 * 
 * The test uses the DasuWithOneASCE DASU defined in the CDB
 * by submitting inputs and checking the fields of output
 * published (or not published by the DASU.
 * 
 * @see  [[https://github.com/IntegratedAlarmSystem-Group/ias/issues/52 Issue #52 on github]]
 */
class CheckDasuOutputTimestamps extends FlatSpec with BeforeAndAfter {
  
  val f = new DasuOneAsceCommon(1000)
  
  before {
    f.outputValuesReceived.clear()
    f.outputValuesReceived.clear()
    f.dasu = f.buildDasu()
    f.dasu.get.start()
  }
  
  after {
    f.dasu.get.cleanUp()
    f.dasu = None
    f.outputValuesReceived.clear()
    f.outputValuesReceived.clear()
  }
}