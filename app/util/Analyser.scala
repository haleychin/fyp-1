package util

import scala.collection.mutable.ArrayBuffer
import models.{Insight,AStat}

object Analyser {

  def analyseAttendance(stat: AStat): Insight = {
    var dangerLevel = 0
    var reasons = ArrayBuffer[String]()

    if (stat.attendanceRate < 80) {
      dangerLevel += 1
      reasons += "Attendance Rate is lower than 80%."
    }

    stat.consecutiveMissed.filter(_.length > 3).foreach { array =>
      dangerLevel += 1
      reasons += s"Missed class ${array.length} times consecutively."
    }

    Insight(dangerLevel, reasons)
  }
}
