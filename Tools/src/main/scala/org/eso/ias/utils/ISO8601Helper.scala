package org.eso.ias.utils

import java.util.Date
import java.util.TimeZone
import java.text.DateFormat
import java.text.SimpleDateFormat

/**
 * Helper methods to handle times formatted as 8601:
 * YYYY-MM-DDTHH:MM:SS.mmm
 * 
 * @author acaproni
 */
object ISO8601Helper {
  
  /** Pattern for ISO 8601 */
  val iso8601StringPattern =  "yyyy-MM-dd'T'HH:mm:ss.S"
  
  
  private val df = {
    val sdf = new SimpleDateFormat(iso8601StringPattern)
    sdf.setTimeZone(TimeZone.getTimeZone("UTC"))
    sdf
  }
  
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
  
  /**
   * Parse the passed string into a Date
   */
  def timestampToDate(timestamp: String): Date = {
    ISO8601Helper.df.synchronized { 
      df.parse(timestamp)
    }
  }
  
  /**
   * Parse the passed string into a Date
   */
  def timestampToMillis(timestamp: String): Long = {
    timestampToDate(timestamp).getTime()
  }
}