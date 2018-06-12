package utils

import scala.collection.mutable.{ArrayBuffer,LinkedHashMap}
import models.{Insight,MetricStat,QuestionStat,AStat,CourseworkDetailsAPI,Metric,Question}

object Analyser {

  def analyseAttendance(stat: AStat): Insight = { var dangerLevel = 0
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

        if (stat.questions.contains(question.name)) {
          val qStat = stat.questions.get(question.name).get
          println(question.name)
          qStat.total += question.mark
          qStat.maxMark += question.totalMark
          qStat.frequency += 1
        } else {
          val qStat = QuestionStat(
            question.mark,
            question.totalMark
          )
          stat.questions += (question.name -> qStat)
        }
      } else {
        val qStat = QuestionStat(
          question.mark,
          question.totalMark
        )
        val questionMap = LinkedHashMap(question.name -> qStat)
        val stat = MetricStat(
          question.mark,
          question.totalMark,
          questionMap,
          metric.description
        )
        map += (metric.name -> stat)
      }

    }

    map.foreach { case (_, value) =>
      value.average = value.total / value.frequency
      value.percentage = value.total / value.maxMark * 100

      value.questions.foreach { case (_, value) =>
        value.average = value.total / value.frequency
        value.percentage = value.total / value.maxMark * 100
      }
    }

    map
  }
}
