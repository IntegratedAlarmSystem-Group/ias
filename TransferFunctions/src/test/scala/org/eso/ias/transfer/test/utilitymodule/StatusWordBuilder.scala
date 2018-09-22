package org.eso.ias.transfer.test.utilitymodule

import org.eso.ias.tranfer.utlitymodule.StatusWord

/** Build the status word with the desired bits set
  *
  * @param bitsSet the positions of the bits to set (first bit is in position 0)
  */
class StatusWordBuilder(bitsSet: List[Int]) {
  require(Option(bitsSet).isDefined)
  require(
    bitsSet.forall(bitPos => bitPos>=0 && bitPos<StatusWord.monitorPointNames.size),
    "Invalid position of bits to set "+bitsSet.mkString(","))

  // The string representing the status word with the bits set
  val statusWordString: String = {
    val strings =for {i <- 0 until StatusWord.monitorPointNames.size
      bitVal = if (bitsSet.contains(i)) "1" else "0"
      bitStr = StatusWord.bitsToNames(i)+":"+bitVal
    } yield bitStr
    strings.mkString(",")
  }

  /** The StatusWord decoding  the string */
  val statusWord = new StatusWord(statusWordString)

}
