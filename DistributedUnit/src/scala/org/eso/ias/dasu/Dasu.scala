package org.eso.ias.dasu

/**
 * The Distributed Alarm System Unit or DASU
 * 
 * @constructor create a DASU with the give identifier
 * @param is the identifier of the DASU 
 */
class Dasu(val id: String) {
  println(id+" built")
  
  // -Get DASU configuration from CDB
  // -get ASCEs to run in the DASU
  
}

object Dasu {
  def main(args: Array[String]) {
    require(args.length==1,"DASU identifier missing in command line")
    
    val dasu = new Dasu(args(0))
  }
}