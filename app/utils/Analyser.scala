package utils

import scala.collection.mutable.{ArrayBuffer,LinkedHashMap}
import models.{Insight,AStat,CourseworkDetailsAPI,Metric,Question}

case class MetricStat(
  var total: Double,
  var maxMark: Double,
  var average: Double,
  var percentage: Double,
  var frequency: Int)

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

  def analyseExam(data: Seq[(Question, Metric)]): LinkedHashMap[String, MetricStat] = {
    val map = LinkedHashMap[String, MetricStat]()
    data.foreach { case (question, metric) =>
      if (map.contains(metric.name)) {
        val stat = map.get(metric.name).get
        stat.total += question.mark
        stat.maxMark += question.totalMark
        stat.frequency += 1
        println("============")
        println("Mark: " + question.mark)
        println("Total: " + question.totalMark)
        println("Stat Total: " + stat.total)
        println("Stat Max: " + stat.maxMark)
      } else {
        val stat = MetricStat(
          question.mark,
          question.totalMark,
          0,
          0,
          1,
        )
        map += (metric.name -> stat)
      }

    }

    map.foreach { case (_, value) =>
      value.average = value.total / value.frequency
      value.percentage = value.total / value.maxMark * 100
    }

    map
  }
}
