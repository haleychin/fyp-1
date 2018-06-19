package utils

import java.text.SimpleDateFormat
import java.util.Date
import java.sql.Date

import models.{AttendanceAPI,CourseworkAPI,CCourseworkAPI,CExamAPI,ExamAPI,Status,Insight,Student}

import scala.collection.mutable.LinkedHashMap

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
    if (rate < 40) {
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

  def intepretFailCount(failCount: Int): (Int,String) = {
    if (failCount > 0) { (1, s"Failed $failCount subject(s)") }
    else { (0,"") }
  }

  def combineInsight(attendance: AttendanceAPI, coursework: CourseworkAPI, students: Seq[Student]): AttendanceAPI = {
    students.foreach { s =>
      val failCountTuple = intepretFailCount(s.failCount)
      val k = s.id
      val optionA = attendance.studentDetails.get(k)
      val optionC = coursework.courseworkDetails.get(k)

      if (optionA.isDefined && optionC.isDefined) {
        val c = optionC.get
        val a = optionA.get
        val dangerLevel = c.insight.dangerLevel + a.insight.dangerLevel + failCountTuple._1
        var reasons = (c.insight.reasons ++ a.insight.reasons).toArray
        if (failCountTuple._2 != "") {
          reasons = reasons :+ failCountTuple._2
        }
        a.insight = Insight(dangerLevel, reasons)
      }
    }


    attendance
  }

  def combineCourseworkAndExamTotal(courseworks: CCourseworkAPI, exam: CExamAPI): CCourseworkAPI = {
    val examDetails = exam.examDetails
    val gradeFrequency = LinkedHashMap[String,Int]()

    courseworks.courseworkDetails.foreach { case (id, c) =>
      val optionExamDetail = examDetails.get(id)

      if (optionExamDetail.isDefined) {
        val examDetail = optionExamDetail.get
        val weightage = examDetail.exam._2
        c.courseworks += ("Exam" -> weightage)
        c.courseworksTotal += ("Exam" -> examDetail.fullWeightage)
        c.totalMark += weightage
        c.fullMark += examDetail.fullWeightage

        val cwPercent = calculatePercent(c.totalMark, c.fullMark)
        val examPercent = calculatePercent(weightage, examDetail.fullWeightage)

        c.status = calculateGrade(
          cwPercent,
          examPercent,
          c.totalMark)
        c.grade  = calculateStatus(
          cwPercent,
          examPercent,
          c.totalMark)

        if (gradeFrequency.contains(c.grade.name)) {
          gradeFrequency.update(c.grade.name, gradeFrequency.get(c.grade.name).get + 1)
        } else {
          gradeFrequency += (c.grade.name -> 1)
        }
      }
    }


    courseworks.statistic.failCount = gradeFrequency.get("F*").getOrElse(0) + gradeFrequency.get("F").getOrElse(0)

    val marks = courseworks.courseworkDetails.values.map(_.totalMark).toSeq
    courseworks.descStat = Stats.computeDescriptiveStatistic(marks)
    courseworks.statistic.gradeFrequency = gradeFrequency

    courseworks
  }

  def combineExamAndCoursework(courseworks: CourseworkAPI, exam: ExamAPI): CourseworkAPI = {
    val examDetails = exam.examDetails
    courseworks.total += exam.weightage
    var cwTotal = 0.0
    courseworks.courseworks.foreach { case (k, v) =>
      cwTotal += v
    }

    val gradeFrequency = LinkedHashMap[String,Int]()

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

        if (gradeFrequency.contains(c.grade.name)) {
          gradeFrequency.update(c.grade.name, gradeFrequency.get(c.grade.name).get + 1)
        } else {
          gradeFrequency += (c.grade.name -> 1)
        }
      }

      courseworks.courseworks += (("Exam", exam.weightage))
    }

    val marks = courseworks.courseworkDetails.values.map(_.total).toSeq
    courseworks.descStat = Stats.computeDescriptiveStatistic(marks)
    courseworks.statistic.gradeFrequency = gradeFrequency

    courseworks
  }
}
