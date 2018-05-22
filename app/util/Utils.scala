package utils

import java.text.SimpleDateFormat
import java.util.Date
import java.sql.Date

object Utils {
  val DATE_FORMAT = "dd/MM/yyyy"

  def getDateAsString(d: java.util.Date): String = {
    val dateFormat = new SimpleDateFormat(DATE_FORMAT)
    dateFormat.format(d)
  }

  def convertStringToDate(s: String): java.sql.Date = {
    val dateFormat = new SimpleDateFormat(DATE_FORMAT)
    val date = dateFormat.parse(s)
    new java.sql.Date(date.getTime());
  }
}
