package org.eso.ias.prototype.utils

import java.util.Date
import java.text.DateFormat
import java.text.SimpleDateFormat

/**
 * Helper methods to handle times formatted as 8601:
 * YYYY-MM-DDTHH:MM:SS.mmm
 * 
 * @author acaproni
 */
object ISO8601Helper {
  private val df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.S")
  
  def now(): String = {
    ISO8601Helper.getTimestamp(System.currentTimeMillis())
  }
  
  /**
   * @param time: The time expressed as milliseconds...
   * @return the ISO8601 time representation of the passed time
   */
  def getTimestamp(time: Long): String = {
    ISO8601Helper.getTimestamp(new Date(time))
  }
  
  /**
   * @param date: The date
   * @return the ISO8601 time representation of the passed date
   */
  def getTimestamp(date: Date): String = {
     ISO8601Helper.df.synchronized {
       df.format(date)
     }
  }
}