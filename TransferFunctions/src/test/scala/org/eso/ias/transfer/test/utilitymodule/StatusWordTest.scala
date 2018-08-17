package org.eso.ias.transfer.test.utilitymodule

import org.eso.ias.tranfer.utlitymodule.StatusWord
import org.scalatest.FlatSpec

/** Test the StatusWord */
class StatusWordTest extends FlatSpec {

  val statusString00="AC-POWER:0,AT-ZENITH:0,HVAC:0,FIRE:0,UPS-POWER:0,STOW-PIN:0,RX-CAB-TEMP:0,DRIVE-CAB-TEMP:0,ANTENNA-POS:0,E-STOP:0"
  val statusString10="AC-POWER:1,AT-ZENITH:0,HVAC:1,FIRE:0,UPS-POWER:1,STOW-PIN:0,RX-CAB-TEMP:1,DRIVE-CAB-TEMP:0,ANTENNA-POS:1,E-STOP:0"
  val statusString01="AC-POWER:0,AT-ZENITH:1,HVAC:0,FIRE:1,UPS-POWER:0,STOW-PIN:1,RX-CAB-TEMP:0,DRIVE-CAB-TEMP:1,ANTENNA-POS:0,E-STOP:1"
  val statusString11="AC-POWER:1,AT-ZENITH:1,HVAC:1,FIRE:1,UPS-POWER:1,STOW-PIN:1,RX-CAB-TEMP:1,DRIVE-CAB-TEMP:1,ANTENNA-POS:1,E-STOP:1"

  behavior of "The status word"

  it must "Build from a passed string" in {
    val status00 = new StatusWord(statusString00)
    val status01 = new StatusWord(statusString01)
    val status10 = new StatusWord(statusString10)
    val status11 = new StatusWord(statusString11)
  }

  it must "return the correct status of each bit" in {
    val status00 = new StatusWord(statusString00)
    val status01 = new StatusWord(statusString01)
    val status10 = new StatusWord(statusString10)
    val status11 = new StatusWord(statusString11)

    StatusWord.monitorPointNames.foreach(name => {
      assert(!status00.statusOf(name))
      assert(status11.statusOf(name))
    })

    for (i <- 0 to 9) {
      assert(!status00.statusOf(i))
      assert(status11.statusOf(i))
    }
    assert(!status01.statusOf("AC-POWER"))
    assert(status10.statusOf("AC-POWER"))

    assert(status01.statusOf("AT-ZENITH"))
    assert(!status10.statusOf("AT-ZENITH"))

    assert(!status01.statusOf("HVAC"))
    assert(status10.statusOf("HVAC"))

    assert(status01.statusOf("FIRE"))
    assert(!status10.statusOf("FIRE"))

    assert(!status01.statusOf("UPS-POWER"))
    assert(status10.statusOf("UPS-POWER"))

    assert(status01.statusOf("STOW-PIN"))
    assert(!status10.statusOf("STOW-PIN"))

    assert(!status01.statusOf("RX-CAB-TEMP"))
    assert(status10.statusOf("RX-CAB-TEMP"))

    assert(status01.statusOf("DRIVE-CAB-TEMP"))
    assert(!status10.statusOf("DRIVE-CAB-TEMP"))

    assert(!status01.statusOf("ANTENNA-POS"))
    assert(status10.statusOf("ANTENNA-POS"))

    assert(status01.statusOf("E-STOP"))
    assert(!status10.statusOf("E-STOP"))
  }
}
