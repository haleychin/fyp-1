package utils

import java.text.SimpleDateFormat
import java.util.Date
import java.sql.Date

import models.{CourseworkAPI, ExamAPI}

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

  def calculatePass(weightage: Double, totalWeightage: Double): String = {
    val rate = weightage / totalWeightage * 100
    if (rate <= 40) {
      "Fail"
    } else {
      "Pass"
    }
  }

  def combineExamAndCoursework(courseworks: CourseworkAPI, exam: ExamAPI): CourseworkAPI = {
    val examDetails = exam.examDetails
    courseworks.courseworks += (("Exam", exam.weightage))
    courseworks.total += exam.weightage
    courseworks.courseworkDetails.foreach { case (id, c) =>
      val examDetail = examDetails.get(id)

      if (examDetail.isDefined) {
        val mark = examDetail.get.exam._2
        c.courseworks += ("Exam" -> mark)
        c.total += mark
        c.status = calculatePass(c.total, courseworks.total)
      }
    }

    val marks = courseworks.courseworkDetails.values.map(_.total).toSeq
    courseworks.descStat = Stats.computeDescriptiveStatistic(marks)

    courseworks
  }
}
