package utils

import scala.collection.mutable.ArrayBuffer
import models.{Insight,AStat,CourseworkDetailsAPI}

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

  def analyseCoursework(data: CourseworkDetailsAPI): Insight = {
    var dangerLevel = 0
    var reasons = ArrayBuffer[String]()

    data.courseworks.foreach { case (k, v) =>
      val rate = Utils.calculatePercent(v, data.courseworksTotal.get(k).get)

      if (rate < 40) {
        dangerLevel += 1
        reasons += s"$k score under 40%."
      }
    }

    Insight(dangerLevel, reasons)
  }
}
