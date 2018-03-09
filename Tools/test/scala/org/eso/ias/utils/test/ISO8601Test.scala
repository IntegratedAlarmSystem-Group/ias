package org.eso.ias.utils.test

import org.eso.ias.utils.ISO8601Helper
import org.scalatest.FlatSpec

import java.util.TimeZone
import java.util.Date
import java.text.SimpleDateFormat

/** 
 *  Tests the conversion of times from milliseconds to string
 *  and vice-versa
 *  
 */
class ISO8601Test extends FlatSpec {
  
  def getHour(timestamp: String): Int = {
    val parts = timestamp.split("T")(1).split(":")(0)
    parts.toInt
  }
  
  behavior of "ISO 8601 converter"
  
  it must "convert millisecibnds to timestamps and vice-versa" in {
    val now = System.currentTimeMillis()
    
    val tstamp = ISO8601Helper.getTimestamp(now)
    
    assert(ISO8601Helper.timestampToMillis(tstamp)==now)
  }
  
  it must "use UTC timezone" in {
    val tZone = TimeZone.getDefault()
    
    val offset = tZone.getOffset(System.currentTimeMillis)/3600000
    
    println("Time shift of default JVM time with UTC "+offset+"h")
    
    val localsdf = new SimpleDateFormat(ISO8601Helper.iso8601StringPattern)
    val now = System.currentTimeMillis()
    
    // If we are using UTC then the timestamp produced by ISO8601Helper
    // is equal to the one produced by localsdf
    // Otherwise there is a offset in the hours
    val utcStr = ISO8601Helper.getTimestamp(now)
    val localStr = localsdf.format(new Date(now))
    
   println("UTC date "+utcStr)
   println("Local date "+localStr)
   
   if (offset==0) {
     assert(utcStr==localStr)
   } else {
     assert(utcStr!=localStr)
     assert(getHour(utcStr)!=getHour(localStr))
   }
  }
  
}