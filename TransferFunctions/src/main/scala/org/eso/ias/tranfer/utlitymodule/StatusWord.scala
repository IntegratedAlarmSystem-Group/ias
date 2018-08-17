package org.eso.ias.tranfer.utlitymodule

/**
  * StatusWord helps dealing with the status word sent by the utility module plugin.
  *
  * The plung sends status worrd like
  * AC-POWER:0,AT-ZENITH:0,HVAC:0,FIRE:0,UPS-POWER:1,STOW-PIN:0,RX-CAB-TEMP:0,DRIVE-CAB-TEMP:0,ANTENNA-POS:0,E-STOP:0
  *
  * The name of the antenna is in the ID of the monitor point that is something like UMStatus-DV05
  */
class StatusWord(umStatusWord: String) {
  require(Option(umStatusWord).isDefined && !umStatusWord.isEmpty,"Invalid empty status word")

  /**
    * The stats reported by the utility module.
    *
    * The key is the name of the monitor point
    * The value is a boolean (True means ON)
    */
  val status: Map[String,Boolean] = {
    val parts = umStatusWord.split(",")
    require(parts.length==10,"Invalid format of status word "+umStatusWord)
    parts.foldLeft(Map.empty[String, Boolean])( (z, name) => {
      val mPointParts=name.split(":")
      require(mPointParts.length==2,"Invalid format "+name)
      val mPointName: String= mPointParts(0).trim
      require(StatusWord.monitorPointNames.contains(mPointName),"unkone status bit name "+mPointName)
      val mPointValue: String = mPointParts(1).trim
      require(mPointValue=="0" || mPointValue=="1","Unknown value of monitor point "+mPointValue)
      z+(mPointName -> (mPointValue=="1"))
    })
  }

  /**
    * Get the status of the bit with the passed number
    *
    * @param bit the number of the bit to query the status
    * @return the status of the bit
    */
  def statusOf(bit: Int): Boolean = {
    require(bit>=0 && bit<=9,"Bit "+bit+" out of range [0,9]")
    status(StatusWord.bitsToNames(bit))
  }

  /**
    * Get the status of the bit with the passed name
    *
    * @param mPointName the name of the bit to query the status
    * @return the status of the bit
    */
  def statusOf(mPointName: String): Boolean = {
    require(Option(mPointName).isDefined && StatusWord.monitorPointNames.contains(mPointName),"Unknown/invalid name "+mPointName)
    status(mPointName)
  }

}

object StatusWord {

  /**
    * Associates the names of the monitor points to the status bits
    */
  val namesToBits: Map[String, Int] = Map(
    "AC-POWER" -> 2,
    "AT-ZENITH" -> 7,
    "HVAC" -> 6,
    "FIRE" -> 0,
    "UPS-POWER" -> 3,
    "STOW-PIN" -> 4,
    "RX-CAB-TEMP" -> 5,
    "DRIVE-CAB-TEMP" -> 8,
    "ANTENNA-POS" -> 9,
    "E-STOP" -> 1)

  /**
    * The name of the monitor point in the status string sent
    * by the utility module plugin
    *
    */
  val monitorPointNames:List[String] = namesToBits.keys.toList

  /** Associate the bits to their name */
  val bitsToNames: Map[Int, String] = monitorPointNames.foldLeft(Map.empty[Int, String])((z, name) => z+(namesToBits(name) -> name))

}
