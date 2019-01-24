package org.eso.ias.heartbeat

/**
  * The HB published in the kafka topic,
  *
  * It is composed of a type and a name (that together are the unique
  * identifier of the tool) plus the hostname where the tool runs
  *
  * The Heartbeat is immutable.
  *
  * @param hbType The type of the tool
  * @param name The name of the tool
  * @param hostName the hostname where the tool runs
  */
class Heartbeat private (val hbType: HeartbeatProducerType, val name: String, val hostName: String) {
  require(Option(hbType).isDefined)
  require(Option(name).isDefined && name.nonEmpty)
  require(!hbType.name().contains(Heartbeat.typeNameSeparator),"Invalid character "+Heartbeat.typeNameSeparator)
  require(!hbType.name().contains(Heartbeat.idHostnameSeparator),"Invalid character "+Heartbeat.idHostnameSeparator)
  require(!name.contains(Heartbeat.typeNameSeparator),"Invalid character "+Heartbeat.typeNameSeparator)
  require(!name.contains(Heartbeat.idHostnameSeparator),"Invalid character "+Heartbeat.idHostnameSeparator)

  /** The ID is composed of the type and the name */
  val id: String = name+Heartbeat.typeNameSeparator+hbType.name()

  /** The string representation of the heartbeat
    *
    * The representation is sent in the HB topic and returned by toString */
  val stringRepr: String = id+Heartbeat.idHostnameSeparator+hostName

  /**
    * Auxiliary constructor
    *
    * @param hbType The type of the tool
    * @param name The name of the tool
    * @return the heartebeat
    */
  def this(hbType: HeartbeatProducerType, name: String) = {
   this(hbType, name, System.getProperty("ias.hostname","Unknown host name"))
  }

  /**
    * Override Any.toString to provide a user readable representation
    * of this object
    *
    * @return the String representation of this object
    */
  override def toString: String = stringRepr
}

object Heartbeat {
  /** Separator between type and name */
  val typeNameSeparator = ':'

  /** The seprator between the ID and the hostname */
  val idHostnameSeparator = '@'

  /**
    * Factory method to build a Heartbeat
    *
    * @param hbType The type of the tool
    * @param name The name of the tool
    * @return the heartebeat
    */
  def apply(hbType: HeartbeatProducerType, name: String): Heartbeat = {
    new Heartbeat(hbType, name, System.getProperty("ias.hostname","Unknown host name"))
  }

  /**
    * Factory method to build a Heartbeat from hist string representation
    * @param StringRepr the string representation of the heartbeat
    * @return the heartebeat
    */
  def apply(stringRepr: String): Heartbeat = {
    require(Option(stringRepr).isDefined && stringRepr.nonEmpty)
    require(-1!=Integer2int(stringRepr.indexOf(typeNameSeparator)),"Type/name separator not found")
    require(-1!=Integer2int(stringRepr.indexOf(idHostnameSeparator)),"Id/hostname separator not found")
    require(stringRepr.indexOf(typeNameSeparator)==stringRepr.lastIndexOf(typeNameSeparator),
      "Invalid string representation: too may "+typeNameSeparator)
    require(stringRepr.indexOf(idHostnameSeparator)==stringRepr.lastIndexOf(idHostnameSeparator),
      "invalid string representation: too may "+idHostnameSeparator)

    val name = stringRepr.split(typeNameSeparator)(0)
    val host = stringRepr.split(idHostnameSeparator)(1)

    val tp = {
      val start = name.length+1
      val end = stringRepr.length-host.length-1
      require(start<end,"Malformed string representation: no type?")
      val tpString = stringRepr.substring(start,end)
      HeartbeatProducerType.valueOf(tpString.toUpperCase)
    }

    new Heartbeat(tp,name,host)
  }

}
