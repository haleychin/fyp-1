package utils

import java.text.SimpleDateFormat
import java.util.Date
import java.sql.Date

import models.{CourseworkAPI, ExamAPI, Status}

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

  def calculatePercent(weightage: Double, totalWeightage: Double): Double = {
    weightage / totalWeightage * 100
  }
  def calculatePass(weightage: Double, totalWeightage: Double): String = {
    val rate = calculatePercent(weightage, totalWeightage)
    if (rate <= 40) {
      "Fail"
    } else {
      "Pass"
    }
  }

  def calculateStatus(cw: Double, exam: Double, total: Double): Status = {
    var reason = ""
    var grade  = "F"
    if (cw < 40) {
      grade = "F*"
      reason = "Fail coursework"
    }
    else if (exam < 40)   {
      grade = "F*"
      if (reason != "") { reason += " & " }
      reason += "Fail exam"
    }
    else if (total >= 70) { grade = "A" }
    else if (total >= 60) { grade = "B" }
    else if (total >= 50) { grade = "C" }
    else if (total >= 40) { grade = "D" }
    else                  { grade = "F" }

    Status(grade, reason)
  }

  def calculateGrade(cw: Double, exam: Double, total: Double): String = {
    if (cw < 40 || exam < 40) { "F*" }
    else if (total >= 70) { "A" }
    else if (total >= 60) { "B" }
    else if (total >= 50) { "C" }
    else if (total >= 40) { "D" }
    else { "F" }
  }

  def combineExamAndCoursework(courseworks: CourseworkAPI, exam: ExamAPI): CourseworkAPI = {
    val examDetails = exam.examDetails
    courseworks.total += exam.weightage
    var cwTotal = 0.0
    courseworks.courseworks.foreach { case (k, v) =>
      cwTotal += v
    }

    courseworks.courseworkDetails.foreach { case (id, c) =>
      val examDetail = examDetails.get(id)
      val cwMark = c.courseworks.map(_._2).sum

      if (examDetail.isDefined) {
        val mark = examDetail.get.exam._2
        c.courseworks += ("Exam" -> mark)
        c.total += mark
        val cwPercent = calculatePercent(cwMark, cwTotal)
        val examPercent = calculatePercent(mark, exam.weightage)

        c.status = calculateGrade(
          cwPercent,
          examPercent,
          c.total)
        c.grade  = calculateStatus(
          cwPercent,
          examPercent,
          c.total)
      }

      courseworks.courseworks += (("Exam", exam.weightage))
    }

    val marks = courseworks.courseworkDetails.values.map(_.total).toSeq
    courseworks.descStat = Stats.computeDescriptiveStatistic(marks)

    courseworks
  }
}
